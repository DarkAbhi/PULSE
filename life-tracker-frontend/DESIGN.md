---
version: alpha
name: Pulse
description: A warm, data-first web design system for fitness, meals, finances, and vehicle care.
colors:
  background: "#F6F7FF"
  on-background: "#1C1C1A"
  surface: "#FFFDF8"
  on-surface: "#1C1C1A"
  on-surface-variant: "#605D56"
  surface-container: "#EAE7DF"
  surface-container-high: "#DEDAD1"
  outline-variant: "#E5E1D9"
  inverse-surface: "#1C1C1A"
  inverse-on-surface: "#F5F2EA"
  primary: "#EDB937"
  on-primary: "#1C1C1A"
  primary-hover: "#D9A425"
  primary-container: "#F8E7AD"
  on-primary-container: "#3B3015"
  action: "#745200"
  secondary: "#EAE7DF"
  on-secondary: "#1C1C1A"
  secondary-hover: "#DEDAD1"
  error: "#AB3028"
  error-container: "#FBE2DB"
  success: "#3E642F"
  success-container: "#E3EDD9"
  warning: "#795200"
  warning-container: "#F8E7AD"
  info: "#285C70"
  info-container: "#E0EFF4"
  horizon:
    surface: "#FFE58C"
    on-surface: "#20211B"
  brand:
    cyan: "#19D3F3"
    blue: "#4C6BFF"
    magenta: "#FF2FD0"
  dark:
    background: "#0D0A1F"
    on-background: "#F5F2EA"
    surface: "#242420"
    on-surface: "#F5F2EA"
    on-surface-variant: "#C8C4BA"
    surface-container: "#30302B"
    surface-container-high: "#3B3B34"
    outline-variant: "#41413A"
    inverse-surface: "#F5F2EA"
    inverse-on-surface: "#1C1C1A"
    primary-container: "#494022"
    on-primary-container: "#F8E7AD"
    action: "#EDC660"
    secondary: "#30302B"
    on-secondary: "#F5F2EA"
    secondary-hover: "#3B3B34"
    error: "#FFB4A8"
    error-container: "#542C26"
    success: "#ADD08E"
    success-container: "#2C3E25"
    warning: "#F0CA6B"
    warning-container: "#494022"
    info: "#A1D6E9"
    info-container: "#253F49"
typography:
  display:
    fontFamily: Geist
    fontSize: 48px
    fontWeight: "700"
    lineHeight: 1.05
    letterSpacing: -0.035em
  headline-lg:
    fontFamily: Geist
    fontSize: 36px
    fontWeight: "700"
    lineHeight: 40px
    letterSpacing: -0.025em
  headline-md:
    fontFamily: Geist
    fontSize: 30px
    fontWeight: "700"
    lineHeight: 36px
    letterSpacing: -0.025em
  headline-sm:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: "700"
    lineHeight: 32px
    letterSpacing: -0.025em
  title-lg:
    fontFamily: Geist
    fontSize: 20px
    fontWeight: "600"
    lineHeight: 28px
  body-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: "400"
    lineHeight: 24px
  body-sm:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: "400"
    lineHeight: 20px
  label-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: "600"
    lineHeight: 20px
  label-sm:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: "500"
    lineHeight: 16px
  label-caps:
    fontFamily: Geist
    fontSize: 13px
    lineHeight: 1.4
  code-sm:
    fontFamily: Geist Mono
    fontSize: 12px
    lineHeight: 16px
rounded:
  sm: 4px
  md: 6px
  DEFAULT: 8px
  lg: 8px
  xl: 12px
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
    backgroundColor: "color-mix(in oklab, #EAE7DF 60%, transparent)"
  list-item-vehicle-dark-hover:
    backgroundColor: "color-mix(in oklab, #30302B 60%, transparent)"
---

## Brand & Style

Pulse is a personal-data hub for fitness, meals, finances, and vehicle care. Its tagline is “Your life, in rhythm.” The dashboard gives people a quick read on their day. Feature screens make it easy to log an entry, make a plan, or review a history.

**Vision.** Give people one dependable place to understand the routines and responsibilities that make up their day.

**Mission.** Make everyday activity easy to record, useful to review over time, and clear enough to act on.

Warm paper and charcoal surfaces, mustard actions, and softly rounded cards give everyday screens a calm, practical feel. The [Pulse mark](public/pulse-logo.png) uses connected nodes and angular strokes in a cyan-to-magenta gradient. The dashboard greeting echoes that gradient in its artwork. Keep the mark at its native proportions with clear space around it.

## Colors

Mustard fills the primary action and checked controls. Pair it with dark ink in both modes. Use the mode’s `action` color for text links and unfilled actions. Keyboard focus uses the same value through `--ring`.

Use `background` for the canvas, `surface` for content cards, and `surface-container` for input fills. Use `on-surface` for key text and `on-surface-variant` for captions and metadata. Light-mode roles sit at the top level of `colors`. The `dark` group overrides the roles that change with appearance. Use the semantic CSS variables in `@app/globals.css` so Light, Dark, and System preferences apply across features. The brand colors describe the existing dashboard artwork in `@app/dashboard/dashboard.css`.

Pair status text with its corresponding soft background and a readable label such as “Service overdue.” Give chart series a legend or direct labels.

## Typography

Use Geist for interface text and Geist Mono for technical strings such as API keys. Standard feature headings use the page heading scale, with a larger desktop size where space allows. The dashboard greeting scales from 48px to 72px with the viewport. The fonts are loaded in `@app/layout.tsx`.

Give the most useful number visual priority, with its unit beside it. Use tabular numerals when comparing amounts, dates, or measurements. Use uppercase labels sparingly for short metadata.

## Layout & Spacing

Start with a single column on narrow screens. Add columns where they help people compare related content. The dashboard centers its content within 1280px and uses 16px side padding on small screens. Use compact gaps within a group and larger gaps between sections. Give long names, dates, and currency amounts room to wrap without crowding actions.

| Area | Lead with | Supporting detail |
| --- | --- | --- |
| Fitness | Workout date and exercise names | Sets, reps, and weight |
| Meal Plan | Selected date and meal time | Planned meals or an action to add one |
| Financial Horizon | Amount, period, and account where relevant | Transaction details, upcoming obligations, or import results |
| Garage | Vehicle name and the record or task | Quantities, odometer readings, costs, and service dates |

## Elevation & Depth

Separate the canvas, cards, and controls with tonal fills. Shared cards have a solid fill and no default outline or shadow. Use dividers between repeated records and keep card position and shadow steady on hover.

## Shapes

Standard buttons use an 8px radius. Shared cards use 28px corners, and the dashboard greeting uses 32px. Keep repeated controls and cards consistent within a screen.

## Motion & Micro-interaction

Keep feedback quick and local. The shared dropdown entrance takes 150ms and eases out. The base canvas and text color transition takes 250ms. The dashboard greeting uses decorative motion and suppresses it when the user prefers reduced motion.

## Voice & Content Strategy

Write in a calm, direct tone. Use sentence case for headings, field labels, and actions, and keep the feature names Fitness, Meal Plan, Financial Horizon, and Garage. Name actions by their outcome, such as “Add vehicle,” “Log a transaction,” or “Save.”

For empty lists, name the missing records and provide the next action when one is available. For loading, name the operation if it needs a label. For errors, say what failed and offer a retry where supported. Keep entered values visible when a form needs correction.

Apply the account’s currency and time zone when formatting values. Preserve calendar-only dates as selected. Keep measurements next to their units, as in `42 L` or `12,450 km`.

## Components

### Buttons & Inputs

- **Buttons.** Use the shared variants in `@app/components/design-system/button.tsx`. Give the main action the mustard fill, use the neutral secondary variant for alternatives, and use the destructive variant for destructive actions. The standard medium button has a 40px minimum height and 8px vertical and 16px horizontal padding.
- **Fields.** Keep labels visible and show errors by their field. The fuel form provides the reference treatment for `input-field` in `@app/components/fuel-form.tsx`. Use shared controls such as `@app/components/design-system/select.tsx` for choices.

### Cards & Elevation

- **Cards.** Use `PulseCard` for content and `PulseCardLink` for cards that navigate. Set spacing and layout at the caller. Both share the surface treatment in `@app/components/design-system/pulse-card.tsx`.
- **Financial Horizon.** The dashboard summary uses `card-horizon` with a warm yellow fill and 24px padding in both modes. See `@app/dashboard/dashboard.css`.

### Lists & Navigation

- **Vehicle rows.** The `list-item-vehicle` recipe keeps each row easy to scan and uses a muted hover fill for linked records. See `@app/garage/vehicles-list.tsx`.

### Dialogs

- **Dialogs.** Use `@app/components/design-system/dialog.tsx` for modal flows and `@app/components/design-system/confirmation-dialog.tsx` for confirmations. Name the action and its consequence.

## Do's and Don'ts

- Give the most important action and the most useful value a clear place in the hierarchy.
- Keep the logo gradient in brand artwork and use themed colors for routine controls and records.
- Keep keyboard focus visible and give icon-only actions accessible names.
- Pair color with text or another visible cue for status and selection.
