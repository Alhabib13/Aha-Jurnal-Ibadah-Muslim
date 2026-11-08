#!/usr/bin/env python3
"""
Ekspor dataset penelitian dari Firestore (HANYA MEMBACA; tidak menulis apa pun ke server).

Menarik semua dokumen `logs`, lalu hanya menyimpan log milik responden yang:
  1. punya dokumen users/{uid},
  2. dokumen itu berisi disetujui_pada (menyetujui di layar persetujuan),
  3. dokumen itu TIDAK berisi ditarik_pada (belum berhenti ikut), dan
  4. tidak ada di AKUN_DIKECUALIKAN (akun uji / akun peneliti).

Keluaran (di folder ekspor/, diabaikan git karena berisi data responden):
  logs.csv       log yang lolos, satu baris per dokumen
  ringkasan.txt  jumlah responden lolos/ditolak beserta alasannya

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
    """Semua dokumen satu koleksi tingkat atas, sebagai daftar (id, field)."""
    dasar = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/{koleksi}"
    hasil, halaman = [], None
    while True:
        url = dasar + "?pageSize=300" + (f"&pageToken={urllib.parse.quote(halaman)}" if halaman else "")
        req = urllib.request.Request(url, headers={"Authorization": "Bearer " + token, "x-goog-user-project": PROJECT})
        try:
            data = json.load(urllib.request.urlopen(req))
        except urllib.error.HTTPError as e:
            sys.exit(f"Gagal membaca {koleksi}: HTTP {e.code} {e.read().decode()[:200]}")
        for d in data.get("documents", []):
            hasil.append((d["name"].rsplit("/", 1)[-1], {k: nilai(v) for k, v in d.get("fields", {}).items()}))
        halaman = data.get("nextPageToken")
        if not halaman:
            return hasil


# ---------- keluaran ----------

def tulis_csv(path, logs):
    lain = sorted({k for l in logs for k in l} - set(KOLOM_LOG) - {"_id"})
    kolom = ["id_log"] + KOLOM_LOG + lain
    with open(path, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(kolom)
        for l in logs:
            w.writerow([l.get("_id")] + ["" if l.get(k) is None else l.get(k) for k in kolom[1:]])


def tulis_ringkasan(path, waktu, total_log, log_lolos, lolos, ditolak, log_ditolak, tanpa_uid, users, dikecualikan):
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

    tulis_csv(os.path.join(keluar, "logs.csv"), log_lolos)
    tulis_ringkasan(os.path.join(keluar, "ringkasan.txt"), waktu, len(logs), log_lolos, lolos, ditolak,
                    log_ditolak, tanpa_uid, users, AKUN_DIKECUALIKAN)
    print(f"{len(log_lolos)} log dari {len(lolos)} responden -> {keluar}")
    print(f"{len(ditolak)} responden ditolak; rincian di ringkasan.txt")


if __name__ == "__main__":
    main()
