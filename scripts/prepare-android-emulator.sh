#!/usr/bin/env bash
# Run from pulse-android after building the debug APK.
set -euo pipefail

reports=${1:-build/reports/device-environment}
mkdir -p "$reports"
timeout 15s adb shell dumpsys activity lastanr > "$reports/startup-last-anr.txt"
timeout 15s adb shell dumpsys window > "$reports/startup-window.txt"

# Fresh Google APIs images can leave a Pixel Launcher ANR dialog above every
# app. Recover only that known launcher failure, never an ANR in Pulse.
focus=$(sed -n '/mCurrentFocus=/p' "$reports/startup-window.txt")
if [[ "$focus" == *'Application Not Responding: com.google.android.apps.nexuslauncher'* ]]; then
  echo "::warning::Pixel Launcher stopped responding during boot; clearing its blocking dialog before testing."
  timeout 15s adb shell am force-stop com.google.android.apps.nexuslauncher
fi

timeout 60s adb install -r -g app/build/outputs/apk/debug/app-debug.apk
timeout 30s adb shell am start -W -n dev.abhishekan.pulse/.MainActivity

# sys.boot_completed alone does not guarantee an interactive emulator. Require
# the installed app to own input focus before starting instrumentation/Maestro.
for attempt in {1..30}; do
  timeout 15s adb shell dumpsys window > "$reports/startup-window-current.txt"
  focus=$(sed -n '/mCurrentFocus=/p' "$reports/startup-window-current.txt")
  printf '%s\n' "$focus" > "$reports/startup-focus.txt"
  if [[ "$focus" == *'dev.abhishekan.pulse/'* && "$focus" != *'Application Not Responding'* ]]; then
    echo "Android emulator ready: Pulse owns input focus."
    exit 0
  fi
  sleep 1
done

echo "::error::The emulator cannot give Pulse input focus. See startup-window-current.txt and startup-last-anr.txt."
exit 1
