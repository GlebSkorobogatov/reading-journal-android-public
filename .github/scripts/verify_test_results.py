"""Require real, successful JUnit execution and expose results in CI annotations."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


def annotation(level: str, message: str) -> None:
    # Escape GitHub workflow commands; XML messages can contain newlines or '%'.
    escaped = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print(f'::{level}::{escaped}')


def fail(message: str) -> None:
    annotation('error', message)
    if args.diagnostic_log is not None:
        if args.diagnostic_log.is_file():
            output = args.diagnostic_log.read_text(errors='replace')
            excerpt = output if len(output) <= 16000 else output[:6000] + '\n[... log truncated ...]\n' + output[-10000:]
            annotation('error', f'Instrumentation Gradle output:\n{excerpt}')
        else:
            annotation('error', 'Instrumentation Gradle log is absent; no test-command output was captured.')
    raise SystemExit(message)


parser = argparse.ArgumentParser()
parser.add_argument('directory', type=Path)
parser.add_argument('--minimum', type=int, required=True)
parser.add_argument('--diagnostic-log', type=Path)
args = parser.parse_args()
files = list(args.directory.rglob('TEST-*.xml'))
if not files:
    fail(f'No JUnit results in {args.directory}')
count = 0
unsuccessful = []
for path in files:
    root = ET.parse(path).getroot()
    cases = list(root.iter('testcase'))
    # AGP 9 emits a testsuites container; AGP 8 used a single testsuite.
    # Validate every declared aggregate without counting nested cases twice.
    if root.tag not in ('testsuite', 'testsuites'):
        fail(f'Unsupported JUnit root {root.tag}: {path}')
    suites = list(root.iter('testsuite'))
    if not suites:
        fail(f'No JUnit suites in {path}; root={root.tag}, attributes={root.attrib}')
    containers = [root, *suites] if root.tag == 'testsuites' else suites
    for container in containers:
        actual = len(list(container.iter('testcase')))
        declared = container.get('tests')
        if (container.tag == 'testsuite' and declared is None) or (
            declared is not None and actual != int(declared)
        ):
            fail(f'Inconsistent test count: {path}; root={root.tag}, '
                 f'container={container.tag}, declared={declared}, actual={actual}')
        for outcome in ('failures', 'errors', 'skipped'):
            if int(container.get(outcome, '0')):
                unsuccessful.append(f'{outcome} in {path}')
    for case in cases:
        for outcome in ('failure', 'error', 'skipped'):
            result = case.find(outcome)
            if result is not None:
                detail = result.get('message') or result.text or outcome
                annotation('error', f"{case.get('classname')}.{case.get('name')}: {detail}")
                unsuccessful.append(f'{outcome}: {case.get("name")}')
    count += len(cases)
if unsuccessful:
    fail(f'{count} tests executed; unsuccessful results: {"; ".join(unsuccessful)}')
if count < args.minimum:
    fail(f'Only {count} tests executed; expected at least {args.minimum}')
message = f'PASS: {count} executed, 0 failed, 0 errors, 0 skipped ({len(files)} report files)'
print(message)
annotation('notice', message)
