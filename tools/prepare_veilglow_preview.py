"""Prepare an independent copy of the completed reef audit for Veilglow photographs."""
from pathlib import Path
import shutil
import msvcrt

ROOT=Path(__file__).resolve().parents[1]
snapshot=ROOT/'build/veilglow-preview-project-v1'
snapshot.mkdir(parents=True,exist_ok=True)
for name in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'):
    shutil.copy2(ROOT/name,snapshot/name)
for name in ('common','common-versioned','common-universal','common-1201','gradle','src/main','src/generated','src/veilglowPreview','.dependencies/shaders'):
    for obsolete in (snapshot / name).rglob("*"):
        if obsolete.is_file() and not (ROOT / name / obsolete.relative_to(snapshot / name)).exists():
            obsolete.unlink()
    shutil.copytree(ROOT/name,snapshot/name,dirs_exist_ok=True)
with (snapshot/'build.gradle').open('a') as file:
    file.write("\nif (project.hasProperty('veilglowPreview')) { minecraft.runs.client { property 'tidalterror.veilglowOutput', '"+(ROOT/'art/veilglow/runtime').as_posix()+"' } }\n")
audited=Path((ROOT/'build/reef-audit-v4/world-path.txt').read_text().strip())
destination=snapshot/'build/veilglow-client-preview'
world=destination/'saves/Veilglow Preview'
if not world.exists():
    # Refuse to copy a world held by an active server/client.
    with (audited/'session.lock').open('r+b') as lock:
        msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            shutil.copytree(audited,world,ignore=shutil.ignore_patterns('session.lock'))
        finally: msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
(destination/'shaderpacks').mkdir(parents=True,exist_ok=True)
(destination/'config').mkdir(exist_ok=True)
shutil.copy2(ROOT/'run/shaderpacks/ComplementaryReimagined_r5.9.3.zip',destination/'shaderpacks')
(destination/'config/oculus.properties').write_text('shaderPack=ComplementaryReimagined_r5.9.3.zip\nenableShaders=true\n')
(destination/'shaderpacks/ComplementaryReimagined_r5.9.3.zip.txt').write_text('WATER_FOG_MULT=15\n')
print('Prepared isolated Veilglow world:',world)
print('Run gradlew.bat -PveilglowPreview runClient --offline from',snapshot)
