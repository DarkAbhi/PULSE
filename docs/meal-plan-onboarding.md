# Meal plan onboarding

The Android app also uses this same account-level state and API. See [Android meal onboarding](../pulse-android/docs/meal-plan-onboarding.md) for its native questionnaire, resume checks, and tests. A Save or Skip on either platform suppresses automatic setup on both.

The web `/meal-plan` page now asks six questions on an account's first visit, followed by review and an explicit **Save preferences** action. **Skip setup** is available on every question and review. After either action commits successfully, that account opens the planner directly on later visits, including other browsers/devices.

Closing a tab, refreshing, following Dashboard, or navigating away never resolves setup. Unsubmitted answers stay only in the component's memory; a fresh visit starts the questions again. Required inputs are goal, eating style, at least one selected meal, and a valid usual time per selected meal. Barriers and food avoidances are optional.

## Implementation

- `life-tracker-frontend/app/meal-plan/page.tsx` authenticates and fetches the authoritative onboarding status without caching. The client gate is keyed by username so state does not carry across account switches.
- `app/meal-plan/onboarding/meal-plan-gate.tsx` renders setup, editable settings, a retry state, or the planner. Browser history restoration rechecks pending setup. Failed reads never imply a new account.
- `meal-onboarding.tsx` implements questions, Back, review edits, Save, and Skip. Failed mutations retain answers. A failed response is reconciled against server state before offering a retry.
- `questionnaire.ts` contains display labels, stable answer codes, defaults, validation, and the central mapping between standard slots and meal kinds.
- `app/components/design-system/{choice-group,switch,time-input}.tsx` supply native input semantics and PULSE token styling. Existing Button, BackLink, and PulseCard are reused.
- Responsive artwork, keyboard focus, grouped choices, progress announcements, reduced-motion transitions, light/dark tokens, and safe-area action spacing support the journey. On narrow screens the footer follows the content so it cannot cover answer choices; on desktop it stays sticky.
- Planner **Meal preferences** opens the same questionnaire with saved answers and Cancel editing. Explicit settings saves do not reset the historical onboarding decision. Selected meals appear as suggested add-meal shortcuts; usual times appear alongside standard slots. All existing meals, snacks, custom slots, and default ranges remain available.

Food avoidances deliberately combine allergies and dislikes, matching the supplied questionnaire. They are not a medical allergy classification. No meal recommendations, notification scheduling, or automatic meal creation are implied by saving these answers.

## Go API and storage

All three endpoints use the current authenticated user, accept no owner ID, and return `Cache-Control: no-store`.

| Method | Route | Body/result |
| --- | --- | --- |
| GET | `/api/meal-plan-onboarding` | `{status, questionnaire_version, decided_at?, preferences}`; missing row yields `pending` |
| PUT | `/api/meal-plan-onboarding` | `{status: "skipped", questionnaire_version: 1}` or `{status: "completed", questionnaire_version: 1, preferences: {...}}` |
| PUT | `/api/meal-preferences` | Full replacement preferences after setup has been skipped/completed; returns updated onboarding state |

The version 1 preference object is:

```json
{
  "goal": "feel_satisfied",
  "barriers": ["too_tired"],
  "dietary_pattern": "vegetarian",
  "food_avoidances": ["peanuts"],
  "meals": [
    {"kind": "lunch", "usual_time": "12:30"},
    {"kind": "dinner", "usual_time": "19:30"}
  ]
}
```

Stable codes and user-facing labels are defined in `questionnaire.ts`; corresponding accepted Go values live in `internal/mealplan/onboarding.go` and schema constraints. Empty optional selections must be `[]`, not null. Enum values, duplicates, required fields, strict `HH:MM`, supported version, unknown JSON fields, trailing JSON, and request size are validated. Validation returns 400, invalid sessions 401, and conflicting terminal decisions/pending settings edits 409 with `{error, state}`.

Migration `00036_meal_plan_onboarding` adds:

- `meal_plan_onboarding`: per-account terminal status, questionnaire version, and decision timestamp. Missing row means pending, including existing users at rollout.
- `meal_preferences`: goal, barriers, eating style, avoidances, and timestamps.
- `meal_preference_times`: selected breakfast/lunch/dinner rows and local wall-clock times.

Account deletion cascades through all three tables. User preferences do not mutate global `meal_times` or existing `meal_plans`. Usual times follow the account timezone as local recurring habits and are not UTC instants.

Repository operations use SQLC-generated queries in a transaction, locking the account row before reading/writing. This serializes first writes when no onboarding row exists and keeps multi-query reads consistent. Completion and all answers commit together. A database failure leaves setup pending. The first terminal decision wins; same-status retries return the committed state without replacing its answers/timestamp. Opposing decisions return 409. The UI respects the returned state and directs users to later settings edits.

Swagger annotations and generated Go/JSON/YAML outputs include the new endpoints and DTOs. Generation uses the repository's pinned SQLC and Swagger versions. Adding migration 36 also required removing latest-migration assumptions from older ownership/retirement migration tests.

## Artwork

The two original food assets were generated using the built-in image-generation tool and optimized to 900×900 WebP, preserving alpha:

- `life-tracker-frontend/public/meal-plan/onboarding/salad.webp`
- `life-tracker-frontend/public/meal-plan/onboarding/pesto.webp`

Exact prompts and provenance are recorded in `meal-plan-design-assets.md`. Next Image supplies responsive variants; images are decorative, dimensioned, and use empty alternative text.

## Verification and release

Frontend commands, from `life-tracker-frontend`:

```sh
npm test
npm run build
npm run test:e2e
```

Browser E2E execution is paused in GitHub Actions while initial Maestro coverage
focuses on native Android smoke tests. The suite below is retained for later work.

The Playwright suite uses an isolated test-only HTTP contract server, the actual Next.js page/server actions, and Chrome. Its fixture does not contact development/production accounts. It covers interrupted setup, failed reads/writes, loss of a successful response, save/skip across browser contexts, later edits, account switching, concurrent-tab decisions, and narrow/dark layouts. The Go tests separately verify real PostgreSQL persistence, transactions, HTTP contracts, user isolation, cascades, and migrations.

Set `PULSE_E2E_PRODUCTION=1` after building to run those browser checks against the production build. The frontend CI job does this and uploads screenshots/traces. The existing Maestro dashboard flow skips setup when needed before asserting the planner. Development seed schema/empty-database/truncate guards now include the new tables; preference answers and onboarding decisions are excluded from exported snapshots so seed accounts start pending.

Backend commands, from `go-backend`, with Go and Docker available:

```sh
make sqlc-generate
make swagger
go test -tags=integration ./...
```

Containerized tests need the repository root's `release.json` accessible one level above the backend. On Docker Desktop, a test runner inside another container uses `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` and the Docker socket for disposable databases. This setup was used during implementation; no application database was modified by these tests.

Release order: apply migration 36 through the existing migration command, deploy the Go API, then deploy the frontend. The rollback drops only these new preference/setup tables; it leaves existing planned meals and meal-time rows intact, but removes saved preferences and dismissal decisions. Deploying the frontend before its API/schema will intentionally show a retry screen rather than silently bypassing setup.
