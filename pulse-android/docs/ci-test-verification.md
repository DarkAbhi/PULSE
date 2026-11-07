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

No tests were disabled or excluded from the CI job.

## Results and limits

Verification used the dedicated `Pulse_CI_API_35` Android 15 emulator on Windows,
JDK 25 and the repository's Gradle wrapper, with isolated HTTP fixtures.

- **253 unit tests passed**, with zero failures, errors or skips, after forcing
  execution of `testDebugUnitTest` and `:core:domain:test`.
- `lint` and `ktlintCheck` passed. The final instrumentation sources also passed
  `:app:ktlintAndroidTestSourceSetCheck` and compiled successfully.
- **All 167 UI/device tests passed together in one complete, unsharded run**,
  with zero failures, errors or skips. The Gradle invocation, including final
  test compilation and formatting validation, completed in **14m 15s**.
- The previously intermittent Meal Plan resize case also passed three
  consecutive focused runs before the final complete regression.

The GitHub-hosted Linux workflow has not been executed for these local changes.
No push, merge or release was performed.
