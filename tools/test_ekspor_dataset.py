"""Tes penyaringan ekspor dataset (tanpa server). Jalankan: python -m unittest tools/test_ekspor_dataset.py"""
import csv
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


if __name__ == "__main__":
    unittest.main()
