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
    print('Exported six transparent 64px sprites, three native blood particle frames, status icon and preview.')

if __name__ == '__main__': main()
