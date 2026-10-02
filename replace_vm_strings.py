import re
import xml.etree.ElementTree as ET

strings_xml_path = r"D:\Projeck APK\app\src\main\res\values\strings.xml"
kt_path = r"D:\Projeck APK\app\src\main\java\com\islami\Aha\ui\settings\SettingsViewModel.kt"

replacements = {
    r'showSnackbar\("Profil ibadah berhasil diperbarui"\)': (
        'settings_snackbar_profile_updated', 'Profil ibadah berhasil diperbarui',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_profile_updated))'
    ),
    r'showSnackbar\("Pengingat global diaktifkan"\)': (
        'settings_snackbar_reminder_enabled', 'Pengingat global diaktifkan',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_reminder_enabled))'
    ),
    r'showSnackbar\("Pengingat global dimatikan"\)': (
        'settings_snackbar_reminder_disabled', 'Pengingat global dimatikan',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_reminder_disabled))'
    ),
    r'showSnackbar\("Suara notifikasi diperbarui"\)': (
        'settings_snackbar_sound_updated', 'Suara notifikasi diperbarui',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_sound_updated))'
    ),
    r'showSnackbar\("Silakan login untuk mengubah password"\)': (
        'settings_snackbar_login_required_password', 'Silakan login untuk mengubah password',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_login_required_password))'
    ),
    r'showSnackbar\("Password lama tidak boleh kosong"\)': (
        'settings_snackbar_old_password_empty', 'Password lama tidak boleh kosong',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_old_password_empty))'
    ),
    r'showSnackbar\("Password baru tidak boleh kosong"\)': (
        'settings_snackbar_new_password_empty', 'Password baru tidak boleh kosong',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_new_password_empty))'
    ),
    r'showSnackbar\("Password baru minimal 6 karakter"\)': (
        'settings_snackbar_password_min_length', 'Password baru minimal 6 karakter',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_password_min_length))'
    ),
    r'showSnackbar\("Password baru harus berbeda dari password lama"\)': (
        'settings_snackbar_password_must_differ', 'Password baru harus berbeda dari password lama',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_password_must_differ))'
    ),
    r'showSnackbar\("Konfirmasi password tidak cocok"\)': (
        'settings_snackbar_password_mismatch', 'Konfirmasi password tidak cocok',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_password_mismatch))'
    ),
    r'showSnackbar\("Password berhasil diubah\. Cek email untuk konfirmasi keamanan\."\)': (
        'settings_snackbar_password_changed', 'Password berhasil diubah. Cek email untuk konfirmasi keamanan.',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_password_changed))'
    ),
    r'showSnackbar\("Email akun tidak ditemukan"\)': (
        'settings_snackbar_email_not_found', 'Email akun tidak ditemukan',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_email_not_found))'
    ),
    r'showSnackbar\("Link reset password dikirim ke \$email"\)': (
        'settings_snackbar_reset_link_sent', 'Link reset password dikirim ke %1$s',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_reset_link_sent, email))'
    ),
    r'showSnackbar\("Silakan login untuk membuka keamanan akun"\)': (
        'settings_snackbar_login_required_security', 'Silakan login untuk membuka keamanan akun',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_login_required_security))'
    ),
    r'showSnackbar\("Tunggu \$cooldownSeconds detik sebelum kirim ulang verifikasi"\)': (
        'settings_snackbar_verify_cooldown', 'Tunggu %1$d detik sebelum kirim ulang verifikasi',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_verify_cooldown, cooldownSeconds))'
    ),
    r'showSnackbar\("Email verifikasi berhasil dikirim"\)': (
        'settings_snackbar_verify_email_sent', 'Email verifikasi berhasil dikirim',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_verify_email_sent))'
    ),
    r'showSnackbar\("Password salah atau gagal menghapus akun"\)': (
        'settings_snackbar_delete_account_failed', 'Password salah atau gagal menghapus akun',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_delete_account_failed))'
    ),
    r'showSnackbar\("Ekspor dibatalkan"\)': (
        'settings_snackbar_export_cancelled', 'Ekspor dibatalkan',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_export_cancelled))'
    ),
    r'showSnackbar\("Impor dibatalkan"\)': (
        'settings_snackbar_import_cancelled', 'Impor dibatalkan',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_import_cancelled))'
    ),
    r'showSnackbar\("Data berhasil diekspor"\)': (
        'settings_snackbar_export_success', 'Data berhasil diekspor',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_export_success))'
    ),
    r'showSnackbar\("File impor tidak ditemukan"\)': (
        'settings_snackbar_import_file_not_found', 'File impor tidak ditemukan',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_import_file_not_found))'
    ),
    r'showSnackbar\("Semua data telah direset"\)': (
        'settings_snackbar_data_reset_success', 'Semua data telah direset',
        r'showSnackbar(appContext.getString(R.string.settings_snackbar_data_reset_success))'
    ),
}

# 1. Update strings.xml
tree = ET.parse(strings_xml_path)
root = tree.getroot()
existing_keys = [child.attrib.get('name') for child in root if child.tag == 'string']

added = False
for pattern, (key, value, replace_with) in replacements.items():
    if key not in existing_keys:
        new_elem = ET.Element('string', {'name': key})
        # If value has formatting like %1$s, we can just assign it
        # ET will handle basic escaping
        new_elem.text = value
        root.append(new_elem)
        added = True

if added:
    ET.indent(tree, space="    ", level=0)
    tree.write(strings_xml_path, encoding="utf-8", xml_declaration=True)
    print("Added new strings to strings.xml")

# 2. Replace in SettingsViewModel.kt
with open(kt_path, 'r', encoding='utf-8') as f:
    content = f.read()

for pattern, (key, value, replace_with) in replacements.items():
    content = re.sub(pattern, replace_with, content)

# One more complex string to handle manually
complex_string_pattern = r'showSnackbar\(\s*"Impor selesai: \$\{parsed\.defaultHabits\.size\} habit, " \+\s*"\$\{parsed\.sunnahHabits\.size\} sunnah, \$\{parsed\.completionRecords\.size\} riwayat"\s*\)'
complex_key = 'settings_snackbar_import_success'
complex_val = 'Impor selesai: %1$d habit, %2$d sunnah, %3$d riwayat'
complex_replace = r'showSnackbar(appContext.getString(R.string.settings_snackbar_import_success, parsed.defaultHabits.size, parsed.sunnahHabits.size, parsed.completionRecords.size))'

if complex_key not in existing_keys:
    new_elem = ET.Element('string', {'name': complex_key})
    new_elem.text = complex_val
    root.append(new_elem)
    ET.indent(tree, space="    ", level=0)
    tree.write(strings_xml_path, encoding="utf-8", xml_declaration=True)

content = re.sub(complex_string_pattern, complex_replace, content)

with open(kt_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("SettingsViewModel.kt updated")
