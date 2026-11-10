# Android CI verification — 8 November 2026

## Maestro shell parsing correction — 11 November 2026

The supplied screenshot shows successful APK installation, activity launch and
startup input-focus verification. The next command fails with `Syntax error:
end of file unexpected (expecting "fi")`, before Maestro runs. Emulator Runner
executes each parsed script line in a separate `sh -c` invocation, so a multiline
`if` block in its `script` input cannot share shell state with its closing `fi`.
Checking that block as one complete Bash program did not cover this behavior.

The compound command now lives in `scripts/run-android-maestro.sh`; the action
invokes it with one `bash` command. It retains the startup check, Java 21,
native-only flow selection, JUnit/debug outputs and original failure status.
Diagnostic collection is bounded so an unresponsive device cannot delay it
indefinitely.

Three regression checks extract the actual Maestro workflow commands and run
each in a separate shell with simulated device/Maestro tools. They pass for
success, Maestro failure with diagnostics, and diagnostic failure that preserves
Maestro's exit status. Bash syntax validation also passed. The Hermes job now
runs these checks. No Android app code or smoke-flow assertions changed; the
real Maestro flows and Linux GitHub job have not been rerun for this shell fix.

## Follow-up: 50 CI failures — 10 November 2026

The supplied device artifact records 167 tests, 50 failures, zero skips, and
27m 40.89s of test execution. Of these failures, **49** throw the same
15-second `awaitAppWindowFocus` timeout; none is the new 120-second runner
timeout. The remaining failure asserts that the Fitness calendar month heading
is displayed immediately after navigation.

The captured screen shows **“Pixel Launcher isn't responding”**. Window state
identifies `Application Not Responding: com.google.android.apps.nexuslauncher`
as `mCurrentFocus`, and the activity dump records an input-dispatch ANR because
the launcher had no focused window. Its recorded ANR time precedes the first
TestRunner start by about 45 seconds. This establishes a boot-time launcher
failure and a blocking system dialog; unrelated feature assertions that operate
through Compose semantics can still run while real window-focus checks fail.

Both instrumentation and Maestro now use `scripts/prepare-android-emulator.sh`.
It saves startup window/ANR state, force-stops Pixel Launcher only when its ANR
dialog owns focus, installs and launches the built debug app, then requires
Pulse to own input focus. Instrumentation bounds the entire startup check to
120 seconds. A still-blocked emulator fails before the suite and retains
diagnostics; an ANR in Pulse is not cleared or accepted. The existing focus
assertions, test coverage, deadlines, image and renderer remain unchanged.

The Fitness regression test now scrolls the month heading into view before
checking its visibility and its alignment with the calendar controls, using the
existing bounded scroll helper.

The complete local run through the updated wrapper passed **165/167** tests,
including **all 50 cases that failed in the supplied artifact**. It exposed two
additional timing races: Horizon checked for a date picker immediately after a
tap while the IME was transitioning, and Meal Plan checked responsive spacing
after waiting only for root width/density. Horizon now hides the keyboard and
waits for each dialog; both tests wait for complete window dimensions, density,
focus and layout after resizing. The original dialog, spacing, non-overlap and
tap-target assertions remain enabled. The corrected pair passed a focused
Gradle/UTP run with the configured 120-second method timeout, then two consecutive
direct AndroidJUnitRunner repeats: **6/6 executions** across three runs, zero
failures or skips. Together with the 165 passing cases from the complete run,
all 167 distinct cases passed across verification. A new complete run after
the final two timing corrections has not been performed.

Eight wrapper regression checks cover healthy startup, launcher-ANR recovery,
unfocused startup, rejection of a Pulse ANR, ordinary failure, the overall
deadline, a separately identified startup deadline, and an unresponsive device
during diagnostic collection. The real
API 35 emulator also passed startup focus verification. Debug/test APK builds,
Android test formatting, and both shell scripts' syntax checks passed.
All eight wrapper checks and the five existing report checks passed. Launcher
ANR recovery was covered by simulated device responses; the real local emulator
passed the healthy startup path. The updated Linux GitHub job has not run yet.

## Instrumentation cancellation — 10 November 2026

Run `42778282245`, job `129701868149`, was cancelled because it exceeded the
configured **90-minute job limit**. Instrumentation occupied 1h 26m 2s after
setup/build. GitHub explicitly annotated the job with “The job has exceeded
the maximum execution time of 1h30m0s”. There was no assertion failure or test
name in the final Gradle output. The protobuf `sun.misc.Unsafe` messages are
deprecation warnings; emulator ColorBuffer errors alone do not establish which
test or emulator operation stalled.

The old failure handler only ran after Gradle returned. Job-level cancellation
could terminate it first, and `!cancelled()` skipped the artifact upload. That
left no device report to diagnose the original stall.

`scripts/run-android-instrumentation.sh` now streams TestRunner start/finish
events to Actions while retaining full logcat and Gradle output. It passes a
120-second test-method timeout to AndroidJUnitRunner, and wraps Gradle in a
45-minute host deadline with a 30-second forced-termination grace period.
The host deadline also covers hangs in test setup, rules, teardown, or device
communication that the method timeout cannot reliably stop.

On failure, the wrapper preserves the failing exit status and collects power,
window/activity state, a screenshot and logcat, with bounded diagnostic commands.
It also requests a Java thread dump from the debuggable app when available.
The instrumentation action has a 55-minute step limit, Maestro has 15 minutes,
and report upload uses `always()`. The complete suite still runs sequentially;
no class filters, shards, retries, test exclusions or graphics-driver changes
were introduced.

Regression checks in `scripts/test_android_instrumentation.py` exercise success,
failure through the output pipeline, host deadline expiry, and an unresponsive
device without running Android or waiting for the actual deadline. The Hermes
CI job runs these checks. This repairs timeout handling and observability;
the exact cause of the original Linux stall remains unconfirmed without a
reproduction or device diagnostics from that run.

Local validation on 10 November: the current debug and test APKs built
successfully. Direct AndroidJUnitRunner execution on the dedicated API 35
Windows emulator passed **167/167 tests**, zero failures or ignored tests, in
**641.361 seconds (10m 41s)**. A subsequent Gradle/UTP run with
`timeout_msec=120000` passed the Account resize/large-text, Horizon resize/native
picker, and Meal Plan live-resize cases: **3/3**, zero failures or ignored tests.
All four wrapper regression checks and all five existing report checks passed;
the workflow YAML and Bash syntax were validated. The Linux-hosted workflow has
not been rerun with these changes, and the original stall did not reproduce.

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
