"""Prepare an independent source copy for Coral Crusher native regression checks."""
from pathlib import Path
import shutil
import argparse

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--name', default='crusher-combat-verification-project-v1')
args = parser.parse_args()
if not args.name or Path(args.name).name != args.name or args.name in ('.', '..'):
    raise ValueError('Snapshot name must be one directory name')
snapshot = root / 'build' / args.name
snapshot.mkdir(parents=True, exist_ok=True)
for name in ('build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat'):
    shutil.copy2(root / name, snapshot / name)
for name in ('common', 'common-versioned', 'common-universal', 'common-1201', 'gradle', 'src/main', 'src/generated', 'src/gameTest', 'src/modelTest', '.dependencies/shaders'):
    for obsolete in (snapshot / name).rglob("*"):
        if obsolete.is_file() and not (root / name / obsolete.relative_to(snapshot / name)).exists():
            obsolete.unlink()
    shutil.copytree(root / name, snapshot / name, dirs_exist_ok=True)
print(snapshot)
