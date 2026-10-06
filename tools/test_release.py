import tempfile
from pathlib import Path
import unittest
import json
import zipfile
import hashlib
import release

class ReleaseTests(unittest.TestCase):
    def test_version_bump_releases(self):
        self.assertTrue(release.should_release('0.0.2','0.0.1','push',True))
    def test_ordinary_push_does_not_republish(self):
        self.assertFalse(release.should_release('0.0.1','0.0.1','push',True))
    def test_first_release_and_workflow_repair(self):
        self.assertTrue(release.should_release('0.0.1','0.0.1','push',False))
    def test_manual_retry(self):
        self.assertTrue(release.should_release('0.0.1','0.0.1','workflow_dispatch',True))
    def test_invalid_versions(self):
        for value in ('', '../../oops', '1.0.0;script', 'v1.0.0'):
            with self.subTest(value=value), self.assertRaises(ValueError):
                release.version('mod_version='+value)
    def test_crlf_properties(self):
        self.assertEqual(release.version('mod_version = 0.0.1\r\n'),'0.0.1')
    def test_preflight_collision(self):
        with tempfile.TemporaryDirectory() as temp:
            notes=Path(temp)/'notes.txt';notes.write_text('Release notes')
            with self.assertRaises(ValueError):
                release.preflight('0.0.1','new','old',notes)
            release.preflight('0.0.1','same','same',notes)
    def test_missing_and_empty_changelog(self):
        with tempfile.TemporaryDirectory() as temp:
            notes=Path(temp)/'notes.txt'
            for content in (None, '  \n'):
                if content is not None: notes.write_text(content)
                with self.assertRaises(ValueError):
                    release.preflight('0.0.1','sha',None,notes)


class ArtifactTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.folder = Path(self.temp.name)
        self.version = release.current()
        self.jar = self.folder/f'tidalterror-{self.version}.jar'
        self.entries = {}
        props = release.properties((release.ROOT/'gradle.properties').read_text())
        for dirname in ('src/main/resources','src/generated/resources'):
            base=release.ROOT/dirname
            for path in base.rglob('*'):
                if path.is_file() and '.cache' not in path.parts:
                    self.entries[path.relative_to(base).as_posix()] = path.read_bytes()
        text=self.entries['META-INF/mods.toml'].decode()
        for key,value in props.items(): text=text.replace('${'+key+'}',value)
        self.entries['META-INF/mods.toml']=text.encode()
    def tearDown(self):
        self.temp.cleanup()
    def write_jar(self):
        with zipfile.ZipFile(self.jar,'w') as jar:
            for name,content in self.entries.items(): jar.writestr(name,content)
    def test_valid_production_resources(self):
        self.write_jar()
        self.assertEqual(len(release.verify_jar(self.jar,self.version)),64)
    def test_missing_resource(self):
        del self.entries['tidalterror.mixins.json']
        self.write_jar()
        with self.assertRaises(AssertionError): release.verify_jar(self.jar,self.version)
    def test_wrong_version(self):
        self.write_jar()
        with self.assertRaises(AssertionError): release.verify_jar(self.jar,self.version+'-wrong')
    def test_fixture_leak(self):
        source=next((release.ROOT/'src/gameTest').rglob('*.java'))
        package=release.re.search(r'^package\s+([\w.]+)\s*;',source.read_text(),release.re.M)[1]
        self.entries[package.replace('.','/')+'/'+source.stem+'.class']=b'fixture'
        self.write_jar()
        with self.assertRaises(AssertionError): release.verify_jar(self.jar,self.version)
    def test_bundle_tampering_and_wrong_commit(self):
        self.write_jar()
        digest=hashlib.sha256(self.jar.read_bytes()).hexdigest()
        manifest=dict(version=self.version,commit='source',sha256=digest,curseforge_project=release.PROJECT_ID)
        (self.folder/'release-manifest.json').write_text(json.dumps(manifest))
        (self.folder/'SHA256SUMS').write_text(f'{digest}  {self.jar.name}\n')
        release.verify_bundle(self.folder,self.version,'source')
        with self.assertRaises(AssertionError): release.verify_bundle(self.folder,self.version,'different')
        self.entries['extra.txt']=b'tampered';self.write_jar()
        with self.assertRaises(AssertionError): release.verify_bundle(self.folder,self.version,'source')

if __name__ == '__main__':
    unittest.main()
