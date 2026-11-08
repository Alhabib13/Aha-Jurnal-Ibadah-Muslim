#!/usr/bin/env python3
"""
Ekspor dataset penelitian dari Firestore (HANYA MEMBACA; tidak menulis apa pun ke server).

Menarik semua dokumen `logs`, lalu hanya menyimpan log milik responden yang:
  1. punya dokumen users/{uid},
  2. dokumen itu berisi disetujui_pada (menyetujui di layar persetujuan),
  3. dokumen itu TIDAK berisi ditarik_pada (belum berhenti ikut), dan
  4. tidak ada di AKUN_DIKECUALIKAN (akun uji / akun peneliti).

Keluaran (di folder ekspor/, diabaikan git karena berisi data responden):
  logs.csv                   log yang lolos, satu baris per dokumen (apa adanya dari server)
  logs_dengan_inferensi.csv  logs.csv + baris "tidak" hasil penyimpulan (kolom asal), lihat
                             inferensi_tidak(): keputusan B dan 4.4
  ringkasan.txt              responden lolos/ditolak beserta alasannya, kelengkapan
                             selisih_menit_dari_adzan, dan kode responden yang dipakai >1 ID

Autentikasi: token akses Google dari variabel lingkungan AHA_ACCESS_TOKEN bila ada;
selain itu memakai login Firebase CLI di komputer ini (`firebase login`).

Pemakaian:
  python tools/ekspor_dataset.py                 # ke ekspor/dataset_<waktu>/
  python tools/ekspor_dataset.py --keluar D:/x   # ke folder lain
"""
import argparse
import csv
import datetime
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

PROJECT = "aha-jurnal-ibadah-muslim"

# Akun yang TIDAK boleh masuk dataset (akun uji, akun peneliti). Ubah di sini saja.
AKUN_DIKECUALIKAN = {
    "CGaucK5NM1W8rfpSUjn4h5Q3YSv1": "akun uji penelitian (khabibhusein14)",
    "wFx2J0kZoIfNjWwbJF0pZx2I5qw2": "HP peneliti (Xiaomi, akun tanpa login)",
    "XeBUPqWyjEWS40m6BHLstKZYBxV2": "HP uji Mi 10T Pro (akun tanpa login, 8 Nov 2026)",
}

# Urutan kolom CSV = field `logs` (ResearchSchema.Log). Field tak dikenal ditambahkan di belakang.
KOLOM_LOG = [
    "user_id", "tanggal", "jenis_ibadah", "nama_detail", "nama_detail_asli", "status",
    "jumlah", "jilid", "jam_pencatatan", "sumber_pencatatan", "jumlah_ayat", "juz",
    "offset_waktu", "selisih_menit_dari_adzan", "mode_pencatatan", "waktu_dzikir",
    "target_hitungan", "durasi_detik",
]

ALASAN_DIKECUALIKAN = "dikecualikan (akun uji/peneliti)"
ALASAN_TANPA_USERS = "tidak ada users/{uid}"
ALASAN_TANPA_SETUJU = "users/{uid} tanpa disetujui_pada"
ALASAN_DITARIK = "berhenti ikut (ditarik_pada terisi)"


# ---------- logika penyaringan (murni, diuji di tools/test_ekspor_dataset.py) ----------

def alasan_ditolak(uid, profil, dikecualikan=None):
    """None bila responden lolos; selain itu alasan penolakannya. `profil` = isi users/{uid} atau None."""
    dikecualikan = AKUN_DIKECUALIKAN if dikecualikan is None else dikecualikan
    if uid in dikecualikan:
        return ALASAN_DIKECUALIKAN
    if profil is None:
        return ALASAN_TANPA_USERS
    if profil.get("disetujui_pada") is None:
        return ALASAN_TANPA_SETUJU
    if "ditarik_pada" in profil:
        return ALASAN_DITARIK
    return None


def saring(logs, users, dikecualikan=None):
    """
    logs:  daftar dict field log (beserta "_id").
    users: dict uid -> isi users/{uid} (hanya dokumen yang ada).
    Hasil: (log_lolos, responden_lolos, ditolak{uid: alasan}, log_ditolak_per_alasan, log_tanpa_user_id)
    Responden = setiap uid yang punya log atau punya users/{uid}.
    """
    kandidat = {l.get("user_id") for l in logs if l.get("user_id")} | set(users)
    ditolak, lolos = {}, set()
    for uid in kandidat:
        alasan = alasan_ditolak(uid, users.get(uid), dikecualikan)
        if alasan is None:
            lolos.add(uid)
        else:
            ditolak[uid] = alasan
    log_lolos, log_ditolak, tanpa_uid = [], {}, 0
    for l in logs:
        uid = l.get("user_id")
        if not uid:
            tanpa_uid += 1
        elif uid in lolos:
            log_lolos.append(l)
        else:
            log_ditolak[ditolak[uid]] = log_ditolak.get(ditolak[uid], 0) + 1
    log_lolos.sort(key=lambda l: (l.get("user_id", ""), l.get("tanggal", ""), str(l.get("jam_pencatatan", ""))))
    return log_lolos, lolos, ditolak, log_ditolak, tanpa_uid


def nilai(v):
    """Nilai REST Firestore -> nilai Python biasa."""
    jenis, x = next(iter(v.items()))
    if jenis == "mapValue":
        return {k: nilai(b) for k, b in x.get("fields", {}).items()}
    if jenis == "arrayValue":
        return [nilai(i) for i in x.get("values", [])]
    if jenis == "nullValue":
        return None
    if jenis == "integerValue":
        return int(x)
    if jenis in ("doubleValue", "booleanValue", "stringValue", "timestampValue"):
        return x
    return json.dumps(x)


# ---------- penyimpulan status "tidak" (keputusan B & 4.4) ----------

SLOT_SHOLAT = ["subuh", "tengah_hari", "ashar", "maghrib", "isya"]
WAKTU_DZIKIR_BERJENDELA = {"bada_sholat", "pagi", "petang", "sebelum_tidur"}


def _tz(offset):
    """'+07:00' -> datetime.timezone; tidak terbaca = WIB."""
    try:
        tanda = -1 if offset[0] == "-" else 1
        j, m = offset[1:].split(":")
        return datetime.timezone(tanda * datetime.timedelta(hours=int(j), minutes=int(m)))
    except Exception:
        return datetime.timezone(datetime.timedelta(hours=7))


def inferensi_tidak(log_lolos, users, dzikir_habits=None):
    """
    Baris "tidak" yang disimpulkan, TIDAK pernah ditulis aplikasi:
      - hanya pada hari yang punya >= 1 catatan apa pun dari responden itu (keputusan B);
      - sholat wajib: 5 slot; slot tengah hari = jumat bagi laki-laki di hari Jumat, selain itu
        dzuhur; slot yang sudah punya baris (dikerjakan / tidak / udzur) dilewati, jadi hari
        Mode Cuti tidak pernah disimpulkan "tidak";
      - dzikir: hanya habit dzikir buatan pengguna yang waktunya berjendela (bukan "bebas"),
        mulai hari habit itu dibuat (keputusan 4.4).
    dzikir_habits: {uid: [ {id, slug, waktu_dzikir, dibuat_pada(ms)} ]}.
    Hasil: daftar dict log dengan asal="inferensi", jam & sumber kosong.
    """
    dzikir_habits = dzikir_habits or {}
    per_hari = {}
    for l in log_lolos:
        per_hari.setdefault((l["user_id"], l.get("tanggal")), []).append(l)
    hasil = []
    for (uid, tanggal), baris in sorted(per_hari.items()):
        if not tanggal:
            continue
        hari = datetime.date.fromisoformat(tanggal)
        ada = {(b.get("jenis_ibadah"), b.get("nama_detail")) for b in baris}
        laki = users.get(uid, {}).get("jenis_kelamin") == "laki_laki"

        def baru(jenis, detail, id_akhir=None, **lain):
            id_log = f"inferensi:{uid}_{tanggal}_{jenis}_{detail}" + (f"_{id_akhir}" if id_akhir else "")
            return {"_id": id_log, "user_id": uid, "tanggal": tanggal, "jenis_ibadah": jenis,
                    "nama_detail": detail, "status": "tidak", "asal": "inferensi", **lain}

        for slot in SLOT_SHOLAT:
            if slot == "tengah_hari":
                if ("sholat_wajib", "dzuhur") in ada or ("sholat_wajib", "jumat") in ada:
                    continue
                detail = "jumat" if (laki and hari.weekday() == 4) else "dzuhur"
            else:
                if ("sholat_wajib", slot) in ada:
                    continue
                detail = slot
            hasil.append(baru("sholat_wajib", detail))

        tz = _tz(next((b.get("offset_waktu") for b in baris if b.get("offset_waktu")), "+07:00"))
        for h in dzikir_habits.get(uid, []):
            if h.get("waktu_dzikir") not in WAKTU_DZIKIR_BERJENDELA:
                continue
            try:
                dibuat = datetime.datetime.fromtimestamp(int(h["dibuat_pada"]) / 1000, tz).date()
            except Exception:
                continue
            if hari < dibuat:
                continue
            penanda = f"_dzikir_{h.get('slug')}_{h.get('id')}_"
            if any(penanda in str(b.get("_id", "")) for b in baris):
                continue
            hasil.append(baru("dzikir", h.get("slug"), h.get("id"), waktu_dzikir=h.get("waktu_dzikir")))
    return hasil


def kelengkapan_selisih(log_lolos):
    """{uid: (jumlah_null, jumlah_relevan)} untuk sholat wajib & dzikir pagi/petang yang dikerjakan."""
    hasil = {}
    for l in log_lolos:
        relevan = l.get("status") == "dikerjakan" and (
            l.get("jenis_ibadah") == "sholat_wajib"
            or (l.get("jenis_ibadah") == "dzikir" and l.get("nama_detail") in ("dzikir_pagi", "dzikir_petang")))
        if not relevan:
            continue
        n, t = hasil.get(l["user_id"], (0, 0))
        hasil[l["user_id"]] = (n + (l.get("selisih_menit_dari_adzan") is None), t + 1)
    return hasil


def kode_ganda(lolos, users):
    """{kode_responden: [uid, ...]} untuk kode yang dipakai lebih dari satu ID (mis. instal ulang)."""
    per_kode = {}
    for u in lolos:
        kode = users.get(u, {}).get("kode_responden")
        if kode:
            per_kode.setdefault(kode.upper(), []).append(u)
    return {k: sorted(v) for k, v in per_kode.items() if len(v) > 1}


# ---------- akses Firestore (baca saja) ----------

def token_akses():
    if os.environ.get("AHA_ACCESS_TOKEN"):
        return os.environ["AHA_ACCESS_TOKEN"]
    cfg_path = os.path.expanduser("~/.config/configstore/firebase-tools.json")
    try:
        cfg = json.load(open(cfg_path, encoding="utf-8"))
        refresh = cfg["tokens"]["refresh_token"]
    except (OSError, KeyError):
        sys.exit("Belum login. Jalankan `firebase login` atau isi AHA_ACCESS_TOKEN.")
    # Klien OAuth publik milik Firebase CLI (sama dengan yang dipakai `firebase` sendiri).
    body = urllib.parse.urlencode({
        "refresh_token": refresh,
        "client_id": "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com",
        "client_secret": "j9iVZfS8kkCEFUPaAeJV0sAi",
        "grant_type": "refresh_token",
    }).encode()
    return json.load(urllib.request.urlopen("https://oauth2.googleapis.com/token", body))["access_token"]


def ambil_koleksi(koleksi, token):
    """Semua dokumen satu koleksi (boleh subkoleksi, mis. users/{uid}/dzikir_habits), sebagai (id, field)."""
    dasar = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/{koleksi}"
    hasil, halaman = [], None
    while True:
        url = dasar + "?pageSize=300" + (f"&pageToken={urllib.parse.quote(halaman)}" if halaman else "")
        req = urllib.request.Request(url, headers={"Authorization": "Bearer " + token, "x-goog-user-project": PROJECT})
        try:
            data = json.load(urllib.request.urlopen(req))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return hasil
            sys.exit(f"Gagal membaca {koleksi}: HTTP {e.code} {e.read().decode()[:200]}")
        for d in data.get("documents", []):
            hasil.append((d["name"].rsplit("/", 1)[-1], {k: nilai(v) for k, v in d.get("fields", {}).items()}))
        halaman = data.get("nextPageToken")
        if not halaman:
            return hasil


# ---------- keluaran ----------

def tulis_csv(path, logs, dengan_asal=False):
    lain = sorted({k for l in logs for k in l} - set(KOLOM_LOG) - {"_id", "asal"})
    kolom = ["id_log"] + (["asal"] if dengan_asal else []) + KOLOM_LOG + lain
    with open(path, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(kolom)
        for l in logs:
            baris = dict(l)
            if dengan_asal:
                baris.setdefault("asal", "tercatat")
            w.writerow([l.get("_id")] + ["" if baris.get(k) is None else baris.get(k) for k in kolom[1:]])


def tulis_ringkasan(path, waktu, total_log, log_lolos, lolos, ditolak, log_ditolak, tanpa_uid, users, dikecualikan,
                    inferensi=()):
    per_alasan = {}
    for uid, alasan in ditolak.items():
        per_alasan.setdefault(alasan, []).append(uid)
    log_per_uid = {}
    for l in log_lolos:
        log_per_uid[l["user_id"]] = log_per_uid.get(l["user_id"], 0) + 1
    baris = [
        f"Ekspor dataset Aha — {waktu} (proyek {PROJECT})",
        "",
        f"Log di server            : {total_log}",
        f"Log masuk dataset        : {len(log_lolos)}",
        f"Log tanpa user_id        : {tanpa_uid}",
        f"Baris 'tidak' disimpulkan: {len(inferensi)} (hanya di logs_dengan_inferensi.csv)",
        "",
        f"Responden lolos          : {len(lolos)}",
        f"  di antaranya tanpa log : {sum(1 for u in lolos if u not in log_per_uid)}",
        f"Responden ditolak        : {len(ditolak)}",
    ]
    for alasan in [ALASAN_DIKECUALIKAN, ALASAN_TANPA_USERS, ALASAN_TANPA_SETUJU, ALASAN_DITARIK]:
        uids = sorted(per_alasan.get(alasan, []))
        baris.append(f"  - {alasan}: {len(uids)} responden, {log_ditolak.get(alasan, 0)} log")
        for u in uids:
            ket = f" ({dikecualikan[u]})" if u in dikecualikan else ""
            baris.append(f"      {u}{ket}")
    baris += ["", "Responden lolos (uid, kode responden, jumlah log):"]
    for u in sorted(lolos):
        kode = users.get(u, {}).get("kode_responden") or "-"
        baris.append(f"  {u}  {kode}  {log_per_uid.get(u, 0)}")
    kel = kelengkapan_selisih(log_lolos)
    n_null = sum(n for n, _ in kel.values())
    n_all = sum(t for _, t in kel.values())
    baris += ["", "Kelengkapan selisih_menit_dari_adzan (sholat wajib & dzikir pagi/petang yang dikerjakan):",
              f"  total kosong: {n_null} dari {n_all}" + (f" ({100 * n_null / n_all:.0f}%)" if n_all else "")]
    for u in sorted(kel):
        n, t = kel[u]
        baris.append(f"  {u}  kosong {n}/{t}")
    ganda = kode_ganda(lolos, users)
    baris += ["", "Kode responden yang dipakai lebih dari satu ID (kemungkinan orang yang sama, mis. instal ulang):"]
    baris += [f"  {k}: {', '.join(v)}" for k, v in sorted(ganda.items())] or ["  (tidak ada)"]
    baris += ["", "Akun yang dikecualikan (AKUN_DIKECUALIKAN):"]
    baris += [f"  {u}  {k}" for u, k in dikecualikan.items()]
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(baris) + "\n")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--keluar", help="folder keluaran (bawaan: ekspor/dataset_<waktu>/ di akar proyek)")
    args = ap.parse_args()

    waktu = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    akar = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    keluar = args.keluar or os.path.join(akar, "ekspor", f"dataset_{waktu}")
    os.makedirs(keluar, exist_ok=True)

    token = token_akses()
    logs = [{"_id": i, **f} for i, f in ambil_koleksi("logs", token)]
    users = dict(ambil_koleksi("users", token))
    log_lolos, lolos, ditolak, log_ditolak, tanpa_uid = saring(logs, users)
    dzikir = {u: [{"id": i, **f} for i, f in ambil_koleksi(f"users/{u}/dzikir_habits", token)] for u in lolos}
    inferensi = inferensi_tidak(log_lolos, users, dzikir)

    tulis_csv(os.path.join(keluar, "logs.csv"), log_lolos)
    gabungan = sorted(log_lolos + inferensi, key=lambda l: (l.get("user_id", ""), l.get("tanggal", ""), l.get("_id", "")))
    tulis_csv(os.path.join(keluar, "logs_dengan_inferensi.csv"), gabungan, dengan_asal=True)
    tulis_ringkasan(os.path.join(keluar, "ringkasan.txt"), waktu, len(logs), log_lolos, lolos, ditolak,
                    log_ditolak, tanpa_uid, users, AKUN_DIKECUALIKAN, inferensi)
    print(f"{len(log_lolos)} log dari {len(lolos)} responden -> {keluar}")
    print(f"{len(ditolak)} responden ditolak; rincian di ringkasan.txt")


if __name__ == "__main__":
    main()
