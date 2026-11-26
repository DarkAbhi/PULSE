# Frontend architecture

Pulse's web frontend is feature-first. The Next.js App Router layer stays thin, and business code lives in the feature that owns it.
`tests/architecture/import-boundaries.test.mjs` enforces the rules below and runs with `npm test`.

## Layout

```text
app/          Routes only: page/layout files, metadata, params, redirects, route handlers, globals.css
features/     Business domains (auth, dashboard, financial-horizon, fitness, garage,
              meal-plan, notifications, profile, quests)
components/
  ui/         Pulse design system primitives, with stories colocated (*.stories.tsx)
  layout/     Domain-independent layout: PulseAppBar, page frame
  shared/     Domain-independent application components (LocalDate)
lib/          Technical infrastructure: api/, auth/, user/, currency/, time-zone/, utils/
providers/    App-wide React providers
tests/        Cross-cutting tests (architecture rules); browser journeys stay in e2e/
```

Create a folder only when it has content. There is no `src/` directory, and `@/*` maps to the project root.

## Inside a feature

```text
features/garage/
  screens/     Route-level composition, one per page (garage-screen.tsx, vehicle-detail-screen.tsx)
  components/  Feature UI; group a page's components in a subfolder (components/vehicle-detail/)
  api/         Server actions ("use server") and browser-safe API clients the UI calls
  server/      Server-only loaders used by screens; they import "server-only"
  types/ utils/ styles/ stores/   As needed; styles/ holds theme CSS and Tailwind class maps
  index.ts     Client-safe public API, only when another feature needs something
  server.ts    Server-only public API, only when needed; it imports "server-only"
```

A large domain splits into subdomains that own their tabs, dialogs, actions and types. For example, `features/financial-horizon/` has `accounts/`, `budgets/`, `obligations/`, `overview/`, `planned-purchases/`, `subscriptions/` and `transactions/`. Its domain-wide pieces sit at the feature root in `components/`, `api/`, `types/` and `utils/`.

Route files resolve `params` and `searchParams`, declare `metadata`, and render a screen:

```tsx
export default async function GymVisitPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <WorkoutDetailScreen visitId={id} />;
}
```

## Dependency rules

```text
app/ → features/ (public entry points and screens/) → components/, lib/, providers/
components/ui → lib     components/layout → ui, lib     components/shared → components, providers, lib
providers → lib         lib → lib
```

- `features/` never imports `app/`, and the shared layers never import `features/` or `app/`.
- Another feature's code is imported only through `@/features/<name>` (`index.ts`) or `@/features/<name>/server` (`server.ts`), never through deep paths. `app/` may also import `screens/*`.
- Code inside a feature imports its own modules directly, not through its own `index.ts`.
- `index.ts` must not re-export server-only code. Server components that need the session go in `server.ts`.
- The feature and module dependency graphs must stay acyclic.

The current cross-feature edges are deliberate:

- Every screen renders `TopAppBar` from `@/features/notifications/server`. The app bar's only feature-aware part is the notification center, so notifications owns it.
- The dashboard reads types from financial-horizon and quests, and `NotificationDropdown` from notifications.
- Profile uses the dashboard view preference from `@/features/dashboard`.

## Import exceptions

All application imports use `@/`, with two exceptions where the tool resolves imports without tsconfig paths:

- `*.test.mjs` files run under `node --test`, so they import their colocated module relatively (`./currency.mjs`).
- `app/globals.css` imports feature theme stylesheets relatively, because Tailwind inlines CSS `@import`s with its own resolver. These themes register Tailwind `@theme` tokens, so they must stay in the global stylesheet. Other feature themes are imported by their screen.

Stories are excluded from `tsconfig.json`, because Storybook is installed with `--no-save` and is missing in CI. As a result, `.storybook/main.ts` declares the `@/` alias for Vite itself.

## Data loading and shared client state

- Screens never call the backend directly. A feature's `server/` loader does: either `readJSON` from `lib/api/server-api.ts`, which returns `{ status, data }` with `status` 0 when the backend is unreachable, or a loader-specific fetch when the page needs per-part errors. Redirect and not-found decisions stay in the screen.
- A feature with one shared client state splits it by subdomain hook instead of by component. `features/financial-horizon/components/financial-horizon-client.tsx` owns the summary, the error banner and one transition, and passes them as `HorizonMutations` to `useBudgets`, `useFixedObligations`, `usePlannedPurchases`, `useSubscriptions` and `useTransactionEditor`. Every mutation therefore updates the same totals and pending state.
- A dialog with its own form mounts only while open, so each opening starts fresh (`features/meal-plan/components/add-meal-dialog.tsx`).

## Where code goes

- Used by one feature: keep it in that feature, even if several of its screens share it.
- Domain-independent and used by more than one feature: move it to `components/` or `lib/` (for example, `lib/utils/format-duration.mjs`).
- Session, cookies and backend transport: `lib/api/server-api.ts` (server-only) and `lib/api/api-transport.ts`.
