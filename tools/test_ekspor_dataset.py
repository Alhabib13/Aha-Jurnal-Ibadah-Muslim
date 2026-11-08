"""Tes penyaringan ekspor dataset (tanpa server). Jalankan: python -m unittest tools/test_ekspor_dataset.py"""
import csv
import datetime
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ekspor_dataset as e  # noqa: E402

SETUJU = {"disetujui_pada": "2026-11-01T00:00:00Z", "jenis_kelamin": "laki_laki"}


def log(uid, tanggal="2026-11-02", **f):
    return {"_id": f"{uid}_{tanggal}_x", "user_id": uid, "tanggal": tanggal, "jenis_ibadah": "sholat_wajib",
            "status": "dikerjakan", **f}


class SaringTest(unittest.TestCase):
    def setUp(self):
        self.users = {
            "ok": dict(SETUJU, kode_responden="R-01"),
            "tanpaSetuju": {"jenis_kelamin": "perempuan"},
            "setujuNull": dict(SETUJU, disetujui_pada=None),
            "ditarik": dict(SETUJU, ditarik_pada="2026-11-05T00:00:00Z"),
            "uji": dict(SETUJU),
            "okTanpaLog": dict(SETUJU),
        }
        self.logs = [log("ok"), log("ok", "2026-11-03"), log("tanpaSetuju"), log("setujuNull"),
                     log("ditarik"), log("ditarik", "2026-11-04"), log("uji"), log("yatim"),
                     {"_id": "rusak", "tanggal": "2026-11-02"}]
        self.hasil = e.saring(self.logs, self.users, dikecualikan={"uji": "akun uji"})

    def test_hanya_yang_setuju_dan_tidak_ditarik_yang_lolos(self):
        log_lolos, lolos, *_ = self.hasil
        self.assertEqual({"ok", "okTanpaLog"}, lolos)
        self.assertEqual(2, len(log_lolos))
        self.assertTrue(all(l["user_id"] == "ok" for l in log_lolos))

    def test_alasan_penolakan(self):
        _, _, ditolak, log_ditolak, tanpa_uid = self.hasil
        self.assertEqual({
            "tanpaSetuju": e.ALASAN_TANPA_SETUJU,
            "setujuNull": e.ALASAN_TANPA_SETUJU,
            "ditarik": e.ALASAN_DITARIK,
            "uji": e.ALASAN_DIKECUALIKAN,
            "yatim": e.ALASAN_TANPA_USERS,
        }, ditolak)
        self.assertEqual(2, log_ditolak[e.ALASAN_DITARIK])
        self.assertEqual(2, log_ditolak[e.ALASAN_TANPA_SETUJU])
        self.assertEqual(1, tanpa_uid)

    def test_pengecualian_menang_atas_persetujuan(self):
        self.assertEqual(e.ALASAN_DIKECUALIKAN, e.alasan_ditolak("uji", dict(SETUJU), {"uji": "x"}))

    def test_daftar_bawaan_memuat_kedua_akun_peneliti(self):
        self.assertTrue(any(u.startswith("CGaucK") for u in e.AKUN_DIKECUALIKAN))
        self.assertTrue(any(u.startswith("wFx2J0") for u in e.AKUN_DIKECUALIKAN))
        self.assertEqual(e.ALASAN_DIKECUALIKAN, e.alasan_ditolak("CGaucK5NM1W8rfpSUjn4h5Q3YSv1", dict(SETUJU)))

    def test_konversi_nilai_rest(self):
        self.assertEqual(5, e.nilai({"integerValue": "5"}))
        self.assertIsNone(e.nilai({"nullValue": None}))
        self.assertEqual({"a": ["x"]}, e.nilai({"mapValue": {"fields": {"a": {"arrayValue": {"values": [{"stringValue": "x"}]}}}}}))

    def test_csv_dan_ringkasan(self):
        log_lolos, lolos, ditolak, log_ditolak, tanpa_uid = self.hasil
        with tempfile.TemporaryDirectory() as d:
            e.tulis_csv(os.path.join(d, "logs.csv"), log_lolos)
            with open(os.path.join(d, "logs.csv"), encoding="utf-8-sig") as f:
                rows = list(csv.DictReader(f))
            self.assertEqual(["ok", "ok"], [r["user_id"] for r in rows])
            self.assertEqual("", rows[0]["jumlah"])
            e.tulis_ringkasan(os.path.join(d, "r.txt"), "t", len(self.logs), log_lolos, lolos, ditolak,
                              log_ditolak, tanpa_uid, self.users, {"uji": "akun uji"})
            with open(os.path.join(d, "r.txt"), encoding="utf-8") as f:
                teks = f.read()
            self.assertIn("Responden lolos          : 2", teks)
            self.assertIn("Responden ditolak        : 5", teks)
            self.assertIn("R-01", teks)


def sholat(uid, tanggal, detail, status="dikerjakan", **f):
    return {"_id": f"{uid}_{tanggal}_sholat_wajib_{detail}", "user_id": uid, "tanggal": tanggal,
            "jenis_ibadah": "sholat_wajib", "nama_detail": detail, "status": status, "offset_waktu": "+07:00", **f}


class InferensiTest(unittest.TestCase):
    def detail(self, hasil, uid, tanggal, jenis="sholat_wajib"):
        return sorted(h["nama_detail"] for h in hasil if h["user_id"] == uid and h["tanggal"] == tanggal and h["jenis_ibadah"] == jenis)

    def test_hanya_hari_yang_punya_catatan_dan_slot_kosong(self):
        logs = [sholat("p", "2026-11-03", "subuh"), sholat("p", "2026-11-03", "ashar", "tidak")]
        hasil = e.inferensi_tidak(logs, {"p": {"jenis_kelamin": "perempuan"}})
        self.assertEqual(["dzuhur", "isya", "maghrib"], self.detail(hasil, "p", "2026-11-03"))
        self.assertTrue(all(h["status"] == "tidak" and h["asal"] == "inferensi" for h in hasil))
        # Hari tanpa catatan apa pun tidak disimpulkan.
        self.assertEqual([], self.detail(hasil, "p", "2026-11-04"))

    def test_hari_mode_cuti_tidak_disimpulkan(self):
        logs = [sholat("p", "2026-11-03", d, "udzur") for d in ["subuh", "dzuhur", "ashar", "maghrib", "isya"]]
        self.assertEqual([], e.inferensi_tidak(logs, {"p": {"jenis_kelamin": "perempuan"}}))

    def test_tengah_hari_jumat_untuk_laki_laki(self):
        jumat = "2026-11-06"
        hasil = e.inferensi_tidak([sholat("l", jumat, "subuh")], {"l": {"jenis_kelamin": "laki_laki"}})
        self.assertIn("jumat", self.detail(hasil, "l", jumat))
        self.assertNotIn("dzuhur", self.detail(hasil, "l", jumat))
        # Sudah Ganti Dzuhur: tengah hari dianggap terisi.
        hasil = e.inferensi_tidak([sholat("l", jumat, "subuh"), sholat("l", jumat, "dzuhur")], {"l": {"jenis_kelamin": "laki_laki"}})
        self.assertEqual(["ashar", "isya", "maghrib"], self.detail(hasil, "l", jumat))

    def test_dzikir_berjendela_sejak_habit_dibuat(self):
        dibuat = int(datetime.datetime(2026, 11, 3, 12, tzinfo=datetime.timezone(datetime.timedelta(hours=7))).timestamp() * 1000)
        habits = {"p": [{"id": "h1", "slug": "dzikir_pagi", "waktu_dzikir": "pagi", "dibuat_pada": dibuat},
                        {"id": "h2", "slug": "istighfar", "waktu_dzikir": "bebas", "dibuat_pada": dibuat}]}
        logs = [sholat("p", d, "subuh") for d in ["2026-11-02", "2026-11-03", "2026-11-04"]]
        logs.append({"_id": "p_2026-11-04_dzikir_dzikir_pagi_h1_1791600000000", "user_id": "p", "tanggal": "2026-11-04",
                     "jenis_ibadah": "dzikir", "nama_detail": "dzikir_pagi", "status": "dikerjakan"})
        hasil = e.inferensi_tidak(logs, {"p": {}}, habits)
        self.assertEqual([], self.detail(hasil, "p", "2026-11-02", "dzikir"))            # sebelum habit dibuat
        self.assertEqual(["dzikir_pagi"], self.detail(hasil, "p", "2026-11-03", "dzikir"))  # habit ada, tak dicatat
        self.assertEqual([], self.detail(hasil, "p", "2026-11-04", "dzikir"))            # sudah dicatat
        self.assertTrue(all(h["nama_detail"] != "istighfar" for h in hasil))             # waktu bebas


class RingkasanTambahanTest(unittest.TestCase):
    def test_kelengkapan_selisih(self):
        logs = [sholat("a", "2026-11-03", "subuh", selisih_menit_dari_adzan=5),
                sholat("a", "2026-11-03", "dzuhur", selisih_menit_dari_adzan=None),
                sholat("a", "2026-11-03", "ashar", "tidak")]
        self.assertEqual({"a": (1, 2)}, e.kelengkapan_selisih(logs))

    def test_kode_ganda(self):
        users = {"a": {"kode_responden": "R-01"}, "b": {"kode_responden": "r-01"}, "c": {"kode_responden": "R-02"}}
        self.assertEqual({"R-01": ["a", "b"]}, e.kode_ganda({"a", "b", "c"}, users))


if __name__ == "__main__":
    unittest.main()
