import xml.etree.ElementTree as ET

strings_xml_path = r"D:\Projeck APK\app\src\main\res\values\strings.xml"

# Parse the xml file
tree = ET.parse(strings_xml_path)
root = tree.getroot()

# The strings to add
new_strings = {
    "settings_battery_opt_prompt": "Silakan izinkan Aha agar dikecualikan dari penghematan baterai.",
    "settings_battery_already_optimal": "Baterai sudah optimal! (Jika masih telat, silakan cari opsi Mulai Otomatis di pengaturan HP).",
    "settings_autostart_prompt": "Silakan izinkan Aha untuk Mulai Otomatis (AutoStart).",
    "settings_gender_male_active": "Laki-laki (Jadwal Jumat Aktif)",
    "settings_gender_female_active": "Perempuan (Mode Cuti Aktif)",
    "settings_gender_not_set": "Belum Diatur",
    "settings_gender_title": "Profil Ibadah",
    "settings_gender_haidh_warning": "Matikan Mode Cuti Ibadah terlebih dahulu untuk mengubah profil.",
    "settings_fix_notif_title": "Perbaiki Notifikasi (HP China)",
    "settings_fix_notif_subtitle": "Izin Baterai & Mulai Otomatis agar notifikasi tidak mati",
    "settings_gender_desc_female": "Profil perempuan akan mengaktifkan fitur Mode Cuti (Haidh).",
    "settings_gender_desc_male": "Profil laki-laki akan mengaktifkan penyesuaian jadwal Salat Jumat.",
    "settings_gender_desc_default": "Pilih profil Anda untuk menyesuaikan otomatis jadwal ibadah.",
    "settings_gender_male": "Laki-laki",
    "settings_gender_female": "Perempuan",
    "settings_cancel": "Batal"
}

# Check if they exist first to avoid duplicates
existing_keys = [child.attrib.get('name') for child in root if child.tag == 'string']

added = False
for key, value in new_strings.items():
    if key not in existing_keys:
        new_elem = ET.Element('string', {'name': key})
        new_elem.text = value
        root.append(new_elem)
        added = True

if added:
    ET.indent(tree, space="    ", level=0)
    tree.write(strings_xml_path, encoding="utf-8", xml_declaration=True)
    print("Added new strings successfully.")
else:
    print("Strings already exist.")
