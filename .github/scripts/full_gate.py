"""Only a completed full workflow can emit the Full acceptance success check."""
import json
import os


def verify(needs):
    if set(needs) != {'checks', 'build', 'compose'}:
        raise ValueError('Full job set is incomplete')
    if any(job.get('result') != 'success' for job in needs.values()):
        raise ValueError('Full acceptance BLOCKED: missing, skipped, failed or cancelled job')


if __name__ == '__main__':
    verify(json.loads(os.environ['NEEDS_JSON']))
    print('PASS: full Build/lint/JVM and the complete required Android matrix succeeded in this run.')
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as f:
        f.write('Full automatic checks: PASS. Owner acceptance and merge require separate authorization.\n')
