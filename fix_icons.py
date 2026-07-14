import os
from PIL import Image

input_dir = r"D:\Projeck APK\icon"
output_dir = r"D:\Projeck APK\icon_transparent"

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

def remove_white_bg(filename):
    if not filename.lower().endswith(('.png', '.jpg', '.jpeg', '.webp')):
        return

    filepath = os.path.join(input_dir, filename)
    img = Image.open(filepath).convert("RGBA")
    
    datas = img.getdata()
    newData = []
    
    # Toleransi untuk mendeteksi warna latar belakang putih
    for item in datas:
        # Jika pixel hampir putih murni, jadikan transparan
        if item[0] > 230 and item[1] > 230 and item[2] > 230:
            newData.append((255, 255, 255, 0))
        else:
            newData.append(item) # Pertahankan warna asli
            
    img.putdata(newData)
    
    name, _ = os.path.splitext(filename)
    out_filename = name + ".png"
    out_path = os.path.join(output_dir, out_filename)
    
    img.save(out_path, "PNG")
    print(f"Berhasil: {out_filename}")

for f in os.listdir(input_dir):
    remove_white_bg(f)

print("Selesai memproses semua icon!")
