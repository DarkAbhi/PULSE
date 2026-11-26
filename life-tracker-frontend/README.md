This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

Routes live in `app/`; page content lives in `features/<name>/screens/`. Pages auto-update as you edit them.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to optimize and self-host [Bricolage Grotesque](https://fonts.google.com/specimen/Bricolage+Grotesque).

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.

## Docker live reload

`make dev` serves the frontend at http://localhost:3102. Docker uses
`npm run dev:docker` (Webpack) with one-second file polling so edits on Windows
bind mounts trigger Fast Refresh. Direct `npm run dev` still uses Turbopack.
The container keeps `.next` in its own named volume so local builds do not
overwrite the running development server's cache.

After changing the container configuration or startup command, run `make dev-web`
once to apply it to the frontend alone. Normal page and stylesheet edits refresh
automatically; no `make dev-down` is needed. Dependency changes require a frontend
restart to reinstall packages.

## Architecture

Code is organized by feature under `features/`, with thin route files in `app/`. See
[ARCHITECTURE.md](ARCHITECTURE.md) for the layout and the dependency rules that `npm test` enforces.

## Tests

From this directory, with Node.js 24 and npm installed:

```sh
npm ci
npm test
# Run one test file:
npm test -- features/profile/utils/copy-api-key.test.mjs
# Type-check and build:
npm run build
```

The Node tests cover frontend utility behavior and need no database or running
backend. Browser E2E coverage is paused in CI; retained Playwright journeys can
be revisited later. Maestro currently covers small native Android smoke tests;
see [Maestro smoke tests](../maestro/README.md).

## Pulse Design System Storybook

From this directory, run `npm run storybook:install`, then `npm run storybook`
and open [localhost:6006](http://localhost:6006). The component stories use
Pulse's Tailwind styles, font, and light and dark themes. Use **Controls** to
change props, the **Pulse theme** toolbar to switch themes, and the viewport
toolbar to check phone, tablet, and desktop layouts. The stories use local
examples and run without a Pulse account, backend, or database.

Add new design-system stories as `*.stories.tsx` under `.storybook/stories/`.
Import components with `@/`. Run `npm run build-storybook` for a static catalog
in `storybook-static/`. Storybook packages are installed locally on demand.
Run `npm run storybook:install` again after a clean `npm ci`.

## Cards

Use `PulseCard` from `@/components/ui/pulse-card` for card surfaces,
including empty states. It shares the dashboard's themed background, soft corners,
and borderless treatment. Pass `as="section"`, `as="article"`, or `as="aside"`
when the content needs those semantics, and supply sizing, padding, and layout
with `className`. Use the named `PulseCardLink` export for cards that navigate.
Card surfaces should keep the same shadow and position on hover.
Keep the shared surface styling in `globals.css` rather than adding per-page
card outlines, backgrounds, or corner radii. Status colors and artwork may be
applied where needed.


## Account time zones

Profile includes a searchable time zone setting shared with Android. The default is UTC. Dates and times render with the account preference, including server-rendered pages. Date/time forms convert local entries back to UTC instants; calendar-only dates retain their selected date. No extra time zone notices are shown around the app.

Shared conversions and tests live in `lib/time-zone/time-zone.mjs` and `lib/time-zone/time-zone.test.mjs`. Run `npm test` and `npm run build` to verify changes. See [account time zones](../docs/development-history/features/user-time-zones.md) for storage, daylight-saving and deployment details.
