import os
from PIL import Image

input_dir = r"D:\Projeck APK\icon"
output_dir = r"D:\Projeck APK\app\src\main\res\drawable"

def process_image(filename):
    if not (filename.lower().endswith(('.png', '.jpg', '.jpeg', '.webp'))):
        return

    filepath = os.path.join(input_dir, filename)
    img = Image.open(filepath).convert("RGBA")
    
    datas = img.getdata()
    newData = []
    
    # Toleransi warna putih (untuk menghilangkan background putih/terang)
    # Background biasanya sangat terang (R>230, G>230, B>230)
    for item in datas:
        if item[0] > 230 and item[1] > 230 and item[2] > 230:
            newData.append((255, 255, 255, 0)) # Transparan
        else:
            # Jadikan pixel lainnya hitam solid (atau grayscale gelap) agar mudah di-tint oleh Compose
            # Bisa juga dibiarkan aslinya, tapi kalau mau dipakai sebagai Icon tintable, lebih baik hitam pekat
            newData.append((0, 0, 0, 255))
            
    img.putdata(newData)
    
    # Sanitize filename
    name, _ = os.path.splitext(filename)
    # replace spaces and non-alphanumeric
    clean_name = "".join([c if c.isalnum() else "_" for c in name]).lower()
    # hapus awalan angka jika ada (Android resource tidak boleh diawali angka)
    if clean_name and clean_name[0].isdigit():
        clean_name = "ach_" + clean_name
        
    out_filename = clean_name + ".png"
    out_path = os.path.join(output_dir, out_filename)
    
    img.save(out_path, "PNG")
    print(f"Saved: {out_filename}")

for f in os.listdir(input_dir):
    process_image(f)

print("Done processing icons.")
