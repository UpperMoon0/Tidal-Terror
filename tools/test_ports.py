import json
import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch
import ports
import release

class NativeCompletionTests(unittest.TestCase):
    def test_zero_exit_startup_failure_is_not_a_pass(self):
        with self.assertRaises(ValueError):
            ports.verify_results('Failed to start the minecraft server\nBUILD SUCCESSFUL',17)
    def test_partial_or_failed_suites_are_rejected(self):
        for log in ('All 16 required tests passed','All 17 required tests passed\n1 required tests failed'):
            with self.subTest(log=log),self.assertRaises(ValueError):ports.verify_results(log,17)
    def test_report_requires_unique_successful_cases(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'report.xml'
            for body in ('<testcase name="a"/><testcase name="a"/>','<testcase name="a"/><testcase name="b"><failure/></testcase>','<testcase name="a"/>'):
                path.write_text('<testsuite>'+body+'</testsuite>')
                with self.subTest(body=body),self.assertRaises(ValueError):ports.verify_results('All 2 required tests passed',2,path)
            path.write_text('<testsuite><testcase name="a"/><testcase name="b"/></testsuite>')
            ports.verify_results('All 2 required tests passed',2,path)

class ClientLoadingTests(unittest.TestCase):
    def test_ready_marker_does_not_hide_a_broken_item_model(self):
        with self.assertRaises(ValueError): ports.verify_client("Couldn't parse item model 'tidalterror:reef_spear'\nTIDAL_PORT_CLIENT_READY:")
    def test_completed_loading_required(self):
        with self.assertRaises(ValueError): ports.verify_client('BUILD SUCCESSFUL')
        ports.verify_client('TIDAL_PORT_CLIENT_READY: resources loaded')

class MatrixBundleTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.folder=Path(self.temp.name)
        self.manifest=dict(version='0.0.3',commit='source',curseforge_project=release.PROJECT_ID,sha256='forge-digest',ports=[])
        for target,spec in ports.TARGETS.items():
            self.manifest['ports'].append(dict(target=target,jar=f'tidalterror-{target}-0.0.3.jar',sha256=target,**{field:spec[field] for field in ('minecraft','loader','java')}))
    def tearDown(self):self.temp.cleanup()
    def write(self):
        (self.folder/'release-manifest.json').write_text(json.dumps(self.manifest))
        checksums='forge-digest  tidalterror-0.0.3.jar\n'+''.join(f"{r['sha256']}  {r['jar']}\n" for r in self.manifest['ports'])
        (self.folder/'SHA256SUMS').write_text(checksums)
    def verify(self):
        with patch.object(release,'verify_jar',return_value='forge-digest'),patch.object(ports,'verify_jar',side_effect=lambda path,target,version:target):
            return release.verify_bundle(self.folder,'0.0.3','source')
    def test_complete_matrix_passes_and_downloads_every_jar(self):
        self.write();manifest=self.verify()
        self.assertEqual(7,len(release.bundle_assets(manifest)))
    def test_incomplete_and_duplicate_matrix_rejected(self):
        for records in (self.manifest['ports'][:-1],self.manifest['ports']+[self.manifest['ports'][0]]):
            with self.subTest(records=records):
                original=self.manifest['ports'];self.manifest['ports']=records;self.write()
                with self.assertRaises(AssertionError):self.verify()
                self.manifest['ports']=original
    def test_wrong_loader_java_filename_and_digest_rejected(self):
        record=self.manifest['ports'][0]
        for field,value in (('loader','forge'),('java',25),('jar','../../evil.jar'),('sha256','changed')):
            with self.subTest(field=field):
                original=record[field];record[field]=value;self.write()
                with self.assertRaises(AssertionError):self.verify()
                record[field]=original
    def test_receipt_must_match_source_and_exact_artifact(self):
        receipt=dict(project_id=release.PROJECT_ID,file_id=1,sha256='jar',commit='source')
        release.verify_receipt(receipt,'jar','source')
        for field,value in (('file_id',0),('project_id',0),('commit','other'),('sha256','other')):
            with self.subTest(field=field):
                changed=receipt|{field:value}
                with self.assertRaises(AssertionError):release.verify_receipt(changed,'jar','source')

if __name__=='__main__':unittest.main()
