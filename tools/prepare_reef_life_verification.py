"""Freeze the complete current sources for native ambient AI verification."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1]
snapshot=ROOT/'build/reef-life-verification-v1';snapshot.mkdir(parents=True,exist_ok=True)
for name in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'):
    shutil.copy2(ROOT/name,snapshot/name)
for name in ('gradle','src/main','src/generated','src/reefLifeTest','src/veilglowTest','src/shardbackTest','src/modelTest','src/gameTest/resources','.dependencies/shaders'):
    shutil.copytree(ROOT/name,snapshot/name,dirs_exist_ok=True)
print(snapshot)
