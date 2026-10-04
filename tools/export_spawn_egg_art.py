"""Export generated egg art to consistent transparent 64px inventory sprites."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json, shutil

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'art-source/spawn-eggs-v2'
DEST=ROOT/'src/main/resources/assets/tidalterror/textures/item'
manifest=json.loads((SOURCE/'generation-manifest.json').read_text())
sheet=Image.new('RGB',(800,235),'#858585')
draw=ImageDraw.Draw(sheet)
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',18)
checks=[]
for col,entry in enumerate(manifest['entries']):
    name=entry['mob']+'_spawn_egg'
    source=SOURCE/(name+'-source.png')
    if not source.exists(): shutil.copy2(entry['generated_path'],source)
    im=Image.open(source).convert('RGBA')
    alpha=im.getchannel('A').point(lambda a:255 if a>=128 else 0)
    bounds=alpha.getbbox()
    assert bounds and alpha.getextrema()==(0,255), 'Missing transparent background'
    im.putalpha(alpha)
    im=im.crop(bounds)
    scale=48/max(im.size)
    im=im.resize(tuple(max(1,round(v*scale)) for v in im.size),Image.Resampling.NEAREST)
    final=Image.new('RGBA',(64,64),(0,0,0,0))
    final.paste(im,((64-im.width)//2,(64-im.height)//2))
    target=DEST/(name+'.png')
    backup=SOURCE/(name+'-previous.png')
    if target.exists() and not backup.exists(): shutil.copy2(target,backup)
    final.save(target)
    assert final.size==(64,64) and final.getchannel('A').getextrema()==(0,255)
    preview=final.resize((160,160),Image.Resampling.NEAREST)
    sheet.paste(preview,(20+col*200,12),preview)
    label=entry['mob'].replace('_',' ').title()
    draw.text((100+col*200,194),label,anchor='mm',fill='white',font=font)
    checks.append({'mob':entry['mob'],'size':[64,64],'bounds':list(final.getbbox()),'transparent':True})
sheet.save(SOURCE/'spawn-eggs-preview.png')
(SOURCE/'export-checks.json').write_text(json.dumps(checks,indent=2)+'\n')
print(json.dumps(checks,indent=2))
