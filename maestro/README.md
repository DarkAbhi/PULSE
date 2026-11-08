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
MAESTRO_CLI_NO_ANALYTICS=1 maestro test --format junit --output test-results/maestro.xml --debug-output test-results/maestro-debug maestro/android/
```

Use Java 21 for Maestro. The Android build uses Java 25. CI installs both and
runs the smoke tests after instrumentation tests on the same emulator, then
uploads their JUnit report and debug files in `pulse-android-device-reports`.

## Paused browser coverage

The existing Maestro browser flows are retained under [`web/`](web/) for later
work. They are excluded from CI. The frontend Playwright journeys are also
paused in CI; frontend unit tests, type checking and production builds remain.
Always select `maestro/android/` explicitly, rather than running this entire
directory, which also contains the paused browser flows.

## Verification

Both native flows passed locally on the dedicated API 35 emulator using Maestro
2.11.0 and Java 21: **2/2 flows in 26 seconds**. The debug APK built successfully;
workflow YAML, native-only flow selection and the CI shell script were validated.
The updated GitHub-hosted workflow still needs execution after these changes are
pushed.
