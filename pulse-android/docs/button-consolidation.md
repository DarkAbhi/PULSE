# Button consolidation — 29 October 2026

## Decision and completed plan

Separate names make text and icon actions easy to recognize at call sites, but
they do not require separate public components. Their layout and interaction
differences still matter. The consolidation therefore preserves the original
Material Button, TextButton and IconButton implementations behind one Pulse API.

1. Audit the three implementations and all callers, including debug galleries.
2. Add `PulseButtonStyle.Standard`, `Text` and `Icon` to `PulseButton`. Retain
   existing colors, padding, shapes, pressed feedback, targets and labels.
   Share text/leading-icon/loading content, and support loading on every style.
3. Migrate 79 text actions and 21 icon actions across 37 caller files. Delete
   the two obsolete component files and update the design-system documentation.
4. Check builds, formatting, lint, interaction states and representative screens.

A token comparison against the original caller files confirmed that only button
names, imports, explicit style arguments and formatting changed. All callbacks,
labels, modifiers, conditions and other arguments remain the same. No obsolete
component declarations, imports or calls remain in the Android sources.

## Validation

| Check | Result |
| --- | --- |
| Debug app and instrumentation APK builds | Passed |
| Release Kotlin compilation | Passed |
| Project formatting and Android debug lint | Passed |
| Unit tests, including domain | 213 passed; one existing localization failure |
| New unified-button device test | Passed: all three styles, light/dark themes, enabled/disabled/loading, click counts, icon labels, >=48dp targets |
| Existing design-system device tests | Passed: light, dark, doubled text, state retained across theme changes |
| Gallery actions/recreation and selection semantics | Both passed |
| Dashboard notification icon interaction | Passed |
| Purchase creation, individual deletion and bulk-clear confirmation | Passed |
| Garage cancel alignment and discard protection | Passed |
| Fitness editor back navigation, cancel and discard | Passed |
| Meal Plan contextual Today action and week date picker | Passed |
| Profile currency search, save and restoration | Passed |
| Source whitespace check | Passed |

Device checks ran on the Pixel Tablet API 37 emulator. Rendered foundation and
purchase-screen screenshots were also inspected. This is representative coverage,
not an exhaustive check of every device and workflow.
Across the selected device checks, 13 distinct tests passed and the navigation
test described below failed. Repeated foundation/button runs also passed.

The existing `LocalizationResourcesTest.productionPulseControlsDoNotEmbedEnglishCopy`
fails on the literal chevron in unchanged `HorizonLedger.kt:193`.
The broader `AuthNavigationDeviceTest.loginReachesRealDashboardAndLogoutRemovesAuthenticatedNavigation`
successfully logs in, then fails on its hard-coded `₹ 32000.125` expectation at
line 202. The unchanged dashboard uses `UserCurrency.format`, which formats the
selected account currency. Neither the formatter nor this test changed. These
failures prevent claiming that the entire existing test suite is green.
