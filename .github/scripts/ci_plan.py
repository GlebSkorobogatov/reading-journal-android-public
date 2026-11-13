"""Fail-closed change classification, task-path scope and nondecreasing test floors."""
import argparse
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess

ROOT_DOCS = {'README.md', 'AGENTS.md', 'PRD.md', 'ARCHITECTURE.md',
             'UI_DESIGN_SPEC.md', 'IMPLEMENTATION_PLAN.md', 'TEST_PLAN.md'}


def documentation_only(paths):
    return bool(paths) and all(
        path in ROOT_DOCS or (path.startswith('docs/') and Path(path).suffix in {'.md', '.txt'})
        for path in paths
    )


def validate_policy(policy, previous=None):
    for field, floor in [('minimum_jvm', 3), ('minimum_compose', 10)]:
        value = policy.get(field)
        if type(value) is not int or value < floor:
            raise ValueError(f'{field} below UI-01 floor or invalid')
        if previous and value < previous[field]:
            raise ValueError(f'{field} must not decrease')
    apis = policy.get('compose_apis', [])
    if not apis or any(type(x) is not int for x in apis) or len(apis) != len(set(apis)):
        raise ValueError('Invalid API matrix')
    required = {26, 35, 36} | set(previous['compose_apis'] if previous else [])
    if not required <= set(apis):
        raise ValueError('Required API removed')
    if not policy.get('task') or not policy.get('allowed_prefixes') or not policy.get('allowed_files'):
        raise ValueError('Task/scope policy missing')


def validate_scope(paths, policy):
    forbidden_suffixes = {'.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.db',
                          '.sqlite', '.sqlite3', '.zip', '.7z', '.rar', '.apk', '.aab'}
    for name in paths:
        p = PurePosixPath(name)
        if (p.is_absolute() or '..' in p.parts or any(ord(c) < 32 for c in name)
                or p.suffix.lower() in forbidden_suffixes
                or any(part in {'.git', '.gradle', 'build', 'archive', 'archives'} for part in p.parts)
                or p.name in {'local.properties', 'google-services.json', '.env',
                              'key.properties', '.git-credentials'}
                or (p.name.startswith('credentials') and p.suffix.lower() == '.json')
                or p.name.startswith('.env.')):
            raise ValueError('Sensitive/generated/archive path in changes: ' + json.dumps(name))
        if name not in policy['allowed_files'] and not any(name.startswith(x) for x in policy['allowed_prefixes']):
            raise ValueError('Outside current task paths: ' + json.dumps(name))


def git(*args):
    return subprocess.check_output(['git', *args])


def changed(base, head):
    if not re.fullmatch(r'[0-9a-f]{40}', base or '') or not re.fullmatch(r'[0-9a-f]{40}', head or ''):
        raise ValueError('Missing/invalid diff endpoints')
    return [x.decode('utf8') for x in git('diff', '--name-only', '--no-renames', '-z', base, head).split(b'\0') if x]


def plan(mode, policy, scope_paths, delta_paths):
    validate_policy(policy)
    validate_scope(scope_paths, policy)
    # An unknown comparison never enables a skip. A full request always rebuilds.
    android = mode == 'full' or delta_paths is None or not documentation_only(delta_paths)
    return {'android': str(android).lower(), 'minimum_jvm': str(policy['minimum_jvm']),
            'minimum_compose': str(policy['minimum_compose']), 'apis': json.dumps(policy['compose_apis'])}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--mode', choices=['quick', 'full'], required=True)
    args = parser.parse_args()
    policy = json.loads(Path('.github/ci-policy.json').read_text())
    head = os.environ.get('CI_HEAD') or git('rev-parse', 'HEAD').decode().strip()
    if git('rev-parse', 'HEAD').decode().strip() != head:
        raise SystemExit('Checkout does not match requested Head')
    base = os.environ.get('CI_BASE', '')
    if not base:
        try:
            base = git('rev-parse', 'HEAD^').decode().strip()
        except subprocess.CalledProcessError:
            base = '0' * 40
    try:
        scope_paths = changed(base, head)
    except (ValueError, subprocess.CalledProcessError):
        scope_paths = git('ls-files', '-z').decode().rstrip('\0').split('\0')
        # If the baseline is unavailable, check the entire requested tree.
        empty_tree = subprocess.check_output(['git', 'hash-object', '-t', 'tree', '--stdin'],
                                             input=b'').decode().strip()
        subprocess.run(['git', 'diff', '--check', empty_tree, head], check=True)
    else:
        subprocess.run(['git', 'diff', '--check', base, head], check=True)
    try:
        previous = json.loads(git('show', base + ':.github/ci-policy.json'))
    except (subprocess.CalledProcessError, json.JSONDecodeError):
        previous = None
    validate_policy(policy, previous)
    before = os.environ.get('CI_BEFORE') or base
    try:
        delta_paths = changed(before, head)
    except (ValueError, subprocess.CalledProcessError):
        delta_paths = None
    result = plan(args.mode, policy, scope_paths, delta_paths)
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
            for key, value in result.items():
                f.write(f'{key}={value}\n')
    print(json.dumps({'task': policy['task'], 'mode': args.mode, **result,
                      'changed_paths': len(scope_paths), 'delta_known': delta_paths is not None}))


if __name__ == '__main__':
    main()
