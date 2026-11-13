"""Validate public document inventory, relative links, exclusions and exact checksums."""
import argparse
import hashlib
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[2]
SUMS = ROOT / 'docs/SHA256SUMS.txt'
ROOT_DOCS = ['README.md', 'AGENTS.md', 'PRD.md', 'ARCHITECTURE.md',
             'UI_DESIGN_SPEC.md', 'IMPLEMENTATION_PLAN.md', 'TEST_PLAN.md']
FORBIDDEN = re.compile(r'interview-snapshots|docs/decisions/archive|ccarenv_|Bad credentials|'
                       r'payments have failed|productionresultssa|'
                       r'github\.com/GlebSkorobogatov/reading-journal-android(?!-public)', re.I)


def documents():
    paths = [ROOT / x for x in ROOT_DOCS]
    paths += sorted((ROOT / 'docs').rglob('*.md'))
    paths += [ROOT / 'docs/FILES.txt']
    if any(not p.is_file() for p in paths):
        raise ValueError('Required document or inventory absent')
    return sorted(paths)


def expected_sums():
    return ''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' +
                   p.relative_to(ROOT).as_posix() + '\n' for p in documents())


def check_links():
    count = 0
    for path in documents():
        text = path.read_text()
        if FORBIDDEN.search(text):
            raise ValueError('Excluded private-history reference in ' + str(path.relative_to(ROOT)))
        # Validate Markdown links (including percent-encoded Cyrillic); external links are not fetched.
        for match in re.finditer(r'!?\[[^\]]*\]\(([^\s)]+)(?:\s+"[^"]*")?\)', text):
            url = match.group(1).strip('<>')
            parts = urlsplit(url)
            if parts.scheme or parts.netloc or not parts.path:
                continue
            target = (path.parent / unquote(parts.path)).resolve()
            if not target.is_relative_to(ROOT) or not target.exists():
                raise ValueError('Broken/escaping document link: ' + str(path.relative_to(ROOT)) + ' -> ' + url)
            if parts.fragment and target.suffix == '.md':
                anchors = set()
                seen = {}
                for heading in re.findall(r'^#{1,6}\s+(.+?)\s*#*$', target.read_text(), re.M):
                    slug = re.sub(r'[^\w\- ]', '', heading.lower()).replace(' ', '-')
                    number = seen.get(slug, 0)
                    seen[slug] = number + 1
                    anchors.add(slug if number == 0 else f'{slug}-{number}')
                if unquote(parts.fragment) not in anchors:
                    raise ValueError('Missing document heading: ' + url)
            count += 1
    return count


def check_inventory():
    listed = (ROOT / 'docs/FILES.txt').read_text().splitlines()
    if listed != sorted(set(listed)):
        raise ValueError('FILES.txt must be sorted and unique')
    for item in listed:
        path = ROOT / item
        if not path.resolve().is_relative_to(ROOT) or not path.is_file():
            raise ValueError('Absent/escaping inventory item')
    if (ROOT / '.git').exists():
        tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().rstrip('\0').split('\0')
        if set(tracked) != set(listed):
            raise ValueError('FILES.txt differs from Git index; update inventory before checksums')
    return len(listed)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    inventory = check_inventory()
    links = check_links()
    expected = expected_sums()
    if args.write:
        SUMS.write_text(expected)
    elif not SUMS.is_file() or SUMS.read_text() != expected:
        raise SystemExit('Documentation checksums differ; run python3 .github/scripts/verify_docs.py --write')
    print(f'PASS: {inventory} inventory files; {len(documents())} document hashes; {links} local links; exclusions checked')


if __name__ == '__main__':
    main()
