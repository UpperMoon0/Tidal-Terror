"""Nearest-neighbor 64px export; retain generated RGB and enforce paired raw alpha."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json, shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art-source/food'
DEST = ROOT / 'src/main/resources/assets/tidalterror/textures/item'
manifest = json.loads((SOURCE/'generation-manifest.json').read_text())
sheet = Image.new('RGB', (760, 720), '#152534')
draw = ImageDraw.Draw(sheet)
font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 20)
draw.text((250, 12), 'RAW', font=font, fill='white')
draw.text((520, 12), 'COOKED', font=font, fill='white')
checks = []
for row, entry in enumerate(manifest['entries']):
    images = {}
    for state in ('raw', 'cooked'):
        source = SOURCE/f"{state}_{entry['id']}-source.png"
        if not source.exists():
            shutil.copy2(entry[state], source)
        images[state] = Image.open(source).convert('RGBA').resize((64,64), Image.Resampling.NEAREST)
    raw, cooked = images['raw'], images['cooked']
    mask = raw.getchannel('A').point(lambda a: 255 if a>=128 else 0)
    raw.putalpha(mask)
    # Generation preserves pose; resolve tiny alpha edge differences deterministically.
    cp, rp = cooked.load(), raw.load()
    solid = [(x,y) for y in range(64) for x in range(64) if cp[x,y][3]>=128]
    filled = 0
    for y in range(64):
        for x in range(64):
            if rp[x,y][3] and cp[x,y][3]<128:
                nx,ny = min(solid, key=lambda p:(p[0]-x)**2+(p[1]-y)**2)
                cp[x,y] = cp[nx,ny]; filled+=1
    cooked.putalpha(mask)
    for state, im in images.items():
        im.save(DEST/f"{state}_{entry['id']}.png")
        sheet.paste(im.resize((128,128),Image.Resampling.NEAREST),(245 if state=='raw' else 515, 45+row*165),im.resize((128,128),Image.Resampling.NEAREST))
    draw.text((12,90+row*165), entry['id'].replace('_',' ').title(),font=font,fill='white')
    assert raw.getchannel('A').tobytes()==cooked.getchannel('A').tobytes()
    assert mask.getextrema()==(0,255)
    checks.append({'item':entry['id'],'size':[64,64],'alpha_equal':True,'edge_pixels_filled':filled})
sheet.save(SOURCE/'food-preview.png')
(SOURCE/'export-checks.json').write_text(json.dumps(checks,indent=2)+'\n')
print(json.dumps(checks,indent=2))
