---
version: alpha
name: Pulse
description: A data-first web design system for fitness, meals, finances, and vehicle care.
colors:
  background: "#F6F7FF"
  on-background: "#1A1740"
  surface: "#FFFFFF"
  on-surface: "#1A1740"
  on-surface-variant: "#5E5A7D"
  surface-container: "#F1F2FB"
  surface-container-high: "#E6E5F5"
  outline-variant: "rgb(26 23 64 / 12%)"
  inverse-surface: "#1A1740"
  inverse-on-surface: "#F4F1FF"
  primary: "#415FE8"
  on-primary: "#FFFFFF"
  primary-hover: "#3248BB"
  primary-container: "rgb(76 107 255 / 15%)"
  on-primary-container: "#342B76"
  action: "#4440AD"
  secondary: "#F1F2FB"
  on-secondary: "#1A1740"
  secondary-hover: "#E6E5F5"
  error: "#BF2D24"
  error-container: "#FCEAE7"
  success: "#087658"
  success-container: "#E1F5EC"
  warning: "#925900"
  warning-container: "#FFF2CD"
  info: "#3856A5"
  info-container: "#E8EEFF"
  horizon:
    surface: "#FFE08A"
    on-surface: "#3A2A00"
  brand:
    cyan: "#19D3F3"
    blue: "#4C6BFF"
    magenta: "#FF2FD0"
  dark:
    background: "#0D0A1F"
    on-background: "#F4F1FF"
    surface: "#1A1636"
    on-surface: "#F4F1FF"
    on-surface-variant: "#A49EC4"
    surface-container: "#231E45"
    surface-container-high: "#342C5D"
    outline-variant: "rgb(244 241 255 / 14%)"
    inverse-surface: "#F4F1FF"
    inverse-on-surface: "#1A1740"
    primary: "#F4F1FF"
    on-primary: "#0D0A1F"
    primary-hover: "#DCD5FF"
    primary-container: "rgb(76 107 255 / 15%)"
    on-primary-container: "#E5DCFF"
    action: "#C1B4FF"
    secondary: "#231E45"
    on-secondary: "#F4F1FF"
    secondary-hover: "#342C5D"
    error: "#FF7A6B"
    error-container: "#492638"
    success: "#3DDCA6"
    success-container: "#193C39"
    warning: "#FFC83D"
    warning-container: "#3C3129"
    info: "#ADC3FF"
    info-container: "#263054"
typography:
  display:
    fontFamily: Bricolage Grotesque
    fontSize: 48px
    fontWeight: "700"
    lineHeight: 1.05
    letterSpacing: -0.035em
  headline-lg:
    fontFamily: Bricolage Grotesque
    fontSize: 36px
    fontWeight: "700"
    lineHeight: 40px
    letterSpacing: -0.025em
  headline-md:
    fontFamily: Bricolage Grotesque
    fontSize: 30px
    fontWeight: "700"
    lineHeight: 36px
    letterSpacing: -0.025em
  headline-sm:
    fontFamily: Bricolage Grotesque
    fontSize: 24px
    fontWeight: "700"
    lineHeight: 32px
    letterSpacing: -0.025em
  title-lg:
    fontFamily: Bricolage Grotesque
    fontSize: 20px
    fontWeight: "600"
    lineHeight: 28px
  body-md:
    fontFamily: Bricolage Grotesque
    fontSize: 16px
    fontWeight: "400"
    lineHeight: 24px
  body-sm:
    fontFamily: Bricolage Grotesque
    fontSize: 14px
    fontWeight: "400"
    lineHeight: 20px
  label-md:
    fontFamily: Bricolage Grotesque
    fontSize: 14px
    fontWeight: "600"
    lineHeight: 20px
  label-sm:
    fontFamily: Bricolage Grotesque
    fontSize: 12px
    fontWeight: "500"
    lineHeight: 16px
  label-caps:
    fontFamily: Bricolage Grotesque
    fontSize: 13px
    lineHeight: 1.4
  code-sm:
    fontFamily: Bricolage Grotesque
    fontSize: 12px
    lineHeight: 16px
rounded:
  sm: 4px
  md: 6px
  DEFAULT: 8px
  lg: 8px
  xl: 12px
  control: 14px
  card: 28px
  hero: 32px
  full: 999px
spacing:
  base: 8px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  dashboard-max-width: 1280px
motion:
  menu-enter:
    duration: 150ms
    timingFunction: ease-out
  theme-color-change:
    duration: 250ms
    timingFunction: ease
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.DEFAULT}"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
    textColor: "{colors.on-primary}"
  button-secondary:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.on-secondary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.DEFAULT}"
  button-secondary-hover:
    backgroundColor: "{colors.secondary-hover}"
    textColor: "{colors.on-secondary}"
  button-secondary-dark:
    backgroundColor: "{colors.dark.secondary}"
    textColor: "{colors.dark.on-secondary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.DEFAULT}"
  button-secondary-dark-hover:
    backgroundColor: "{colors.dark.secondary-hover}"
    textColor: "{colors.dark.on-secondary}"
  button-destructive:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.error}"
    typography: "{typography.label-md}"
    rounded: "{rounded.DEFAULT}"
  button-destructive-dark:
    backgroundColor: "{colors.dark.error-container}"
    textColor: "{colors.dark.error}"
    typography: "{typography.label-md}"
    rounded: "{rounded.DEFAULT}"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.card}"
  card-dark:
    backgroundColor: "{colors.dark.surface}"
    textColor: "{colors.dark.on-surface}"
    rounded: "{rounded.card}"
  card-horizon:
    backgroundColor: "{colors.horizon.surface}"
    textColor: "{colors.horizon.on-surface}"
    rounded: "{rounded.card}"
    padding: "{spacing.lg}"
  input-field:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.DEFAULT}"
  input-field-dark:
    backgroundColor: "{colors.dark.surface-container}"
    textColor: "{colors.dark.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.DEFAULT}"
  list-item-vehicle:
    backgroundColor: transparent
    rounded: "{rounded.xl}"
    padding: "{spacing.base}"
  list-item-vehicle-hover:
    backgroundColor: "color-mix(in oklab, #F1F2FB 60%, transparent)"
  list-item-vehicle-dark-hover:
    backgroundColor: "color-mix(in oklab, #231E45 60%, transparent)"
---

## Brand & Style

Pulse is a personal-data hub for fitness, meals, finances, and vehicle care. Its tagline is “Your life, in rhythm.” The dashboard gives people a quick read on their day. Feature screens make it easy to log an entry, make a plan, or review a history.

**Vision.** Give people one dependable place to understand the routines and responsibilities that make up their day.

**Mission.** Make everyday activity easy to record, useful to review over time, and clear enough to act on.

Cool neutral surfaces, indigo actions and softly rounded cards give the feature screens a consistent base. The [Pulse mark](public/pulse-logo.png) uses connected nodes and angular strokes in a cyan-to-magenta gradient. The dashboard greeting echoes that gradient in its artwork. Keep the mark at its native proportions with clear space around it.

## Colors

Primary actions and checked controls use indigo with white text in light mode and a pale fill with dark text in dark mode. Use `text-action` for unfilled actions and `outline-ring` for keyboard focus.

Use `background` for the canvas, `surface` for content cards, and `surface-container` for input fills. Use `on-surface` for key text and `on-surface-variant` for captions and metadata. Light-mode roles sit at the top level of `colors`. The `dark` group overrides the roles that change with appearance. Use the semantic CSS variables in `@app/globals.css` so Light, Dark, and System preferences apply across features. Feature theme stylesheets define named tones for hero artwork, charts and feature-specific states. Reference the shared brand tokens when an illustration uses those colors.

Pair status text with its corresponding soft background and a readable label such as “Service overdue.” Give chart series a legend or direct labels.

## Typography

Use Bricolage Grotesque throughout the interface, including technical strings such as API keys. Standard feature headings use the page heading scale, with a larger desktop size where space allows. The dashboard greeting scales from 48px to 72px with the viewport. The font is loaded in `@app/layout.tsx`.

Give the most useful number visual priority, with its unit beside it. Use tabular numerals when comparing amounts, dates, or measurements. Use uppercase labels sparingly for short metadata.

## Layout & Spacing

Spacing uses Tailwind's spacing scale, from `0` up to `16` (64px). Never use arbitrary `[...]` values.

Start with a single column on narrow screens. Add columns where they help people compare related content. The dashboard centers its content within 1280px and uses 16px side padding on small screens. Use compact gaps within a group and larger gaps between sections. Give long names, dates, and currency amounts room to wrap without crowding actions.

| Area | Lead with | Supporting detail |
| --- | --- | --- |
| Fitness | Workout date and exercise names | Sets, reps, and weight |
| Meal Plan | Selected date and meal time | Planned meals or an action to add one |
| Financial Horizon | Amount, period, and account where relevant | Transaction details, upcoming obligations, or import results |
| Garage | Vehicle name and the record or task | Quantities, odometer readings, costs, and service dates |

## Elevation & Depth

Separate the canvas, cards, and controls with tonal fills. Cards are raised by default with the soft `shadow-card` elevation and have no outline. Cards layered on another surface or placed inside a dialog use `elevation="flat"`, never a caller-side `shadow-none`. Use dividers between repeated records and keep card position and shadow steady on hover.

## Shapes

Standard buttons use an 8px radius. The shared control radius is 14px, cards use 28px and gradient tiles use 32px. Compact cards nested in a card or dialog use 14px. Screens never set their own card radius. Keep repeated controls and cards consistent within a screen.

## Motion & Micro-interaction

Keep feedback quick and local. The shared dropdown entrance takes 150ms and eases out. The base canvas and text color transition takes 250ms. The dashboard greeting uses decorative motion and suppresses it when the user prefers reduced motion.

## Voice & Content Strategy

Write in a calm, direct tone. Use sentence case for headings, field labels, and actions, and keep the feature names Fitness, Meal Plan, Financial Horizon, and Garage. Name actions by their outcome, such as “Add vehicle,” “Log a transaction,” or “Save.”

For empty lists, name the missing records and provide the next action when one is available. For loading, name the operation if it needs a label. For errors, say what failed and offer a retry where supported. Keep entered values visible when a form needs correction.

Apply the account’s currency and time zone when formatting values. Preserve calendar-only dates as selected. Keep measurements next to their units, as in `42 L` or `12,450 km`.

## Components

Use the `Pulse` prefix for component names and `pulse-` for component filenames in the web design system.

### Buttons & Inputs

- **Buttons.** Use the shared variants in `@app/components/design-system/pulse-button.tsx`. Use the primary fill for routine actions, the gradient variant for prominent actions, and the secondary or outline variant for alternatives. Pass the shared size and shape props for pill and icon controls. The standard medium button has a 40px minimum height and 8px vertical and 16px horizontal padding.
- **Fields.** Keep labels visible and show errors by their field. The fuel form provides the reference treatment for `input-field` in `@app/components/fuel-form.tsx`. Use shared controls such as `@app/components/design-system/pulse-select.tsx` for choices.

### Cards & Elevation

- **Cards.** Use `PulseCard` for content and `PulseCardLink` for cards that navigate. `@app/components/design-system/pulse-card.tsx` owns the radius, fill, shadow, padding scale and focus ring; callers pass layout through `className`. Card styles use theme tokens (`bg-card-fill`, `from-tile-start`, `shadow-tile` and so on), never arbitrary `[...]` values. Choose the look with named options:
  - `variant`: `surface` (default) is the solid themed card. `tile` is the gradient feature tile, coloured with `--tile-start` and `--tile-end`, with white text unless `--tile-foreground` is set.
  - `elevation`: `raised` (default) or `flat` for cards layered on another surface or inside a dialog.
  - `size`: `default` or `compact` for the smaller radius on nested cards.
  - `padding`: `sm` (p-4, 16px) for dense rows and nested cards, `md` (p-6, 24px) for content cards, or `lg` (p-8, 32px) for hero and empty-state cards. Each option is one fixed value at every breakpoint. Screens never set card padding with a `p-*` class. `none` (default) is for cards whose children own the padding, such as tables and divided lists.
  - Tint a surface card with `--card-fill` instead of a `bg-*` class.
- **Quick-action tiles.** `@app/dashboard/quick-actions/quick-action-card.tsx` takes a `tone` (`fitness`, `garage`, `meal` or `finance`). The tones' tile colours live in `@app/dashboard/dashboard-theme.css`. Tiles step their type and decor up at container breakpoints (`@xs`, `@sm`, `@md`), never with `clamp()` or `cqi` values.
- **Financial Horizon.** The dashboard summary uses `card-horizon` with a warm yellow fill and 24px padding in both modes. Its fill is defined in `@app/financial-horizon/financial-theme.css` and shared with the pool summary.

### Lists & Navigation

- **Vehicle rows.** The `list-item-vehicle` recipe keeps each row easy to scan and uses a muted hover fill for linked records. See `@app/garage/vehicles-list.tsx`.

### Dialogs

- **Dialogs.** Use `@app/components/design-system/pulse-dialog.tsx` for modal flows and `@app/components/design-system/pulse-confirmation-dialog.tsx` for confirmations. Name the action and its consequence.

## Do's and Don'ts

- Give the most important action and the most useful value a clear place in the hierarchy.
- Keep the logo gradient in brand artwork and use themed colors for routine controls and records.
- Keep keyboard focus visible and give icon-only actions accessible names.
- Pair color with text or another visible cue for status and selection.
