# Pulse design system

Implementation and standards, 18 October 2026. See [the pre-implementation audit](design-system-audit.md).

## Architecture

`core:ui` remains the single owner. `PulseTheme.colors` exposes an immutable
`PulseColors` through a CompositionLocal; the existing saved Light/Dark/System
preference and Activity system-bar handling are preserved. Typography, spacing,
shapes and elevation are stable shared values, accessed through
`PulseTheme.typography`, `.spacing`, `.shapes`, `.elevation`. No dynamic color or
font downloads occur at runtime. A nested PulseTheme installs its own palette.

`PulseMaterialBridge.kt` is the only application source permitted to use
MaterialTheme (enforced by ArchitectureTest). It explicitly maps **every** Material
ColorScheme, Typography and Shapes role from Pulse tokens. It disables surface
tinting and supplies the root content color from the active Pulse palette. This is an implementation adapter, not the public theme API. Existing
Material implementations retain focus, interaction, semantics and insets; controls
add explicit token styling for their backgrounds, foregrounds, selected states,
shapes and elevation where applicable. No Material light/dark scheme factory or
Material typography/shape default supplies an application visual token.

```kotlin
PulseTheme {
    PulseCard {
        PulseText(title, style = PulseTextStyle.Title)
        PulseText(description, color = PulseTheme.colors.textSecondary)
    }
}
```

Existing `PulseTokens` dimensions are retained for source compatibility; spacing,
card/control shape and elevation values delegate to the new standards. Responsive
widths, calendar geometry, icon dimensions and picker input-mode breakpoints
remain behavior/layout constants. New UI should use the semantic scale rather
than introduce raw colors or text sizes. No new module or dependency was added.

## Typography

Fonts are bundled resources and licensed under SIL OFL, with each license packaged
in `assets/font-licenses`. Fonts come from the Google Fonts repository:
[Barlow Condensed](https://github.com/google/fonts/tree/main/ofl/barlowcondensed),
[Instrument Serif](https://github.com/google/fonts/tree/main/ofl/instrumentserif),
[DM Sans](https://github.com/google/fonts/tree/main/ofl/dmsans).
DM Sans explicitly sets the variable weight axis to 400/500/600/700. Instrument
Serif uses its actual Regular face, not a synthesized semibold. Condensed display
styles request tabular numbers. Normal Android text scaling remains enabled.

| Token | Size / line height (sp) | Weight | Family | Usage |
| --- | --- | --- | --- | --- |
| displayXL | 64 / 68 | Bold | Barlow Condensed | Hero numerals |
| displayLarge | 48 / 52 | Bold | Barlow Condensed | Featured statistics |
| displayMedium | 40 / 44 | Bold | Barlow Condensed | Metrics |
| heading1 | 32 / 40 | Semibold | DM Sans | Screen heading |
| heading2 | 28 / 36 | Semibold | DM Sans | Major section |
| heading3 | 24 / 32 | Semibold | DM Sans | Subsection |
| title1 | 22 / 28 | Semibold | DM Sans | Card title |
| title2 | 20 / 28 | Medium | DM Sans | Secondary card title |
| title3 | 18 / 24 | Medium | DM Sans | Small title |
| body1 | 16 / 24 | Regular | DM Sans | General content |
| body2 | 14 / 20 | Regular | DM Sans | Secondary content |
| body3 | 13 / 20 | Regular | DM Sans | Compact supporting copy |
| label1 | 14 / 20 | Semibold | DM Sans | Controls |
| label2 | 12 / 16 | Semibold | DM Sans | Metadata |
| caption | 12 / 16 | Regular | DM Sans | Captions |
| overline | 11 / 16 | Bold | DM Sans | Short category labels |
| editorial | 32 / 40 | Regular | Instrument Serif | Select featured headings |

Letter spacing is 0sp except overline (1sp). Use overline sparingly; do not use it
for long instructions or essential body content. `PulseTextStyle` preserves Body,
Label, Title, Display, Metric, mapping respectively to body1, label1, title1,
heading1, displayMedium. Additional roles expose the full scale. Reserve editorial
for special headings; serif is deliberately not applied to every existing title.

## Colors

Mustard is the filled-action identity, with dark ink on top. `action` is its
accessible companion for text links, focus, progress and unfilled controls.
`primarySoft` uses `onPrimarySoft`, particularly in dark mode. Decorative coral,
sage, sky and lavender are not status text colors. Status roles have separate
accessible foregrounds and soft backgrounds. Grays were darkened from the initial
proposal so secondary and tertiary text also pass AA on surfaceStrong.

| Token | Light | Dark |
| --- | --- | --- |
| background | `#F5F2EA` | `#1C1C1A` |
| surface | `#FFFDF8` | `#242420` |
| surfaceMuted | `#EAE7DF` | `#30302B` |
| surfaceStrong | `#DEDAD1` | `#3B3B34` |
| textPrimary | `#1C1C1A` | `#F5F2EA` |
| textSecondary | `#605D56` | `#C8C4BA` |
| textTertiary | `#625F58` | `#B5B1A6` |
| primary | `#EDB937` | `#EDB937` |
| primaryPressed | `#D9A425` | `#D9A425` |
| onPrimary | `#1C1C1A` | `#1C1C1A` |
| primarySoft | `#F8E7AD` | `#494022` |
| onPrimarySoft | `#3B3015` | `#F8E7AD` |
| action | `#745200` | `#EDC660` |
| border | `#D8D4CB` | `#57574D` |
| outline | `#89857B` | `#979388` |
| divider | `#E5E1D9` | `#41413A` |
| success | `#3E642F` | `#ADD08E` |
| successSoft | `#E3EDD9` | `#2C3E25` |
| warning | `#795200` | `#F0CA6B` |
| warningSoft | `#F8E7AD` | `#494022` |
| error | `#AB3028` | `#FFB4A8` |
| errorSoft | `#FBE2DB` | `#542C26` |
| info | `#285C70` | `#A1D6E9` |
| infoSoft | `#E0EFF4` | `#253F49` |
| coral | `#D96A4F` | `#E88B73` |
| sage | `#7E9B65` | `#9BB681` |
| sky | `#79B9D1` | `#95CDE1` |
| lavender | `#A58AC7` | `#BDA5D9` |
| inverseSurface | `#1C1C1A` | `#F5F2EA` |
| inverseText | `#F5F2EA` | `#1C1C1A` |
| inverseAction | `#EDC660` | `#745200` |

Use `textPrimary` on neutral surfaces, `textSecondary` for supporting content,
and `textTertiary` sparingly. Filled primary actions use `primary`/`onPrimary`;
pressed primary actions use `primaryPressed`/`onPrimary`. Status text must also
name the state (not rely on color). Decorative accents require ink and a contrast
check before becoming a new text/background pairing. `border`/`divider` are
subtle decoration; interactive boundaries use `outline` (tested at >=3:1).

## Spacing, shapes, elevation

Spacing is a 4dp grid: none 0, tiny 4, small 8, compact 12, medium 16,
comfortable 20, cardPadding 24, large 32, extraLarge 40, target 48, spacious 64.
Card padding stays 24dp; existing screen margins and arrangements stay intact.

Shapes: none 0dp, extraSmall 4dp, small 8dp, medium 12dp, large 16dp,
extraLarge 20dp, pill 50%. Cards use large, buttons/fields small, chips pill,
dialogs extraLarge, menus/snackbars medium. Elevation: none 0dp, low 2dp,
medium 4dp, high 8dp. Cards and buttons stay flat; dropdowns use low shadow.

## Component migration and retained implementations

| Component | Pulse styling | Preserved implementation behavior |
| --- | --- | --- |
| PulseText | Full semantic scale and bundled fonts | Text semantics and scaling |
| PulseButton | Mustard, pressed mustard, disabled neutral; primary/secondary/tertiary/destructive variants | Click, focus, loading/disabled semantics, >=48dp target |
| PulseTextButton / IconButton | Action/ink colors, neutral disabled colors | Click, labels, focus and target |
| PulseCard | Flat neutral/soft primary, explicit paired foreground, 16dp corners | Content scope and padding |
| PulseTextField / PasswordField | DM Sans, neutral surface, focus/error outlines, 8dp corners | Keyboard, selection, password state, validation and autofill |
| PulseSelectionField | Neutral outlined trigger, 12dp menu, low shadow | Focus/keyboard dismissal and selection callbacks |
| PulseFilterChip | Mustard selection, checkmark cue, ink labels, pill | Selection, disabled state and target |
| PulseCheckbox / RadioButton / Switch | Semantic selected/unselected colors | Parent toggle/select semantics and single callback |
| PulseCalendarDay | Mustard selection, dark ink, action today outline, semantic activity indicator | Date semantics, enabled/selected states and geometry |
| PulseTopAppBar | Flat canvas and ink | Navigation/actions and insets |
| Bottom navigation / rail | Mustard indicator, ink selected, neutral supporting content | Destination selection, scroll and insets |
| PulseDialog / date/time dialogs | Neutral flat surfaces, 20dp corners; explicit picker selected colors | Dismissal, scroll, time/date state and accessibility input modes |
| PulseSnackbar | Inverse surface/text/action, 12dp corners | Queue, timeout, dismiss and action |
| Loading / transfer progress | Action indicator and neutral track | Live regions and progress semantics |
| Empty/error states | Shared text, card and action tokens | Retry callbacks and announced errors |

Material 3 remains installed deliberately. All the controls above retain Material
mechanics behind the existing Pulse abstractions, plus Text and Surface for
content/semantics. App/login/gallery Scaffold, app Icon, snackbar host/state and
Horizon's linear indicator are infrastructure primitives still imported directly;
Scaffold colors are explicit, Icon inherits the enclosing Pulse control's content
color, and the linear indicator reads the fully specified compatibility scheme.
There are no MaterialTheme reads outside the private bridge. These do not introduce
a second styling API. No existing bottom sheet, avatar, badge, list-item or tab
wrapper existed; unused new abstractions were not added.

No business logic, ViewModel, repository, API, validation, destination, argument,
permission, persistence, analytics or dependency-injection behavior was changed.
Feature edits replace color-token references; app/login additionally state their
existing Scaffold surface colors explicitly. XML Activity styling remains the
platform NoActionBar parent for launch compatibility. Legacy unused XML purple/
teal resource entries are not Compose theme inputs and remain untouched.

## Showcase

Open the existing debug Component gallery. Its controls and states tabs still
work; the controls tab now also includes the complete palette, typography scale,
button variants and status samples. No new production destination was created.
`PulseDesignSystemPreview.kt` provides light, dark and 200% text previews of a
compact foundation specimen. DesignSystemDeviceTest also renders the specimen and captures PNG test artifacts;
see [validation results](design-system-validation.md) for the device combinations
actually executed and the earlier native preview captures.
Existing gallery previews still cover interactive controls, calendar and states.

This request attached a written design brief, not reference screenshots; the
implementation follows its palette, typography and visual-language guidance.

## Accessibility and validation contract

DesignSystemTest checks >=4.5:1 for the supported text/surface, primary/pressed,
soft-primary, status/soft-status and inverse pairs in both modes; outlines and
calendar markers meet >=3:1. It also checks type hierarchy and existing >=48dp
control targets. Device checks cover theme switching, disabled/loading callbacks,
large-text reachability, and specimen rendering. Existing gallery and feature
suites cover state restoration, forms, navigation, date/time selection and retries.
These checks do not replace a manual TalkBack, keyboard, and real-device review.
No decorative motion was introduced; existing animation behavior is preserved.

## Subsequent screen redesign

Keep future changes separate: Home can sharpen the relationship between its
independently loading summaries; Horizon needs deliberate hierarchy for amounts,
ledger rows and editor sections; Fitness needs density review around calendar and
workout details; Garage and Meal Plan can simplify long forms and metadata groups.
At 200% text size review horizontal action rows and dense tablet layouts individually.
Use the editorial and condensed roles selectively during that phase. No layouts
or information groupings were changed here.

## Packaged font integrity

| Resource | SHA-256 |
| --- | --- |
| barlow_condensed_bold.ttf | `e476562ec9c1e16cf16475895b511f08c804f438cc9a9f80a44ea50a0eeb5b65` |
| dm_sans.ttf | `8cd08d97e89c24d0aa92edd2f0f4c8ee6195eee9b7c9f154865a58b02f0c1c0d` |
| instrument_serif_regular.ttf | `498efd461f6ddfcb7a111bf9a565709d2085d48201d501ead960d93e84ffbb88` |
