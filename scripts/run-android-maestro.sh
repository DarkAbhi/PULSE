#!/usr/bin/env bash
# Run from pulse-android. Keep compound commands out of emulator-runner's
# line-by-line script input: it starts a separate sh process for each line.
set -euo pipefail

reports=build/reports/maestro
mkdir -p "$reports"
rm -f -- "$reports/junit.xml"
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
: "${ANDROID_SERIAL:?Select the smoke emulator explicitly}"
timeout --signal=TERM --kill-after=5s 90s bash "$script_dir/wait-android-device.sh"
timeout --signal=TERM --kill-after=5s 120s bash "$script_dir/prepare-android-emulator.sh" "$reports"

run_maestro() {
  # An interrupted CLI must not authorize recovery using an older run's report.
  rm -f -- "$attempt/junit.xml"
  # Flatten avoids the hidden .maestro directory excluded by artifact uploads.
  JAVA_HOME="${JAVA_HOME_21_X64:?Java 21 is required for Maestro}" \
    PATH="$JAVA_HOME_21_X64/bin:$PATH" \
    maestro --device "$ANDROID_SERIAL" test --format junit --output "$attempt/junit.xml" \
      --debug-output "$attempt/debug" --flatten-debug-output ../maestro/android/
}

diagnostics() {
  timeout 15s adb devices -l > "$attempt/devices.txt" 2>&1 || true
  timeout 15s adb logcat -d > "$attempt/logcat.txt" 2>&1 || true
  timeout 15s adb shell dumpsys window > "$attempt/window.txt" 2>&1 || true
  tail -n 200 "$attempt/debug/maestro.log" || true
}

attempt="$reports/attempt-1"
mkdir -p "$attempt"
status=0
if run_maestro; then
  echo "Maestro smoke tests passed"
else
  status=$?
  diagnostics
  if python "$script_dir/maestro_transport_failure.py" "$attempt/junit.xml"; then
    echo "::warning::Maestro lost its device connection. Preserving attempt-1 and allowing one fresh session after transport recovery."
    timeout --signal=TERM --kill-after=5s 90s bash "$script_dir/wait-android-device.sh"
    attempt="$reports/attempt-2"
    mkdir -p "$attempt"
    if run_maestro; then
      status=0
      echo "Maestro smoke tests passed after device connection recovery"
    else
      status=$?
      diagnostics
    fi
  fi
fi
# Keep the usual report location while retaining both attempts and their evidence.
if [[ -f "$attempt/junit.xml" ]]; then cp "$attempt/junit.xml" "$reports/junit.xml"; fi
exit "$status"
