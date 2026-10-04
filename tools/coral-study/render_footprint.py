"""Plot the native climate sample; no invented or interpolated biome cells."""
from pathlib import Path
import csv
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parents[2]
rows=[tuple(map(int,r)) for r in csv.reader((root/'build/reef-audit-v2/climate-footprint.csv').open())]
im=Image.new('RGB',(1100,1200),(12,25,34));d=ImageDraw.Draw(im)
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',25)
d.text((30,28),'Native reef footprint · 4.1 km × 4.1 km',font=font,fill='white')
for x,z,reef,floor in rows:
    px=30+(x+2048)//64*16;py=95+(z+2048)//64*16
    color=(18,137,165) if reef and floor<=-40 else (77,192,169) if reef else (31,57,62)
    d.rectangle((px,py,px+15,py+15),fill=color)
d.text((30,1153),'Blue: deep reef · Teal: gradual reef shelves · Dark: other biomes',font=font,fill='white')
im.save(root/'art/coral-structure-study/reef-footprint.png')
