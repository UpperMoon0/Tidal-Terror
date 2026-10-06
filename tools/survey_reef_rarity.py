"""Sample native reef coverage in an isolated source snapshot; never load dev saves."""
from collections import Counter
from pathlib import Path
import json
import re
import shutil
import subprocess
import sys
import ports

ROOT=Path(__file__).resolve().parents[1]
SEEDS={0,123456789,-987654321,42,20261006}

def summarize(log):
    rows=[]
    for line in log.splitlines():
        if 'REEF_SURVEY seed=' not in line:continue
        seed=int(re.search(r'seed=(-?\d+)',line)[1])
        counts=Counter({key:int(value) for key,value in re.findall(r'([\w:.-]+)=(\d+)',line.split('actual={')[1].split('}')[0])})
        samples=sum(counts.values())
        if samples!=65536:raise ValueError(f'Incomplete grid for seed {seed}')
        reef=counts['tidalterror:coral_cathedral']
        ocean=sum(value for key,value in counts.items() if 'ocean' in key or key=='tidalterror:coral_cathedral')
        rows.append(dict(seed=seed,samples=samples,reef=reef,ocean=ocean,reef_ocean_percent=100*reef/ocean,reef_world_percent=100*reef/samples,biomes=dict(counts)))
    if len(rows)!=len(SEEDS) or {row['seed'] for row in rows}!=SEEDS:raise ValueError('Missing or duplicate survey seeds')
    samples=sum(row['samples'] for row in rows);reef=sum(row['reef'] for row in rows);ocean=sum(row['ocean'] for row in rows)
    return dict(runtime='NeoForge 26.1.2',sample_y=32,spacing=128,square_side=32768,seeds=rows,samples=samples,reef_ocean_percent=100*reef/ocean,reef_world_percent=100*reef/samples)

def main():
    snapshot=ROOT/'build/reef-rarity-survey'
    snapshot.mkdir(parents=True,exist_ok=True)
    files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode('utf8').split('\0')
    for name in sorted(set(files)-{''}):
        source=ROOT/name
        if not source.is_file():continue
        destination=snapshot/name;destination.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,destination)
    tests=snapshot/'src/portTest26/java/com/nhat/tidal_terror/gametest'
    for name in ('ReefSurvey.java','PortTestBootstrap.java'):shutil.copy2(ROOT/'tools/reef_rarity'/name,tests/name)
    log_path=snapshot/'survey.log'
    command=[str(snapshot/('gradlew.bat' if sys.platform=='win32' else 'gradlew')),'-Pmultiversion','-PportTests',':neoforge-26.1.2:runPortTest','--no-daemon','--no-configuration-cache']
    with log_path.open('w',encoding='utf8') as log:
        result=subprocess.run(command,cwd=snapshot,stdout=log,stderr=subprocess.STDOUT)
    log=log_path.read_text(encoding='utf8',errors='replace')
    if result.returncode:raise RuntimeError(f'Native survey failed; see {log_path}')
    ports.verify_results(log,1,snapshot/'neoforge-26.1.2/build/port-test-report.xml')
    result=summarize(log)
    (snapshot/'result.json').write_text(json.dumps(result,indent=2),encoding='utf8')
    print(f"Reef coverage: {result['reef_ocean_percent']:.2f}% of ocean samples; {result['reef_world_percent']:.2f}% of all samples")
    print(f'Full counts and connected-patch measurements: {snapshot}')

if __name__=='__main__':main()
