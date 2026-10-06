"""Native port verification and production-jar guards; no publication side effects."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import shutil
import subprocess
import sys
import tomllib
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TARGETS = {
    'fabric-1.20.1': dict(minecraft='1.20.1', loader='fabric', java=17, tests=17),
    'fabric-1.21.1': dict(minecraft='1.21.1', loader='fabric', java=21, tests=17),
    'neoforge-1.21.1': dict(minecraft='1.21.1', loader='neoforge', java=21, tests=17),
    'neoforge-26.1.2': dict(minecraft='26.1.2', loader='neoforge', java=25, tests=15),
}

def verify_results(log, expected, report=None):
    # Server launchers can return zero after a startup exception. Native completion is mandatory.
    if not re.search(rf'All {expected} required tests passed', log):
        raise ValueError(f'Missing native completion for all {expected} required tests')
    if re.search(r'(required tests failed|Failed to start the minecraft server|Mixin apply.*failed)', log, re.I):
        raise ValueError('Native test run contains a failure')
    if report is not None:
        tree = ET.parse(report)
        cases = list(tree.iter('testcase'))
        if len(cases) != expected or any(list(case.iter('failure')) or list(case.iter('error')) or list(case.iter('skipped')) for case in cases):
            raise ValueError('Missing, skipped or failed native test cases')
        identities = {(case.get('classname'), case.get('name')) for case in cases}
        if len(identities) != expected:
            raise ValueError('Duplicate test cases do not establish full coverage')

def verify_jar(path, target, version):
    spec = TARGETS[target]
    with zipfile.ZipFile(path) as jar:
        entries = jar.namelist()
        names = set(entries)
        if len(names) != len(entries):
            raise ValueError('Duplicate archive entries')
        if spec['loader'] == 'fabric':
            meta = json.loads(jar.read('fabric.mod.json'))
            assert meta['id'] == 'tidalterror' and meta['version'] == version
            assert meta['depends']['minecraft'] == spec['minecraft']
            for dependency in ('fabricloader', 'fabric-api', 'architectury', 'terrablender'):
                assert dependency in meta['depends']
            assert 'META-INF/neoforge.mods.toml' not in names
            configs = meta['mixins']
        else:
            meta = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())
            assert meta['mods'][0]['modId'] == 'tidalterror' and meta['mods'][0]['version'] == version
            dependencies = {d['modId']:d for d in meta['dependencies']['tidalterror']}
            assert dependencies['minecraft']['versionRange'] == '['+spec['minecraft']+']'
            for dependency in ('neoforge','minecraft','architectury','terrablender'):
                assert dependencies[dependency]['type'] == 'required'
            assert 'fabric.mod.json' not in names
            configs = [entry['config'] for entry in meta['mixins']]
            extensions = json.loads(jar.read(meta['mods'][0]['enumExtensions']))
            assert len(extensions['entries']) == 4
        assert 'META-INF/mods.toml' not in names
        for config in configs:
            if isinstance(config,dict): config=config['config']
            mixins=json.loads(jar.read(config))
            for mixin in mixins.get('mixins',[])+mixins.get('client',[]):
                assert mixins['package'].replace('.','/')+'/'+mixin+'.class' in names
            if spec['loader']=='fabric':
                assert mixins.get('refmap') in names, 'Production Fabric mixins need their mapped refmap'
        base='com/nhat/tidal_terror/'
        for name in ('TidalTerror','effects/ModEffects','items/ReefSpearItem','items/ReefArmorItem','entities/FangArrowEntity',
                     'entities/coral_crusher/CoralCrusherEntity','entities/cathedral_ray/CathedralRayEntity',
                     'entities/shardback/ShardbackEntity','entities/veilglow/VeilglowEntity'):
            assert base+name+'.class' in names, 'Missing production class '+name
        for name in names:
            if name.endswith('.class') and name.startswith(base):
                data=jar.read(name)
                assert struct.unpack('>H',data[6:8])[0] <= spec['java']+44, 'Wrong Java target '+name
                assert '/gametest/' not in name and '/testing/' not in name, 'Test fixture leaked '+name
            assert not name.startswith(('world/','logs/','art-source/'))
        modern=spec['minecraft']!='1.20.1'
        recipe='recipe' if modern else 'recipes'
        loot='loot_table' if modern else 'loot_tables'
        for name in ('reef_spear','reef_helmet','reef_chestplate','reef_leggings','reef_boots','fang_arrow'):
            data=json.loads(jar.read(f'data/tidalterror/{recipe}/{name}.json'))
            if name.startswith('reef_') and name!='reef_spear': assert data['type']=='tidalterror:reef_armor_upgrade'
        for name in ('coral_crusher','shardback','cathedral_ray','veilglow'):
            json.loads(jar.read(f'data/tidalterror/{loot}/entities/{name}.json'))
        biome=json.loads(jar.read('data/tidalterror/worldgen/biome/coral_cathedral.json'))
        assert all('tidalterror:'+name in biome['spawners'] for name in ('crusher','ray','veilglow','shardback'))
        for name in ('shardback_plate','crusher_tooth','reef_helmet','reef_chestplate','reef_leggings','reef_boots'):
            assert f'assets/tidalterror/textures/item/{name}.png' in names
        assert 'assets/tidalterror/textures/item/reef_spear_model.png' in names
        assert 'assets/tidalterror/textures/item/reef_spear.png' not in names
        if modern:
            for name in ('serration','hemorrhage'):
                enchant=json.loads(jar.read(f'data/tidalterror/enchantment/{name}.json'))
                assert enchant['supported_items']=='#tidalterror:bleeding_weapons'
        if spec['minecraft']=='26.1.2':
            model=json.loads(jar.read('assets/tidalterror/items/reef_spear.json'))
            assert model['model']['type']=='minecraft:special'
            assert model['model']['model']['type']=='tidalterror:reef_spear'
            assert 'parent' not in json.loads(jar.read('assets/tidalterror/models/item/reef_spear.json'))
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run_tests(target):
    folder=ROOT/target/'build'
    folder.mkdir(parents=True,exist_ok=True)
    report=folder/'port-test-report.xml'
    report.unlink(missing_ok=True)
    log_path=folder/'port-test.log'
    wrapper=ROOT/('gradlew.bat' if sys.platform=='win32' else 'gradlew')
    with log_path.open('w',encoding='utf-8') as log:
        process=subprocess.Popen([str(wrapper),'-Pmultiversion','-PportTests',f':{target}:runPortTest','--no-daemon','--no-configuration-cache'],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
        try:
            for line in process.stdout:
                log.write(line);log.flush();print(line,end='',flush=True)
            code=process.wait(timeout=30)
        finally:
            if process.poll() is None: process.terminate();process.wait(timeout=30)
    evidence=ROOT/'build/port-evidence'/target
    evidence.mkdir(parents=True,exist_ok=True)
    shutil.copy2(log_path,evidence/log_path.name)
    if report.exists(): shutil.copy2(report,evidence/report.name)
    if code: raise ValueError(f'Native test process exited {code}')
    verify_results(log_path.read_text(encoding='utf-8'),TARGETS[target]['tests'],None if target=='neoforge-1.21.1' else report)
    print(f'{target}: all {TARGETS[target]["tests"]} native tests verified')

def verify_client(log):
    if 'TIDAL_PORT_CLIENT_READY:' not in log:
        raise ValueError('Native client did not complete resource/model loading')
    if re.search(r"(Couldn't parse item model|Unable to load model|Missing textures in model|Failed to load texture|Mixin apply.*failed|Reported exception thrown)", log, re.I):
        raise ValueError('Client resource or rendering setup failed')

def run_client(target):
    folder=ROOT/target/'build'
    folder.mkdir(parents=True,exist_ok=True)
    wrapper=ROOT/('gradlew.bat' if sys.platform=='win32' else 'gradlew')
    log_path=folder/'client-smoke.log'
    with log_path.open('w',encoding='utf-8') as log:
        process=subprocess.Popen([str(wrapper),'-Pmultiversion','-PclientSmoke',f':{target}:runClient','--no-daemon','--no-configuration-cache'],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
        try:
            for line in process.stdout:
                log.write(line);log.flush();print(line,end='',flush=True)
            code=process.wait(timeout=30)
        finally:
            if process.poll() is None: process.terminate();process.wait(timeout=30)
    evidence=ROOT/'build/port-evidence'/target
    evidence.mkdir(parents=True,exist_ok=True)
    shutil.copy2(log_path,evidence/log_path.name)
    if code: raise ValueError(f'Native client exited {code}')
    verify_client(log_path.read_text(encoding='utf-8'))
    print(f'{target}: native client resources and model layers verified')

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('action',choices=['test','client','jar'])
    parser.add_argument('target',choices=TARGETS)
    parser.add_argument('--jar',type=Path)
    parser.add_argument('--version')
    args=parser.parse_args()
    if args.action=='test':run_tests(args.target)
    elif args.action=='client':run_client(args.target)
    else:
        if not args.jar or not args.version:parser.error('jar verification needs --jar and --version')
        print(verify_jar(args.jar,args.target,args.version))

if __name__=='__main__':main()
