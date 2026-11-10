#!/usr/bin/env bash
# Run from pulse-android. Keep compound commands out of emulator-runner's
# line-by-line script input: it starts a separate sh process for each line.
set -euo pipefail

reports=build/reports/maestro
mkdir -p "$reports"
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
timeout --signal=TERM --kill-after=5s 120s bash "$script_dir/prepare-android-emulator.sh" "$reports"

# Flatten avoids the hidden .maestro directory that upload-artifact excludes.
if JAVA_HOME="${JAVA_HOME_21_X64:?Java 21 is required for Maestro}" \
  PATH="$JAVA_HOME_21_X64/bin:$PATH" \
  maestro test --format junit --output "$reports/junit.xml" \
    --debug-output "$reports/debug" --flatten-debug-output ../maestro/android/; then
  echo "Maestro smoke tests passed"
else
  status=$?
  timeout 15s adb logcat -d > "$reports/logcat.txt" 2>&1 || true
  timeout 15s adb shell dumpsys window > "$reports/window.txt" 2>&1 || true
  tail -n 200 "$reports/debug/maestro.log" || true
  exit "$status"
fi
