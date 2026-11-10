#!/usr/bin/env bash
# Run from pulse-android, with the emulator already booted.
set -euo pipefail

reports=build/reports/device-environment
mkdir -p "$reports"

cleanup() {
  status=$?
  trap - EXIT
  if (( status != 0 )); then
    if (( status == 124 )); then
      echo "::error::Instrumentation exceeded its 45-minute deadline (exit $status). See the last TestRunner event and device diagnostics."
    else
      echo "::error::Instrumentation failed (exit $status). See test progress and device diagnostics."
    fi
    # Bound every diagnostic: a wedged emulator must not consume the job's
    # remaining time and prevent artifact upload again.
    timeout 15s adb shell dumpsys power > "$reports/power.txt" 2>&1 || true
    timeout 15s adb shell dumpsys window > "$reports/window.txt" 2>&1 || true
    timeout 15s adb shell dumpsys activity > "$reports/activity.txt" 2>&1 || true
    timeout 15s adb exec-out screencap -p > "$reports/screen.png" 2> "$reports/screen-error.txt" || true
    test_pid=$(timeout 10s adb shell pidof dev.abhishekan.pulse | tr -d '\r') || true
    if [[ "$test_pid" =~ ^[0-9]+$ ]]; then
      timeout 10s adb shell run-as dev.abhishekan.pulse kill -3 "$test_pid" || true
    fi
    timeout 15s adb logcat -d > "$reports/logcat-final.txt" 2>&1 || true
  fi
  kill "$logcat_pid" "$progress_pid" 2>/dev/null || true
  wait "$logcat_pid" "$progress_pid" 2>/dev/null || true
  exit "$status"
}

# Gradle/UTP can buffer test results until the suite ends. Stream the runner's
# started/finished events so a hang identifies the last test in Actions logs.
adb logcat -c
adb logcat -v threadtime > "$reports/logcat.txt" 2>&1 &
logcat_pid=$!
adb logcat -v brief TestRunner:I AndroidRuntime:E '*:S' &
progress_pid=$!
trap cleanup EXIT

# Keep time for diagnostics, emulator shutdown, Maestro and report upload before
# the job's 90-minute cap. The host deadline also covers setup/teardown hangs,
# which AndroidJUnitRunner's per-test-method timeout cannot interrupt reliably.
timeout --signal=TERM --kill-after=30s 45m \
  ./gradlew --no-daemon --stacktrace --console=plain \
  -Pandroid.testInstrumentationRunnerArguments.timeout_msec=120000 \
  :app:connectedDebugAndroidTest 2>&1 | tee "$reports/gradle.txt"
