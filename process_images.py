from PIL import Image
import numpy as np
import os

def process(inp, out):
    img = Image.open(inp).convert('RGBA')
    data = np.array(img)
    r, g, b, a = data.T
    # Tolerance for white
    white_areas = (r > 200) & (g > 200) & (b > 200)
    data[..., 3][white_areas.T] = 0
    
    # Also tint remaining pixels to white/gray or leave as is?
    # The user wants to change color in dark/light mode in compose
    # So we just need it as an alpha mask (black silhouette with transparent background is best)
    # Let's make everything non-white into black.
    non_white = ~white_areas.T
    data[..., 0][non_white] = 0
    data[..., 1][non_white] = 0
    data[..., 2][non_white] = 0
    
    Image.fromarray(data).save(out)

process('D:\\Projeck APK\\1 (1).jfif', 'D:\\Projeck APK\\app\\src\\main\\res\\drawable\\ic_gender_male.png')
process('D:\\Projeck APK\\1 (2).jfif', 'D:\\Projeck APK\\app\\src\\main\\res\\drawable\\ic_gender_female.png')
