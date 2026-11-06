# Card borders and shared component adoption

Source audit: 30 October 2026. Covers Android production Kotlin sources in app,
core/ui and every feature module; debug/gallery and test sources were also searched
for direct Material card usage. This is a source audit, not an on-device visual review.
Locations below use feature package filenames and one-based source line numbers.

## Meal calendar list change

Removed the 1dp, 30%-opacity artwork-tinted outline from `MealAgenda.kt` /
`MealIllustratedCard` (Surface at line 151). This single renderer covers every meal
group in the calendar agenda, including phone and two-column layouts. Artwork,
gradient, rounded shape and actions are retained. Its edit button still has its own
outline; that is separate from the card perimeter.

## Design system API

`core/ui/.../components/PulseCard.kt:16` exposes `border: BorderStroke? = null`
and forwards it to Material Card at line 27. Callers can supply both stroke width
and any color. There is no separate `borderColor` argument or mandatory border.
Default and highlighted cards are borderless unless a caller supplies a border
or adds a border modifier. Highlighting changes background and content colors.
Shape and content padding are configurable; elevation defaults to zero.

## Remaining explicit card perimeter borders

All listed strokes are 1dp (`PulseTokens.focusWidth / 2` also equals 1dp).

| Feature / source | Card | Border color | Uses PulseCard? |
| --- | --- | --- | --- |
| Garage / `GarageOverview.kt:89` | EfficiencyCards | primary at 30% opacity | Yes |
| Garage / `GarageOverview.kt:209` | OverviewCare | divider | Yes |
| Garage / `GarageOverview.kt:239` | CareCount | divider | Yes |
| Garage / `GarageOverview.kt:263` | PressureCard | divider | Yes |
| Garage / `GarageOverview.kt:347` | PressureValues | primary when highlighted, otherwise divider | Yes |
| Garage / `FuelHistoryCards.kt:110` | FuelHistoryCard | divider | Yes |
| Garage / `MaintenanceHistoryCard.kt:71` | MaintenanceHistoryCard | divider | Yes |
| Garage / `MaintenanceHistoryCard.kt:186` | MaintenanceReceipts attachment cards | divider | Yes |
| Garage / `TiresAirSection.kt:104` | AirFillHistoryCard | divider | Yes |
| Fitness / `FitnessStats.kt:94` | FitnessStatCard | divider | No, Surface |
| Fitness / `FitnessScreen.kt:274` | SelectedDay workout items | divider | No, clickable Surface |
| Fitness / `WorkoutDetailContent.kt:310` | WorkoutExerciseCard | divider | No, Surface |
| Horizon / `BudgetList.kt:92` | BudgetCard | surface blended 13% toward category accent, or error when over budget | No, Surface |
| Horizon / `BudgetEditor.kt:74` | BudgetEditor container | border | No, Surface |
| Horizon / `SubscriptionList.kt:184` | SubscriptionListCard | divider | No, Surface |
| Horizon / `SubscriptionEditor.kt:239` | SubscriptionSection | border | No, Surface |

## Cards and panels bypassing PulseCard

All direct Material `Card` calls are inside PulseCard itself; no feature directly
uses Material Card, ElevatedCard or OutlinedCard. However, that does **not** mean
all visual cards use PulseCard: the following use Surface instead.

| Feature | Surface-backed card/panel locations | Perimeter border |
| --- | --- | --- |
| Meals | `MealAgenda.kt:151` meal groups | Removed |
| Meals | `MealCalendar.kt:115` calendar panel | None |
| Home | `DashboardSections.kt:141` IllustratedDashboardCard | None |
| Fitness | `FitnessStats.kt:89`, `FitnessScreen.kt:260`, `WorkoutDetailContent.kt:310` | See table above |
| Fitness | `WorkoutDetailContent.kt:168` information panel | None |
| Horizon | `BudgetList.kt:88`, `BudgetEditor.kt:70`, `SubscriptionList.kt:180`, `SubscriptionEditor.kt:235` | See table above |
| Horizon | `BudgetEditor.kt:179` preview | None |
| Horizon | `SubscriptionList.kt:99` summary cards, `:245` renewal panel, `:356` detail tiles | None |
| Horizon | `SubscriptionEditor.kt:153` explanation panel | None |
| Horizon | `SubscriptionPaymentsSheet.kt:110` monthly payment groups | None |
| Horizon | `HorizonLedger.kt:78` monthly transaction groups | None |
| Horizon | `PlannedPurchaseList.kt:54` summary, `:114` empty state, `:159` PurchaseListCard | None |
| Horizon | `ObligationList.kt:73` ObligationCard | None |
| Horizon | `HorizonProjections.kt:38` ProjectionCard | None |
| Horizon | `HorizonMetrics.kt:125` HorizonMetricCard | None |

Auth, Profile, Notifications, the app shell and Garage use PulseCard for their
card containers. Meals, Fitness and Horizon also use PulseCard in other places.
Icons, pills, selection tiles, dialogs and buttons are not counted as card containers.

## Other outlines on or near cards

These are separate controls/decorations, not additional card perimeter borders:

- Meals: `MealAgenda.kt:212` edit button (`border` at 50%); `MealCalendar.kt:88`
  meal-times button (`border`), `:129/:156` calendar arrows (`border`), `:202`
  date tiles (primary when active, otherwise border).
- Garage: `FuelHistoryCards.kt:218` badges (primary at 25% or divider);
  `MaintenanceHistoryCard.kt:296` badges (action at 25% or divider), `:250-263`
  dashed attachment-upload outline (action when enabled, otherwise divider);
  `GarageScreen.kt:398` delete-vehicle button (error at 40%).
- Fitness: `FitnessComponents.kt:78/:89` calendar controls (divider), `:106`
  legend ring (caller color); `ExerciseDialogs.kt:71` modal perimeter (border),
  `:195` metadata pills (border).
- Horizon: `FixedObligationEditor.kt:280` category selection tiles (action when
  selected, otherwise border at 65%); `ObligationDayField.kt:117` day options
  (action when checked, otherwise border); `SubscriptionEditor.kt:319` segmented
  choices (success/action when selected, otherwise border);
  `PlannedPurchaseList.kt:218` delete button (error at 24% enabled / 10% disabled).
- Shared core components also own control outlines: PulseButton (secondary),
  PulseSelectionField, PulseSegmentedTabs, PulseCalendarDay (today/activity/badge
  rings), and PulseNavigationRail (keyboard focus outline). Text-field/chip
  outlines and dividers are control styling, rather than card borders.

## Scope of remediation

Only the requested meal card perimeter was changed. The remaining borders and
Surface-backed cards above are inventoried for review; no app-wide restyling or
component migration was applied. PulseCard's current fixed background choices,
Column wrapper and non-clickable API need consideration before migrating custom
gradient, tinted, or clickable surfaces without changing their appearance/behavior.

Validation: mealplan debug Kotlin compilation and module ktlint checks passed;
`git diff --check` passed. No emulator/device visual check was performed.
