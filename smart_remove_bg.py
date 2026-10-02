import os
import shutil
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
    # Background biasanya sangat terang (R>240, G>240, B>240)
    for item in datas:
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            newData.append((255, 255, 255, 0)) # Transparan
        else:
            # KEEP ORIGINAL COLOR! Do not turn black.
            newData.append(item)
            
    img.putdata(newData)
    
    # Sanitize filename
    name, _ = os.path.splitext(filename)
    clean_name = "".join([c if c.isalnum() else "_" for c in name]).lower()
    if clean_name and clean_name[0].isdigit():
        clean_name = "ach_" + clean_name
        
    out_filename = clean_name + "_color.png"
    out_path = os.path.join(output_dir, out_filename)
    
    img.save(out_path, "PNG")
    print(f"Saved: {out_filename}")

for f in os.listdir(input_dir):
    process_image(f)

print("Done processing icons with original colors preserved.")
