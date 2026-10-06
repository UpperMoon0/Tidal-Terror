"""Completion gates must track their own annotated native fixtures."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]

class NativeSuiteGateTests(unittest.TestCase):
    def test_independent_suite_counts_match_native_fixtures(self):
        build=(ROOT/'build.gradle').read_text(encoding='utf8')
        for flag,folder in [('veilglowTests','veilglowTest'),('shardbackTests','shardbackTest'),('foodTests','foodTest'),('spawnPoolTests','spawnPoolTest')]:
            with self.subTest(suite=flag):
                actual=sum(len(re.findall(r'@GameTest\s*\(',source.read_text(encoding='utf8'))) for source in (ROOT/'src'/folder/'java').rglob('*.java'))
                start=build.index("if (project.hasProperty('"+flag+"'))")
                end=build.find('\nif (',start+1)
                gate=build[start:end]
                self.assertGreater(actual,0)
                self.assertIn(f'{actual} GAME TESTS COMPLETE',gate)
                self.assertIn(f'All {actual} required tests passed',gate)

if __name__=='__main__':unittest.main()
