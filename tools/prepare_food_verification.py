"""Isolate the food build/test output from running clients and other verification."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1]
snapshot=ROOT/'build/food-verification-project-v1'
snapshot.mkdir(parents=True,exist_ok=True)
for name in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'):
    shutil.copy2(ROOT/name,snapshot/name)
for name in ('common','common-versioned','common-universal','common-1201','gradle','src/main','src/generated','src/foodTest','src/gameTest/resources','.dependencies/shaders'):
    for obsolete in (snapshot / name).rglob("*"):
        if obsolete.is_file() and not (ROOT / name / obsolete.relative_to(snapshot / name)).exists():
            obsolete.unlink()
    shutil.copytree(ROOT/name,snapshot/name,dirs_exist_ok=True)
print(snapshot)
