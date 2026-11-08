# Android CI verification — 8 November 2026

## Workflow

`.github/workflows/tests.yml` now runs all Android instrumentation tests in one
`android-ui` job on one API 35 emulator. The three-entry shard matrix and runner
shard arguments are removed. The job has a 90-minute limit and uploads one
`pulse-android-device-reports` artifact. Unit tests, lint and formatting retain
their separate job.

The previous shards shortened resize-heavy emulator sessions. One job avoids
three copies of APK building and emulator startup, at the cost of a longer
individual UI job.

## Corrections

- Supply isolated meal-onboarding fixtures when the account navigation test
  enters Meals, and wait for its asynchronous setup read.
- Dismiss the exercise selector through the current bottom-sheet interaction.
- Enter Maintenance before opening its editors; use current back labels,
  accessibility labels and editable-field selectors in Garage and Horizon.
- Expand half-open sheets before reaching their lower actions.
- Keep the visibility wait without a duplicate assertion that races keyboard
  dismissal before a semantic click.
- Wait for Android density and Compose window dimensions to match display
  overrides before asserting Meal Plan's responsive layout.

### Window-focus failures reported after the first local pass

The reported failures included Espresso selecting an unfocused activity or
popup root, Back events that did not dismiss sheets, and clipboard/IME checks
after recreation or resizing. The earlier local pass did not establish that
these interactions were stable in CI.

`DeviceWindowTestActions.kt` now waits for the focusable dialog/popup window,
hides the IME using each app window's own token, and issues Android's system
Back action without Espresso root selection. Navigation and sheet-dismissal
assertions remain enabled. Account and Horizon resize checks also wait for
the requested dimensions, density and font scale before window-sensitive
actions; dialog confirmations wait for focus before tapping Copy token.

These helpers use Android's public [window inspection API](https://developer.android.com/reference/android/view/inspector/WindowInspector)
and [system action API](https://developer.android.com/reference/android/app/UiAutomation#performGlobalAction(int)).

The CI script explicitly wakes/unlocks the emulator, keeps it awake, and
enables the software keyboard. If Gradle fails, it records power state,
window state and logcat before emulator cleanup, preserves the failing exit
status and uploads these files with the device reports. Screen sleep was not
confirmed from the supplied traces; the explicit settings remove that
environmental variable and the diagnostics support investigating any further
GitHub-only failure.

No tests were disabled or excluded from the CI job.

## Results and limits

Verification used the dedicated `Pulse_CI_API_35` Android 15 emulator on Windows,
JDK 25 and the repository's Gradle wrapper, with isolated HTTP fixtures.

- The unit suite passed; its reports contain **253 passing tests**, with zero
  failures, errors or skips.
- `lint` and `ktlintCheck` passed. The final instrumentation sources also passed
  `:app:ktlintAndroidTestSourceSetCheck` and compiled successfully.
- **All 167 UI/device tests passed together in one complete, unsharded run**,
  with zero failures, errors or skips. The Gradle invocation, including final
  device invocation completed in **10m 55s** after the focus corrections.
- Nine reported or related focus, clipboard, keyboard, Back and resize cases
  passed **three consecutive focused runs: 27/27 executions**, with zero
  failures, errors or skips. This includes the additional Account failures
  in the attachment and the related Horizon section-navigation case.
- The updated CI command block passed Bash syntax validation. No automatic
  retry or test exclusion was added to the workflow.

The GitHub-hosted Linux workflow has not been executed for these local changes.
No push, merge or release was performed.

## Maestro smoke integration — 9 November 2026

Browser E2E execution is paused in CI: the Maestro browser job and frontend
Playwright steps are removed. Existing browser flows remain under `maestro/web/`
for later work. Frontend unit tests and builds remain enabled; database-seed
checks retain their PostgreSQL service in a separate `seed-database` job.

The Android job now installs Java 21 alongside Java 25 and Maestro 2.11.0. After
instrumentation it reinstalls the debug APK and runs only `maestro/android/` on
the same emulator. The two flows cover the sign-in screen and required-field
validation. No complete credential pair is submitted, and no backend, database,
account or secrets are needed. Their JUnit report and debug files are included
in `pulse-android-device-reports`.

Local verification: **2/2 native Maestro flows passed in 26 seconds** on API 35.
The debug APK built successfully. Workflow YAML, preservation of frontend and
seed checks, native-only flow selection and Bash syntax were verified. This
integration does not change Android production or instrumentation-test sources;
the instrumentation results above are from the preceding verification. The
changes have not been pushed or executed by GitHub Actions.
