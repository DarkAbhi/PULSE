#!/usr/bin/env bash
# Call under timeout. A single successful probe can precede an adbd reconnect.
set -euo pipefail
: "${ANDROID_SERIAL:?Select the test emulator explicitly}"
stable=0
while (( stable < 5 )); do
  state=$(timeout 5s adb get-state 2>/dev/null | tr -d '\r') || state=''
  boot=$(timeout 5s adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || boot=''
  probe=$(timeout 5s adb shell echo pulse-maestro-ready 2>/dev/null | tr -d '\r') || probe=''
  if [[ "$state" == device && "$boot" == 1 && "$probe" == pulse-maestro-ready ]]; then
    stable=$((stable + 1))
  else
    stable=0
  fi
  sleep 1
done
echo "Android transport stable: $ANDROID_SERIAL"
