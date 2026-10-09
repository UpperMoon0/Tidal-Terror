from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase
from zipfile import ZipFile

from audit_world_preset_conflicts import OWN_MARKER, TARGET, find_conflicts


class PresetCollisionAuditTests(TestCase):
    def test_detects_competing_mod_and_datapack(self):
        with TemporaryDirectory() as root:
            base = Path(root)
            mod = base / 'other-biomes.jar'
            other = base / 'safe.zip'
            with ZipFile(mod, 'w') as jar:
                jar.writestr(TARGET, '{}')
            with ZipFile(other, 'w') as jar:
                jar.writestr('data/other/tags/example.json', '{}')
            self.assertEqual(find_conflicts([base]), [str(mod) + ':' + TARGET])

    def test_does_not_report_tidal_terror_own_jar(self):
        with TemporaryDirectory() as root:
            self_jar = Path(root) / 'tidal-terror.jar'
            with ZipFile(self_jar, 'w') as jar:
                jar.writestr(TARGET, '{}')
                jar.writestr(OWN_MARKER, '{}')
            self.assertEqual(find_conflicts([self_jar]), [])

    def test_detects_extracted_datapack_and_ignores_irrelevant_files(self):
        with TemporaryDirectory() as root:
            base = Path(root)
            target = base / TARGET
            target.parent.mkdir(parents=True)
            target.write_text('{}', encoding='utf-8')
            (base / 'normal.json').write_text('{}', encoding='utf-8')
            self.assertEqual(find_conflicts([base]), [str(target)])
