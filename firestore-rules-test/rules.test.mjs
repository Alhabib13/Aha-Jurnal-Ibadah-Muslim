// Uji Firestore Security Rules di emulator.
// Jalankan: npm test   (dari folder firestore-rules-test)
import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";

const ALICE = "aliceUid123";
const BOB = "bobUid456";
const TGL = "2026-10-09";

let env;
const db = (uid) => env.authenticatedContext(uid).firestore();
const anon = () => env.unauthenticatedContext().firestore();

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-aha",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});
after(() => env.cleanup());
beforeEach(() => env.clearFirestore());

const profil = (o = {}) => ({
  jenis_kelamin: "perempuan",
  rentang_umur: "15-18",
  status_aktivitas: "pelajar",
  tanggal_mulai: serverTimestamp(),
  ...o,
});

const log = (o = {}) => ({
  user_id: ALICE,
  tanggal: TGL,
  jenis_ibadah: "sholat_wajib",
  nama_detail: "subuh",
  nama_detail_asli: "Sholat Subuh",
  status: "dikerjakan",
  jumlah: null,
  jilid: null,
  jam_pencatatan: Timestamp.now(),
  sumber_pencatatan: "buka_aplikasi",
  ...o,
});

// Cermin ResearchSchema.logId: jenis multi-baris & sunnah_lainnya butuh id lokal.
const logId = (o = {}, uid = ALICE, idLokal = "sesi-1") => {
  const d = log(o);
  const butuhIdLokal =
    ["tilawah", "qadha_sholat"].includes(d.jenis_ibadah) || d.nama_detail === "sunnah_lainnya";
  return [uid, d.tanggal, d.jenis_ibadah, d.nama_detail, butuhIdLokal ? idLokal : null]
    .filter((x) => x != null)
    .join("_");
};

const tulisLog = (o = {}, { as = ALICE, id } = {}) =>
  setDoc(doc(db(as), "logs", id ?? logId(o)), log(o));

const tanpa = (key) => {
  const d = log();
  delete d[key];
  return d;
};

async function seed(path, data) {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), path), data));
}

describe("users/{uid}: profil penelitian", () => {
  test("pemilik membuat profil lengkap", () =>
    assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), profil())));

  test("umur & aktivitas boleh null", () =>
    assertSucceeds(
      setDoc(doc(db(ALICE), "users", ALICE), profil({ rentang_umur: null, status_aktivitas: null }))
    ));

  test("jenis_kelamin wajib", () => {
    const p = profil();
    delete p.jenis_kelamin;
    return assertFails(setDoc(doc(db(ALICE), "users", ALICE), p));
  });

  test("jenis_kelamin tidak boleh null", () =>
    assertFails(setDoc(doc(db(ALICE), "users", ALICE), profil({ jenis_kelamin: null }))));

  test("jenis_kelamin di luar enum ditolak", () =>
    assertFails(setDoc(doc(db(ALICE), "users", ALICE), profil({ jenis_kelamin: "MALE" }))));

  test("rentang_umur di luar enum ditolak", () =>
    assertFails(setDoc(doc(db(ALICE), "users", ALICE), profil({ rentang_umur: "17" }))));

  test("status_aktivitas di luar enum ditolak", () =>
    assertFails(setDoc(doc(db(ALICE), "users", ALICE), profil({ status_aktivitas: "guru" }))));

  test("kunci opsional tetap wajib ada (boleh bernilai null)", () => {
    const p = profil();
    delete p.rentang_umur;
    return assertFails(setDoc(doc(db(ALICE), "users", ALICE), p));
  });

  test("field tambahan (mis. email) ditolak", () =>
    assertFails(setDoc(doc(db(ALICE), "users", ALICE), profil({ email: "a@b.c" }))));

  test("tanggal_mulai harus waktu server, bukan jam HP", () =>
    assertFails(setDoc(doc(db(ALICE), "users", ALICE), profil({ tanggal_mulai: Timestamp.now() }))));

  test("pengguna lain tidak bisa membuat/membaca profil orang lain", async () => {
    await assertFails(setDoc(doc(db(BOB), "users", ALICE), profil()));
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: Timestamp.now() }));
    await assertFails(getDoc(doc(db(BOB), "users", ALICE)));
    await assertFails(getDoc(doc(anon(), "users", ALICE)));
    await assertSucceeds(getDoc(doc(db(ALICE), "users", ALICE)));
  });

  test("update umur boleh, tanggal_mulai tidak boleh berubah", async () => {
    const mulai = Timestamp.fromDate(new Date("2026-10-01T00:00:00Z"));
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: mulai }));
    await assertSucceeds(
      setDoc(doc(db(ALICE), "users", ALICE), profil({ tanggal_mulai: mulai, rentang_umur: "19-25" }))
    );
    await assertFails(updateDoc(doc(db(ALICE), "users", ALICE), { tanggal_mulai: Timestamp.now() }));
  });

  test("hapus hanya oleh pemilik", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: Timestamp.now() }));
    await assertFails(deleteDoc(doc(db(BOB), "users", ALICE)));
    await assertSucceeds(deleteDoc(doc(db(ALICE), "users", ALICE)));
  });
});

describe("logs: struktur & kepemilikan", () => {
  test("log sholat wajib dikerjakan yang valid", () => assertSucceeds(tulisLog()));

  test("tanpa login ditolak", () =>
    assertFails(setDoc(doc(anon(), "logs", logId()), log())));

  test("user_id milik orang lain ditolak", () =>
    assertFails(tulisLog({ user_id: BOB }, { id: logId({}, BOB) })));

  test("ID dokumen harus berawalan uid_tanggal_ pemilik", async () => {
    await assertFails(tulisLog({}, { id: `${BOB}_${TGL}_sholat_wajib_subuh` }));
    await assertFails(tulisLog({}, { id: `${ALICE}_2026-10-08_sholat_wajib_subuh` }));
    await assertFails(tulisLog({}, { id: "acak123" }));
  });

  test("slot sunnah buatan pengguna boleh punya akhiran id lokal", () =>
    assertSucceeds(
      tulisLog(
        { jenis_ibadah: "sholat_sunnah", nama_detail: "sunnah_lainnya", nama_detail_asli: "Sholat Hajat" },
        { id: `${ALICE}_${TGL}_sholat_sunnah_sunnah_lainnya_6f1c2a9e-1b2c-4d5e-8f90-123456789abc` }
      )
    ));

  for (const key of ["jam_pencatatan", "sumber_pencatatan", "status", "jumlah", "nama_detail_asli", "jilid"]) {
    test(`kunci ${key} wajib ada`, () =>
      assertFails(setDoc(doc(db(ALICE), "logs", logId()), tanpa(key))));
  }

  test("data profil tidak boleh disalin ke log", () =>
    assertFails(tulisLog({ jenis_kelamin: "perempuan" })));

  test("selisih dari adzan tidak boleh disimpan", () =>
    assertFails(tulisLog({ selisih_menit: 12 })));

  test("format tanggal salah ditolak", async () => {
    await assertFails(tulisLog({ tanggal: "09-10-2026" }));
    await assertFails(tulisLog({ tanggal: "2026-13-01" }));
  });

  test("jenis_ibadah di luar enum ditolak", () =>
    assertFails(tulisLog({ jenis_ibadah: "sedekah", nama_detail: null })));

  test("nama_detail harus cocok dengan jenis_ibadah", async () => {
    await assertFails(tulisLog({ nama_detail: "pagi" }));
    await assertFails(tulisLog({ nama_detail: "jumat" }));
    await assertFails(tulisLog({ nama_detail: null }));
    await assertFails(tulisLog({ jenis_ibadah: "dzikir", nama_detail: "subuh" }));
  });

  test("nama_detail_asli lebih dari 100 karakter ditolak", () =>
    assertFails(tulisLog({ nama_detail_asli: "x".repeat(101) })));
});

describe("logs: status tiga nilai", () => {
  test("dikerjakan wajib punya jam_pencatatan bertipe timestamp", async () => {
    await assertFails(tulisLog({ jam_pencatatan: null }));
    await assertFails(tulisLog({ jam_pencatatan: Date.now() }));
  });

  test("dikerjakan wajib punya sumber_pencatatan yang sah", async () => {
    await assertSucceeds(tulisLog({ sumber_pencatatan: "dari_notifikasi" }));
    await assertFails(tulisLog({ sumber_pencatatan: null }));
    await assertFails(tulisLog({ sumber_pencatatan: "widget" }));
  });

  test("tidak: jam & sumber wajib null", async () => {
    await assertSucceeds(tulisLog({ status: "tidak", jam_pencatatan: null, sumber_pencatatan: null }));
    await assertFails(tulisLog({ status: "tidak", sumber_pencatatan: null }));
    await assertFails(tulisLog({ status: "tidak", jam_pencatatan: null }));
  });

  test("tidak hanya untuk sholat wajib dan dzikir", async () => {
    const t = { status: "tidak", jam_pencatatan: null, sumber_pencatatan: null };
    await assertSucceeds(tulisLog({ ...t, jenis_ibadah: "dzikir", nama_detail: "petang" }));
    await assertFails(tulisLog({ ...t, jenis_ibadah: "puasa", nama_detail: "senin_kamis" }));
    await assertFails(tulisLog({ ...t, jenis_ibadah: "sholat_sunnah", nama_detail: "dhuha" }));
    await assertFails(tulisLog({ ...t, jenis_ibadah: "tilawah", nama_detail: null, jumlah: 1 }));
  });

  test("udzur: hanya sholat wajib & puasa, jam & sumber null", async () => {
    const u = { status: "udzur", jam_pencatatan: null, sumber_pencatatan: null };
    await assertSucceeds(tulisLog(u));
    await assertSucceeds(tulisLog({ ...u, jenis_ibadah: "puasa", nama_detail: "ramadan" }));
    await assertFails(tulisLog({ ...u, jenis_ibadah: "dzikir", nama_detail: "pagi" }));
    await assertFails(tulisLog({ status: "udzur" }));
  });

  test("status di luar enum ditolak (tidak boleh digabung)", () =>
    assertFails(tulisLog({ status: "tidak_udzur", jam_pencatatan: null, sumber_pencatatan: null })));
});

describe("logs: jumlah & jilid", () => {
  const tilawah = { jenis_ibadah: "tilawah", nama_detail: null, nama_detail_asli: null };
  const iqro = { jenis_ibadah: "iqro", nama_detail: null, nama_detail_asli: null };

  test("tilawah: jumlah halaman bulat >= 1", async () => {
    await assertSucceeds(tulisLog({ ...tilawah, jumlah: 5 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: 0 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: 2.5 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: null }));
    await assertFails(tulisLog({ ...tilawah, jumlah: "5" }));
  });

  test("tilawah: jilid & nama_detail harus null", async () => {
    await assertFails(tulisLog({ ...tilawah, jumlah: 5, jilid: 1 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: 5, nama_detail: "pagi" }));
  });

  test("iqro: jilid dan halaman sebagai dua field terpisah", async () => {
    await assertSucceeds(tulisLog({ ...iqro, jilid: 3, jumlah: 12 }));
    await assertFails(tulisLog({ ...iqro, jilid: null, jumlah: 12 }));
    await assertFails(tulisLog({ ...iqro, jilid: 3.12, jumlah: 12 }));
  });

  test("iqro: jilid 1-10 (Iqro, Qiroati, Tilawati, UMMI, An-Nahdliyah)", async () => {
    await assertSucceeds(tulisLog({ ...iqro, jilid: 1, jumlah: 1 }));
    await assertSucceeds(tulisLog({ ...iqro, jilid: 7, jumlah: 12 }));
    await assertSucceeds(tulisLog({ ...iqro, jilid: 10, jumlah: 100 }));
    await assertFails(tulisLog({ ...iqro, jilid: 0, jumlah: 12 }));
    await assertFails(tulisLog({ ...iqro, jilid: 11, jumlah: 12 }));
    await assertFails(tulisLog({ ...iqro, jilid: 3, jumlah: 101 }));
  });

  test("ibadah lain: jumlah & jilid harus null", async () => {
    await assertFails(tulisLog({ jumlah: 1 }));
    await assertFails(tulisLog({ jilid: 1 }));
  });
});

describe("logs: bentuk ID per jenis", () => {
  const tilawah = { jenis_ibadah: "tilawah", nama_detail: null, nama_detail_asli: null };
  const qadha = { jenis_ibadah: "qadha_sholat", nama_detail: null, nama_detail_asli: "Qadha" };

  test("tilawah: beberapa sesi per hari, masing-masing satu baris", async () => {
    await assertSucceeds(tulisLog({ ...tilawah, jumlah: 3 }, { id: logId(tilawah, ALICE, "pagi-a1") }));
    await assertSucceeds(tulisLog({ ...tilawah, jumlah: 2 }, { id: logId(tilawah, ALICE, "malam-b2") }));
    const snap = await getDocs(
      query(collection(db(ALICE), "logs"), where("user_id", "==", ALICE), where("jenis_ibadah", "==", "tilawah"))
    );
    if (snap.size !== 2) throw new Error(`harusnya 2 sesi, dapat ${snap.size}`);
  });

  test("tilawah: wajib punya id sesi", () =>
    assertFails(tulisLog({ ...tilawah, jumlah: 3 }, { id: `${ALICE}_${TGL}_tilawah` })));

  test("slot tetap tidak boleh punya baris kedua (akhiran ditolak)", async () => {
    await assertFails(tulisLog({}, { id: `${ALICE}_${TGL}_sholat_wajib_subuh_x1` }));
    await assertFails(tulisLog({}, { id: `${ALICE}_${TGL}_sholat_wajib_dzuhur` }));
    await assertFails(
      tulisLog({ jenis_ibadah: "dzikir", nama_detail: "pagi" }, { id: `${ALICE}_${TGL}_dzikir_pagi_2` })
    );
  });

  test("iqro satu baris per hari tanpa akhiran", async () => {
    const iqro = { jenis_ibadah: "iqro", nama_detail: null, jilid: 2, jumlah: 5 };
    await assertSucceeds(tulisLog(iqro, { id: `${ALICE}_${TGL}_iqro` }));
    await assertFails(tulisLog(iqro, { id: `${ALICE}_${TGL}_iqro_s1` }));
  });

  test("sunnah_lainnya wajib punya id lokal", () =>
    assertFails(
      tulisLog(
        { jenis_ibadah: "sholat_sunnah", nama_detail: "sunnah_lainnya" },
        { id: `${ALICE}_${TGL}_sholat_sunnah_sunnah_lainnya` }
      )
    ));

  test("qadha_sholat: jenis sendiri, detail null, id lokal wajib", async () => {
    await assertSucceeds(tulisLog(qadha));
    await assertFails(tulisLog(qadha, { id: `${ALICE}_${TGL}_qadha_sholat` }));
    await assertFails(tulisLog({ ...qadha, nama_detail: "subuh" }));
  });

  test("qadha tidak lagi diterima sebagai sholat sunnah atau sholat wajib", async () => {
    await assertFails(tulisLog({ jenis_ibadah: "sholat_sunnah", nama_detail: "qadha" }));
    await assertFails(tulisLog({ jenis_ibadah: "sholat_wajib", nama_detail: "qadha" }));
  });

  test("qadha_sholat hanya berstatus dikerjakan", async () => {
    const nol = { jam_pencatatan: null, sumber_pencatatan: null };
    await assertFails(tulisLog({ ...qadha, ...nol, status: "tidak" }));
    await assertFails(tulisLog({ ...qadha, ...nol, status: "udzur" }));
  });

  test("puasa pengganti Ramadan tetap puasa/qadha", () =>
    assertSucceeds(tulisLog({ jenis_ibadah: "puasa", nama_detail: "qadha", nama_detail_asli: "Pengganti Ramadan" })));
});

describe("logs: baca, ubah, hapus", () => {
  beforeEach(async () => {
    await seed(`logs/${logId()}`, log());
    await seed(`logs/${logId({}, BOB)}`, log({ user_id: BOB }));
  });

  test("pemilik membaca lognya sendiri", () =>
    assertSucceeds(getDoc(doc(db(ALICE), "logs", logId()))));

  test("pengguna lain tidak bisa membaca", () =>
    assertFails(getDoc(doc(db(BOB), "logs", logId()))));

  test("query wajib difilter user_id sendiri", async () => {
    const col = collection(db(ALICE), "logs");
    await assertSucceeds(getDocs(query(col, where("user_id", "==", ALICE))));
    await assertFails(getDocs(col));
    await assertFails(getDocs(query(col, where("user_id", "==", BOB))));
  });

  test("ubah status tidak -> dikerjakan pada slot yang sama", async () => {
    await seed(`logs/${logId()}`, log({ status: "tidak", jam_pencatatan: null, sumber_pencatatan: null }));
    await assertSucceeds(tulisLog());
  });

  test("slot (jenis/nama_detail/tanggal) tidak boleh diubah", async () => {
    await assertFails(updateDoc(doc(db(ALICE), "logs", logId()), { nama_detail: "dzuhur" }));
    await assertFails(updateDoc(doc(db(ALICE), "logs", logId()), { tanggal: "2026-10-10" }));
  });

  test("pengguna lain tidak bisa menimpa log", () =>
    assertFails(setDoc(doc(db(BOB), "logs", logId()), log({ user_id: BOB }))));

  test("hapus hanya oleh pemilik", async () => {
    await assertFails(deleteDoc(doc(db(BOB), "logs", logId())));
    await assertSucceeds(deleteDoc(doc(db(ALICE), "logs", logId())));
  });
});

describe("regresi: path lama v1.1.1 tetap berfungsi", () => {
  test("habit_completions", () =>
    assertSucceeds(
      setDoc(doc(db(ALICE), `users/${ALICE}/habit_completions/${TGL}_default_1`), {
        habitKey: "default_1",
        dateKey: TGL,
        category: "Sholat Fardhu",
        source: "DEFAULT",
        createdAt: Date.now(),
      })
    ));

  test("sunnah_habits", () =>
    assertSucceeds(
      setDoc(doc(db(ALICE), `users/${ALICE}/sunnah_habits/abc`), {
        name: "Dhuha",
        category: "SHOLAT",
        frequencyLabel: "Setiap hari",
        reminderEnabled: false,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      })
    ));

  test("meta/profile", () =>
    assertSucceeds(
      setDoc(doc(db(ALICE), `users/${ALICE}/meta/profile`), {
        genderProfile: "FEMALE",
        isHaidhMode: false,
        haidhDates: [],
      })
    ));

  test("subkoleksi lama orang lain tetap tertutup", () =>
    assertFails(getDoc(doc(db(BOB), `users/${ALICE}/meta/profile`))));

  test("path tak dikenal tetap ditolak", () =>
    assertFails(setDoc(doc(db(ALICE), "sembarang/x"), { a: 1 })));
});
