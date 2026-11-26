# Frontend testing

Two runners, each with its own file names:

| Kind | Runner | Command | Files |
|---|---|---|---|
| Unit | `node --test` | `npm test` | `**/*.test.mjs`, next to the module they test |
| Architecture rules | `node --test` | `npm test` | `tests/architecture/import-boundaries.test.mjs` |
| Browser journeys | Playwright | `npm run test:e2e` | `e2e/<domain>/*.spec.ts` |

Run one file with `npm test -- lib/currency/currency.test.mjs` or `npx playwright test e2e/fitness`.

`npm test` runs in CI (`.github/workflows/tests.yml`) and through `make frontend-test`. E2E runs locally only. It starts the app on port 3107 and a fake backend on port 18081 (set `PULSE_E2E_API_PORT` to use another).

## Layout

```text
features/<feature>/**/x.test.mjs   Unit tests, colocated with x.ts or x.mjs
lib/**/x.test.mjs                  Unit tests for shared infrastructure
tests/
  architecture/                    Rules that span the whole codebase
  helpers/                         Helpers used by tests in more than one module (stub-global.mjs)
e2e/
  auth/ dashboard/ fitness/ meal-plan/ financial-horizon/   Journeys owned by one domain
  navigation/                      Journeys across features (the app bar, back links)
  helpers/                         environment.ts (ports, URLs), session.ts (signIn)
  fixtures/backend/                Fake backend: server.mjs plus one *-routes.mjs per domain
```

Create a test folder only when it has content. There are no component tests yet: the project has no DOM test runner, and the design system's states are documented in Storybook (`components/**/*.stories.tsx`).

## Unit tests

- Name them `x.test.mjs` beside `x`. Node runs them without a build step, so they import the module relatively (`./currency.mjs`) and shared helpers through the package import `#tests/*`.
- Tests stay `.mjs` and are not type-checked. They use hand-written stand-ins for browser globals and partial inputs on purpose, and typing those would need casts throughout.
- Stub a global with `stubProperty(context, globalThis, "document", …)` from `#tests/helpers/stub-global.mjs`. It restores the original when the test ends.

## E2E tests

- Put each journey in the folder of the domain the user is working in. A journey across features goes in `navigation/` or the folder it starts from.
- Start every test with `signIn(context, "<session>")` from `@/e2e/helpers/session`. The session token selects that spec's data in the fake backend. The first `signIn` for a token in each test resets that data, so a test passes the same way alone, in any order and on repeat. Use a token of your own for new fixture data.
- Add backend behavior to the domain's `e2e/fixtures/backend/*-routes.mjs`. Answer only your spec's token, and if the route stores data, clear it in that module's `reset(token)`.
- Prefer roles, labels and visible text over CSS selectors, and wait on assertions, never on timers.
- Specs import only `e2e/` helpers and packages, never application code. They test the app as a user does.

## Enforced rules

`tests/architecture/import-boundaries.test.mjs` fails `npm test` when:

- production code imports `tests/`, `e2e/`, `#tests/*` or any test, spec, fixture or factory file, so test code never reaches a bundle;
- E2E code imports application modules;
- `tests/` helpers import a feature or a route;
- a `*.spec.*` file is outside `e2e/`, or a unit test is not a `*.test.mjs` outside `e2e/`. Playwright only matches `*.spec.ts`, and `node --test` only finds `*.test.*`, so neither runner picks up the other's files.
