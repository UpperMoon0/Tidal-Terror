"""Compile unchanged production repair handlers against minimal API stubs, then restart the JVM.

This checks handler/algorithm persistence semantics, not native Minecraft/Forge
integration. The deep-province CI job additionally runs native NBT/world restart tests.
"""
from pathlib import Path
import subprocess
import shutil
import tempfile

ROOT = Path(__file__).resolve().parents[2]


def main():
    with tempfile.TemporaryDirectory(prefix='tidal-water-finish-') as temporary:
        build = Path(temporary)
        sources = sorted((ROOT / 'tools/province/water_finish_stubs').rglob('*.java'))
        sources += [ROOT / 'src/main/java/com/nhat/tidal_terror/worldgen/ReefWaterFinish.java',
                    ROOT / 'src/reefWorldgenTestSupport/java/com/nhat/tidal_terror/worldgen/WaterFinishProgressWatchdog.java',
                    ROOT / 'tools/province/WaterFinishRestartCheck.java']
        compiler = ['javac', '--release', '17'] if shutil.which('javac') else ['java', 'com.sun.tools.javac.Main', '-source', '17', '-target', '17']
        subprocess.run([*compiler, '-d', str(build), *map(str, sources)], check=True)
        for phase in ('save', 'resume'):
            subprocess.run(['java', '-cp', str(build), 'WaterFinishRestartCheck', phase,
                            str(build / 'checkpoint.bin')], check=True)


if __name__ == '__main__':
    main()
