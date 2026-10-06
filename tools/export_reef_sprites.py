"""Export retained ImageGen sprites with nearest-neighbor sampling and native transparency."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/tidalterror'

def main():
    entries = json.loads((ROOT/'tools/reef_sprite_sources.json').read_text(encoding='utf-8-sig'))
    board = Image.new('RGBA', (len(entries)*128, 150), (38, 40, 51, 255))
    for i, entry in enumerate(entries):
        source = Image.open(ROOT/entry['path']).convert('RGBA')
        alpha = source.getchannel('A').point(lambda a: 255 if a >= 128 else 0)
        source.putalpha(alpha)
        source = source.crop(source.getbbox())
        source.thumbnail((28, 28), Image.Resampling.NEAREST)
        tile = Image.new('RGBA', (32, 32))
        tile.alpha_composite(source, ((32-source.width)//2, (32-source.height)//2))
        tile.resize((64, 64), Image.Resampling.NEAREST).save(RES/f'textures/item/{entry["name"]}.png')
        board.alpha_composite(tile.resize((128, 128), Image.Resampling.NEAREST), (i*128, 0))
        ImageDraw.Draw(board).text((i*128+4, 132), entry['name'], fill='white')
    board.save(ROOT/'docs/assets/reef-icons/preview.png')
    # Dedicated small blood sprites, authored at native resolution for particle animation.
    folder = RES/'textures/particle'
    folder.mkdir(parents=True, exist_ok=True)
    for stage in range(3):
        tile = Image.new('RGBA', (16, 16))
        draw = ImageDraw.Draw(tile)
        if stage == 0:
            draw.rectangle((7, 3, 8, 10), fill=(185, 12, 24, 255))
            draw.rectangle((5, 7, 10, 11), fill=(208, 18, 31, 255))
            draw.rectangle((6, 8, 7, 10), fill=(245, 48, 53, 255))
            draw.rectangle((7, 12, 8, 12), fill=(112, 8, 20, 255))
        else:
            for x,y in ((6,6),(9,7),(7,10),(11,10),(4,9)):
                r=1 if stage==1 else 0
                draw.rectangle((x-r,y-r,x+r,y+r), fill=(208, 15, 28, 210 if stage==1 else 150))
        tile.save(folder/f'blood_{stage}.png')
    (RES/'particles').mkdir(exist_ok=True)
    (RES/'particles/blood.json').write_text(json.dumps({'textures':[f'tidalterror:blood_{i}' for i in range(3)]}, indent=2)+'\n')
    effect = Image.new('RGBA', (18,18))
    effect.alpha_composite(Image.open(folder/'blood_0.png'), (1,1))
    effect.save(RES/'textures/mob_effect/reef_bleeding.png')
    # Native ArrowRenderer UV layout: crossed 16x5 shaft faces and 5x5 rear cap.
    # Authored directly at its native 32px resolution, using the generated item palette.
    arrow = Image.new('RGBA',(32,32)); draw = ImageDraw.Draw(arrow)
    draw.rectangle((3,2,14,2), fill=(145,99,58,255))
    draw.rectangle((12,1,15,3), fill=(223,229,231,255))
    draw.rectangle((11,2,15,2), fill=(151,102,60,255))
    draw.polygon([(0,2),(3,0),(3,4)],fill=(232,221,198,255))
    draw.point((0,2),fill=(255,245,226,255)); draw.line((3,3,5,3),fill=(112,96,75,255))
    draw.rectangle((0,5,4,9),fill=(204,211,219,255))
    draw.rectangle((1,6,3,8),fill=(238,241,241,255));draw.point((2,7),fill=(145,99,58,255))
    (RES/'textures/entity').mkdir(exist_ok=True);arrow.save(RES/'textures/entity/fang_arrow.png')
    print(f'Exported {len(entries)} transparent 64px sprites, native arrow UV and blood resources.')

if __name__ == '__main__': main()
