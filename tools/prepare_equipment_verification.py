"""Prepare an isolated production build without touching a running preview client's output."""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[1]
snapshot=ROOT/'build/equipment-verification-project-v1'
snapshot.mkdir(parents=True,exist_ok=True)
for name in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'):
    shutil.copy2(ROOT/name,snapshot/name)
for name in ('gradle','src'):
    # Reused snapshots must not retain resources removed from production.
    for target in (snapshot/name).rglob('*'):
        if target.is_file() and not (ROOT/name/target.relative_to(snapshot/name)).exists():
            target.unlink()
    shutil.copytree(ROOT/name,snapshot/name,dirs_exist_ok=True,ignore=shutil.ignore_patterns('.cache'))
(snapshot/'tools').mkdir(exist_ok=True)
for name in ('release.py','test_release.py'):
    shutil.copy2(ROOT/'tools'/name,snapshot/'tools'/name)
shutil.copytree(ROOT/'changelogs',snapshot/'changelogs',dirs_exist_ok=True)
print(snapshot)
