# De-slop report: Pulse Android

Source paths and Gradle commands in this record are relative to `pulse-android/` unless stated otherwise.

**Summary:** 90 code/build/test files changed, +889 / -1063 lines (174 fewer lines, excluding documentation). Removed 24 unnecessary use-case classes, removed the unused dashboard notification path, and moved feature chrome and draft ownership out of the app shell. Production Kotlin files decreased from 273 to 253. Module topology and product flows are preserved.

## Scope

Repository-wide detection covered production Kotlin, feature wiring, module dependencies, and related debug/test callers. The architectural review concentrated on repository boundaries, use cases, networking, ViewModels, the signed-in shell, feature routes, draft retention, and the Home animation/clock. This is a cleanup with recorded exceptions, not a claim that every heuristic in the skill is satisfied.

| Module | Production Kotlin files after cleanup |
| --- | ---: |
| app | 8 |
| core:data | 16 |
| core:domain | 76 |
| core:network | 16 |
| core:ui | 44 |
| feature:auth | 4 |
| feature:fitness | 15 |
| feature:garage | 22 |
| feature:home | 6 |
| feature:horizon | 27 |
| feature:mealplan | 9 |
| feature:notifications | 4 |
| feature:profile | 6 |

## Removed

Each forwarding class below was **INLINE**: its caller now uses the repository directly, and its Koin factory is gone. `GetNotifications` was **DELETE** because it had no production caller. `GetNotificationCount` was **INLINE** because counting the fetched list is simple presentation work.

| Repository | Removed classes |
| --- | --- |
| AuthRepository | CheckSessionExpiry, ForgetSession, Logout, ObserveSession, RestoreSession |
| DashboardRepository | GetHorizon, GetNotifications, GetProfile, GetWorkoutToday, ObserveDashboardChanges |
| FitnessRepository | GetFitnessOverview, ObserveFitnessChanges |
| GarageRepository | GetVehicles |
| HorizonRepository | GetPlannedPurchases, LoadHorizon, ObserveHorizonChanges |
| MealPlanRepository | GetMealTimes |
| NotificationsRepository | ClearNotifications, GetNotificationCenter, GetNotificationCount |
| PreferencesRepository | ObserveTheme, SetTheme |
| ProfileRepository | GetAccountProfile, GetAPIKeys |

- **DELETE:** `DashboardRepository.notifications`, its implementation, and `PulseApi.notifications?limit=5` were an unused alternative to the full notification center. The badge still counts the full center, including counts above five.
- **DELETE:** shell title mirrors, feature form imports, feature ViewModel lookups, copied draft callbacks, and the large form-title switch.
- **DELETE:** debug logging of full request/response bodies and headers, which exposed passwords and newly created API keys.

## Fixed

- **FIX — feature chrome:** Garage, Horizon, Workout, Notifications, Profile, Fitness, and Meal Plan routes own their screen bars through `PulseScreenScaffold`. Horizon reuses its existing editor-label mapping; the shell no longer understands form kinds or reads feature state to draw actions.
- **FIX — draft lifetime:** `UnsavedChanges` derives reports from feature ViewModel state and removes each report when its ViewModel scope ends. Retained tab drafts still warn on logout. Multiple owners from the same feature cannot erase one another's report.
- **FIX — navigation policy:** password edits still block leaving Profile; other feature drafts retain the existing logout-warning policy. The refactor does not add confirmation to ordinary tab changes.
- **FIX — shell state:** onboarding's two booleans became `OnboardingPrompt` in a saveable state holder. Its lifetime remains the signed-in session, including configuration recreation; it is not redefined as an "ever offered" preference.
- **FIX — destinations:** `AppDestination` owns route constants, tab relationships, labels, and icons. One list drives rail/bottom navigation. Shared helpers own single-top and tab navigation behavior.
- **FIX — one-shot instruction:** profile setup uses the `editName` navigation argument. Profile consumes the request in its own saved state so a closed editor does not reopen after recreation.
- **FIX — duplicate validation:** `ExerciseDraft.input` and `issue` are shared by the editor and save path, including malformed weight handling. The composable no longer rebuilds its own independent validation rule.
- **FIX — time ownership:** Home receives time from its ViewModel's injected `Clock`. The minute-aligned flow uses `WhileSubscribed(5_000)`; the greeting renderer and previews receive explicit timestamps.
- **FIX — networking diagnostics:** debug logging retains method/URL/status information without bodies or headers. Signed receipt transfers explicitly suppress logging even when hosted under the API's own origin and path. Release logging remains disabled.
- **FIX — nullability:** cookie domain validation no longer uses a non-null assertion.
- **FIX — verification infrastructure:** the pull-refresh state holder was renamed to `PullRefreshTracker.kt` to satisfy the configured filename rule. The Fitness fixture resets its account timezone before calculating its date boundary, preventing cross-test account state from changing the fake backend's "today".

## Kept: candidates with a reason

- **KEEP:** validation use cases (`Apply*Change`, account validation, exercise/workout mutations, catalog search, receipt operations, and validated reads) encode real input rules or computation.
- **KEEP:** API DTO parsing and storage conversion absorb external nullability, decimal precision, date/time conversion, validation, encryption, and schema differences; these are real boundaries, not identical model copies.
- **KEEP:** platform seams `ReceiptFiles` and `SessionMaterialStore` isolate Android file access and encrypted storage from portable networking/tests.
- **KEEP:** repository cancellation handling calls `remoteFailure`, which immediately rethrows `CancellationException`. `SessionStorage` catches and rethrows `Throwable` to roll back an atomic file write.
- **KEEP:** local picker/dialog/scroll/selector state has one UI owner. Exercise row identities preserve surviving rows even when two sets have identical values.
- **KEEP:** the existing Home orb already shares one transition, reads animation state while drawing, and respects reduced motion. Fixed illustration colors and vector path colors are artwork; this pass does not redesign that artwork.
- **KEEP:** exact layout dimensions and existing feature styling were not mechanically replaced just to shrink a search result. Such substitutions need visual intent, not a blanket search-and-replace.

## Flagged for human decision / further design

1. **Repository contracts remain.** There are ten domain repository interfaces with one live data implementation each. They currently enforce the existing feature-to-domain/data-to-domain module boundaries. Replacing them with concrete data-layer classes would introduce forbidden feature-to-data dependencies; replacing them with constructor function ports would add another indirection. Removing these contracts needs an explicit boundary design, and potentially module changes, which the skill excludes from a de-slop pass. They are not certified compliant with Rule 1 merely because fakes exist.
2. **Account preferences are still process-wide.** `UserCurrency` and `UserTimeZone` retain mutable singleton state. The app's authentication flow owns loading/resetting them, but instance-scoped account context would improve test and account isolation. The Fitness suite exposed one fixture-order dependency, now corrected in its setup. A full migration must cover model defaults, date calculations, formatting, repositories, and restoration together.
3. **Some feature renderers still take a ViewModel.** Garage, Horizon, Meal Plan, and Profile retain their existing large internal renderer/action wiring. The shell no longer depends on those internals, and the changed top-bar actions take values/callbacks, but a complete stateless-renderer conversion is not included. Several files also remain large; splitting by line count alone would add wrappers without removing responsibility.
4. **Styling is not universally tokenized.** Existing illustration palettes, budget-name colors, and layout-specific dp values remain. Theme/token enforcement for all existing screens needs a separately reviewed visual pass; the verification below covers the changed navigation/chrome/draft flows, not every possible visual state in the app.

## Verification

- [x] `ktlintFormat` followed by `ktlintCheck` (formatting ran separately from compilation to avoid Windows mapped-file locks).
- [x] `testDebugUnitTest` and `:core:domain:test`: 228 tests, zero failures/errors.
- [x] `KoinGraphTest.completeProductionGraphHasEveryConstructorDependency`.
- [x] Architecture tests: module direction, domain purity, infrastructure boundaries, immutable exposed ViewModel state, and shared design-system constraints.
- [x] `assembleDebug` and `:app:assembleDebugAndroidTest`.
- [x] Network tests verify that diagnostics omit headers, passwords, response secrets, and explicitly suppressed signed storage URLs.
- [x] Regression tests cover multiple draft owners and one-shot profile setup after recreation.
- [x] Tablet light/dark regression: all eight selected flows passed in each theme (16 checks), on the API 37 tablet emulator at 2560 x 1600 / density 320. The saved app preference was explicitly set for each theme, captures were visually reviewed, and the original preference/system mode were restored.

Detekt is not configured. Device tests use fixture APIs rather than production credentials. Release/R8 builds and every device-test class are outside the verification performed here.

### Tablet regression coverage

| Flow | Light | Dark |
| --- | --- | --- |
| Adaptive navigation, password discard protection, notification back navigation | Passed | Passed |
| Profile setup opens the shared name editor | Passed | Passed |
| Notification dismiss/clear and return to Home | Passed | Passed |
| Garage detail/back navigation survives recreation | Passed | Passed |
| Horizon draft survives rotation/tabs and warns on logout | Passed | Passed |
| Meal Plan draft survives recreation/tabs and warns on logout | Passed | Passed |
| Workout exercise draft survives rotation/tabs and warns on logout | Passed | Passed |
| Workout editor has one app bar and correct back/discard behavior | Passed | Passed |

Local captures are under `build/deslop-visuals/light/` and `build/deslop-visuals/dark/`. Verification output is in `deslop-verification.log`, `deslop-device-light.log`, and `deslop-device-dark.log` (ignored build evidence, not source files).
