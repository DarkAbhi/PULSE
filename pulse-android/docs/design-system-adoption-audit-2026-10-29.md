# Android design system adoption audit

Audit and remediation: 29 October 2026.

## Result and scope

The app was **not fully using its existing design system**. Later feature work had
introduced direct Material buttons, fields, switches, chips and dialogs, and custom
selection controls alongside the Pulse equivalents. Those equivalent controls have
been migrated. Menus, floating actions, dividers and determinate progress now have
shared Pulse wrappers as well.

This is a component-adoption audit, not a visual redesign. Remaining primitive and
specialized UI is explicitly inventoried below; the app does not claim that every
pixel or layout constant comes from a token.

All 13 Gradle modules were inventoried. Main, debug and release Kotlin sources,
Compose imports/calls, custom clickable/selectable/toggleable controls, UI XML,
shared component APIs, previews and existing regression tests were checked. Feature
reviews were split across three agents; the app shell, Auth, Home, Notifications,
core UI, enforcement and combined validation were reviewed centrally.

| Module | Main/debug/release Kotlin files | Files declaring composables | Outcome |
| --- | ---: | ---: | --- |
| app | 10 | 5 | Debug permission actions migrated; shell already uses Pulse navigation, bars and actions |
| core/ui | 40 | 35 | Shared implementations extended; raw Material allowed internally |
| core/domain | 97 | 0 | No UI |
| core/data | 16 | 0 | No UI |
| core/network | 16 | 0 | No UI |
| feature/auth | 5 | 2 | Existing Pulse text, fields, password, actions and feedback compliant |
| feature/home | 7 | 4 | Existing actions/states use Pulse; illustrated presentation retained |
| feature/fitness | 16 | 9 | Actions and custom radio selection migrated |
| feature/horizon | 21 | 13 | Fields, selectors, switches, menus, chips, radio choices and indicators migrated |
| feature/garage | 14 | 8 | Collection actions and vehicle/fuel selection migrated |
| feature/mealplan | 11 | 7 | Chips, floating actions and simple date/week triggers migrated |
| feature/profile | 6 | 3 | Searchable currency/time-zone dialogs migrated |
| feature/notifications | 4 | 2 | Options menu migrated; other controls already Pulse |

Counts include the shared files added during remediation: 263 Kotlin files in
total. Domain/data/network together contain 129 files and no composables. Tests
are excluded from these source counts and were inspected separately for affected
interactions.

## Findings and replacements

Paths below are relative to each module's main Kotlin source package, except the
debug permission screen.

| Location | Previous implementation | Replacement and behavior preserved |
| --- | --- | --- |
| app/debug/LocalNetworkAccess | Two Material Button calls | PulseButton; permission launch, Settings intent and resume detection preserved; explicit Pulse surface colors |
| fitness/WorkoutDetailContent | Material Button in WorkoutAction; raw dividers | PulseButton with primary, soft and soft destructive emphasis; callbacks, enabled conditions, glyphs and modifiers preserved; PulseHorizontalDivider |
| fitness/FitnessComponents | Clickable Today Surface | PulseButton text presentation; date action and enabled condition preserved |
| fitness/ExerciseEditor | Clickable search Row | PulseButton secondary with search icon; same search behavior and focus handling |
| fitness/ExerciseDialogs | Custom selectable exercise rows | PulseRadioButton; separate exercise name and metadata text retained, one selection callback and busy gate |
| garage/GarageCollection | Add Vehicle and View Vehicle Material buttons | PulseButton; existing navigation and trailing arrow retained |
| garage/VehicleEditor | Custom vehicle type and fuel radio tiles; raw divider | PulseRadioButton and PulseHorizontalDivider; same values, selected state, callbacks and keyboard dismissal; fewer columns for label readability |
| garage/FuelEditor | Custom fill-type selectable columns | PulseRadioButton; same fill choice, enabled gate and change callback; choices stack for larger text |
| horizon/FixedObligationEditor | Raw outlined field helper, budget outlined button/dropdown and active Switch | PulseTextField, PulseSelectionField, PulseSwitch; name/amount drafts, decimal keyboard, currency accessible name, budget indices, disabled conditions and supporting hint retained |
| horizon/ObligationDayField | Raw OutlinedButton trigger | PulseButton secondary; same picker and due-day semantics/test tag |
| horizon/HorizonTopBarActions | Two direct menu/menu-item implementations and divider | PulseDropdownMenu/Item and PulseHorizontalDivider; dismissal, availability, delete confirmation and quick-action routing retained |
| horizon/HorizonCards | Raw LinearProgressIndicator | PulseLinearProgressIndicator; identical clamped budget usage calculation |
| horizon/HorizonLedger | Raw divider | PulseHorizontalDivider |
| horizon/HorizonLedgerFilters | Custom filter pills and radio options | PulseFilterChip and PulseRadioButton; category dot, dropdown/sort cues, selected state, search, option callbacks and actionable test tags retained |
| horizon/PlannedPurchaseList | Raw divider and local type overrides | PulseHorizontalDivider and semantic Pulse text styles |
| horizon/HorizonMetrics, HorizonProjections | Independent literal color palettes and local type overrides | Pulse semantic colors and type styles; amounts, calculations and chart data unchanged |
| mealplan/MealEditorArtwork | Direct Material FilterChip | PulseFilterChip; same daypart icon, selected state, disabled state and selection callback; Pulse selected/disabled styling |
| mealplan/MealPlanScreen, MealAgenda | Material floating and extended floating actions | PulseFloatingActionButton / PulseExtendedFloatingActionButton; same placement, visibility conditions and callbacks |
| mealplan/MealCalendar | Clickable Today and week-range Surface triggers | PulseButton; same date/week action and enabled condition |
| mealplan/MealEditor | Clickable date Surface | PulseButton; same focus clearing, picker action, description and meal-date test tag |
| profile/CurrencyCard, TimeZoneCard | Direct AlertDialog | Slot-based PulseDialog; existing search fields, lazy results, local selection, confirmation and cancellation preserved |
| notifications/NotificationsTopBarActions | Direct dropdown menu/menu item | PulseDropdownMenu/Item; same canClear gate, dismiss behavior and destructive confirmation |

Horizon alone had 1 raw field helper, 2 outlined button calls, 1 switch, 3 menus,
4 menu items, 4 dividers and 1 progress call. All now resolve through Pulse APIs.
One menu and its two menu-item calls were absorbed by PulseSelectionField. The
field helper is used for both name and amount. Its 36 literal color constructions
were removed in favor of Pulse roles; backend category colors remain data.

## Shared design system changes

- PulseButton gains trailing icon support and Soft/DestructiveSoft variants. The
  final icon-only slot remains last so existing trailing lambdas still mean icon.
  Existing variants and loading/disabled behavior remain available.
- PulseFilterChip supports leading/trailing content. With a custom leading icon,
  the default selected check appears at the end. Selected and disabled colors
  remain owned by the design system.
- PulseRadioButton supports separate secondary text. The parent owns selection;
  the inner radio has no second click callback.
- PulseSwitch supports a secondary hint and trailing switch layout. The parent
  owns toggling and the inner switch has no separate callback.
- PulseTextField accepts a separate accessible name for above-label fields,
  allowing an amount's currency to remain announced.
- PulseSelectionField accepts a modifier on its actual trigger. Budget test tags
  and minimum height stay on the actionable element rather than its outer Box.
- PulseDialog gains a slot-based overload for searchable pickers; confirmation
  dialogs retain their existing convenience API.
- New menu, floating-action, divider and determinate-progress wrappers retain
  Material mechanics and supply Pulse styling.

## Retained primitives and specialized presentation

These are deliberate exceptions, not undiscovered replacement work. A generic
wrapper with a different content structure is not a behavior-preserving equivalent.

| Primitive or custom presentation | Locations / reason |
| --- | --- |
| Surface and layout containers | Feature card compositions, illustrated Home/Meal Plan cards, charts and previews. PulseCard forces a padded Column; replacing row/overlay/gradient layouts with it would alter composition and sizing. Existing ordinary PulseCard uses remain. |
| Clickable workout record row | FitnessScreen; record row geometry/content does not match fixed-padding PulseCard or text-only PulseButton |
| Meal week-day tiles | MealCalendar; weekday/date rectangular tile and selected/today presentation differ from circular numeric PulseCalendarDay |
| Meal-times navigator | MealCalendar; icon, title, live slot count and chevron form a composite navigation row |
| Obligation category tiles and due-day grid | Horizon; illustrated category tiles and specialized monthly day geometry retain selection semantics and callbacks |
| Adaptive exercise modal | Fitness; full-height weighted form uses a low-level Dialog, rather than AlertDialog mechanics |
| ModalBottomSheet / state | Horizon ledger filters and due-day selection; no existing Pulse sheet equivalent, dismissal/state behavior retained |
| SwipeToDismissBox / state | Notifications; swipe and explicit accessibility dismissal behavior retained |
| PullToRefreshBox, pullToRefresh, state, animated indicator | Feature refresh gesture infrastructure; PulseRefreshIndicator is a linear in-content progress treatment, not an equivalent animated gesture indicator |
| Badge / BadgedBox | Home notification count; explicit Pulse colors, capped visible count and enclosing accessible label retained |
| Icon, Canvas, Image, custom Layout | Decorative/artwork/chart/layout primitives, not general purpose control substitutes |
| Scaffold / SnackbarHost / host state | Shell, login and gallery infrastructure; Pulse styling and PulseSnackbar content retained |
| LocalMinimumInteractiveComponentSize | Fitness calendar's deliberate shared-calendar sizing override |
| LocalContentColor | Decorative purchase icon inherits the enclosing Pulse button's current content color |
| Platform XML theme and resources | Activity launch, system bars, icons and configuration; no XML View-based app controls found |

Home's aurora and illustrated palette, and various feature geometry constants,
remain custom presentation. This audit does not certify complete token-only
styling, nor does it silently redesign those illustrations.

## Regression prevention

ArchitectureTest now scans main/debug/release app and feature sources and rejects
direct Material controls outside an explicit set of retained primitives. It also
rejects wildcard imports and Foundation BasicText/BasicTextField bypasses. The
check examines qualified references as well as imports. Core UI is exempt because
it owns the underlying implementations.

The existing theme ownership check now uses normalized relative paths, fixing
the Windows path separator gap, and includes release sources.

DesignSystemAdoptionDeviceTest checks one callback per switch/radio/chip action,
disabled controls, separate exercise metadata, actionable selection trigger tags,
menu enabled state and dismissal, unchanged option indices, long labels retaining
trailing icons, and extended floating-action label semantics. Existing feature
tests exercise navigation, validation, drafts, mutations, resizing and pickers.

Review caught and corrected three migration risks before final validation: trailing
icons now reserve width beside long button labels, extended floating actions use
the content overload to retain accessible text, and selected meal daypart icons
use onPrimary rather than disappearing against the primary-colored chip.

## Validation

- `ktlintFormat`, then `ktlintCheck`: passed across the project.
- `testDebugUnitTest :core:domain:test`: **215 tests, zero failures/errors/skips**.
  This includes the architecture and localization source checks.
- `lintDebug`: passed across all Android modules, zero errors. There are 16
  warnings: 14 app template/dependency/resource/KTX advisories and two Horizon
  modifier-order/KTX advisories. No dependency upgrades were made.
- `:app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease`: passed.
  Release includes the optimizer/resource shrinker; it has not been installed or
  cold-launched as part of this audit.
- The source localization check also exposed existing symbolic text in Horizon;
  the ledger chevron is now a decorative icon and the numeric percentage format
  comes from strings.xml.

The nine shared-component scenarios passed on Android 17/API 37: all four new
adoption tests, the button interaction test, and all four theme/foundation tests.
The latter cover light, dark, theme changes and doubled text.

The first 15-scenario feature run passed ten and failed five. Investigation found
these test assumptions, corrected without changing the application:

| Scenario | Cause and correction |
| --- | --- |
| Garage vehicle/fuel/pressure/air/maintenance CRUD | The profile fixture omitted currency, yielding the existing GBP default while field assertions expected rupees. The fixture now explicitly returns INR. |
| Fitness calendar/exercise CRUD | The assertion expected the old unused `0 workouts logged` label. It now checks the existing `0 workouts` count and `No workouts logged.` empty state. |
| Horizon quick actions on phone/tablet | Back dismissal was asserted before the popup finished disappearing. A bounded wait precedes the original absence assertion. |
| Horizon budget/obligation CRUD | A touch on the underlying budget trigger could occur during due-day sheet dismissal. A bounded wait now precedes that touch. The selector assertion checks the complete shared label (`Budget (optional): Travel fund`), and an explicit assertion verifies the persisted budget ID alongside category, active and due day. |
| Notification dismiss/clear | The Home icon's existing accessible label includes a count. The test selector now accepts that count while still identifying Notifications. |

Fixture screenshots also exposed an Android System UI ANR overlay during the
first feature run, which overlapped release optimization and two emulators. The
isolated API 35 emulator was cold-started before the final feature rerun, with no
concurrent build workload. That rerun passed 14 of 15 scenarios; the remaining
failure was the old exact budget-label assertion, updated as described above.
The last Horizon scenario then passed individually, including the explicit saved
budget ID check. **All 24 distinct targeted device scenarios passed across these
runs: nine shared-component scenarios on API 37 and fifteen feature scenarios on
API 35.** This is a combined result, not a claim that the original 15-case run
was green.

The fifteen feature scenarios cover:

- Garage collection/long names, classification/recreation, and vehicle, fuel,
  pressure, air and maintenance CRUD.
- Fitness calendar/exercise CRUD, immediate catalog selection/set controls, and
  custom exercise/removal confirmation.
- Horizon quick actions on phone/tablet, budget/obligation CRUD and Home refresh,
  and ledger/filter behavior on phone/tablet.
- Meal Plan CRUD/consumption/empty weeks, custom meal times and associated-meal
  preservation, and contextual Today/week date navigation.
- Profile currency and time-zone search/save/restoration, and notification
  dismissal/clear beyond the displayed limit with return to Home.

Fixture screenshots were visually inspected for Garage collection, Horizon
obligation fields/menus and ledger filters, Meal Plan, and the searchable Profile
dialog with the keyboard open. The clean captures show no System UI ANR overlay.
The final test-only corrections were followed by a passing app formatting check,
unit-test check and rebuilt instrumentation APK. Production sources were unchanged
after the successful debug/release/lint validation.

Build/test artifacts are under `build/reports/design-system-audit/` locally;
feature-run and final Horizon rerun results are `feature-device.txt` and
`horizon-final-device.txt`. These generated artifacts are not committed sources.

An earlier unfiltered run on Android 17 failed in the account resizing scenario
and was stopped during the next scenario while final reviewed component fixes
were being prepared. That interrupted run is not counted as a passing full suite;
the full instrumentation suite is not certified by this audit.

No live backend account or physical device was used. The fixture-driven scenarios
and unit tests reduce regression risk; they do not certify every possible device,
TalkBack interaction, translated layout or real service condition.
