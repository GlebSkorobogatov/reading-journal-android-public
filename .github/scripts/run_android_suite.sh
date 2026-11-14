#!/usr/bin/env bash
# Keep the suite's exit code and collect synthetic emulator evidence before shutdown.
set -euo pipefail
source_sha="${1:?Exact source SHA is required}"
if [[ ! "$source_sha" =~ ^[0-9a-f]{40}$ ]]; then
  echo '::error::Invalid source SHA'
  exit 2
fi
mkdir -p app/build/emulator-screenshots
collect_evidence() {
  local suite_status=$?
  local evidence_status
  trap - EXIT
  set +e
  adb exec-out run-as ru.gleb.readingjournal tar -C files -cf - screenshots > app/build/screenshots.tar
  evidence_status=$?
  if [ "$evidence_status" -eq 0 ]; then
    tar -xf app/build/screenshots.tar -C app/build/emulator-screenshots
    evidence_status=$?
  fi
  if [ "$evidence_status" -ne 0 ]; then
    echo '::error::Unable to collect emulator screenshot evidence'
    if [ "$suite_status" -eq 0 ]; then suite_status=1; fi
  fi
  exit "$suite_status"
}
trap collect_evidence EXIT
echo '::notice::Emulator booted; starting full instrumentation suite.'
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
  -Pandroid.testInstrumentationRunnerArguments.sourceSha="$source_sha" \
  2>&1 | tee app/build/connected-debug.log
