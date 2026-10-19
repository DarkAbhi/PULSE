# Fitness tab redesign

The Android fitness tab follows the supplied calendar and selected-day reference using the existing Pulse light/dark theme. Phones stack the calendar and selected-day content; wider windows place them side by side. Calendar controls retain month navigation, Today, date picking, workout markers, selected/today semantics, and accessible tap targets. Narrow windows and large text retain horizontal calendar scrolling.

The selected-day card shows the actual workout count and an Add workout action. Workouts for this day groups clickable workout rows with their recorded time. Titles are generic because the overview has no workout name. Empty, loading, error, refresh, and uncertain-mutation review states remain supported. Streak/statistics cards and the timezone note are omitted.

The overview response (`go-backend/internal/gym/overview.go`, `FitnessOverviewDto`, and `WorkoutSummaryDto`) contains workout IDs and time labels, but no duration or effort. Neither workout duration/effort chips nor selected-day total duration are displayed. Imported timing remains available on the existing workout detail screen; the redesigned overview does not issue per-workout detail requests or infer durations.

## Verification

- Debug app and instrumentation APK builds passed; 16 fitness unit tests passed. Fitness formatting checks passed and the completed fitness debug lint run reported no issues. A later duplicate lint run was stopped after it stalled; the final formatting/import cleanup compiled successfully.
- Seven of eight distinct existing fitness device scenarios passed across the full phone run and focused reruns, including calendar/workout CRUD, refresh, future dates/date picker, imported details, window resizing, and uncertain mutation recovery.
- The calendar/workout CRUD scenario also passed on the tablet emulator. Phone and tablet screenshots below were visually reviewed, using isolated HTTP fixtures.
- The editor/logout scenario still fails while returning from the profile/logout flow to the unsaved exercise editor. Its original logout expectation remains unchanged. This scenario is not claimed to pass.
- Physical devices, manual TalkBack, and live backend accounts were not exercised.

## Captures

- [Phone calendar](phone.png)
- [Phone selected day](phone-workout.png)
- [Tablet empty day](tablet.png)
- [Tablet recorded workout](tablet-workout.png)

## Compact calendar header

The mobile month label and Previous, Next, Today, and Choose date controls now share a row at standard text sizes. The mobile label uses a localized abbreviated month, and Today has a compact 48dp control. The full month remains exposed to accessibility. Larger text and unusually narrow containers keep a stacked fallback. Tablet presentation retains its full month label.

Debug app/test APK builds and fitness formatting checks passed. The calendar/workout CRUD device scenario passed on both phone and tablet, including new assertions that the heading and navigation controls are vertically centered on the same row.

- [Updated phone header](phone-header.png)
- [Updated tablet header](tablet-header.png)

## Header hierarchy and centered loading

The mobile month/year label now uses the 18sp title style instead of the 12sp label style. Compact arrow and date buttons have 32dp visual outlines and 20dp icons inside 48dp tap targets; Today uses a 40×32dp visual inside a 48dp target.

Initial loading and refresh loading replace the content with one vertically and horizontally centered loading group. The top pull indicator is hidden while this loading group is active; gesture feedback remains available before refreshing.
