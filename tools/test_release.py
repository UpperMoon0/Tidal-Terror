import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch
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
    def test_release_channel_defaults_to_release(self):
        self.assertEqual(release.release_type('mod_version=0.0.5\n'), 'release')
    def test_supported_release_channels(self):
        for channel in ('alpha', 'beta', 'release'):
            with self.subTest(channel=channel):
                self.assertEqual(release.release_type('mod_release_type = '+channel+'\r\n'), channel)
    def test_invalid_release_channels(self):
        for channel in ('', 'Alpha', 'stable', 'alpha;script'):
            with self.subTest(channel=channel), self.assertRaises(ValueError):
                release.release_type('mod_release_type='+channel)
    def test_plan_outputs_configured_channel(self):
        for channel in ('alpha', 'beta', 'release'):
            with self.subTest(channel=channel), \
                 patch.object(release, 'current', return_value='0.0.5'), \
                 patch.object(release, 'current_release_type', return_value=channel), \
                 patch.object(release, 'command', return_value='sha'), \
                 patch.object(release, 'tag_commit', return_value=None), \
                 patch.object(release, 'output') as output, \
                 patch.dict(release.os.environ, {}, clear=True), \
                 patch('sys.argv', ['release.py', 'plan']):
                release.main()
                output.assert_called_once_with(changed=True, value='0.0.5', release_type=channel)
    def test_all_curseforge_upload_steps_use_validated_channel(self):
        workflow=(release.ROOT/'.github/workflows/release.yml').read_text()
        upload_steps=workflow.split('uses: Kira-NT/mc-publish@')[1:]
        self.assertEqual(len(upload_steps), 2, 'Expected Forge and port upload steps')
        for step in upload_steps:
            inputs=step.split('      - name:', 1)[0]
            self.assertIn('version-type: ${{ needs.version-change.outputs.release_type }}', inputs)
        self.assertIn('release_type: ${{ steps.version.outputs.release_type }}', workflow)
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

    def test_package_verifies_docs_commit_after_version_was_released(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root/'gradle.properties').write_text('mod_version=0.0.4\nmod_release_type=beta\n')
            (root/'build/libs').mkdir(parents=True)
            jar = root/'build/libs/tidalterror-0.0.4.jar'
            jar.write_bytes(b'production jar')
            (root/'changelogs').mkdir()
            (root/'changelogs/v0.0.4.md').write_text('# Release notes')
            output = root/'bundle'
            with patch.object(release, 'ROOT', root), \
                 patch.object(release, 'current', return_value='0.0.4'), \
                 patch.object(release, 'command', return_value='docs-commit'), \
                 patch.object(release, 'tag_commit', return_value='released-commit') as tag, \
                 patch.object(release, 'verify_jar', return_value='a'*64) as verify, \
                 patch.object(release, 'verify_bundle') as bundle, \
                 patch.dict(release.os.environ, {}, clear=True), \
                 patch('sys.argv', ['release.py', 'package', '--output', str(output)]):
                release.main()
            verify.assert_called_once_with(jar, '0.0.4')
            bundle.assert_called_once_with(output, '0.0.4', 'docs-commit')
            tag.assert_not_called()
            self.assertEqual((output/jar.name).read_bytes(), jar.read_bytes())
            self.assertEqual(json.loads((output/'release-manifest.json').read_text())['commit'], 'docs-commit')
            self.assertEqual(json.loads((output/'release-manifest.json').read_text())['release_type'], 'beta')

    def test_publication_still_rejects_docs_commit_for_released_version(self):
        with tempfile.TemporaryDirectory() as temp:
            notes = Path(temp)/'v0.0.4.md'
            notes.write_text('# Release notes')
            with self.assertRaisesRegex(ValueError, 'already belongs'):
                release.preflight('0.0.4', 'docs-commit', 'released-commit', notes)


class GitHubChannelTests(unittest.TestCase):
    def test_prepare_and_finalize_commands_for_each_channel(self):
        for channel in ('alpha', 'beta', 'release'):
            with self.subTest(channel=channel), tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                (root/'gradle.properties').write_text('mod_version=0.0.5\nmod_release_type='+channel+'\n')
                (root/'changelogs').mkdir()
                notes = root/'changelogs/v0.0.5.md'
                notes.write_text('# Release notes')
                folder = root/'bundle'
                folder.mkdir()
                manifest = dict(version='0.0.5',release_type=channel,commit='sha',sha256='forge-digest',
                                ports=[dict(target='fabric-1.20.1',jar='fabric.jar',sha256='port-digest')])
                info = dict(isDraft=True,assets=[dict(name=name) for name in release.bundle_assets(manifest)])
                with patch.object(release, 'ROOT', root), \
                     patch.object(release, 'command', return_value='sha'), \
                     patch.object(release, 'tag_commit', return_value='sha'), \
                     patch.object(release, 'verify_bundle', return_value=manifest), \
                     patch.object(release, 'release_info', side_effect=[None, info]), \
                     patch.object(release, 'download'), \
                     patch.object(release, 'output'), \
                     patch.object(release, 'gh') as gh, \
                     patch.dict(release.os.environ, {}, clear=True), \
                     patch('sys.argv', ['release.py', 'prepare', '--output', str(folder)]):
                    release.main()
                expected_flags = ('--prerelease=false', '--latest') if channel == 'release' else ('--prerelease', '--latest=false')
                self.assertEqual(gh.call_args_list[0].args,
                    ('release','create','v0.0.5','--verify-tag','--draft','--title','Tidal Terror v0.0.5','--notes-file',str(notes),*expected_flags))
                # Finalization also corrects metadata on a resumed, older draft.
                for name, digest in (('curseforge-upload.json','forge-digest'), ('curseforge-fabric-1.20.1.json','port-digest')):
                    (folder/name).write_text(json.dumps(dict(project_id=release.PROJECT_ID,file_id=123,commit='sha',sha256=digest)))
                with patch.object(release, 'ROOT', root), \
                     patch.object(release, 'command', return_value='sha'), \
                     patch.object(release, 'tag_commit', return_value='sha'), \
                     patch.object(release, 'verify_bundle', return_value=manifest), \
                     patch.object(release, 'download') as download, \
                     patch.object(release, 'gh') as gh, \
                     patch.dict(release.os.environ, {}, clear=True), \
                     patch('sys.argv', ['release.py', 'finalize', '--output', str(folder)]):
                    release.main()
                gh.assert_called_once_with('release','edit','v0.0.5','--draft=false',*expected_flags)
                self.assertEqual(download.call_count, 2, 'Both Forge and port receipts must still be checked')


class ArtifactTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.folder = Path(self.temp.name)
        self.version = release.current()
        self.jar = self.folder/f'tidalterror-{self.version}.jar'
        self.entries = {}
        props = release.properties((release.ROOT/'gradle.properties').read_text())
        for dirname in ('src/main/resources','src/generated/resources','src/forgeProvince/resources'):
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
    def test_legacy_dependency_rejected(self):
        self.entries['META-INF/mods.toml'] += b'\n[[dependencies.tidalterror]]\nmodId="terrablender"\nmandatory=true\nversionRange="*"\nside="BOTH"\n'
        self.write_jar()
        with self.assertRaises(AssertionError): release.verify_jar(self.jar,self.version)
    def test_vanilla_only_default_rejected(self):
        key='data/minecraft/worldgen/world_preset/normal.json'
        data=json.loads(self.entries[key])
        data['dimensions']['minecraft:overworld']['generator']['biome_source']={'type':'minecraft:multi_noise','preset':'minecraft:overworld'}
        self.entries[key]=json.dumps(data).encode()
        self.write_jar()
        with self.assertRaises(AssertionError): release.verify_jar(self.jar,self.version)
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
        manifest=dict(version=self.version,release_type=release.current_release_type(),commit='source',sha256=digest,curseforge_project=release.PROJECT_ID)
        (self.folder/'release-manifest.json').write_text(json.dumps(manifest))
        (self.folder/'SHA256SUMS').write_text(f'{digest}  {self.jar.name}\n')
        release.verify_bundle(self.folder,self.version,'source')
        with self.assertRaises(AssertionError): release.verify_bundle(self.folder,self.version,'different')
        self.entries['extra.txt']=b'tampered';self.write_jar()
        with self.assertRaises(AssertionError): release.verify_bundle(self.folder,self.version,'source')

    def test_bundle_release_channel_must_match_config(self):
        self.write_jar()
        digest=hashlib.sha256(self.jar.read_bytes()).hexdigest()
        for configured in ('alpha', 'beta', 'release'):
            with self.subTest(configured=configured), patch.object(release, 'current_release_type', return_value=configured):
                manifest=dict(version=self.version,release_type=configured,commit='source',sha256=digest,curseforge_project=release.PROJECT_ID)
                (self.folder/'release-manifest.json').write_text(json.dumps(manifest))
                (self.folder/'SHA256SUMS').write_text(f'{digest}  {self.jar.name}\n')
                release.verify_bundle(self.folder,self.version,'source')
                manifest['release_type']='beta' if configured == 'alpha' else 'alpha'
                (self.folder/'release-manifest.json').write_text(json.dumps(manifest))
                with self.assertRaisesRegex(AssertionError, 'channel mismatch'):
                    release.verify_bundle(self.folder,self.version,'source')

    def test_legacy_bundle_defaults_to_release(self):
        self.write_jar()
        digest=hashlib.sha256(self.jar.read_bytes()).hexdigest()
        manifest=dict(version=self.version,commit='source',sha256=digest,curseforge_project=release.PROJECT_ID)
        (self.folder/'release-manifest.json').write_text(json.dumps(manifest))
        (self.folder/'SHA256SUMS').write_text(f'{digest}  {self.jar.name}\n')
        with patch.object(release, 'current_release_type', return_value='release'):
            release.verify_bundle(self.folder,self.version,'source')
        with patch.object(release, 'current_release_type', return_value='alpha'), self.assertRaises(AssertionError):
            release.verify_bundle(self.folder,self.version,'source')

if __name__ == '__main__':
    unittest.main()
