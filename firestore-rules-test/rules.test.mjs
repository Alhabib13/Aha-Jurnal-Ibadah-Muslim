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
// Tamu yang masuk anonim (Firebase Anonymous Auth).
const dbAnonim = (uid) => env.authenticatedContext(uid, { firebase: { sign_in_provider: "anonymous" } }).firestore();
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
  pilihan_awal: ["dhuha", "dzikir_pagi"],
  disetujui_pada: serverTimestamp(),
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
  jumlah_ayat: null,
  juz: null,
  offset_waktu: "+07:00",
  selisih_menit_dari_adzan: null,
  mode_pencatatan: null,
  waktu_dzikir: null,
  target_hitungan: null,
  durasi_detik: null,
  ...o,
});

// Dzikir mode centang yang sah (dzikir pagi di luar aplikasi).
const DZ = {
  jenis_ibadah: "dzikir", nama_detail: "dzikir_pagi", nama_detail_asli: null,
  mode_pencatatan: "centang", waktu_dzikir: "pagi", target_hitungan: null,
};

// Cermin ResearchSchema.logId: jenis multi-baris & sunnah_lainnya butuh id lokal;
// dzikir: {idHabit}_{sesiMs 13 digit} (satu baris per sesi).
const SESI = 1791600000000;
const logId = (o = {}, uid = ALICE, idLokal = "sesi-1") => {
  const d = log(o);
  if (d.jenis_ibadah === "dzikir") return [uid, d.tanggal, "dzikir", d.nama_detail, idLokal, SESI].join("_");
  const butuhIdLokal = ["tilawah", "qadha_sholat"].includes(d.jenis_ibadah) || d.nama_detail === "sunnah_lainnya";
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
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: mulai, disetujui_pada: mulai }));
    await assertSucceeds(
      setDoc(doc(db(ALICE), "users", ALICE), profil({ tanggal_mulai: mulai, disetujui_pada: mulai, rentang_umur: "19-25" }))
    );
    await assertFails(updateDoc(doc(db(ALICE), "users", ALICE), { tanggal_mulai: Timestamp.now() }));
  });

  test("pilihan_awal: null / [] / nilai katalog sah, selain itu ditolak", async () => {
    const ref = doc(db(ALICE), "users", ALICE);
    await assertSucceeds(setDoc(ref, profil({ pilihan_awal: null })));
    await env.clearFirestore();
    await assertSucceeds(setDoc(ref, profil({ pilihan_awal: [] })));
    await env.clearFirestore();
    await assertFails(setDoc(ref, profil({ pilihan_awal: ["dhuha", "main_game"] })));
    await assertFails(setDoc(ref, profil({ pilihan_awal: "dhuha" })));
    const p = profil();
    delete p.pilihan_awal;
    await assertFails(setDoc(ref, p));
  });

  test("cara aplikasi: buat dengan serverTimestamp, lalu merge tanpa tanggal_mulai", async () => {
    await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), profil()));
    const ubah = { jenis_kelamin: "perempuan", rentang_umur: "26-35", status_aktivitas: "pekerja",
                   pilihan_awal: ["tahajud"] };
    await assertSucceeds(setDoc(doc(db(ALICE), "users", ALICE), ubah, { merge: true }));
  });

  test("hapus hanya oleh pemilik", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: Timestamp.now() }));
    await assertFails(deleteDoc(doc(db(BOB), "users", ALICE)));
    await assertSucceeds(deleteDoc(doc(db(ALICE), "users", ALICE)));
  });
});

describe("users/{uid}: hanya untuk peserta penelitian + manufaktur", () => {
  const ref = (who = ALICE, uid = ALICE) => doc(db(who), "users", uid);
  const LAMA = Timestamp.fromDate(new Date("2026-10-01T00:00:00Z"));

  test("dokumen tanpa profil penelitian ditolak (kosong, token, atau manufaktur saja)", async () => {
    await assertFails(setDoc(ref(), {}));
    await assertFails(setDoc(ref(), { fcm_token: "t", fcm_token_diperbarui: serverTimestamp() }, { merge: true }));
    await assertFails(setDoc(ref(), { manufaktur: "Xiaomi" }, { merge: true }));
  });

  test("token FCM tidak boleh lagi di users/{uid}", async () => {
    await assertFails(setDoc(ref(), { ...profil(), fcm_token: "t", fcm_token_diperbarui: serverTimestamp() }));
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA }));
    await assertFails(setDoc(ref(), { fcm_token: "t", fcm_token_diperbarui: serverTimestamp() }, { merge: true }));
  });

  test("manufaktur bersama profil: saat dibuat dan saat merge", async () => {
    await assertSucceeds(setDoc(ref(), { ...profil(), manufaktur: "Xiaomi" }));
    await assertSucceeds(setDoc(ref(), { manufaktur: "OPPO" }, { merge: true }));
  });

  test("profil lama tanpa manufaktur tetap sah", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA }));
    await assertSucceeds(setDoc(ref(), { rentang_umur: "19-25" }, { merge: true }));
  });

  test("bentuk manufaktur: string 1-64", async () => {
    await assertFails(setDoc(ref(), { ...profil(), manufaktur: "" }));
    await assertFails(setDoc(ref(), { ...profil(), manufaktur: "x".repeat(65) }));
    await assertFails(setDoc(ref(), { ...profil(), manufaktur: 7 }));
  });

  test("profil tidak bisa dicabut lewat update (hanya hapus dokumen)", async () => {
    await seed(`users/${ALICE}`, { ...profil({ tanggal_mulai: LAMA }), manufaktur: "Xiaomi" });
    await assertFails(setDoc(ref(), { manufaktur: "Xiaomi" }));
    await assertSucceeds(deleteDoc(ref()));
  });

  test("orang lain tidak bisa menulis manufaktur", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA }));
    await assertFails(setDoc(ref(BOB), { manufaktur: "Xiaomi" }, { merge: true }));
  });
});

describe("users/{uid}/meta/notifikasi: token FCM", () => {
  const ref = (who = ALICE, uid = ALICE) => doc(db(who), `users/${uid}/meta/notifikasi`);
  const T = (t = "tok-abc") => ({ fcm_token: t, fcm_token_diperbarui: serverTimestamp() });
  const LAMA = Timestamp.fromDate(new Date("2026-10-01T00:00:00Z"));

  test("cara aplikasi: tulis token tanpa dokumen users/{uid} (menolak penelitian)", async () => {
    await assertSucceeds(setDoc(ref(), T()));
    let ada = null;
    await env.withSecurityRulesDisabled(async (ctx) => {
      ada = (await getDoc(doc(ctx.firestore(), "users", ALICE))).exists();
    });
    if (ada !== false) throw new Error("users/{uid} ikut terbuat");
  });

  test("ganti token = tulis ulang dengan cap waktu server", async () => {
    await seed(`users/${ALICE}/meta/notifikasi`, { fcm_token: "lama", fcm_token_diperbarui: LAMA });
    await assertFails(updateDoc(ref(), { fcm_token: "baru" }));
    await assertSucceeds(setDoc(ref(), T("baru")));
  });

  test("fcm_token_diperbarui harus waktu server, keduanya wajib", async () => {
    await assertFails(setDoc(ref(), { fcm_token: "t", fcm_token_diperbarui: Timestamp.now() }));
    await assertFails(setDoc(ref(), { fcm_token: "t" }));
    await assertFails(setDoc(ref(), { fcm_token_diperbarui: serverTimestamp() }));
  });

  test("bentuk token: string 1-4096", async () => {
    await assertFails(setDoc(ref(), T("")));
    await assertFails(setDoc(ref(), { fcm_token: 123, fcm_token_diperbarui: serverTimestamp() }));
    await assertFails(setDoc(ref(), T("x".repeat(4097))));
    await assertSucceeds(setDoc(ref(), T("x".repeat(4096))));
  });

  test("field lain ditolak (mis. manufaktur, lokasi)", async () => {
    await assertFails(setDoc(ref(), { ...T(), manufaktur: "Xiaomi" }));
    await assertFails(setDoc(ref(), { ...T(), lokasi: "jakarta" }));
  });

  test("logout: pemilik menghapus dokumen token", async () => {
    await seed(`users/${ALICE}/meta/notifikasi`, { fcm_token: "t", fcm_token_diperbarui: LAMA });
    await assertSucceeds(deleteDoc(ref()));
  });

  test("orang lain tidak bisa membaca, menulis, atau menghapus token", async () => {
    await assertFails(setDoc(ref(BOB), T()));
    await seed(`users/${ALICE}/meta/notifikasi`, { fcm_token: "t", fcm_token_diperbarui: LAMA });
    await assertFails(getDoc(ref(BOB)));
    await assertFails(deleteDoc(ref(BOB)));
    await assertFails(getDoc(doc(anon(), `users/${ALICE}/meta/notifikasi`)));
  });

  test("dokumen meta lain selain profile & notifikasi tetap ditolak", () =>
    assertFails(setDoc(doc(db(ALICE), `users/${ALICE}/meta/lainnya`), T())));

  test("meta/profile tidak menerima field token", () =>
    assertFails(setDoc(doc(db(ALICE), `users/${ALICE}/meta/profile`), { name: "A", ...T() })));
});

describe("users/{uid}/jadwal_pengingat (notifikasi server tahap 1)", () => {
  const jadwal = (who = ALICE, id = "default_1_20261105", uid = ALICE) =>
    doc(db(who), `users/${uid}/jadwal_pengingat/${id}`);
  const jam = (h) => Timestamp.fromMillis(Date.now() + h * 3600_000);
  const J = (o = {}) => ({
    waktu_kirim: jam(5), jenis_ibadah: "sholat_wajib", nama_detail: "subuh", id_pengingat: "default_1", ...o,
  });

  test("pemilik menulis, membaca, menghapus", async () => {
    await assertSucceeds(setDoc(jadwal(), J()));
    await assertSucceeds(getDoc(jadwal()));
    await assertSucceeds(getDocs(collection(db(ALICE), `users/${ALICE}/jadwal_pengingat`)));
    await assertSucceeds(deleteDoc(jadwal()));
  });

  test("sunnah (uuid), qadha (detail null), puasa sah", async () => {
    const uuid = "3f2a9c1e-7b4d-4e8a-9f00-1234567890ab";
    await assertSucceeds(setDoc(jadwal(ALICE, `${uuid}_20261106`),
      J({ id_pengingat: uuid, jenis_ibadah: "sholat_sunnah", nama_detail: "dhuha" })));
    await assertSucceeds(setDoc(jadwal(ALICE, "q1_20261106"),
      J({ id_pengingat: "q1", jenis_ibadah: "qadha_sholat", nama_detail: null })));
    await assertSucceeds(setDoc(jadwal(ALICE, "p1_20261106"),
      J({ id_pengingat: "p1", jenis_ibadah: "puasa", nama_detail: "senin_kamis" })));
  });

  test("orang lain tidak bisa membaca atau menulis", async () => {
    await assertFails(setDoc(jadwal(BOB), J()));
    await seed(`users/${ALICE}/jadwal_pengingat/default_1_20261105`, J());
    await assertFails(getDoc(jadwal(BOB)));
    await assertFails(deleteDoc(jadwal(BOB)));
    await assertFails(getDoc(doc(anon(), `users/${ALICE}/jadwal_pengingat/default_1_20261105`)));
  });

  test("waktu_kirim: timestamp, tidak lebih dari 8 hari ke depan / 1 hari lalu", async () => {
    await assertFails(setDoc(jadwal(), J({ waktu_kirim: "2026-11-05T04:30" })));
    await assertFails(setDoc(jadwal(), J({ waktu_kirim: jam(24 * 9) })));
    await assertFails(setDoc(jadwal(), J({ waktu_kirim: jam(-48) })));
    await assertSucceeds(setDoc(jadwal(), J({ waktu_kirim: jam(24 * 7) })));
  });

  test("jenis & nama_detail mengikuti skema logs", async () => {
    await assertFails(setDoc(jadwal(), J({ jenis_ibadah: "tilawah", nama_detail: "bacaan_harian" })));
    await assertFails(setDoc(jadwal(), J({ nama_detail: "dhuha" })));
    await assertFails(setDoc(jadwal(), J({ nama_detail: null })));
    await assertFails(setDoc(jadwal(), J({ jenis_ibadah: "qadha_sholat", nama_detail: "subuh" })));
  });

  test("kunci wajib lengkap, tidak boleh ada field lain (mis. koordinat)", async () => {
    const t = J();
    delete t.nama_detail;
    await assertFails(setDoc(jadwal(), t));
    await assertFails(setDoc(jadwal(), J({ lat: -6.2, lng: 106.8 })));
  });

  test("ID dokumen = {id_pengingat}_{yyyyMMdd}", async () => {
    await assertFails(setDoc(jadwal(ALICE, "default_2_20261105"), J()));
    await assertFails(setDoc(jadwal(ALICE, "default_1_2026-11-05"), J()));
    await assertFails(setDoc(jadwal(ALICE, "x_20261105"), J({ id_pengingat: "x y" })));
  });
});

describe("users/{uid}: kode responden & persetujuan penelitian", () => {
  const ref = (who = ALICE) => doc(db(who), "users", ALICE);
  const LAMA = Timestamp.fromDate(new Date("2026-10-01T00:00:00Z"));
  const tanpa = (k) => { const p = profil(); delete p[k]; return p; };

  test("dokumen baru tanpa disetujui_pada ditolak", () =>
    assertFails(setDoc(ref(), tanpa("disetujui_pada"))));

  test("disetujui_pada harus waktu server, tidak boleh jam HP", () =>
    assertFails(setDoc(ref(), profil({ disetujui_pada: Timestamp.now() }))));

  test("disetujui_pada tidak berubah setelah tersimpan", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA }));
    await assertFails(setDoc(ref(), { disetujui_pada: serverTimestamp() }, { merge: true }));
    await assertSucceeds(setDoc(ref(), { rentang_umur: "19-25" }, { merge: true }));
  });

  test("profil lama tanpa disetujui_pada: boleh mendapatkannya sekali (waktu server), boleh juga tidak", async () => {
    await seed(`users/${ALICE}`, tanpa("disetujui_pada"));
    await assertSucceeds(setDoc(ref(), { rentang_umur: "19-25" }, { merge: true }));
    await assertFails(setDoc(ref(), { disetujui_pada: LAMA }, { merge: true }));
    await assertSucceeds(setDoc(ref(), { disetujui_pada: serverTimestamp() }, { merge: true }));
  });

  test("kode_responden opsional: 4-20 huruf/angka/tanda hubung", async () => {
    await assertSucceeds(setDoc(ref(), profil({ kode_responden: "UNPAM-017" })));
    await env.clearFirestore();
    await assertFails(setDoc(ref(), profil({ kode_responden: "AB1" })));
    await assertFails(setDoc(ref(), profil({ kode_responden: "x".repeat(21) })));
    await assertFails(setDoc(ref(), profil({ kode_responden: "kode spasi" })));
    await assertFails(setDoc(ref(), profil({ kode_responden: 1234 })));
    await assertSucceeds(setDoc(ref(), profil()));
  });

  test("kode_responden boleh ditambah, diganti, dan dihapus", async () => {
    const { deleteField } = await import("firebase/firestore");
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA }));
    await assertSucceeds(setDoc(ref(), { kode_responden: "R-0001" }, { merge: true }));
    await assertSucceeds(setDoc(ref(), { kode_responden: "R-0002" }, { merge: true }));
    await assertSucceeds(setDoc(ref(), { kode_responden: deleteField() }, { merge: true }));
  });

  test("orang lain tidak bisa menulis kode responden", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA }));
    await assertFails(setDoc(ref(BOB), { kode_responden: "R-0001" }, { merge: true }));
  });
});

describe("users/{uid}: ditarik_pada (berhenti ikut penelitian)", () => {
  const ref = (who = ALICE) => doc(db(who), "users", ALICE);
  const LAMA = Timestamp.fromDate(new Date("2026-10-01T00:00:00Z"));
  const peserta = () => seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA }));

  test("pemilik boleh menandai ditarik_pada dengan waktu server (update seperti aplikasi)", async () => {
    await peserta();
    await assertSucceeds(updateDoc(ref(), { ditarik_pada: serverTimestamp() }));
  });

  test("ditarik_pada tidak boleh jam HP atau waktu lain", async () => {
    await peserta();
    await assertFails(updateDoc(ref(), { ditarik_pada: Timestamp.now() }));
    await assertFails(updateDoc(ref(), { ditarik_pada: LAMA }));
  });

  test("ditarik_pada yang tersimpan tidak bisa digeser, tapi boleh dibiarkan saat profil diperbarui", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA, ditarik_pada: LAMA }));
    await assertFails(updateDoc(ref(), { ditarik_pada: Timestamp.fromDate(new Date("2026-10-02T00:00:00Z")) }));
    await assertSucceeds(setDoc(ref(), { kode_responden: "R-0001" }, { merge: true }));
  });

  test("setuju lagi: pemilik boleh menghapus ditarik_pada", async () => {
    const { deleteField } = await import("firebase/firestore");
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA, ditarik_pada: LAMA }));
    await assertSucceeds(setDoc(ref(), { ditarik_pada: deleteField() }, { merge: true }));
  });

  test("orang lain tidak bisa menandai atau menghapus ditarik_pada", async () => {
    const { deleteField } = await import("firebase/firestore");
    await peserta();
    await assertFails(updateDoc(ref(BOB), { ditarik_pada: serverTimestamp() }));
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA, ditarik_pada: LAMA }));
    await assertFails(setDoc(ref(BOB), { ditarik_pada: deleteField() }, { merge: true }));
  });

  test("dokumen baru tidak boleh langsung berisi ditarik_pada", () =>
    assertFails(setDoc(ref(), profil({ ditarik_pada: serverTimestamp() }))));

  test("pengiriman profil aplikasi (merge + hapus ditarik_pada) lolos untuk dokumen baru maupun lama", async () => {
    const { deleteField } = await import("firebase/firestore");
    await assertSucceeds(setDoc(ref(), profil({ ditarik_pada: deleteField() }), { merge: true }));
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: LAMA, disetujui_pada: LAMA, ditarik_pada: LAMA }));
    await assertSucceeds(setDoc(ref(), { rentang_umur: "26-35", ditarik_pada: deleteField() }, { merge: true }));
  });

  test("menandai ditarik_pada pada dokumen yang tidak ada ditolak (aplikasi menganggapnya selesai)", () =>
    assertFails(updateDoc(ref(), { ditarik_pada: serverTimestamp() })));
});

describe("logs: Tertinggal untuk puasa & ibadah sunnah", () => {
  const tt = (o = {}) => ({ status: "tidak", sumber_pencatatan: "buka_aplikasi", nama_detail_asli: null, ...o });

  test("puasa sunnah & Ramadan: tidak dari kartu Beranda", async () => {
    await assertSucceeds(tulisLog(tt({ jenis_ibadah: "puasa", nama_detail: "senin_kamis" })));
    await assertSucceeds(tulisLog(tt({ jenis_ibadah: "puasa", nama_detail: "ramadan", sumber_pencatatan: "dari_notifikasi" })));
  });

  test("sholat sunnah (preset & buatan pengguna) dan qadha", async () => {
    await assertSucceeds(tulisLog(tt({ jenis_ibadah: "sholat_sunnah", nama_detail: "dhuha" })));
    await assertSucceeds(tulisLog(tt({ jenis_ibadah: "sholat_sunnah", nama_detail: "sunnah_lainnya" })));
    await assertSucceeds(tulisLog(tt({ jenis_ibadah: "qadha_sholat", nama_detail: null })));
  });

  test("untuk jenis baru, tidak HANYA sebagai pengakuan (jam + sumber wajib)", async () => {
    await assertFails(tulisLog(tt({ jenis_ibadah: "puasa", nama_detail: "senin_kamis", jam_pencatatan: null })));
    await assertFails(tulisLog(tt({ jenis_ibadah: "puasa", nama_detail: "senin_kamis", jam_pencatatan: null, sumber_pencatatan: null })));
    await assertFails(tulisLog(tt({ jenis_ibadah: "sholat_sunnah", nama_detail: "dhuha", sumber_pencatatan: "dari_notifikasi_dibuka" })));
  });

  test("tilawah & iqro tetap tanpa Tertinggal", async () => {
    await assertFails(tulisLog(tt({ jenis_ibadah: "tilawah", nama_detail: "bacaan_harian", jumlah: 1, jumlah_ayat: 1, juz: 1 })));
    await assertFails(tulisLog(tt({ jenis_ibadah: "iqro", nama_detail: null, jumlah: 1, jilid: 1 })));
  });

  test("Tertinggal lalu dikerjakan: tertimpa", async () => {
    await assertSucceeds(tulisLog(tt({ jenis_ibadah: "puasa", nama_detail: "senin_kamis" })));
    await assertSucceeds(tulisLog({ jenis_ibadah: "puasa", nama_detail: "senin_kamis", nama_detail_asli: null }));
  });
});

describe("Anonymous Auth: tamu anonim", () => {
  const ANON = "anonUid789";

  test("menulis log, profil penelitian (dengan persetujuan), token, dan jadwal miliknya", async () => {
    const d = dbAnonim(ANON);
    await assertSucceeds(setDoc(doc(d, "logs", logId({ user_id: ANON }, ANON)), log({ user_id: ANON })));
    await assertSucceeds(setDoc(doc(d, "users", ANON), profil()));
    await assertSucceeds(setDoc(doc(d, `users/${ANON}/meta/notifikasi`), { fcm_token: "t", fcm_token_diperbarui: serverTimestamp() }));
    await assertSucceeds(setDoc(doc(d, `users/${ANON}/meta/profile`), { genderProfile: "FEMALE", isHaidhMode: false, haidhDates: [] }));
    await assertSucceeds(setDoc(doc(d, `users/${ANON}/sunnah_habits/s1`), {
      name: "Sholat Dhuha", category: "SHOLAT", frequencyLabel: "Setiap hari", reminderEnabled: false,
      createdAt: Date.now(), updatedAt: Date.now() }));
  });

  test("tanpa persetujuan (disetujui_pada) users/{uid} anonim tidak bisa dibuat", async () => {
    const p = profil();
    delete p.disetujui_pada;
    await assertFails(setDoc(doc(dbAnonim(ANON), "users", ANON), p));
  });

  test("tidak bisa membaca atau menulis data orang lain", async () => {
    await seed(`users/${ALICE}`, profil({ tanggal_mulai: Timestamp.now() }));
    await seed(`logs/${logId()}`, log());
    const d = dbAnonim(ANON);
    await assertFails(getDoc(doc(d, "users", ALICE)));
    await assertFails(getDoc(doc(d, "logs", logId())));
    await assertFails(setDoc(doc(d, "logs", logId()), log()));
    await assertFails(setDoc(doc(d, "logs", logId({ user_id: ANON }, ALICE)), log({ user_id: ANON })));
  });

  test("setelah ditautkan ke Google (UID sama), data lama tetap bisa diperbarui", async () => {
    await seed(`users/${ANON}`, profil({ tanggal_mulai: Timestamp.now() }));
    const google = env.authenticatedContext(ANON, { firebase: { sign_in_provider: "google.com" } }).firestore();
    await assertSucceeds(setDoc(doc(google, "users", ANON), { rentang_umur: "19-25" }, { merge: true }));
  });

  test("tanpa login sama sekali tetap ditolak", () =>
    assertFails(setDoc(doc(anon(), "logs", logId()), log())));
});

describe("permintaan_hapus/{uid}: akun anonim yang menolak dihapus langsung", () => {
  const ANON = "anonUid789";
  const ref = (who = ANON, uid = ANON) => doc(dbAnonim(who), `permintaan_hapus/${uid}`);

  test("pemilik mengajukan sekali dengan waktu server", async () => {
    await assertSucceeds(setDoc(ref(), { diminta_pada: serverTimestamp() }));
    await assertFails(setDoc(ref(), { diminta_pada: serverTimestamp() }));
  });

  test("waktu harus waktu server dan tidak boleh ada field lain", async () => {
    await assertFails(setDoc(ref(), { diminta_pada: Timestamp.now() }));
    await assertFails(setDoc(ref(), { diminta_pada: serverTimestamp(), alasan: "x" }));
    await assertFails(setDoc(ref(), {}));
  });

  test("tidak bisa dibaca, diubah, atau dihapus dari aplikasi", async () => {
    await seed(`permintaan_hapus/${ANON}`, { diminta_pada: Timestamp.now() });
    await assertFails(getDoc(ref()));
    await assertFails(updateDoc(ref(), { diminta_pada: serverTimestamp() }));
    await assertFails(deleteDoc(ref()));
  });

  test("tidak bisa mengajukan atas nama UID lain", () =>
    assertFails(setDoc(ref(ANON, ALICE), { diminta_pada: serverTimestamp() })));
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

  for (const key of ["jam_pencatatan", "sumber_pencatatan", "status", "jumlah", "nama_detail_asli", "jilid",
                     "jumlah_ayat", "juz", "offset_waktu", "selisih_menit_dari_adzan",
                     "mode_pencatatan", "waktu_dzikir", "target_hitungan", "durasi_detik"]) {
    test(`kunci ${key} wajib ada`, () =>
      assertFails(setDoc(doc(db(ALICE), "logs", logId()), tanpa(key))));
  }

  test("data profil tidak boleh disalin ke log", () =>
    assertFails(tulisLog({ jenis_kelamin: "perempuan" })));

  test("field di luar skema ditolak", () =>
    assertFails(tulisLog({ selisih_menit: 12 })));

  test("format tanggal salah ditolak", async () => {
    await assertFails(tulisLog({ tanggal: "09-10-2026" }));
    await assertFails(tulisLog({ tanggal: "2026-13-01" }));
  });

  test("jenis_ibadah di luar enum ditolak", () =>
    assertFails(tulisLog({ jenis_ibadah: "sedekah", nama_detail: null })));

  test("nama_detail harus cocok dengan jenis_ibadah", async () => {
    await assertFails(tulisLog({ nama_detail: "pagi" }));
    // "jumat" hanya sah untuk sholat_wajib (lihat blok sholat Jumat).
    await assertFails(tulisLog({ jenis_ibadah: "sholat_sunnah", nama_detail: "jumat" }));
    await assertFails(tulisLog({ nama_detail: null }));
    await assertFails(tulisLog({ ...DZ, nama_detail: "subuh" }));
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
    await assertSucceeds(tulisLog({ sumber_pencatatan: "tidak_diketahui" }));
    await assertFails(tulisLog({ sumber_pencatatan: null }));
    await assertFails(tulisLog({ sumber_pencatatan: "widget" }));
  });

  test("dari_notifikasi_dibuka sah untuk dikerjakan, tidak untuk baris tidak/udzur", async () => {
    await assertSucceeds(tulisLog({ sumber_pencatatan: "dari_notifikasi_dibuka" }));
    await assertFails(tulisLog({ status: "tidak", sumber_pencatatan: "dari_notifikasi_dibuka" }));
    await assertFails(tulisLog({ status: "udzur", jam_pencatatan: null, sumber_pencatatan: "dari_notifikasi_dibuka" }));
    await assertFails(tulisLog({ sumber_pencatatan: "dari_notifikasi_buka" }));
  });

  test("tidak_diketahui hanya untuk dikerjakan, bukan pengganti null", async () => {
    await assertFails(tulisLog({ status: "tidak", jam_pencatatan: null, sumber_pencatatan: "tidak_diketahui" }));
    await assertFails(tulisLog({ status: "udzur", jam_pencatatan: null, sumber_pencatatan: "tidak_diketahui" }));
    await assertFails(tulisLog({ sumber_pencatatan: "tidak diketahui" }));
  });

  test("tidak: jam & sumber wajib null", async () => {
    await assertSucceeds(tulisLog({ status: "tidak", jam_pencatatan: null, sumber_pencatatan: null }));
    await assertFails(tulisLog({ status: "tidak", sumber_pencatatan: null }));
    await assertFails(tulisLog({ status: "tidak", jam_pencatatan: null }));
  });

  test("tidak dari tombol Tertinggal: sholat wajib, jam + sumber dari_notifikasi / buka_aplikasi", async () => {
    const tt = { status: "tidak", sumber_pencatatan: "dari_notifikasi" };
    await assertSucceeds(tulisLog(tt));
    await assertFails(tulisLog({ ...tt, jam_pencatatan: null }));
    // Tombol Tertinggal di kartu Beranda.
    await assertSucceeds(tulisLog({ ...tt, sumber_pencatatan: "buka_aplikasi" }));
    await assertFails(tulisLog({ ...DZ, ...tt, sumber_pencatatan: "buka_aplikasi", nama_detail: "dzikir_petang",
      mode_pencatatan: null, waktu_dzikir: "petang" }));
    await assertFails(tulisLog({ ...tt, sumber_pencatatan: "tidak_diketahui" }));
    await assertFails(tulisLog({ ...tt, selisih_menit_dari_adzan: 5 }));
    await assertFails(tulisLog({ ...DZ, ...tt, nama_detail: "dzikir_petang", mode_pencatatan: null, waktu_dzikir: "petang" }));
    await assertFails(tulisLog({ status: "udzur", sumber_pencatatan: "dari_notifikasi" }));
  });

  test("tidak dari Tertinggal lalu dicentang: tertimpa jadi dikerjakan", async () => {
    await assertSucceeds(tulisLog({ status: "tidak", sumber_pencatatan: "dari_notifikasi" }));
    await assertSucceeds(tulisLog({ sumber_pencatatan: "buka_aplikasi" }));
  });

  test("tidak hanya untuk sholat wajib dan dzikir", async () => {
    const t = { status: "tidak", jam_pencatatan: null, sumber_pencatatan: null };
    await assertSucceeds(tulisLog({ ...DZ, ...t, nama_detail: "dzikir_petang", mode_pencatatan: null, waktu_dzikir: "petang" }));
    await assertFails(tulisLog({ ...t, jenis_ibadah: "puasa", nama_detail: "senin_kamis" }));
    await assertFails(tulisLog({ ...t, jenis_ibadah: "sholat_sunnah", nama_detail: "dhuha" }));
    await assertFails(tulisLog({ ...t, jenis_ibadah: "tilawah", nama_detail: null, jumlah: 1 }));
  });

  test("udzur: hanya sholat wajib & puasa, jam & sumber null", async () => {
    const u = { status: "udzur", jam_pencatatan: null, sumber_pencatatan: null };
    await assertSucceeds(tulisLog(u));
    await assertSucceeds(tulisLog({ ...u, jenis_ibadah: "puasa", nama_detail: "ramadan" }));
    await assertFails(tulisLog({ ...DZ, ...u }));
    await assertFails(tulisLog({ status: "udzur" }));
  });

  test("status di luar enum ditolak (tidak boleh digabung)", () =>
    assertFails(tulisLog({ status: "tidak_udzur", jam_pencatatan: null, sumber_pencatatan: null })));
});

describe("logs: jumlah & jilid", () => {
  const tilawah = { jenis_ibadah: "tilawah", nama_detail: "bacaan_harian", nama_detail_asli: null, jumlah_ayat: 10, juz: 1 };
  const iqro = { jenis_ibadah: "iqro", nama_detail: null, nama_detail_asli: null };

  test("tilawah: jumlah halaman bulat >= 1", async () => {
    await assertSucceeds(tulisLog({ ...tilawah, jumlah: 5 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: 0 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: 2.5 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: null }));
    await assertFails(tulisLog({ ...tilawah, jumlah: "5" }));
  });

  test("tilawah: jilid harus null, nama_detail salah satu dari tiga jenis sesi", async () => {
    await assertFails(tulisLog({ ...tilawah, jumlah: 5, jilid: 1 }));
    await assertFails(tulisLog({ ...tilawah, jumlah: 5, nama_detail: "pagi" }));
    await assertFails(tulisLog({ ...tilawah, jumlah: 5, nama_detail: null }));
    for (const jenis of ["bacaan_harian", "hafalan", "murajaah"]) {
      const t = { ...tilawah, nama_detail: jenis, jumlah: 2 };
      await assertSucceeds(tulisLog(t, { id: logId(t, ALICE, `s-${jenis.length}`) }));
    }
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

describe("logs: tilawah model rentang", () => {
  const t = { jenis_ibadah: "tilawah", nama_detail: "hafalan", nama_detail_asli: null, jumlah: 1, jumlah_ayat: 5, juz: 30 };

  test("batas jumlah halaman 1-604", async () => {
    await assertSucceeds(tulisLog({ ...t, jumlah: 604, jumlah_ayat: 6236, juz: 1 }));
    await assertFails(tulisLog({ ...t, jumlah: 605 }));
  });

  test("jumlah_ayat bulat 1-6236", async () => {
    await assertFails(tulisLog({ ...t, jumlah_ayat: 0 }));
    await assertFails(tulisLog({ ...t, jumlah_ayat: 6237 }));
    await assertFails(tulisLog({ ...t, jumlah_ayat: 2.5 }));
    await assertFails(tulisLog({ ...t, jumlah_ayat: null }));
  });

  test("juz bulat 1-30", async () => {
    await assertFails(tulisLog({ ...t, juz: 0 }));
    await assertFails(tulisLog({ ...t, juz: 31 }));
    await assertFails(tulisLog({ ...t, juz: null }));
  });

  test("jenis lain tidak boleh punya jumlah_ayat/juz", async () => {
    await assertFails(tulisLog({ juz: 1 }));
    await assertFails(tulisLog({ jumlah_ayat: 3 }));
    const iqro = { jenis_ibadah: "iqro", nama_detail: null, jilid: 2, jumlah: 5 };
    await assertSucceeds(tulisLog(iqro, { id: `${ALICE}_${TGL}_iqro` }));
    await assertFails(tulisLog({ ...iqro, juz: 1 }, { id: `${ALICE}_${TGL}_iqro` }));
    await assertFails(tulisLog({ ...iqro, jumlah_ayat: 5 }, { id: `${ALICE}_${TGL}_iqro` }));
  });

  test("ID tilawah wajib memuat jenis sesi", async () => {
    await assertSucceeds(tulisLog(t, { id: `${ALICE}_${TGL}_tilawah_hafalan_abc-1` }));
    await assertFails(tulisLog(t, { id: `${ALICE}_${TGL}_tilawah_abc-1` }));
    await assertFails(tulisLog(t, { id: `${ALICE}_${TGL}_tilawah_murajaah_abc-1` }));
  });
});

describe("logs: offset_waktu & selisih_menit_dari_adzan", () => {
  test("offset_waktu wajib, format +HH:MM", async () => {
    for (const ok of ["+07:00", "+08:00", "+09:00", "-03:30", "+00:00", "+05:45"]) {
      await assertSucceeds(tulisLog({ offset_waktu: ok }));
    }
    for (const bad of [null, "07:00", "+7:00", "+07", "WIB", "+15:00", "+07:60", 7]) {
      await assertFails(tulisLog({ offset_waktu: bad }));
    }
  });

  test("offset_waktu juga wajib untuk baris tidak/udzur", async () => {
    const nol = { jam_pencatatan: null, sumber_pencatatan: null };
    await assertSucceeds(tulisLog({ ...nol, status: "udzur" }));
    await assertFails(tulisLog({ ...nol, status: "udzur", offset_waktu: null }));
  });

  test("selisih: sholat wajib & dzikir boleh bulat -1440..1440 atau null", async () => {
    await assertSucceeds(tulisLog({ selisih_menit_dari_adzan: 12 }));
    await assertSucceeds(tulisLog({ selisih_menit_dari_adzan: -5 }));
    await assertSucceeds(tulisLog({ selisih_menit_dari_adzan: null }));
    const dz = { ...DZ, selisih_menit_dari_adzan: 30 };
    await assertSucceeds(tulisLog(dz));
    await assertFails(tulisLog({ selisih_menit_dari_adzan: 1441 }));
    await assertFails(tulisLog({ selisih_menit_dari_adzan: 12.5 }));
    await assertFails(tulisLog({ selisih_menit_dari_adzan: "12" }));
  });

  test("selisih harus null untuk jenis lain dan untuk status bukan dikerjakan", async () => {
    await assertFails(tulisLog({ jenis_ibadah: "puasa", nama_detail: "senin_kamis", selisih_menit_dari_adzan: 5 }));
    const t = { jenis_ibadah: "tilawah", nama_detail: "bacaan_harian", jumlah: 1, jumlah_ayat: 3, juz: 1, selisih_menit_dari_adzan: 5 };
    await assertFails(tulisLog(t));
    await assertFails(tulisLog({ status: "tidak", jam_pencatatan: null, sumber_pencatatan: null, selisih_menit_dari_adzan: 5 }));
  });
});

describe("logs: dzikir katalog + mode pencatatan", () => {
  test("sembilan slug katalog diterima (mode centang)", async () => {
    const slugs = ["dzikir_bada_sholat", "dzikir_pagi", "dzikir_petang", "dzikir_sebelum_tidur",
      "istighfar", "sholawat", "tahlil", "asmaul_husna"];
    for (const slug of slugs) await assertSucceeds(tulisLog({ ...DZ, nama_detail: slug, waktu_dzikir: "bebas" }));
    const lain = { ...DZ, nama_detail: "dzikir_lainnya", nama_detail_asli: "Hizib", waktu_dzikir: "bebas" };
    await assertSucceeds(tulisLog(lain, { id: logId(lain, ALICE, "habit-1") }));
  });

  test("slug lama pagi/petang & slug asing ditolak", async () => {
    await assertFails(tulisLog({ ...DZ, nama_detail: "pagi" }, { id: `${ALICE}_${TGL}_dzikir_pagi` }));
    await assertFails(tulisLog({ ...DZ, nama_detail: "wirid" }, { id: `${ALICE}_${TGL}_dzikir_wirid` }));
  });

  test("ATURAN KEJUJURAN: mode centang -> jumlah & durasi wajib null", async () => {
    await assertSucceeds(tulisLog(DZ));
    await assertFails(tulisLog({ ...DZ, jumlah: 33 }));
    await assertFails(tulisLog({ ...DZ, durasi_detik: 120 }));
    await assertFails(tulisLog({ ...DZ, target_hitungan: 100, jumlah: 100 }));
    await assertSucceeds(tulisLog({ ...DZ, target_hitungan: 100 }));
  });

  test("mode layar: jumlah & durasi wajib bulat positif", async () => {
    const layar = { ...DZ, mode_pencatatan: "layar", jumlah: 33, durasi_detik: 180, target_hitungan: 33 };
    await assertSucceeds(tulisLog(layar));
    await assertFails(tulisLog({ ...layar, jumlah: null }));
    await assertFails(tulisLog({ ...layar, durasi_detik: null }));
    await assertFails(tulisLog({ ...layar, jumlah: 0 }));
    await assertFails(tulisLog({ ...layar, jumlah: 2.5 }));
    await assertFails(tulisLog({ ...layar, durasi_detik: 86401 }));
  });

  test("mode & waktu wajib sah untuk dikerjakan", async () => {
    await assertFails(tulisLog({ ...DZ, mode_pencatatan: null }));
    await assertFails(tulisLog({ ...DZ, mode_pencatatan: "tasbih" }));
    await assertFails(tulisLog({ ...DZ, waktu_dzikir: null }));
    await assertFails(tulisLog({ ...DZ, waktu_dzikir: "malam" }));
    await assertFails(tulisLog({ ...DZ, target_hitungan: 0 }));
    await assertFails(tulisLog({ ...DZ, target_hitungan: 100001 }));
  });

  test("nama_detail_asli hanya untuk dzikir_lainnya", async () => {
    await assertFails(tulisLog({ ...DZ, nama_detail_asli: "Dzikir Pagi" }));
    const lain = { ...DZ, nama_detail: "dzikir_lainnya", waktu_dzikir: "bebas" };
    await assertFails(tulisLog({ ...lain, nama_detail_asli: null }, { id: logId(lain, ALICE, "habit-2") }));
    await assertFails(tulisLog({ ...lain, nama_detail_asli: "" }, { id: logId(lain, ALICE, "habit-2") }));
    await assertFails(tulisLog({ ...lain, nama_detail_asli: "Hizib" }, { id: `${ALICE}_${TGL}_dzikir_dzikir_lainnya_habit-2` }));
  });

  test("ID per sesi: dua sesi sehari = dua baris; tanpa penanda sesi ditolak", async () => {
    const bada = { ...DZ, nama_detail: "dzikir_bada_sholat", waktu_dzikir: "bada_sholat" };
    const id1 = `${ALICE}_${TGL}_dzikir_dzikir_bada_sholat_habit-9_1791600000000`;
    const id2 = `${ALICE}_${TGL}_dzikir_dzikir_bada_sholat_habit-9_1791621600000`;
    await assertSucceeds(tulisLog(bada, { id: id1 }));
    await assertSucceeds(tulisLog(bada, { id: id2 }));
    await assertSucceeds(tulisLog(bada, { id: id1 })); // percobaan ulang menimpa dokumen yang sama
    const snap = await getDocs(query(collection(db(ALICE), "logs"), where("user_id", "==", ALICE),
      where("nama_detail", "==", "dzikir_bada_sholat")));
    if (snap.size !== 2) throw new Error(`harusnya 2 sesi, dapat ${snap.size}`);
    await assertFails(tulisLog(bada, { id: `${ALICE}_${TGL}_dzikir_dzikir_bada_sholat` }));
    await assertFails(tulisLog(bada, { id: `${ALICE}_${TGL}_dzikir_dzikir_bada_sholat_habit-9` }));
    await assertFails(tulisLog(bada, { id: `${ALICE}_${TGL}_dzikir_dzikir_bada_sholat_habit-9_179160000` }));
    await assertFails(tulisLog(bada, { id: `${ALICE}_${TGL}_dzikir_dzikir_bada_sholat_habit_9_1791600000000` }));
  });

  test("keputusan 4.4: 'tidak' ditolak untuk waktu bebas, diterima untuk slug berjendela", async () => {
    const t = { status: "tidak", jam_pencatatan: null, sumber_pencatatan: null, mode_pencatatan: null };
    for (const [slug, waktu] of [["dzikir_bada_sholat", "bada_sholat"], ["dzikir_pagi", "pagi"],
                                 ["dzikir_petang", "petang"], ["dzikir_sebelum_tidur", "sebelum_tidur"]]) {
      await assertSucceeds(tulisLog({ ...DZ, ...t, nama_detail: slug, waktu_dzikir: waktu }));
    }
    for (const slug of ["istighfar", "sholawat", "tahlil", "asmaul_husna"]) {
      await assertFails(tulisLog({ ...DZ, ...t, nama_detail: slug, waktu_dzikir: "bebas" }));
    }
    const lain = { ...DZ, ...t, nama_detail: "dzikir_lainnya", nama_detail_asli: "Hizib", waktu_dzikir: "bebas" };
    await assertFails(tulisLog(lain, { id: logId(lain, ALICE, "habit-3") }));
    // Slug berjendela dengan waktu bebas / tanpa waktu juga ditolak (tidak bisa disimpulkan).
    await assertFails(tulisLog({ ...DZ, ...t, nama_detail: "dzikir_pagi", waktu_dzikir: "bebas" }));
    await assertFails(tulisLog({ ...DZ, ...t, nama_detail: "dzikir_pagi", waktu_dzikir: null }));
    // Slug ber-waktu bebas tidak bisa diakali dengan waktu berjendela.
    await assertFails(tulisLog({ ...DZ, ...t, nama_detail: "istighfar", waktu_dzikir: "pagi" }));
  });

  test("baris 'tidak' dzikir: tanpa jumlah/durasi", async () => {
    const t = { ...DZ, status: "tidak", jam_pencatatan: null, sumber_pencatatan: null };
    await assertSucceeds(tulisLog(t));
    await assertFails(tulisLog({ ...t, jumlah: 1, mode_pencatatan: "layar" }));
  });

  test("kolom dzikir wajib null untuk jenis lain", async () => {
    await assertFails(tulisLog({ mode_pencatatan: "centang" }));
    await assertFails(tulisLog({ waktu_dzikir: "pagi" }));
    await assertFails(tulisLog({ target_hitungan: 5 }));
    await assertFails(tulisLog({ durasi_detik: 5 }));
  });
});

describe("penanda & target tilawah", () => {
  const penanda = (uid, jenis) => doc(db(uid), `users/${uid}/penanda/${jenis}`);

  test("pemilik menyimpan penanda tiga jenis sesi + iqro", async () => {
    await assertSucceeds(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 2, ayat: 142 }));
    await assertSucceeds(setDoc(penanda(ALICE, "hafalan"), { surah: 114, ayat: 6 }));
    await assertSucceeds(setDoc(penanda(ALICE, "murajaah"), { surah: 78, ayat: 1 }));
    await assertSucceeds(setDoc(penanda(ALICE, "iqro"), { jilid: 3, halaman: 12 }));
  });

  test("posisi mustahil ditolak (memakai jumlah ayat per surah)", async () => {
    await assertFails(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 1, ayat: 8 }));
    await assertFails(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 114, ayat: 7 }));
    await assertFails(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 0, ayat: 1 }));
    await assertFails(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 115, ayat: 1 }));
    await assertSucceeds(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 2, ayat: 286 }));
    await assertFails(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 2, ayat: 287 }));
  });

  test("field tambahan / jenis tak dikenal / iqro di luar batas ditolak", async () => {
    await assertFails(setDoc(penanda(ALICE, "bacaan_harian"), { surah: 2, ayat: 1, juz: 1 }));
    await assertFails(setDoc(penanda(ALICE, "tadarus"), { surah: 2, ayat: 1 }));
    await assertFails(setDoc(penanda(ALICE, "iqro"), { jilid: 11, halaman: 1 }));
    await assertFails(setDoc(penanda(ALICE, "iqro"), { surah: 1, ayat: 1 }));
  });

  test("penanda orang lain tidak bisa dibaca/ditulis", async () => {
    await seed(`users/${ALICE}/penanda/bacaan_harian`, { surah: 1, ayat: 1 });
    await assertFails(getDoc(doc(db(BOB), `users/${ALICE}/penanda/bacaan_harian`)));
    await assertFails(setDoc(doc(db(BOB), `users/${ALICE}/penanda/bacaan_harian`), { surah: 1, ayat: 1 }));
    await assertSucceeds(getDoc(penanda(ALICE, "bacaan_harian")));
  });

  test("target harian hanya dokumen tilawah, satuan halaman 1-604", async () => {
    const target = (id) => doc(db(ALICE), `users/${ALICE}/target/${id}`);
    await assertSucceeds(setDoc(target("tilawah"), { halaman_per_hari: 1 }));
    await assertFails(setDoc(target("tilawah"), { halaman_per_hari: 0 }));
    await assertFails(setDoc(target("tilawah"), { halaman_per_hari: 2.5 }));
    await assertFails(setDoc(target("tilawah"), { halaman_per_hari: 1, ayat_per_hari: 5 }));
    await assertFails(setDoc(target("hafalan"), { halaman_per_hari: 1 }));
    await assertFails(getDoc(doc(db(BOB), `users/${ALICE}/target/tilawah`)));
  });
});

describe("habit dzikir (konfigurasi, bukan log)", () => {
  const hd = (uid, id, who = uid) => doc(db(who), `users/${uid}/dzikir_habits/${id}`);
  const H = { slug: "dzikir_pagi", mode_pencatatan: "centang", waktu_dzikir: "pagi", dibuat_pada: 1791600000000 };

  test("pemilik menyimpan habit katalog; target & nama opsional sesuai slug", async () => {
    await assertSucceeds(setDoc(hd(ALICE, "kartu-pagi"), H));
    await assertSucceeds(setDoc(hd(ALICE, "3f2a9c1e-7b4d"), { ...H, slug: "istighfar", waktu_dzikir: "bebas", target_hitungan: 7 }));
    await assertSucceeds(setDoc(hd(ALICE, "lain-1"), { ...H, slug: "dzikir_lainnya", waktu_dzikir: "bebas", nama_detail_asli: "Ratib" }));
    // Mode layar sah di skema (dipakai Tahap 3); UI yang membatasinya.
    await assertSucceeds(setDoc(hd(ALICE, "layar-1"), { ...H, mode_pencatatan: "layar" }));
    await assertSucceeds(getDoc(hd(ALICE, "kartu-pagi")));
    await assertSucceeds(deleteDoc(hd(ALICE, "kartu-pagi")));
  });

  test("bentuk tidak sah ditolak", async () => {
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, slug: "wirid" }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, mode_pencatatan: "tasbih" }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, waktu_dzikir: "malam" }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, target_hitungan: 0 }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, target_hitungan: 100001 }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, target_hitungan: 33.5 }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, jumlah: 33 }));
    const { dibuat_pada, ...tanpaDibuat } = H;
    await assertFails(setDoc(hd(ALICE, "x"), tanpaDibuat));
    // nama_detail_asli: wajib untuk dzikir_lainnya, dilarang untuk slug lain.
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, slug: "dzikir_lainnya", waktu_dzikir: "bebas" }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, slug: "dzikir_lainnya", waktu_dzikir: "bebas", nama_detail_asli: "" }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, slug: "dzikir_lainnya", waktu_dzikir: "bebas", nama_detail_asli: "a".repeat(101) }));
    await assertFails(setDoc(hd(ALICE, "x"), { ...H, nama_detail_asli: "Dzikir pagi" }));
    // ID harus cocok dengan pola idHabit di ID log.
    await assertFails(setDoc(hd(ALICE, "habit_1"), H));
  });

  test("habit orang lain tidak bisa dibaca/ditulis/dihapus", async () => {
    await seed(`users/${ALICE}/dzikir_habits/h1`, H);
    await assertFails(getDoc(hd(ALICE, "h1", BOB)));
    await assertFails(setDoc(hd(ALICE, "h2", BOB), H));
    await assertFails(deleteDoc(hd(ALICE, "h1", BOB)));
  });
});

describe("logs: bentuk ID per jenis", () => {
  const tilawah = { jenis_ibadah: "tilawah", nama_detail: "bacaan_harian", nama_detail_asli: null, jumlah_ayat: 10, juz: 1 };
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
      tulisLog(DZ, { id: `${ALICE}_${TGL}_dzikir_dzikir_pagi_2` })
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

describe("meta/profile: profil ibadah permanen & rentang usia", () => {
  const prof = (uid = ALICE) => doc(db(uid), `users/${uid}/meta/profile`);

  test("dokumen v1.2.0 (punya rentangUsia): jenis kelamin sekali terisi tidak bisa diganti atau dikosongkan", async () => {
    await assertSucceeds(setDoc(prof(), { genderProfile: "UNSPECIFIED", isHaidhMode: false, rentangUsia: "19-25" }));
    await assertSucceeds(setDoc(prof(), { genderProfile: "FEMALE" }, { merge: true }));
    await assertFails(setDoc(prof(), { genderProfile: "MALE" }, { merge: true }));
    await assertFails(setDoc(prof(), { genderProfile: "UNSPECIFIED" }, { merge: true }));
    // Field lain tetap bisa diubah selama jenis kelamin tidak disentuh atau sama.
    await assertSucceeds(setDoc(prof(), { isHaidhMode: true, haidhDates: ["2026-10-14"] }, { merge: true }));
    await assertSucceeds(setDoc(prof(), { genderProfile: "FEMALE", isHaidhMode: false }, { merge: true }));
  });

  test("dokumen v1.1.1 (tanpa rentangUsia): jenis kelamin masih boleh diganti", async () => {
    await assertSucceeds(setDoc(prof(), { genderProfile: "FEMALE", isHaidhMode: false, haidhDates: [] }));
    await assertSucceeds(setDoc(prof(), { genderProfile: "MALE" }, { merge: true }));
    await assertSucceeds(setDoc(prof(), { genderProfile: "UNSPECIFIED" }, { merge: true }));
    // Begitu rentangUsia tersimpan (onboarding v1.2.0), kunci berlaku.
    await assertSucceeds(setDoc(prof(), { genderProfile: "FEMALE", rentangUsia: "19-25" }, { merge: true }));
    await assertFails(setDoc(prof(), { genderProfile: "MALE" }, { merge: true }));
  });

  test("rentang usia & izin wali divalidasi, usia boleh berubah", async () => {
    await assertSucceeds(setDoc(prof(), { rentangUsia: "15-18", izinWali: true }));
    await assertSucceeds(setDoc(prof(), { rentangUsia: "19-25", izinWali: false }, { merge: true }));
    await assertSucceeds(setDoc(prof(), { rentangUsia: ">35" }, { merge: true }));
    // Kode lama (sebelum v1.2.0) tetap sah supaya dokumen lama bisa diperbarui.
    await assertSucceeds(setDoc(prof(), { rentangUsia: "18+" }, { merge: true }));
    await assertFails(setDoc(prof(), { rentangUsia: "20" }, { merge: true }));
    await assertFails(setDoc(prof(), { izinWali: "ya" }, { merge: true }));
    await assertFails(setDoc(prof(), { genderProfile: "lainnya" }, { merge: true }));
  });

  test("profil orang lain tetap tidak bisa ditulis", async () => {
    await assertFails(setDoc(doc(db(BOB), `users/${ALICE}/meta/profile`), { genderProfile: "MALE" }));
  });

  test("field tak dikenal (role, isAdmin) ditolak saat dibuat maupun diubah", async () => {
    await assertFails(setDoc(prof(), { name: "Alice", role: "admin" }));
    await assertSucceeds(setDoc(prof(), { name: "Alice", updatedAt: serverTimestamp() }));
    await assertFails(setDoc(prof(), { role: "admin" }, { merge: true }));
    await assertFails(setDoc(prof(), { isAdmin: true }, { merge: true }));
    await assertFails(updateDoc(prof(), { updatedAt: 123 }));
  });

  test("semua field yang ditulis aplikasi tetap diterima", () =>
    assertSucceeds(setDoc(prof(), {
      name: "Alice", email: "a@b.c", genderProfile: "FEMALE", rentangUsia: "18+", izinWali: false,
      isHaidhMode: false, haidhDates: [], avatarBase64: "abc", avatarUrl: null,
      lastUsernameChangedAt: Date.now(), updatedAt: serverTimestamp(),
    })));

  test("dokumen lama dengan field lain tetap bisa diperbarui (field itu tidak disentuh)", async () => {
    await seed(`users/${ALICE}/meta/profile`, { name: "Alice", fieldLama: "x" });
    await assertSucceeds(setDoc(prof(), { isHaidhMode: true, updatedAt: serverTimestamp() }, { merge: true }));
    await assertFails(setDoc(prof(), { fieldLama: "y" }, { merge: true }));
  });
});

describe("logs: sholat Jumat (slot tengah hari)", () => {
  const JUMAT = "2026-10-09"; // Jumat
  const KAMIS = "2026-10-08";
  const jumat = (o = {}) => ({ nama_detail: "jumat", nama_detail_asli: "Sholat Jumat", tanggal: JUMAT, ...o });
  const dzuhur = (o = {}) => ({ nama_detail: "dzuhur", nama_detail_asli: "Sholat Dzuhur", tanggal: JUMAT, ...o });

  test("jumat sah di hari Jumat, dengan selisih adzan & sumber seperti sholat wajib lain", async () => {
    await assertSucceeds(tulisLog(jumat({ selisih_menit_dari_adzan: 25, sumber_pencatatan: "dari_notifikasi" })));
  });

  test("jumat ditolak di hari selain Jumat", () =>
    assertFails(tulisLog(jumat({ tanggal: KAMIS }))));

  test("dzuhur di hari biasa tidak terpengaruh", () =>
    assertSucceeds(tulisLog(dzuhur({ tanggal: KAMIS }))));

  test("ID dokumen jumat = {uid}_{tanggal}_sholat_wajib_jumat", async () => {
    await assertFails(tulisLog(jumat(), { id: `${ALICE}_${JUMAT}_sholat_wajib_dzuhur` }));
    await assertSucceeds(tulisLog(jumat(), { id: `${ALICE}_${JUMAT}_sholat_wajib_jumat` }));
  });

  test("Tertinggal Jumat (dari Beranda atau notifikasi), lalu dicentang", async () => {
    await assertSucceeds(tulisLog(jumat({ status: "tidak", sumber_pencatatan: "buka_aplikasi" })));
    await assertSucceeds(tulisLog(jumat({ sumber_pencatatan: "buka_aplikasi" })));
  });

  test("saling meniadakan: jumat ditolak bila dzuhur hari itu ada, dan sebaliknya", async () => {
    await seed(`logs/${logId(dzuhur())}`, log(dzuhur()));
    await assertFails(tulisLog(jumat()));
    await env.clearFirestore();
    await seed(`logs/${logId(jumat())}`, log(jumat()));
    await assertFails(tulisLog(dzuhur()));
    await assertFails(tulisLog(dzuhur({ status: "tidak", sumber_pencatatan: "dari_notifikasi" })));
  });

  test("ganti Jumat -> Dzuhur: hapus dulu lalu tulis (urutan sinkron aplikasi) atau dalam satu batch", async () => {
    const { writeBatch } = await import("firebase/firestore");
    await seed(`logs/${logId(jumat())}`, log(jumat()));
    await assertSucceeds(deleteDoc(doc(db(ALICE), "logs", logId(jumat()))));
    await assertSucceeds(tulisLog(dzuhur()));
    // Kembali ke Jumat dalam satu batch.
    const fs = db(ALICE);
    const b = writeBatch(fs);
    b.delete(doc(fs, "logs", logId(dzuhur())));
    b.set(doc(fs, "logs", logId(jumat())), log(jumat()));
    await assertSucceeds(b.commit());
  });

  test("menulis ulang slot yang sama (tidak -> dikerjakan) tetap boleh", async () => {
    await seed(`logs/${logId(jumat())}`, log(jumat({ status: "tidak", sumber_pencatatan: "dari_notifikasi" })));
    await assertSucceeds(tulisLog(jumat()));
  });

  test("catatan pengguna lain tidak mengganggu", async () => {
    await seed(`logs/${logId(dzuhur({ user_id: BOB }), BOB)}`, log(dzuhur({ user_id: BOB })));
    await assertSucceeds(tulisLog(jumat()));
  });

  test("Mode Cuti: udzur untuk jumat tetap mengikuti aturan sholat wajib", () =>
    assertSucceeds(tulisLog(jumat({ status: "udzur", jam_pencatatan: null, sumber_pencatatan: null,
      nama_detail_asli: null }))));
});

describe("logs: tanggal tidak boleh di masa depan", () => {
  // Tanggal kalender UTC, digeser n hari dari sekarang.
  const hari = (n) => new Date(Date.now() + n * 86400000).toISOString().slice(0, 10);

  test("hari ini, kemarin, dan besok (selisih zona waktu) diterima", async () => {
    await assertSucceeds(tulisLog({ tanggal: hari(0) }));
    await assertSucceeds(tulisLog({ tanggal: hari(-1) }));
    await assertSucceeds(tulisLog({ tanggal: hari(1) }));
  });

  test("lusa dan seterusnya ditolak", async () => {
    await assertFails(tulisLog({ tanggal: hari(2) }));
    await assertFails(tulisLog({ tanggal: "2099-01-01" }));
  });

  test("tanggal mustahil ditolak", () => assertFails(tulisLog({ tanggal: "2026-02-31" })));

  test("dokumen lama tidak bisa digeser ke masa depan lewat update", async () => {
    const lama = log({ tanggal: hari(0) });
    await seed(`logs/${logId(lama)}`, lama);
    await assertFails(setDoc(doc(db(ALICE), "logs", logId(lama)), { ...lama, tanggal: "2099-01-01" }));
  });
});

describe("admin hanya lewat token", () => {
  const adminDb = (claims) => env.authenticatedContext(ALICE, claims).firestore();

  test("role admin di meta/profile tidak memberi akses admin", async () => {
    await seed(`users/${ALICE}/meta/profile`, { role: "admin" });
    await assertFails(setDoc(doc(db(ALICE), "app_config/flags"), { x: 1 }));
    await assertFails(setDoc(doc(db(ALICE), "hadith_contents/h1"), { x: 1 }));
  });

  test("custom claim admin tetap bisa menulis konten", async () => {
    await assertSucceeds(setDoc(doc(adminDb({ admin: true }), "app_config/flags"), { x: 1 }));
    await assertSucceeds(setDoc(doc(adminDb({ role: "admin" }), "surah_verses/s1"), { x: 1 }));
  });
});

// Tiruan PERSIS tiap tulisan Firestore v1.1.1 (commit 636bd8f, versi di Play Store).
// Long Kotlin = integer; FieldValue.serverTimestamp() = serverTimestamp();
// SetOptions.merge() = { merge: true }; FieldValue.delete() = deleteField().
describe("regresi v1.1.1 (636bd8f): tulisan persis versi produksi", () => {
  const profilRef = (who = ALICE) => doc(db(who), `users/${ALICE}/meta/profile`);
  const sunnahRef = (id = "7b3f0c2e-1d4a-4c8e-9f21-0a1b2c3d4e5f") => doc(db(ALICE), `users/${ALICE}/sunnah_habits/${id}`);
  const completionRef = (id = `${TGL}_default_1`) => doc(db(ALICE), `users/${ALICE}/habit_completions/${id}`);
  const AWAL = Timestamp.fromDate(new Date("2026-09-01T00:00:00Z"));

  // AuthRepository.syncUserPreferences: ganti jenis kelamin, toggle Mode Cuti, dismissGenderPrompt.
  const syncUserPreferences = (gender, isHaidhMode, haidhDates) =>
    setDoc(profilRef(), { genderProfile: gender, isHaidhMode, haidhDates, updatedAt: serverTimestamp() }, { merge: true });

  // Dokumen profil khas pengguna v1.1.1 (tanpa rentangUsia / izinWali).
  const profilV111 = (o = {}) => ({
    name: "Fatimah", email: "fatimah@example.com", genderProfile: "FEMALE",
    isHaidhMode: false, haidhDates: [], updatedAt: AWAL, ...o,
  });

  // UserHabitRepository.syncUpsertToCloud: set() 9 field tanpa merge.
  const sunnahV111 = (o = {}) => ({
    name: "Sholat Dhuha", category: "SHOLAT", frequencyLabel: "Setiap hari", rakaat: 2,
    reminderEnabled: true, reminderTime: "08:00", completedDateKey: null,
    createdAt: 1790000000000, updatedAt: 1790000000000, ...o,
  });

  const completionV111 = (o = {}) => ({
    habitKey: "default_1", dateKey: TGL, category: "Sholat Fardhu", source: "DEFAULT",
    createdAt: 1790000000000, ...o,
  });

  test("login pertama: upsertProfileBasics (name, email, updatedAt) membuat meta/profile", () =>
    assertSucceeds(setDoc(profilRef(),
      { name: "Fatimah", email: "fatimah@example.com", updatedAt: serverTimestamp() }, { merge: true })));

  test("login berikutnya: upsertProfileBasics memperbarui dokumen yang ada", async () => {
    await seed(`users/${ALICE}/meta/profile`, profilV111());
    await assertSucceeds(setDoc(profilRef(),
      { name: "Fatimah Az", email: "fatimah@example.com", updatedAt: serverTimestamp() }, { merge: true }));
  });

  test("ganti nama: name + lastUsernameChangedAt (Long ms) + updatedAt", async () => {
    await seed(`users/${ALICE}/meta/profile`, profilV111());
    await assertSucceeds(setDoc(profilRef(),
      { name: "Nama Baru", lastUsernameChangedAt: 1793000000000, updatedAt: serverTimestamp() }, { merge: true }));
  });

  test("unggah avatar lalu hapus avatar (FieldValue.delete)", async () => {
    const { deleteField } = await import("firebase/firestore");
    await seed(`users/${ALICE}/meta/profile`, profilV111());
    await assertSucceeds(setDoc(profilRef(),
      { avatarBase64: "iVBORw0KGgo" + "A".repeat(5000), updatedAt: serverTimestamp() }, { merge: true }));
    await assertSucceeds(setDoc(profilRef(),
      { avatarBase64: deleteField(), updatedAt: serverTimestamp() }, { merge: true }));
  });

  test("toggle Mode Cuti beserta tanggalnya (nyala lalu mati)", async () => {
    await seed(`users/${ALICE}/meta/profile`, profilV111());
    await assertSucceeds(syncUserPreferences("FEMALE", true, ["2026-11-01", "2026-11-02"]));
    await assertSucceeds(syncUserPreferences("FEMALE", false, ["2026-11-01", "2026-11-02", "2026-11-03"]));
  });

  test("ganti jenis kelamin di Pengaturan, lalu Mode Cuti tetap tersinkron", async () => {
    await seed(`users/${ALICE}/meta/profile`, profilV111({ genderProfile: "MALE" }));
    await assertSucceeds(syncUserPreferences("FEMALE", false, []));
    await assertSucceeds(syncUserPreferences("FEMALE", true, ["2026-11-04"]));
  });

  test("dismissGenderPrompt mengirim jenis kelamin lokal (bisa UNSPECIFIED) di atas yang tersimpan", async () => {
    await seed(`users/${ALICE}/meta/profile`, profilV111({ genderProfile: "MALE" }));
    await assertSucceeds(syncUserPreferences("UNSPECIFIED", false, []));
  });

  test("dismissGenderPrompt sebelum dokumen ada (membuat dokumen)", () =>
    assertSucceeds(syncUserPreferences("UNSPECIFIED", false, [])));

  test("sunnah_habits: upsert 9 field persis (termasuk null)", async () => {
    await assertSucceeds(setDoc(sunnahRef(), sunnahV111()));
    await assertSucceeds(setDoc(sunnahRef(), sunnahV111({ rakaat: null, reminderEnabled: false, reminderTime: null,
      completedDateKey: TGL, updatedAt: 1793000000000 })));
    await assertSucceeds(setDoc(sunnahRef("p1"), sunnahV111({ name: "Puasa Senin Kamis", category: "PUASA",
      frequencyLabel: "Hari: Sen • Kam", rakaat: null })));
  });

  test("habit_completions: DEFAULT dan SUNNAH", async () => {
    await assertSucceeds(setDoc(completionRef(), completionV111({ createdAt: Date.now() })));
    await assertSucceeds(setDoc(completionRef(`${TGL}_7b3f0c2e`), completionV111({
      habitKey: "7b3f0c2e", category: "SHOLAT", source: "SUNNAH", createdAt: Date.now() })));
  });

  test("baca: fetchUserPreferences, sunnah & completion (restore saat login)", async () => {
    await seed(`users/${ALICE}/meta/profile`, profilV111());
    await assertSucceeds(getDoc(profilRef()));
    await assertSucceeds(getDocs(collection(db(ALICE), `users/${ALICE}/sunnah_habits`)));
    await assertSucceeds(getDocs(collection(db(ALICE), `users/${ALICE}/habit_completions`)));
  });

  test("hapus habit sunnah & batal centang (delete dokumen)", async () => {
    await seed(`users/${ALICE}/sunnah_habits/s1`, sunnahV111());
    await seed(`users/${ALICE}/habit_completions/${TGL}_default_1`, completionV111());
    await assertSucceeds(deleteDoc(sunnahRef("s1")));
    await assertSucceeds(deleteDoc(completionRef()));
  });

  test("hapus akun: batch delete sunnah + completion, lalu delete meta/profile", async () => {
    const { writeBatch } = await import("firebase/firestore");
    await seed(`users/${ALICE}/sunnah_habits/s1`, sunnahV111());
    await seed(`users/${ALICE}/habit_completions/${TGL}_default_1`, completionV111());
    await seed(`users/${ALICE}/meta/profile`, profilV111());
    const fs = db(ALICE); // batch & referensinya harus dari instance yang sama
    const b = writeBatch(fs);
    b.delete(doc(fs, `users/${ALICE}/sunnah_habits/s1`));
    b.delete(doc(fs, `users/${ALICE}/habit_completions/${TGL}_default_1`));
    await assertSucceeds(b.commit());
    await assertSucceeds(deleteDoc(profilRef()));
  });

  test("hapus akun gagal: pemulihan dari cadangan (null dibuang, updatedAt jadi Timestamp)", async () => {
    const { writeBatch } = await import("firebase/firestore");
    // Cadangan v1.1.1 = doc.data() dengan nilai null dibuang; batch.set tanpa merge.
    const cadanganSunnah = sunnahV111();
    for (const k of Object.keys(cadanganSunnah)) if (cadanganSunnah[k] === null) delete cadanganSunnah[k];
    const cadanganProfil = profilV111({ avatarBase64: "iVBORw0KGgo", lastUsernameChangedAt: 1793000000000,
      haidhDates: ["2026-10-20"], isHaidhMode: true });
    const fs = db(ALICE); // batch & referensinya harus dari instance yang sama
    const b = writeBatch(fs);
    b.set(doc(fs, `users/${ALICE}/sunnah_habits/s1`), cadanganSunnah);
    b.set(doc(fs, `users/${ALICE}/habit_completions/${TGL}_default_1`), completionV111());
    await assertSucceeds(b.commit());
    // Profil dipulihkan dengan set(profileData, merge).
    await assertSucceeds(setDoc(profilRef(), cadanganProfil, { merge: true }));
  });

  test("profil dari versi lebih lama (avatarUrl) tetap bisa diperbarui v1.1.1", async () => {
    await seed(`users/${ALICE}/meta/profile`, profilV111({ avatarUrl: "https://x/y.png" }));
    await assertSucceeds(syncUserPreferences("FEMALE", true, ["2026-11-04"]));
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
