"""Package actual shader captures without modifying their pixels."""
from pathlib import Path
import hashlib, html, json, shutil, zipfile, sys
from PIL import Image

root = Path(__file__).resolve().parents[1]
lush = '--lush' in sys.argv
version = 'v4' if lush else 'v3'
out = root / ('art/coral-cathedral-' + version)
records = json.loads((out / 'capture-manifest.json').read_text())
assert len(records) == (13 if lush else 12)
assert not (out / 'failure.txt').exists()
cards = []
for record in records:
    name = record['file']
    assert record['shadersActive'] and 'Complementary' in record['shaderPack']
    with Image.open(out / name) as image:
        assert image.size == (1920, 1080)
    title = name.removesuffix('.png').split('-', 1)[1].replace('-', ' ').title()
    cards.append(f'<a href="{html.escape(name)}"><img loading="lazy" src="{html.escape(name)}"><span>{title}</span></a>')
(out / 'index.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Coral Cathedral</title>
<style>body{background:#081b25;color:#e5f4f4;font:17px system-ui;max-width:1500px;margin:40px auto;padding:0 24px}
h1{font-size:42px;margin-bottom:8px}p{color:#aacad1;line-height:1.6}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(380px,1fr));gap:24px}
a{color:inherit;text-decoration:none;background:#102b38;border-radius:12px;overflow:hidden}img{width:100%;display:block}span{display:block;padding:14px}</style>
<h1>Coral Cathedral</h1><p>Fresh deep reef with gradual ocean boundaries and fully flooded water.
Actual Minecraft 1.20.1 framebuffer captures using Complementary Reimagined, Oculus and Embeddium.
Click any image for its original 1920 × 1080 PNG. The Coral Crusher was spawned through Minecraft’s native natural spawning.</p><main>'''
    + ''.join(cards) + '</main></html>', encoding='utf-8')
shutil.copy2(root / ('build/reef-audit-v4/passed.txt' if lush else 'build/reef-audit-v2/passed.txt'), out / 'validation.txt')
if lush:
    log=(root/'build/reef-lush-shader-final.txt').read_text(errors='replace')
    for record in records[:12]:
        assert 'REEF_PREVIEW LIVE_WATER PASS '+Path(record['file']).stem+' floatingAir=0 bubbles=0' in log
    assert 'REEF_PREVIEW CREATIVE_GUI PASS' in log
    assert 'All 6 required tests passed' in (root/'build/reef-lush-gametests.txt').read_text(errors='replace')
    with (out/'validation.txt').open('a') as report:
        report.write('\nAll 6 native GameTests passed, including unchanged animal predicates across 64 vanilla biomes and solid-collision rejection.\nAll 12 live shader views: floatingAir=0 bubbles=0. Native creative GUI verified custom egg and Tidal Terror tab.\n')
        for line in (root/'build/reef-lush-native.txt').read_text(errors='replace').splitlines():
            if 'REEF_AUDIT DEEP_FAUNA PASS' in line:report.write(line+'\n')
(out / 'README.txt').write_text('''CORAL CATHEDRAL — NATIVE SHADER PREVIEWS
Seed 7142026, Minecraft 1.20.1 / Forge 47.2.0.
Complementary Reimagined r5.9.3, Oculus 1.8.0, Embeddium 0.3.31.
All 12 PNGs are unedited 1920 x 1080 screenshots from the actual game framebuffer.
01-07: deep basin, gardens, giant corals, water surface and aerial views.
08-10: Coral Crusher created by NaturalSpawner; the photographed individual is held still.
11-12: transition into the adjacent vanilla ocean.
Native spawn attempts are accelerated only in the development fixture.
The release mod keeps normal spawning rates and requires the Coral Cathedral biome.
Native validation and per-shot shader/camera metadata are included.
Open index.html to browse. These images may be used as CurseForge previews.
Changes affect newly generated chunks; old terrain is preserved.
''', encoding='utf-8')
if lush:
    with (out/'README.txt').open('a',encoding='utf-8') as readme:
        readme.write('\nVersion 4: 13 PNGs total; image 13 verifies the custom egg in the Tidal Terror creative tab.\nSeabed colonies use world-coordinate clusters rather than per-chunk quotas; one-block coral plants and fans are substantially denser.\nDolphins, turtles and tropical fish can spawn naturally in deep reef water. Native stress-test counts are accelerated fixture counts, not production density.\n')
    readme=out/'README.txt'
    readme.write_text(readme.read_text(encoding='utf-8').replace('All 12 PNGs','All 13 PNGs'),encoding='utf-8')
files = sorted(p for p in out.iterdir() if p.is_file() and p.name != 'SHA256SUMS.txt')
(out / 'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.name + '\n' for p in files))
archive = root / ('art/coral-cathedral-'+version+'-preview-pack.zip')
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(out.iterdir()):
        if p.is_file(): z.write(p, 'coral-cathedral/' + p.name)
print(archive)
