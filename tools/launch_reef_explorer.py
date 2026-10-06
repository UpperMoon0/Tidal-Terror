from pathlib import Path
import shutil,subprocess
root=Path.cwd();snapshot=root/'build/reef-explore-project-v5';snapshot.mkdir(exist_ok=True)
for name in ['build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat']:
 shutil.copy2(root/name,snapshot/name)
for name in ['common','common-versioned','common-universal','common-1201','gradle','src/main','src/generated','src/explore','.dependencies/shaders']:
 for obsolete in (snapshot / name).rglob("*"):
     if obsolete.is_file() and not (root / name / obsolete.relative_to(snapshot / name)).exists():
         obsolete.unlink()
 shutil.copytree(root/name,snapshot/name,dirs_exist_ok=True)
run=snapshot/'run';(run/'shaderpacks').mkdir(parents=True,exist_ok=True);(run/'config').mkdir(exist_ok=True)
shutil.copy2(root/'run/shaderpacks/ComplementaryReimagined_r5.9.3.zip',run/'shaderpacks')
(run/'config/oculus.properties').write_text('shaderPack=ComplementaryReimagined_r5.9.3.zip\nenableShaders=true\n')
(run/'shaderpacks/ComplementaryReimagined_r5.9.3.zip.txt').write_text('WATER_FOG_MULT=15\n')
with (snapshot/'build.gradle').open('a') as f:
 f.write("\nsourceSets.main.java.srcDir 'src/explore/java'\nminecraft.runs.client { property 'tidalterror.originalProject', '"+root.as_posix()+"'; property 'forge.logging.console.level', 'info' }\n")
print(snapshot)

startup=subprocess.STARTUPINFO()
startup.dwFlags |= subprocess.STARTF_USESHOWWINDOW
startup.wShowWindow=0
with (snapshot/'explorer-output.txt').open('w') as out,(snapshot/'explorer-errors.txt').open('w') as err:
 process=subprocess.Popen(['cmd.exe','/c','gradlew.bat','runClient','--offline'],cwd=snapshot,
                          stdout=out,stderr=err,startupinfo=startup,creationflags=subprocess.CREATE_NO_WINDOW)
print('Explorer launcher PID',process.pid)
