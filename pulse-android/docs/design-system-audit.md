# Design system audit — 18 October 2026

Recorded before implementation. Scope: Compose UI foundation, shared components,
UI token references, debug gallery, packaged fonts and design-system checks.
No navigation, persistence, ViewModel, API, validation or screen arrangement changes.

## Existing architecture

`:core:ui/PulseTheme.kt` owns a Material `ColorScheme`, `Typography`, `Shapes`
and `PulseTokens`. `PulseTheme` directly installs MaterialTheme. Light/dark modes
already exist and follow the saved appearance setting; no dynamic colors exist.
The Activity XML parent is the Android platform NoActionBar theme, not Material 3;
it must remain compatible with Activity startup and edge-to-edge rendering.
There are no XML screen layouts or bundled font resources.

## Reusable foundations

All common controls already have Pulse wrappers: text, buttons, icon/text buttons,
cards, fields/password/selectors, chips, checkbox/radio/switch, calendar cells,
top bar, bottom navigation, rail, dialogs, date/time pickers, snackbar and progress.
Keep these public signatures, state and accessibility behavior. PulseText already
provides five semantic roles. PulseTokens already supplies 4/8/16/24/32dp spacing,
48dp targets, responsive widths, 24dp cards, 16dp controls and zero elevation.
The debug-only PulseComponentGallery and existing contrast/device tests are reusable.

## Inconsistencies and dependencies

The current rust-orange primary is unsuitable for the requested mustard identity.
All display roles share 36sp; headline/title roles collapse to 24sp; bodyLarge and
bodyMedium are identical; all label roles use 14sp. Fonts are platform sans and
monospace. Shapes are more rounded than the proposed language. Wrappers inherit
Material defaults for colors, type, shape and elevation in many controls.
Feature screens, app chrome and previews directly read MaterialTheme colors.
Material implementations also supply Surface, Scaffold, Icon and progress in
features, plus snackbar host/state in the app. There is no separate reusable
badge/avatar/list-row/tab/bottom-sheet abstraction to migrate.
Hardcoded styling is largely centralized already. Preserve responsive breakpoints,
calendar geometry, icon sizes, spacer arrangements and picker input-mode thresholds;
these are layout/behavior dimensions, not blanket spacing replacements.

## Migration architecture and risks

Keep the existing module and Pulse API. Introduce immutable Pulse-owned colors,
semantic type scale, spacing, shape and elevation tokens, exposed by PulseTheme
CompositionLocals. Retained Material implementations receive a private, fully mapped
compatibility theme driven by those tokens. Feature code reads PulseTheme only;
wrappers explicitly style their surfaces and controls. This keeps native focus,
semantics, insets, keyboard, picker and navigation mechanics intact.

Mustard is not readable as small text on a cream surface: use a separate dark
`action` color for links/focus/progress and mustard `primary` for filled surfaces,
with dark ink on mustard. Supporting gray and semantic status colors must pass
contrast checks rather than copy decorative swatches into text roles.
Bundle Barlow Condensed Bold, Instrument Serif Regular and DM Sans locally with
licenses. Reserve the serif for an explicit editorial role. Font/scale changes can
alter wrapping, especially at 200% font size; validate with the existing device
suite and retain min-height rather than fixed-height interactive controls.
