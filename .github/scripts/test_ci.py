"""Regression tests for CI decisions and evidence guards; never Android-test evidence."""
import copy
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

HERE = Path(__file__).resolve().parent


def load(name):
    spec = importlib.util.spec_from_file_location(name, HERE / (name + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


ci = load('ci_plan')
gate = load('full_gate')
POLICY = json.loads((HERE.parent / 'ci-policy.json').read_text())


class Decisions(unittest.TestCase):
    def test_docs_only_delta_skips_android_even_when_pr_contains_app(self):
        self.assertEqual(ci.plan('quick', POLICY, ['app/build.gradle.kts', 'README.md'], ['README.md'])['android'], 'false')

    def test_unknown_or_empty_delta_builds(self):
        for paths in [None, [], ['LICENSE'], ['.github/workflows/android.yml'], ['docs/image.png']]:
            with self.subTest(paths=paths):
                self.assertEqual(ci.plan('quick', POLICY, ['README.md'], paths)['android'], 'true')

    def test_deleted_or_renamed_code_in_delta_builds(self):
        self.assertFalse(ci.documentation_only(['README.md', 'app/src/main/java/Old.kt']))
        self.assertFalse(ci.documentation_only(['app/src/main/java/Old.kt', 'docs/notes.md']))

    def test_full_request_rebuilds_document_only_head(self):
        self.assertEqual(ci.plan('full', POLICY, ['README.md'], ['README.md'])['android'], 'true')

    def test_current_task_rejects_secrets_archives_and_other_layers(self):
        for name in ['.env', 'docs/backup.zip', 'docs/archive/x.md', 'local.properties',
                     'docs/key.properties', 'docs/credentials-owner.json', 'docs/.git-credentials',
                     'app/data.sqlite', 'app/src/main/java/ru/gleb/readingjournal/data/Store.kt', '../README.md']:
            with self.subTest(path=name), self.assertRaises(ValueError):
                ci.validate_scope([name], POLICY)

    def test_floors_and_matrix_cannot_be_weakened(self):
        for key, value in [('minimum_jvm', 2), ('minimum_compose', 9), ('compose_apis', [35, 36])]:
            p = copy.deepcopy(POLICY);p[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                ci.validate_policy(p)

    def test_future_task_floor_cannot_decrease(self):
        previous = copy.deepcopy(POLICY);previous['minimum_compose'] = POLICY['minimum_compose'] + 2
        with self.assertRaises(ValueError):ci.validate_policy(POLICY, previous)

    def test_full_gate_rejects_skips_failures_cancellation_and_missing_jobs(self):
        success = {k: {'result': 'success'} for k in ['checks', 'build', 'compose']}
        gate.verify(success)
        for value in ['skipped', 'failure', 'cancelled', None]:
            bad = copy.deepcopy(success);bad['compose']['result'] = value
            with self.subTest(value=value), self.assertRaises(ValueError):gate.verify(bad)
        with self.assertRaises(ValueError):gate.verify({'build': {'result': 'success'}})


class JunitGuard(unittest.TestCase):
    def invoke(self, body, minimum=1):
        with tempfile.TemporaryDirectory() as tmp:
            if body is not None:Path(tmp, 'TEST-synthetic.xml').write_text(body)
            return subprocess.run(['python3', str(HERE / 'verify_test_results.py'), tmp, '--minimum', str(minimum)], capture_output=True).returncode

    def test_accepts_actual_cases(self):
        self.assertEqual(self.invoke('<testsuite tests="1"><testcase name="synthetic"/></testsuite>'), 0)

    def test_rejects_missing_zero_and_inconsistent_results(self):
        for body in [None, '<testsuite tests="0"/>', '<testsuite tests="2"><testcase/></testsuite>']:
            with self.subTest(body=body):self.assertNotEqual(self.invoke(body), 0)

    def test_rejects_failed_errored_and_skipped_cases(self):
        for tag in ['failure', 'error', 'skipped']:
            with self.subTest(tag=tag):self.assertNotEqual(self.invoke(f'<testsuite tests="1"><testcase><{tag}/></testcase></testsuite>'), 0)

    def test_rejects_below_required_count(self):
        self.assertNotEqual(self.invoke('<testsuite tests="1"><testcase/></testsuite>', 3), 0)


class GitIntegration(unittest.TestCase):
    """Exercise real before/base/head comparisons in isolated synthetic repositories."""
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.env = {**os.environ, 'GIT_CONFIG_GLOBAL': os.devnull,
                    'GIT_AUTHOR_NAME': 'CI Fixture', 'GIT_COMMITTER_NAME': 'CI Fixture',
                    'GIT_AUTHOR_EMAIL': 'ci-fixture@example.invalid',
                    'GIT_COMMITTER_EMAIL': 'ci-fixture@example.invalid',
                    'GITHUB_OUTPUT': str(self.root / 'outputs')}
        self.git('init', '--initial-branch=main')
        (self.root / '.github').mkdir()
        (self.root / '.github/ci-policy.json').write_text(json.dumps(POLICY))
        (self.root / 'README.md').write_text('Synthetic CI fixture\n')
        self.base = self.commit()

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, env=self.env,
                                       stderr=subprocess.DEVNULL, text=True).strip()

    def commit(self):
        self.git('add', '--all')
        self.git('commit', '-m', 'Synthetic fixture')
        return self.git('rev-parse', 'HEAD')

    def invoke(self, before=None, requested_head=None, requested_base=None):
        env = {**self.env, 'CI_HEAD': requested_head or self.git('rev-parse', 'HEAD'),
               'CI_BASE': requested_base or self.base, 'CI_BEFORE': before or self.base}
        return subprocess.run(['python3', str(HERE / 'ci_plan.py'), '--mode', 'quick'],
                              cwd=self.root, env=env, capture_output=True, text=True)

    def test_real_docs_delta_does_not_rebuild_the_entire_pr(self):
        path = self.root / 'app/src/main/java/ru/gleb/readingjournal/ui/home/Fixture.kt'
        path.parent.mkdir(parents=True)
        path.write_text('// Synthetic fixture, never application code\n')
        code_head = self.commit()
        (self.root / 'README.md').write_text('Updated synthetic fixture\n')
        self.commit()
        result = self.invoke(before=code_head)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)['android'], 'false')

    def test_missing_delta_endpoint_fails_closed_to_build(self):
        (self.root / 'README.md').write_text('Updated fixture\n')
        self.commit()
        result = self.invoke(before='0' * 40)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)['android'], 'true')

    def test_missing_scope_baseline_checks_whole_tree_and_builds(self):
        result = self.invoke(requested_base='0' * 40, before='0' * 40)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)['android'], 'true')

    def test_whitespace_failure_is_not_swallowed_as_unknown_diff(self):
        (self.root / 'README.md').write_text('Synthetic trailing whitespace   \n')
        self.commit()
        self.assertNotEqual(self.invoke().returncode, 0)

    def test_requested_head_must_match_checkout(self):
        (self.root / 'README.md').write_text('Updated fixture\n')
        self.commit()
        self.assertNotEqual(self.invoke(requested_head=self.base).returncode, 0)


if __name__ == '__main__':
    unittest.main()
