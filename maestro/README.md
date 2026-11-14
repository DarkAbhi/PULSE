# Maestro smoke tests

CI runs only the two native Android flows in [`android/`](android/):

- `launch.yaml`: cleared app state opens the sign-in screen and its controls.
- `required-fields.yaml`: empty credentials show local validation; entering
  only a username clears its error while the password remains required.

These flows never submit a complete credential pair. They need an installed
debug APK and an API 35 emulator, but no backend, database, account or secrets.
Each flow clears the test app's state. Use a test emulator, not an installation
whose local session you want to keep.

```sh
cd pulse-android
./gradlew :app:assembleDebug
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
cd ..
MAESTRO_CLI_NO_ANALYTICS=1 maestro test --format junit --output test-results/maestro.xml --debug-output test-results/maestro-debug --flatten-debug-output maestro/android/
```

Use Java 21 for Maestro. The Android build uses Java 25. CI installs both and
runs the smoke tests after instrumentation tests on a separate, freshly created
emulator. This keeps instrumentation processes, display overrides and device
settings out of the smoke-test environment. The ADB server is restarted before
the smoke emulator starts, removing transports left by the preceding device.
Maestro uses port 5556 and explicitly selects that emulator, instead of reusing
the instrumentation device's port. Five consecutive successful ADB/boot/shell
probes are required before starting its session.
Both stages reuse the built APKs.

CI uses `--flatten-debug-output` so Maestro's command records, screenshots and
logs are written directly under the report directory, instead of a hidden
`.maestro` directory excluded by artifact uploads. On failure, CI also captures
logcat and window state, prints the last 200 Maestro log lines and preserves the
failing exit status. Reports are uploaded in `pulse-android-device-reports`.
If every failed JUnit case explicitly identifies a lost device connection, CI
waits for stable transport and allows one fresh Maestro session for the suite.
Assertion failures, mixed assertion/transport failures, unknown errors and missing
reports are never retried. A second failure fails CI. Both attempts remain under
`build/reports/maestro/attempt-1/` and `attempt-2/`; `junit.xml` at the report root
contains the final attempt. Recovery is announced as a workflow warning.

## Paused browser coverage

The existing Maestro browser flows are retained under [`web/`](web/) for later
work. They are excluded from CI. The frontend Playwright journeys are also
paused in CI; frontend unit tests, type checking and production builds remain.
Always select `maestro/android/` explicitly, rather than running this entire
directory, which also contains the paused browser flows.

## Verification

Both native flows passed together on the isolated API 35 emulator using Maestro
2.11.0 and Java 21: **2/2 flows in 29 seconds, then 2/2 in 25 seconds after an ADB
server reset**. Workflow YAML, emulator isolation, native-only flow selection,
Bash syntax and preservation of a failing Maestro exit status were validated.
The GitHub report contained only `Unknown error`; local diagnostics reproduced
an emulator transport disconnect. The updated GitHub-hosted workflow still needs
execution to confirm that the isolated startup resolves its failure.
