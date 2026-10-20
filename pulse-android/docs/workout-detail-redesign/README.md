# Workout detail redesign

Implemented on 20 October 2026 in the Android fitness feature.

## Behavior

- Imported workouts show the visit date, linked Apple Watch label, available start/end/duration/energy tiles, and read-only notice. No watch illustration.
- Visits without any timing or energy show the compact notice from the reference design. Missing values are omitted, never substituted with zero or inferred.
- Empty workouts show a bundled clipboard/dumbbell illustration and Add exercise action.
- Exercise cards show available equipment/muscles, numbered set tiles, edit/remove actions, and View instructions when catalogue instructions exist.
- Instructions open in a dismissible, scrollable dialog (also when accessing catalogue information in the editor).
- Summary contains only exercise and set counts, with singular/plural labels. It sits beside exercises on wide screens and above them on phones.
- Swipe down to refresh, with an accessible refresh action and progress indicator. Refresh is disabled while editing, saving, or already loading. Delete confirmations, editor, mutation recovery, initial loading, and refresh-in-place behavior use the existing callbacks and state.
- Colors follow the existing Pulse theme, including dark mode.

## Data limits and design adjustments

`CatalogExercise` provides name, equipment, muscles, and instructions; it has no prose description or exercise-image URL. The descriptive squat sentence in the reference is therefore omitted. The bundled barbell illustration is decorative and shown for barbell equipment on sufficiently wide cards; it is not an instructional exercise image. Custom exercises without a catalogue match do not show invented equipment, muscles, or instructions.

Timing and energy are supplied by the existing workout API. They remain read-only. Refresh reloads the workout through the existing API; it does not trigger an Apple Watch sync. The timezone notice is omitted from the UI. Times retain the app's Asia/Kolkata convention.

## Generated assets

Built-in image generation tool used with transparent backgrounds. The production PNGs are in `feature/fitness/src/main/res/drawable-nodpi/`:

- `workout_empty.png`
- `workout_barbell.png`

Final prompts:

> Use case: stylized-concept. Create one isolated fitness app empty-state illustration: a small ivory clipboard with golden yellow clip and three neutral checklist marks, with a charcoal dumbbell with golden collars resting in front. Polished soft 3D clay illustration, warm cream and golden yellow palette, soft studio light, subtle pale cream organic blob behind objects, tiny golden accent rays. Landscape composition with generous transparent margin. No text, no lettering, no watch, no logos. Intended to match a warm cream Android workout detail screen.

> Use case: stylized-concept. Asset type: fitness app exercise card decoration. One isolated charcoal black barbell with silver bar and round black weight plates, three quarter perspective horizontally, polished soft 3D clay render. Warm ivory organic blob and a few small golden yellow accent rays behind. Transparent background. Landscape compact composition, no text, no logos, no people, no watch. Warm cream yellow UI illustration style.

## Verification

- Fitness feature unit tests, Kotlin formatting, debug app build, and instrumented-test build.
- `WorkoutRedesignTest` covers imported metrics/empty state, counts, modal instructions opening/closing, custom exercises without catalogue data, dark mode, and remove confirmation.
- `WorkoutLoadingLayoutTest` verifies centered initial loading, swipe-to-refresh, stable content during refresh, and blocked duplicate refresh gestures while loading or saving.
- Both instrumented test classes run on phone (1344×2992) and tablet (2560×1600) emulators.
- Screenshots in `phone/` and `tablet/` are rendered from controlled test fixtures, not live user records. The app shell toolbar is outside these component test captures.
