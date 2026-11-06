# Meal preferences and shared onboarding

The Android meal planner uses the same account-level onboarding record as the web frontend. No device-local “seen” preference, Room record, or Android-specific completion status is stored.

## Behavior

- On first entry, Android reads `GET /api/meal-plan-onboarding` through the authenticated shared Ktor client. Until that read succeeds, it shows loading or a retry state rather than assuming setup is pending.
- `completed` and `skipped` both open the planner directly. This includes decisions made on the web or another Android device.
- A pending account gets the same six questions, answer codes, default meal selections, default usual times, and review-and-save screen as the web.
- **Save preferences** sends the complete validated answer set to `PUT /api/meal-plan-onboarding` with `status=completed` and `questionnaire_version=1`. **Skip setup** sends `status=skipped` without answers. The shared Go transaction persists the decision for both platforms.
- Opening questions, selecting answers, Back, leaving the page, and closing the app never resolve onboarding. The ViewModel retains drafts during activity recreation; a fresh ViewModel/process starts a fresh draft and reads server status again.
- The route refreshes onboarding status on resume. Completing setup on the web while Android is paused dismisses Android's pending journey when it resumes. Existing in-memory answers are preserved if the backend still says pending.
- An opposing terminal decision returns the backend's authoritative state. Android accepts that state instead of overwriting it. A lost response after a successful save/skip is reconciled with a fresh read. If neither request confirms success, the form and answers remain available for retry.
- **Meal preferences** in the planner opens saved answers for explicit later edits. Cancel makes no request. Settings saves use `PUT /api/meal-preferences` and preserve the original terminal onboarding decision, including a prior Skip.
- Account navigation is already keyed by username. The repository also rejects a response if the authentication generation changed while it was in flight. A 401 expires only the session that started the request.

Both clients must use the same configured API and signed-in account. The existing Go API and migration 36 from the web implementation are prerequisites; this Android integration requires no new database migration.

## Implementation

| Layer | Source |
| --- | --- |
| Domain | `core/domain/.../model/MealPreferences.kt`, `repository/MealPreferencesRepository.kt` |
| Network | `core/network/.../dto/MealPreferencesDtos.kt`, additional methods on `MealPlanApi` |
| Data | `core/data/.../PulseMealPreferencesRepository.kt`, registered in `dataModule` |
| UI state | `feature/mealplan/.../MealOnboardingViewModel.kt`, registered in `mealPlanModule` |
| Route and screens | `MealPlanRoute`, `MealOnboardingScreen.kt`, `res/values/meal_onboarding.xml` |
| Shared control | `core/ui/.../components/PulseProgressIndicator.kt` |

The version field is always serialized, including when the JSON serializer omits default-valued fields. Times use strict `HH:mm` on the wire and `LocalTime` in the domain, interpreted in the existing account timezone. Unknown states, invalid answers, and malformed responses produce failures rather than a false dismissal. Cancellation is propagated.

The UI reuses PulseButton, PulseCard, PulseRadioButton, PulseCheckbox, PulseSwitch, PulseTimePickerDialog, loading/error states, theme colors, and typography. Back is an accessible icon button using the existing mirrored back-arrow asset and a 48dp touch target; its spoken label remains “Previous question.” Skip stays visible at the other end of the header.

Phones omit the decorative food banner. A compact **You → Food → Rhythm** strip groups the six questions into three chapters, with two progress marks per chapter. The header carries the question count, leaving the body for the question and answers. Tighter answer-card padding preserves the native controls' minimum touch targets. Longer avoidance lists and larger text can still scroll in the question body above an independent action area.

Wide tablets use an illustrated side panel with the heading “Meals, your way.”, contextual copy for the current chapter, a smaller food plate, and an icon-led map of priorities, food preferences, and daily rhythm. Completed chapters show checkmarks. The panel can scroll at larger font sizes or shorter heights; it does not displace the question controls. All colors and shapes come from Pulse tokens. No new image assets were needed.

Strings live in Android resources. Selection groups, headings, chapter progress, and errors expose native accessibility semantics.

Review uses compact summary rows with individually labeled Edit actions, consistent with the space-efficient phone layout.

The existing generated web salad and pesto artwork is reused unchanged at `feature/mealplan/src/main/res/drawable-nodpi/meal_onboarding_{salad,pesto}.webp`. Original prompts/provenance remain in the repository's `docs/meal-plan-design-assets.md`.

## Verification

Domain tests cover all answer codes, defaults, required answers, duplicates, and selected times. Repository tests cover the shared authenticated endpoints, the exact web payload shape, terminal state reads, conflict responses, malformed responses, cancellation, and stale account responses. Nine ViewModel tests cover web-to-Android dismissal, Android terminal writes, partial drafts, required questions, retries, lost responses, resume refresh, settings, and duplicate submission protection.

The existing meal planner instrumentation fixture now supplies shared onboarding state. Its onboarding journeys exercise real app navigation, production Ktor/data/ViewModel code, the questionnaire, later editing, recreation, lost-response reconciliation, resume refresh, tablet resizing, and larger text. The fixture substitutes HTTP responses and never changes a real account. Actual PostgreSQL persistence and first-terminal-wins transactions are covered by the Go tests from the web implementation.

Useful checks from `pulse-android`:

```sh
./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:mealplan:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.abhishekan.pulse.MealPlanDeviceTest
./gradlew ktlintCheck
```

Screenshots and the final verification record are stored in `docs/meal-plan-onboarding/`.
