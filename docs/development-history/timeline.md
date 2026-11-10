# Development timeline

This is an index reconstructed from committed documentation and Git history, not a retrospective claim about unrecorded work. Each inventory date is the document's first committed appearance; the last-update column shows later revisions. Document-internal dates may precede those commits. Existing plans and review findings are preserved with their implementation follow-ups.

## Milestones

| Date | Recorded work and reasoning | Evidence |
| --- | --- | --- |
| 2026-09-25 | Meal Plan and Garage feature write-ups connect product workflows to backend, web and schema source maps. | [Meal Plan](features/meal-plan-feature.md), [Garage](features/vehicle-garage-feature.md) |
| 2026-10-18–20 | Android design-system adoption, dashboard/garage artwork, fitness calendar, workout details and exercise editor evolve with recorded responsive checks. | [Design system](android/design-system.md), [Fitness](android/fitness-redesign/README.md), [Workout details](android/workout-detail-redesign/README.md), [Exercise editor](android/add-exercise-redesign/README.md) |
| 2026-10-28–30 | Shared time zones/currency, finance editor redesigns and component audits record cross-platform consistency decisions. | [Time zones](features/user-time-zones.md), [Currency](features/user-currency.md), [Planned purchases](design/planned-purchase-redesign/README.md), [Button consolidation](android/button-consolidation.md) |
| 2026-10-31–11-03 | Vehicle catalog, identity flow, Rust review, ownership validation and maintenance/reminder iterations document contracts and correctness constraints. | [Catalog](features/vehicle-catalog.md), [Rust review](reviews/rust-backend-review.md), [Maintenance](features/vehicle-maintenance-poc.md) |
| 2026-11-01–02 | Account ownership rollout records data isolation, deployment ordering and rollback. | [Ownership](features/user-ownership.md) |
| 2026-11-06 | Meal onboarding moves from proposal to shared account-level web/Android state, with implementation and captured verification. | [Plan](features/meal-plan-onboarding-plan.md), [Implementation](features/meal-plan-onboarding.md), [Android](android/meal-plan-onboarding.md), [Web captures](verification/meal-plan-onboarding/README.md) |
| 2026-11-07 | Dependency refresh and usage audit record version choices, removals and verification limits. | [Refresh](reviews/dependency-refresh-2026-11-07.md), [Audit](reviews/dependency-usage-audit-2026-11-07.md) |
| 2026-11-08–11 | Android CI/device-test investigation records emulator focus, instrumentation stability and native smoke-test work. | [CI verification](android/ci-test-verification.md), [Current Maestro guide](../../maestro/README.md) |
| 2026-11-11 | Documentation refactor separates checkout/setup from operating guides and consolidates build records into this categorized archive. | [Documentation conventions and references](../README.md) |

## Dated document inventory

| First committed | Last updated before refactor | Record |
| --- | --- | --- |
| 2026-09-25 | 2026-10-31 | [Vehicle Garage Feature](features/vehicle-garage-feature.md) |
| 2026-09-25 | 2026-11-06 | [Meal Plan Feature](features/meal-plan-feature.md) |
| 2026-10-18 | 2026-10-18 | [Design system audit — 18 October 2026](android/design-system-audit.md) |
| 2026-10-18 | 2026-10-18 | [Design-system validation — 18 October 2026](android/design-system-validation.md) |
| 2026-10-18 | 2026-10-19 | [Dashboard artwork](android/dashboard-artwork.md) |
| 2026-10-18 | 2026-11-02 | [Pulse design system](android/design-system.md) |
| 2026-10-18 | 2026-11-06 | [Meal planner artwork](design/meal-plan-design-assets.md) |
| 2026-10-19 | 2026-10-19 | [Android garage artwork](design/garage-design-assets.md) |
| 2026-10-19 | 2026-10-19 | [Add-vehicle artwork](android/garage-editor-artwork.md) |
| 2026-10-20 | 2026-10-20 | [Add exercise redesign](android/add-exercise-redesign/README.md) |
| 2026-10-20 | 2026-10-20 | [Fitness tab redesign](android/fitness-redesign/README.md) |
| 2026-10-20 | 2026-10-28 | [Workout detail redesign](android/workout-detail-redesign/README.md) |
| 2026-10-28 | 2026-10-28 | [User time zones](features/user-time-zones.md) |
| 2026-10-28 | 2026-10-28 | [Get a production APK in Telegram](../guides/android-release.md) |
| 2026-10-29 | 2026-10-29 | [Planned purchase form](design/planned-purchase-redesign/README.md) |
| 2026-10-29 | 2026-10-29 | [Account currency](features/user-currency.md) |
| 2026-10-29 | 2026-10-29 | [Button consolidation — 29 October 2026](android/button-consolidation.md) |
| 2026-10-29 | 2026-10-29 | [Android design system adoption audit](android/design-system-adoption-audit-2026-10-29.md) |
| 2026-10-30 | 2026-10-30 | [Card borders and shared component adoption](android/card-border-audit-2026-10-30.md) |
| 2026-10-31 | 2026-10-31 | [Rust review: entire rust-backend](reviews/rust-backend-review.md) |
| 2026-10-31 | 2026-10-31 | [Vehicle form redesign verification](verification/vehicle-redesign/README.md) |
| 2026-10-31 | 2026-10-31 | [Garage vehicle selection](android/garage-vehicle-catalog.md) |
| 2026-10-31 | 2026-11-09 | [Garage vehicle catalog](features/vehicle-catalog.md) |
| 2026-11-01 | 2026-11-01 | [De-slop report: Pulse Android](android/de-slop-report.md) |
| 2026-11-01 | 2026-11-02 | [User ownership rollout](features/user-ownership.md) |
| 2026-11-02 | 2026-11-03 | [Vehicle maintenance proof of concept](features/vehicle-maintenance-poc.md) |
| 2026-11-06 | 2026-11-06 | [Meal plan onboarding implementation plan](features/meal-plan-onboarding-plan.md) |
| 2026-11-06 | 2026-11-06 | [Meal onboarding verification](verification/meal-plan-onboarding/README.md) |
| 2026-11-06 | 2026-11-06 | [Meal preferences and shared onboarding](android/meal-plan-onboarding.md) |
| 2026-11-06 | 2026-11-06 | [Android meal onboarding verification](android/meal-plan-onboarding/README.md) |
| 2026-11-06 | 2026-11-09 | [Meal plan onboarding](features/meal-plan-onboarding.md) |
| 2026-11-07 | 2026-11-07 | [Dependency refresh — November 7, 2026](reviews/dependency-refresh-2026-11-07.md) |
| 2026-11-07 | 2026-11-07 | [Dependency usage audit — November 7, 2026](reviews/dependency-usage-audit-2026-11-07.md) |
| 2026-11-08 | 2026-11-11 | [Android CI verification — 8 November 2026](android/ci-test-verification.md) |
