import os
import re

file_path = r"D:\Projeck APK\app\src\main\java\com\islami\Aha\ui\settings\SettingsScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (r'"Silakan izinkan Aha agar dikecualikan dari penghematan baterai."', r'context.getString(R.string.settings_battery_opt_prompt)'),
    (r'"Baterai sudah optimal! \(Jika masih telat, silakan cari opsi Mulai Otomatis di pengaturan HP\)."', r'context.getString(R.string.settings_battery_already_optimal)'),
    (r'"Silakan izinkan Aha untuk Mulai Otomatis \(AutoStart\)."', r'context.getString(R.string.settings_autostart_prompt)'),
    (r'"Laki-laki \(Jadwal Jumat Aktif\)"', r'stringResource(R.string.settings_gender_male_active)'),
    (r'"Perempuan \(Mode Cuti Aktif\)"', r'stringResource(R.string.settings_gender_female_active)'),
    (r'"Belum Diatur"', r'stringResource(R.string.settings_gender_not_set)'),
    (r'title = "Profil Ibadah"', r'title = stringResource(R.string.settings_gender_title)'),
    (r'"Matikan Mode Cuti Ibadah terlebih dahulu untuk mengubah profil."', r'context.getString(R.string.settings_gender_haidh_warning)'),
    (r'title = "Perbaiki Notifikasi \(HP China\)"', r'title = stringResource(R.string.settings_fix_notif_title)'),
    (r'subtitle = "Izin Baterai & Mulai Otomatis agar notifikasi tidak mati"', r'subtitle = stringResource(R.string.settings_fix_notif_subtitle)'),
    (r'text = "Profil Ibadah"', r'text = stringResource(R.string.settings_gender_title)'),
    (r'"Profil perempuan akan mengaktifkan fitur Mode Cuti \(Haidh\)."', r'stringResource(R.string.settings_gender_desc_female)'),
    (r'"Profil laki-laki akan mengaktifkan penyesuaian jadwal Salat Jumat."', r'stringResource(R.string.settings_gender_desc_male)'),
    (r'"Pilih profil Anda untuk menyesuaikan otomatis jadwal ibadah."', r'stringResource(R.string.settings_gender_desc_default)'),
    (r'contentDescription = "Laki-laki"', r'contentDescription = stringResource(R.string.settings_gender_male)'),
    (r'text = "Laki-laki"', r'text = stringResource(R.string.settings_gender_male)'),
    (r'contentDescription = "Perempuan"', r'contentDescription = stringResource(R.string.settings_gender_female)'),
    (r'text = "Perempuan"', r'text = stringResource(R.string.settings_gender_female)'),
    (r'Text\("Batal"', r'Text(stringResource(R.string.settings_cancel)')
]

for old, new in replacements:
    content = re.sub(old, new, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("SettingsScreen.kt updated!")
