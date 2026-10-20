# Design-system validation — 18 October 2026

## Final source checks

The final implementation includes the selected-chip checkmark and its selected-state
assertion. Emulator checks resumed after the user confirmed both devices were ready.

| Check | Result |
| --- | --- |
| Full build, including debug and optimized release packages | Passed |
| Android instrumentation APK compilation | Passed |
| Project Kotlin formatting checks | Passed |
| Android lint | Passed, no errors |
| Unit tests across all modules | 171 passed, 0 failures/errors/skips |
| Working-tree whitespace check | Passed |

Final full-build command:

```sh
./gradlew build :app:assembleDebugAndroidTest --offline --max-workers=2 -Dorg.gradle.jvmargs=-Xmx4096m
```

The larger heap and worker limit were command-line options; project/toolchain
configuration was not changed. The final build completed successfully in 3m 3s.
The standard debug build also passed. Android lint retains nine existing warnings:
seven unused legacy XML colors, a redundant Activity label, and the AGP-version
advisory. No dependency upgrades were made. Instrumentation compilation retains
the existing Compose test-rule deprecation warning.

DesignSystemTest checks semantic text/surface contrast at 4.5:1, control outlines
and calendar markers at 3:1, compatibility color pairs (including fixed accents),
semantic type hierarchy and >=48dp touch-target tokens. ArchitectureTest prevents
MaterialTheme reads outside the compatibility adapter. Existing unit checks also
cover state, validation, repository contracts, localization and dependency wiring.

## Device results — resumed on final source

The final debug and instrumentation APKs were reused; no Kotlin source was newer
than either APK. Tests ran sequentially on the existing, booted Pixel 9 Pro and
Pixel Tablet emulators. No AVD was created, wiped or rebooted during this resumed run.
Direct instrumentation results were checked by completed test counts and status,
not the command exit code alone.

| Device/check | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: |
| Pixel 9 Pro: design-system and gallery | 6 | 0 | 0 |
| Pixel 9 Pro: remaining feature checks | 48 | 2 | 1 |
| Pixel Tablet: design-system, gallery and tablet rail | 7 | 0 | 0 |
| Total | 61 | 2 | 1 |

The four design-system checks exercise light/dark font and palette rendering,
200% text reachability, theme switching with retained selection/input state,
and enabled/disabled/loading/destructive action behavior. The selected-chip
checkmark and selected-state assertion are included in these final-source runs.
The gallery checks cover recreation and interactive controls. The tablet rail
check also passed, covering direct feature navigation and the unsaved-password guard.

All 51 broader phone checks were accounted for. The first invocation reached its
900-second harness limit after 44 completed checks; only the seven uncompleted
checks were rerun, and all seven passed. The tablet rail test correctly skips on
a phone. Passing coverage includes authentication/navigation, onboarding,
session storage, fitness, garage, finance, localization and meal planning.

Two account checks remain unresolved:

- `resizingAndLargeTextKeepAccountFormsAndNotificationsUsable`: timed out waiting
  for the IME after resizing. The [failure capture](design-system-previews/account-keyboard-failure.png)
  shows Google's password-save dialog covering the app. Temporarily disabling the
  configured autofill service did not prevent that dialog; the retry reproduced
  the failure. No password was saved through that dialog.
- `oneTimeTokenSurvivesRotationCopiesSensitivelyAndCanBeRevoked`: timed out waiting
  for “Your API key” after Activity recreation. The root cause was not established;
  this run does not verify token-dialog restoration, sensitive copying or revocation
  through that test.

These failures are not counted as passes or attributed to the theme without
supporting evidence. No production behavior or unrelated test source was changed
to bypass them. Earlier device-disconnection/startup failures were superseded by
this resumed run, but the full device suite is not green.

The harness snapshots and restores each emulator's display overrides, font scale,
hardware-keyboard IME setting and autofill service. Read-back matched both original
snapshots exactly after testing. Both emulators were left running.

## Preview artifacts

Current preview source:
`core/ui/src/debug/kotlin/dev/abhishekan/pulse/core/ui/gallery/PulseDesignSystemPreview.kt`.
It contains light, dark and 200% text Compose previews. The existing debug gallery
also contains the complete palette/type catalog and interactive controls/states.
No production destination was added.

The native captures below show the final selected-chip checkmark. They were
visually inspected for font loading, palette, readability and clipping.

| Device | Light | Dark | 200% text |
| --- | --- | --- | --- |
| Phone | [Light](design-system-previews/phone-light.png) | [Dark](design-system-previews/phone-dark.png) | [Large text](design-system-previews/phone-large-text.png) |
| Tablet | [Light](design-system-previews/tablet-light.png) | [Dark](design-system-previews/tablet-dark.png) | [Large text](design-system-previews/tablet-large-text.png) |

## Not verified

Manual TalkBack, physical-device behavior, keyboard/focus traversal and reduced-motion
behavior were not separately audited. Existing interaction implementations and
animations were preserved. No individual production screen was redesigned.
Future layout recommendations and remaining Material implementation dependencies
are recorded in [the design-system standard](design-system.md).
