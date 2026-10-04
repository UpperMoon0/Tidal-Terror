"""Version-driven release guards; never reads or prints publication tokens."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import tempfile
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPO = 'UpperMoon0/Tidal-Terror'
PROJECT_ID = 1726637

def command(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()

def properties(text):
    return dict(re.findall(r'^([\w.]+)[ \t]*=[ \t]*(.*?)[ \t\r]*$', text, re.M))

def version(text):
    value = properties(text).get('mod_version', '')
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?', value):
        raise ValueError(f'Invalid mod_version: {value!r}')
    return value

def current():
    return version((ROOT / 'gradle.properties').read_text())

def tag_commit(value):
    try:
        return subprocess.check_output(['git', 'rev-list', '-n', '1', f'refs/tags/v{value}'], cwd=ROOT, text=True, stderr=subprocess.DEVNULL).strip()
    except subprocess.CalledProcessError:
        return None

def should_release(value, previous, event, tagged):
    return event == 'workflow_dispatch' or value != previous or not tagged

def output(**values):
    text = ''.join(f'{k}={str(v).lower() if isinstance(v, bool) else v}\n' for k,v in values.items())
    if os.getenv('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as stream:
            stream.write(text)
    print(text, end='')

def preflight(value, sha, tagged, notes):
    if tagged and tagged != sha:
        raise ValueError(f'v{value} already belongs to {tagged}; bump mod_version before releasing {sha}')
    if not notes.is_file() or not notes.read_text(encoding='utf-8').strip():
        raise ValueError(f'Missing or empty release changelog: {notes}')

def verify_jar(path, value):
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        text = jar.read('META-INF/mods.toml').decode()
        if '${' in text:
            raise ValueError('Unexpanded metadata placeholders')
        meta = tomllib.loads(text)
        mod = meta['mods'][0]
        assert mod['modId'] == 'tidalterror' and mod['version'] == value
        assert meta['license'] == 'MIT' and mod['authors'] == 'NsTut'
        deps = {d['modId']: d for d in meta['dependencies']['tidalterror']}
        assert deps['minecraft']['versionRange'] == '[1.20.1]'
        for name in ('forge', 'minecraft', 'terrablender'):
            assert deps[name]['mandatory'] and deps[name]['side'] == 'BOTH'
        assert deps['terrablender']['versionRange'] == '[3.0.1.6,3.1)'
        icon = jar.read(mod['logoFile'])
        assert icon[:8] == b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II', icon[16:24]) == (500, 500)
        assert 'tidalterror.mixins.json' in names
        json.loads(jar.read('tidalterror.mixins.json'))
        # Every production resource must survive packaging.
        for folder in ('src/main/resources', 'src/generated/resources'):
            base = ROOT / folder
            for source in base.rglob('*'):
                if source.is_file() and '.cache' not in source.parts:
                    relative = source.relative_to(base).as_posix()
                    assert relative in names, f'Missing resource: {relative}'
        # Opt-in fixture and preview classes must never ship.
        for folder in (ROOT / 'src').iterdir():
            if folder.is_dir() and folder.name not in ('main', 'generated'):
                for source in folder.rglob('*.java'):
                    match = re.search(r'^package\s+([\w.]+)\s*;', source.read_text(encoding='utf-8'), re.M)
                    if match:
                        prefix = match[1].replace('.', '/') + '/' + source.stem
                        assert not any(n == prefix+'.class' or n.startswith(prefix+'$') for n in names), f'Test/preview leaked: {prefix}'
        assert not any(n.startswith(('art-source/', 'world/', 'logs/')) for n in names)
    return hashlib.sha256(path.read_bytes()).hexdigest()

def verify_bundle(folder, value, sha):
    manifest = json.loads((folder / 'release-manifest.json').read_text())
    assert manifest['version'] == value and manifest['commit'] == sha
    assert manifest['curseforge_project'] == PROJECT_ID
    jar = folder / f'tidalterror-{value}.jar'
    assert manifest['sha256'] == verify_jar(jar, value), 'Release jar checksum mismatch'
    assert (folder / 'SHA256SUMS').read_text() == f"{manifest['sha256']}  {jar.name}\n"
    return manifest

def gh(*args):
    return command('gh', *args, '--repo', REPO)

def release_info(tag):
    result = subprocess.run(['gh','release','view',tag,'--repo',REPO,'--json','isDraft,assets'],cwd=ROOT,text=True,capture_output=True)
    if result.returncode:
        # Only a genuine 404 may create a new release; auth/network errors stop.
        if 'release not found' in result.stderr.lower() or '404' in result.stderr:
            return None
        raise RuntimeError(result.stderr)
    return json.loads(result.stdout)

def download(tag, asset, folder):
    gh('release','download',tag,'--pattern',asset,'--dir',str(folder),'--clobber')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['plan','preflight','package','prepare','receipt','finalize'])
    parser.add_argument('--output', type=Path, default=ROOT / 'build/release')
    args = parser.parse_args()
    value = current()
    sha = os.getenv('GITHUB_SHA') or command('git','rev-parse','HEAD')
    tag = f'v{value}'
    notes = ROOT / f'changelogs/{tag}.txt'
    if args.action == 'plan':
        before = os.getenv('BEFORE_SHA','')
        previous = None
        if before and set(before) != {'0'}:
            previous = version(command('git','show',f'{before}:gradle.properties'))
        output(changed=should_release(value,previous,os.getenv('GITHUB_EVENT_NAME',''),tag_commit(value)), value=value)
        return
    preflight(value,sha,tag_commit(value),notes)
    folder = args.output.resolve()
    if args.action == 'preflight':
        print(f'Release {value} preflight passed')
    elif args.action == 'package':
        jars = [p for p in (ROOT/'build/libs').glob('*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
        if len(jars) != 1 or jars[0].name != f'tidalterror-{value}.jar':
            raise ValueError(f'Expected one normal release jar, got {jars}')
        digest = verify_jar(jars[0],value)
        folder.mkdir(parents=True,exist_ok=True)
        shutil.copy2(jars[0],folder/jars[0].name)
        manifest = dict(version=value,commit=sha,sha256=digest,curseforge_project=PROJECT_ID,minecraft='1.20.1',loader='forge',java=17)
        (folder/'release-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
        (folder/'SHA256SUMS').write_text(f'{digest}  {jars[0].name}\n')
        verify_bundle(folder,value,sha)
        print(f'Verified {jars[0].name}: {digest}')
    elif args.action == 'prepare':
        manifest = verify_bundle(folder,value,sha)
        info = release_info(tag)
        if info is None:
            gh('release','create',tag,'--target',sha,'--draft','--title',f'Tidal Terror {tag}','--notes-file',str(notes))
            gh('release','upload',tag,str(folder/f'tidalterror-{value}.jar'),str(folder/'SHA256SUMS'),str(folder/'release-manifest.json'))
            info = release_info(tag)
        assets = {x['name'] for x in info['assets']}
        # Recover an interrupted draft upload; never overwrite existing assets.
        for asset in ('release-manifest.json','SHA256SUMS',f'tidalterror-{value}.jar'):
            if asset not in assets:
                if not info['isDraft']:
                    raise ValueError(f'Published release is missing {asset}')
                gh('release','upload',tag,str(folder/asset))
        with tempfile.TemporaryDirectory() as temp:
            saved = Path(temp)
            for asset in ('release-manifest.json','SHA256SUMS',f'tidalterror-{value}.jar'):
                download(tag,asset,saved)
            assert verify_bundle(saved,value,sha) == manifest, 'Reserved release differs from this build'
            if 'curseforge-upload.json' in assets:
                download(tag,'curseforge-upload.json',saved)
                receipt = json.loads((saved/'curseforge-upload.json').read_text())
                assert receipt['sha256'] == manifest['sha256'] and receipt['commit'] == sha
                assert receipt['file_id'] > 0 and receipt['project_id'] == PROJECT_ID
                output(upload=False)
            else:
                if not info['isDraft']:
                    raise ValueError('Public release has no CurseForge receipt; inspect before uploading')
                output(upload=True)
    elif args.action == 'receipt':
        manifest = verify_bundle(folder,value,sha)
        file_id = int(os.environ['CF_FILE_ID'])
        assert file_id > 0
        receipt = dict(project_id=PROJECT_ID,file_id=file_id,url=os.getenv('CF_FILE_URL',''),commit=sha,sha256=manifest['sha256'])
        path = folder/'curseforge-upload.json'
        path.write_text(json.dumps(receipt,indent=2)+'\n')
        gh('release','upload',tag,str(path))
        print(f'CurseForge upload recorded: project {PROJECT_ID}, file {file_id}')
    elif args.action == 'finalize':
        manifest = verify_bundle(folder,value,sha)
        download(tag,'curseforge-upload.json',folder)
        receipt = json.loads((folder/'curseforge-upload.json').read_text())
        assert receipt['sha256'] == manifest['sha256'] and receipt['commit'] == sha
        assert receipt['project_id'] == PROJECT_ID and receipt['file_id'] > 0
        gh('release','edit',tag,'--draft=false','--latest')
        print(f'Published GitHub release {tag}; CurseForge file {receipt["file_id"]}')

if __name__ == '__main__':
    main()
