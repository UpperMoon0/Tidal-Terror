"""Copy production sources into an isolated preview project and create a fresh private world.

No existing world is read or copied. Evidence returns to the source project's art folder.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[1]
snapshot=ROOT/'build/equipment-preview-project-v1'
snapshot.mkdir(parents=True,exist_ok=True)
for name in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'):
    shutil.copy2(ROOT/name,snapshot/name)
for name in ('gradle','src/main','src/generated','src/equipmentPreview','.dependencies/shaders'):
    shutil.copytree(ROOT/name,snapshot/name,dirs_exist_ok=True)
evidence=ROOT/'art/reef-equipment/runtime'
with (snapshot/'build.gradle').open('a',encoding='utf-8') as file:
    file.write("\nproject.ext.equipmentEvidence = '"+(evidence/'passed.txt').as_posix()+"'\n")
    file.write("if (project.hasProperty('equipmentPreview')) { minecraft.runs.client { property 'tidalterror.equipmentOutput', '"+evidence.as_posix()+"' } }\n")
run=snapshot/'build/equipment-client-preview'
(run/'shaderpacks').mkdir(parents=True,exist_ok=True)
(run/'config').mkdir(exist_ok=True)
shader='ComplementaryReimagined_r5.9.3.zip'
shutil.copy2(ROOT/'run/shaderpacks'/shader,run/'shaderpacks'/shader)
(run/'config/oculus.properties').write_text(f'shaderPack={shader}\nenableShaders=true\n')
(run/'shaderpacks'/f'{shader}.txt').write_text('WATER_FOG_MULT=15\n')
print('Prepared isolated equipment client project:',snapshot)
print('Run gradlew.bat -PequipmentPreview runClient --offline from that project')
