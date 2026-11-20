# Android meal onboarding verification

Source paths and Gradle commands in this record are relative to `pulse-android/` unless stated otherwise.

Verified on 6 November 2026 using the isolated Pulse CI emulator (Android 15/API 35).

## Results

- **130 unit tests passed** across domain (19), data (71), meal planner (23), and app (17). This includes the production Koin graph, localization, and shared-design-system adoption checks.
- **17 meal planner instrumentation tests passed**, including four new onboarding cases: web completion bypasses Android setup and restores editable answers; Android Save survives a lost response and recreation; Android Skip persists and web completion is picked up on resume; tablet and large-text actions remain reachable.
- The final compact review layout was additionally verified by rerunning the review Edit → Back to review → Save journey with a lost response after commit.
- Debug app and instrumentation APK builds passed.
- Meal planner/data Android lint and formatting checks for every changed module and the app passed. The final review changes passed meal planner lint and app/meal planner formatting checks again.

The unit/network tests and device fixture exercise the actual Ktor, repository, ViewModel, and app navigation code against isolated HTTP fixtures. They never access a real account. Shared PostgreSQL persistence, authenticated user scoping, and transactional terminal decisions remain covered by the Go tests from the web implementation. No new migration or application-database change was needed for Android.

The test helper distinguishes fixed onboarding actions from scrolling forms and waits for visible targets after keyboard inset changes. This keeps the existing meal creation/recovery flows covered alongside the new journey.

## Previews

The revised layout replaces the visible “Previous question” action with the native back icon, removes food banners from phones, and adds the compact **You → Food → Rhythm** chapter strip. Wide tablets combine artwork with contextual copy and a three-stage journey map. Normal phone goal choices are verified to fit in the viewport; long lists and larger text retain scrolling.

The redesign pass on 6 November 2026 passed all 17 meal planner emulator cases again, plus app/meal planner unit tests, Android lint, localization/design-system rules, and formatting. Device assertions cover the icon's Back behavior, retained selection, absence of phone artwork, visible goal choices, tablet supporting copy, and larger text.

- [Phone: first question](goal.png)
- [Food avoidances](avoidances.png)
- [Selected meal times](times.png)
- [Review and Save](review.png)
- [Tablet](tablet.png)
- [Larger text](large-text.png)

See [implementation and API behavior](../meal-plan-onboarding.md) for source locations and the shared web/Android state contract.
