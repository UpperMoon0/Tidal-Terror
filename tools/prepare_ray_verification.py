"""Copy sources for isolated verification, avoiding concurrent class replacement."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1]
snapshot=ROOT/'build/ray-verification-project-v1'
snapshot.mkdir(parents=True,exist_ok=True)
for name in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'):
    shutil.copy2(ROOT/name,snapshot/name)
for name in ('gradle','src/main','src/generated','src/rayTest','src/modelTest','src/gameTest/resources','.dependencies/shaders'):
    shutil.copytree(ROOT/name,snapshot/name,dirs_exist_ok=True)
print(snapshot)
