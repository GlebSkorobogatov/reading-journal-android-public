"""Synthetic process stubs test exit-code/evidence ordering, never render evidence."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('run_android_suite.sh').resolve()


class AndroidEvidenceExit(unittest.TestCase):
    def invoke(self, suite_status=0, adb_status=0, tar_status=0):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            binary = root / 'bin'
            binary.mkdir()
            commands = {
                root / 'gradlew': '#!/bin/sh\nprintf "synthetic suite output\\n"\nexit "$MOCK_SUITE_STATUS"\n',
                binary / 'adb': '#!/bin/sh\nprintf "adb\\n" >> "$MOCK_TRACE"\nprintf "synthetic archive stub"\nexit "$MOCK_ADB_STATUS"\n',
                binary / 'tar': '#!/bin/sh\nprintf "tar\\n" >> "$MOCK_TRACE"\nexit "$MOCK_TAR_STATUS"\n',
            }
            for path, body in commands.items():
                path.write_text(body)
                path.chmod(0o700)
            trace = root / 'trace.txt'
            env = {**os.environ, 'PATH': str(binary) + os.pathsep + os.environ['PATH'],
                   'MOCK_SUITE_STATUS': str(suite_status), 'MOCK_ADB_STATUS': str(adb_status),
                   'MOCK_TAR_STATUS': str(tar_status), 'MOCK_TRACE': str(trace)}
            result = subprocess.run(['bash', str(SCRIPT), '1' * 40], cwd=root, env=env,
                                    text=True, capture_output=True)
            return result, trace.read_text().splitlines(), (root / 'app/build/connected-debug.log').read_text()

    def test_suite_exit_code_is_preserved_when_capture_succeeds(self):
        for status in (0, 1, 7):
            with self.subTest(status=status):
                result, trace, output = self.invoke(suite_status=status)
                self.assertEqual(result.returncode, status)
                self.assertEqual(trace, ['adb', 'tar'])
                self.assertIn('synthetic suite output', output)

    def test_missing_evidence_rejects_a_successful_suite(self):
        for statuses in ((1, 0), (0, 9)):
            with self.subTest(statuses=statuses):
                result, trace, _ = self.invoke(adb_status=statuses[0], tar_status=statuses[1])
                self.assertNotEqual(result.returncode, 0)
                self.assertIn('Unable to collect', result.stdout)
                self.assertIn('adb', trace)

    def test_capture_failure_never_hides_the_original_suite_failure(self):
        result, trace, _ = self.invoke(suite_status=7, tar_status=9)
        self.assertEqual(result.returncode, 7)
        self.assertEqual(trace, ['adb', 'tar'])


if __name__ == '__main__':
    unittest.main()
