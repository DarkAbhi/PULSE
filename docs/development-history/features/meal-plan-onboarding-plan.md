# Meal plan onboarding implementation plan

Status: implemented. See [the implementation guide](meal-plan-onboarding.md) for final behavior, source locations, verification, and release order. The proposal below is preserved as the design rationale.

## Scope and investigation findings

Build a skippable, account-scoped onboarding journey on the web `/meal-plan` page. Persist the answers and the explicit dismissal decision in PostgreSQL through the Go API. Opening a page, advancing a question, navigating away, refreshing, or closing a tab must never mark onboarding finished.

The six supplied screenshots are content and visual references. They show six questions, with inconsistent total counts (10 and 12); this proposal uses those six questions followed by a review screen. It does not invent the unseen questions. Progress reads “Question 1 of 6” through “Question 6 of 6”; review reads “Review your preferences.”

Verified implementation points:

| Area | Current source and implication |
| --- | --- |
| Web entry | `life-tracker-frontend/app/meal-plan/page.tsx` authenticates the session, loads meals and meal times, and uses the user's timezone. Add the onboarding read here. |
| Web interaction | `app/meal-plan/meal-plan-client.tsx` owns planner state and existing dialogs. Add a gate before mounting the planner's interactive content. |
| Authenticated transport | `app/meal-plan/actions.ts` uses `app/lib/server-api.ts` and structured action results. Extend this pattern for reads, save, skip, and later preference editing. |
| Design system | `app/components/design-system` supplies Button, BackLink, PulseCard, Select, Dialog, and dialog actions. There are no shared radio, checkbox, switch, or time-input components in that directory. |
| Appearance | `app/globals.css` defines light/dark semantic tokens; layout uses Geist and the existing theme provider. Retain these. |
| Backend | `go-backend/internal/mealplan` already separates handlers, services, repositories, DTOs, and SQLC queries. Extend this feature slice. |
| Database | `meal_times` contains shared defaults and private ranges; `meal_plans` contains scheduled meals and consumption state. Neither stores the questionnaire or onboarding dismissal. No matching preference/onboarding model was found elsewhere in the Go/web code. |
| Shared defaults | Breakfast, Lunch, Evening Snacks, and Dinner are system rows with ranges. A user's usual meal time is a separate concept and must not change these shared rows. |
| Generation | `go-backend/sqlc.yaml` includes the mealplan queries; `Makefile` pins SQLC and Swagger generation commands. Swagger outputs already document existing meal endpoints. |
| Tests | Frontend uses Node's built-in test runner. Go has service tests and PostgreSQL container tests; migration tests also run with the integration tag. No frontend browser-test framework is declared in `package.json`. |
| Assets | Web public assets currently have no meal artwork. Existing generated Android food illustrations and their provenance are documented in `docs/development-history/design/meal-plan-design-assets.md`. |

The working tree was clean at investigation time. Current highest migration is 00035; reserve 00036 only after checking again at implementation time. Some existing prose in `docs/development-history/features/meal-plan-feature.md` predates the current user-timezone implementation; code takes precedence.

## Journey and question content

Keep Back and an explicit “Skip setup” action available throughout, including review. Use “Continue” rather than automatically advancing when an answer is selected. Save is a deliberate final action. Back preserves all in-memory answers.

| Step | Content | Input and rules |
| --- | --- | --- |
| 1 | “What's your goal?” / “What would feel most helpful right now?” | Required single choice: Feel full and satisfied; Find more calm around food; Nourish myself without dieting. |
| 2 | “What gets in the way?” | Optional multiple choices: I'm hungry again soon after eating; I'm too tired to figure out meals; I don't know what to add; Food advice feels overwhelming. Empty selection is valid. |
| 3 | “How do you like to eat?” | Required single choice: No preference; Vegetarian; Vegan; Pescatarian. No preference is an explicit saved answer, not a missing value. |
| 4 | “Anything to avoid?” | Optional multiple choices: Shellfish, Fish, Peanuts, Tree nuts, Dairy, Eggs, Gluten, Soy, Sesame, Pork, Beef, Poultry, Mushrooms, Onion & garlic, Spicy food, Alcohol. Empty selection is valid. |
| 5 | “Where would a little help go furthest?” | Breakfast, Lunch, Dinner toggles. Start with all three selected, matching the reference; require at least one for completion. Snack slots remain available in the existing planner. |
| 6 | “When do you usually eat these meals?” | One labeled editable time for each selected meal, initially 09:00, 12:30, 19:30. Only the first two are visible in the references; dinner is a proposed editable default. Changing step 5 updates which times are required. |
| Review | All answers with Edit links | “Save preferences” commits the whole validated answer set. Success returns to the planner. |

For step 4, keep the source's combined meaning by storing `food_avoidances`; do not label every avoidance a diagnosed allergy or promise allergy-safe recommendations. Distinguishing allergies from dislikes would require additional input beyond these screenshots. Use recognizable food artwork/icons (the screenshot's bread icon is not an accurate sesame illustration).

Use stable machine codes in storage and API payloads, separate from display copy. For example, `feel_satisfied`, `food_advice_overwhelming`, `no_preference`, `tree_nuts`, and `onion_garlic`. Document the complete enum mapping alongside the DTOs.

## First-visit and persistence behavior

The API returns one of `pending`, `skipped`, or `completed`. No database onboarding row means `pending`, including existing accounts after rollout. No automatic version-based re-prompt is introduced.

| Event | Persistent effect | Next visit |
| --- | --- | --- |
| Open, answer, Continue, Back | None; answers live in component state | Show onboarding again |
| Refresh, close browser/tab, navigate away | None | Show onboarding again, starting at question 1 |
| Click Skip setup; server commits | Store `skipped` and decision timestamp | Open planner directly, across browsers/devices |
| Click Save preferences; server commits | Atomically store all answers and `completed` | Open planner directly, across browsers/devices |
| Skip/save request fails | No acknowledged terminal state; retain the form and show Retry | Still eligible unless a commit actually succeeded |
| Commit succeeds but response is lost | Re-read status or retry idempotently | Respect the committed backend decision |

Do not use a localStorage/cookie “seen” flag. Do not write completion during mount/unmount, visibility changes, or unload. In-memory drafts are sufficient for the stated requirement; draft recovery is outside this proposal.

Gate rendering on a successful onboarding-status read, so the planner and journey do not flash in sequence. Distinguish a failed read from `pending`; show a retry state instead of assuming the user is new. Expired sessions follow the existing login flow. On mutations, disable duplicate submissions and only exit after success or a confirming status read.

First committed terminal decision wins for simultaneous tabs: repeat identical terminal requests return the current state; conflicting terminal actions return 409 with authoritative state. The client refreshes the gate and explains if setup was already resolved elsewhere. The write and status check must be atomic, not a read-then-write race.

Expose a “Meal preferences” entry in planner settings for skipped users to fill them in later and completed users to edit. Manual edits never trigger automatic onboarding or reset its terminal status.

## Visual and interaction direction

Use a calm, food-led composition within PULSE's current visual language. One large, original overhead food plate is the distinctive element; questions remain quiet and easy to scan. Desktop uses an artwork panel beside a left-aligned question column. Mobile uses a shallow cropped artwork header, the question, and a sticky action footer with safe-area spacing. Long avoidance lists scroll without the footer covering options.

Reuse existing semantic colors: background #f5f2ea, card #fffdf8, text #1c1c1a, primary #edb937, muted text #605d56, sage #7e9b65; dark mode comes from the existing token equivalents. Keep Geist, existing type scales, and existing button shapes. Avoid copying the screenshots' oversized mobile spacing onto desktop.

Reuse Button, BackLink, and PulseCard. Add small shared native-based RadioGroup, CheckboxGroup, Switch, and TimeInput primitives with token styling because equivalents are absent. Prefer a labeled time input with direct typing and browser picker support over a drag-only ruler. Show time in the user's preferred/local presentation and serialize it as `HH:MM`.

Use native fieldsets/legends, visible focus, arrow-key radio behavior, accessible switch names, at least 44px interaction targets, and selection indicators beyond color. Move focus to the next question heading and announce progress. Associate errors with inputs; preserve choices on failures; respect reduced motion. Prefer an inline page journey over a modal, avoiding focus-trap and accidental backdrop dismissal issues. Leaving the page is allowed and does not count as Skip.

During implementation, use the image-generation skill to create two or three original transparent food-plate assets (salad, pesto-style dish, tacos or a similarly balanced dish), with no text or baked-in UI. Match lighting and plate scale across images. Optimize generated sources into responsive WebP/AVIF derivatives under `public/meal-plan/onboarding/`; record prompts and provenance in the asset document. Keep art decorative with empty alt text, explicit dimensions, and modest mobile downloads. Generate and inspect assets as part of implementation, once the layout is established.

## Database and API proposal

Add an additive up/down migration, provisionally `00036_meal_plan_onboarding`. Do not edit applied migrations or infer completion from existing meal rows.

| Table | Proposed fields and constraints |
| --- | --- |
| `meal_plan_onboarding` | `user_id` primary key/FK to users with cascade delete; terminal `status` constrained to skipped/completed; `questionnaire_version` initially 1; `decided_at` timestamptz. Pending is represented by no row. |
| `meal_preferences` | `user_id` primary key/FK; goal code; dietary pattern code; non-null text arrays for barriers and food avoidances, defaulting to empty arrays; created/updated timestamps. Validate allowed scalar and array codes in service and database constraints. |
| `meal_preference_times` | `user_id` FK to preferences with cascade delete; meal kind constrained to breakfast/lunch/dinner; `usual_time` as time without time zone; composite primary key `(user_id, meal_kind)`. Presence means selected. |

Store chosen times as recurring local wall-clock times, interpreted using the existing account timezone. They are not UTC instants and do not schedule notifications. No duplicate timezone field is needed. Preserve times across timezone changes as local eating habits; label the timezone in the UI.

Save preferences, selected meal rows, and terminal completion in one transaction. Skip creates only a terminal decision, not empty preference records. Add transaction support to the repository using the generated queries' transaction binding; test rollback and concurrent first-write behavior. The service enforces one or more meal choices on a full preference save; database keys and checks protect the stored structure.

| Authenticated endpoint | Contract |
| --- | --- |
| `GET /api/meal-plan-onboarding` | 200: status, questionnaire version, terminal timestamp if any, and nullable saved preferences. Missing row returns pending; use no-store. |
| `PUT /api/meal-plan-onboarding` | Body discriminated by status: skipped needs no answers; completed requires the full preference object. Atomically commit terminal decision and any answers. Return 200 with authoritative state; 400 invalid payload; 401 expired/missing auth; 409 conflicting terminal decision. |
| `PUT /api/meal-preferences` | Explicit settings save after a terminal onboarding decision. Validate and replace all preferences atomically without resetting onboarding. Return the saved preferences. Reject pending users here so completion cannot be bypassed accidentally. |

Completion/preferences payload contains `goal`, `barriers`, `dietary_pattern`, `food_avoidances`, and `meals: [{kind, usual_time}]`, plus questionnaire version on onboarding writes. Return empty arrays rather than null. Reject invalid/unknown enums, unknown fields, malformed/trailing JSON, unsupported versions, missing required fields, duplicate selections, duplicate meal kinds, and invalid times. Do not silently discard bad data. Bound payload size. Do not accept a client-supplied owner or decision timestamp.

All reads/writes derive user identity from the existing authenticated session. Scope every query by that ID. Existing meal CRUD and Android API contracts remain compatible. Update handler annotations and regenerate Swagger JSON/YAML/Go outputs; add documented DTO examples and failure responses.

## How saved preferences affect the planner

These answers become editable account preferences, not automatically generated meals. There is no existing recipe/recommendation engine in this feature, so saving must not claim that meals have been generated or filtered for allergens.

Apply selected meal kinds to suggested empty-state/add-meal shortcuts, while keeping every existing planned meal and custom slot visible. Show a saved usual time alongside the corresponding standard meal category where helpful. Keep existing start/end ranges and meal snapshots intact; do not invent a duration around the user's chosen point time or hide meals because a category was not selected. Resolve standard category mappings centrally rather than scattering display-name comparisons; introduce stable category identifiers if planner integration needs them. Existing snacks and user-created slots remain usable.

## Implementation sequence and affected files

1. **Contracts and schema:** finalize enum mapping and DTOs; write the new migration and SQLC queries; regenerate query outputs. Add transactional repository operations and tests before attaching UI.
2. **Go service and HTTP:** extend `internal/mealplan/{mealplan,service,repository,handler}.go`, route registration through the existing handler, and add preference/onboarding test files. Keep existing startup wiring unless a separate service is justified. Regenerate `go-backend/docs` with `make swagger`.
3. **Web journey:** extend `app/meal-plan/actions.ts` and `page.tsx`; add a dedicated onboarding gate, questionnaire definition, pure state/validation helpers, question components, review, and styles under `app/meal-plan/onboarding/`. Keep the already-large planner component focused on planning.
4. **Design-system and assets:** implement the missing form primitives, generate the food artwork, optimize it, and verify layout in both themes at phone/tablet/desktop widths. Reuse primitives in journey/settings.
5. **Settings and planner integration:** add editable preferences, selected-meal shortcuts, and usual-time display; retain existing CRUD behavior and data.
6. **Verification and documentation:** run backend/frontend checks, browser journey verification, migration upgrade/rollback tests, and update the feature/API/asset documentation. Deploy the additive schema, compatible Go API, then the web frontend.

## Verification and acceptance criteria

Backend unit and PostgreSQL tests:

- Exact round-trip for every question/enum, empty optional arrays, and all selected meal/time combinations.
- Required-answer validation and malformed payload tests, including invalid time, duplicate meal, unsupported version, and zero selected meals.
- Fresh user reads pending without creating records; existing user with meals also reads pending.
- Skip persists without preferences; completion persists all answers; transaction failures cannot leave partial answers or false completion.
- Repeated requests, response-loss retries, two-tab conflicting writes, and concurrent first completion preserve one coherent result.
- Cross-user reads/writes and unauthenticated requests cannot access another user's answers; account deletion removes related rows.
- Upgrade against existing populated data, down/up against a disposable database, constraints, and regression coverage for shared meal defaults and existing meal CRUD.
- Inspect migration tests that assume the latest migration is a particular feature (for example `TestRetiredPersonalTablesUpgradeAndRollback` rolls back one migration); anchor them to explicit versions so adding a migration does not invalidate their assumptions.

Frontend automated checks:

- Extend `node --test` coverage for the pure step reducer, validation, conditional meal times, Back/review edits, and authoritative-state handling.
- Run `npm test` and `npm run build` in `life-tracker-frontend`.
- Add a small Playwright journey suite as test-only tooling: pending entry, successful completion, successful skip, refresh/tab-close before either decision, failed mutations, later settings edits, and account switching. Verify committed status in a fresh browser context with the same account.
- Keyboard and screen-reader semantics, focus movement, light/dark contrast, reduced motion, 320px/mobile/tablet/desktop layout, and unobscured long lists/footer. Inspect screenshots from the actual rendered UI.

Run Go generation/checks using the repository's pinned commands: `make sqlc-generate`, `make swagger`, `go test ./...`, and `go test -tags=integration ./...`. The database tests require Docker. Only report checks as passing after actually running them; investigation alone is not a test run.

Done means the saved answers can be read back and edited; Skip and Save suppress future automatic onboarding for that account; every other way of leaving leaves onboarding pending; the planner remains compatible with existing meals and time slots.
