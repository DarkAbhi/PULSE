# Meal Plan Feature

## 1. Feature Overview

The Meal Plan is a personal daily and weekly planning tool inside Life Tracker. It lets a user organize meals by date and time of day, mark meals as consumed, and customize the meal-time ranges that appear in the planner.

The feature is presented as one weekly planner at `/meal-plan`. The page combines:

- A Monday-to-Sunday week navigator.
- A selected-day view.
- Meal cards grouped into configurable time-of-day sections.
- Quick actions to add meals, mark them consumed, and remove them.
- A settings flow for creating and deleting custom meal times.

The feature is intentionally lightweight: a meal is currently represented by a name, date, optional meal-time assignment, and consumed state. It does not currently model ingredients, recipes, calories, nutrition, servings, grocery lists, or meal notes.

## 2. Product Perspective

### 2.1 Opening the planner

A signed-in user can open Meal Plan from the dashboard. The page loads the current day and current week using the `Asia/Kolkata` application timezone.

The initial view includes:

- The current Monday-to-Sunday week.
- The current date selected.
- The current day marked with a visual indicator and a `Today` label.
- The user's existing meal-time configuration.
- Meals returned for the recent/default API query.

If the session is invalid, the server-rendered page redirects to the login entry point. If the backend cannot be reached, the page still renders its client planner shell and the client-side loading/error behavior handles later requests.

### 2.2 Weekly navigation

The user can move through the plan by:

- Selecting any day in the visible seven-day week.
- Moving to the previous week.
- Moving to the next week.
- Returning to the current week.

The week range is displayed in a compact date label. Changing the week triggers a range query and shows a loading indicator while the new meal data is fetched. Selecting a day changes the selected-day section without requiring a full page navigation.

The responsive calendar shows four day controls per row on smaller screens and seven across on wider screens. Each day control includes its weekday, calendar date, selected state, and today marker.

### 2.3 Daily meal planning

For the selected date, the user sees either:

- A friendly empty state with an `Add meal` action when no meals are planned.
- Meal-time sections when meals exist.

Each configured meal-time section shows:

- Its name.
- Its start and end time.
- An action to add a meal directly to that time slot.
- Meals assigned to the slot.
- A message when the slot has no meals yet.

The user adds a meal by entering a required name and optionally choosing a time-of-day slot. When the add flow is opened from a specific section, that section is preselected. A new meal can also be created without a slot, although the current UI normally starts with the first available meal time selected.

Meals without a valid current time assignment appear in an `Other Meals` section. This preserves the meal instead of dropping it when a custom meal time is removed.

### 2.4 Tracking consumption

Every meal has a consumed toggle. The control behaves like a checkbox:

- Unconsumed meals show their normal name styling.
- Consumed meals are visually muted and struck through.
- The user can toggle a meal by clicking its control or its name.
- The client updates the display optimistically.
- If the backend update fails, the previous state is restored.

This makes the planner useful during the day as a lightweight checklist, not only as advance planning.

### 2.5 Removing meals

The user can remove a meal from the selected day using the delete icon. A confirmation dialog names the meal and the day before the destructive request is sent.

After successful deletion, the meal is removed from local state and the planner remains on the same selected day. Backend deletion is scoped to the signed-in user, so an ID belonging to another user cannot be deleted through the endpoint.

### 2.6 Default meal times

The planner starts with four default time-of-day sections:

- Breakfast: 07:00 to 10:00.
- Lunch: 12:00 to 14:30.
- Evening Snacks: 16:30 to 18:30.
- Dinner: 19:30 to 22:00.

Default meal times are shared system configuration. They are labeled as `Default` in the management modal and cannot be deleted from the UI.

### 2.7 Custom meal times

A user can create their own time-of-day slot, such as `Pre-workout Snack`, by providing:

- A name.
- A start time.
- An end time.

The UI validates that a name and both times are present before submitting. The backend accepts `HH:MM` and `HH:MM:SS` input and rejects invalid time formats.

Custom slots are user-owned. The management modal lists them alongside defaults and allows a custom slot to be deleted. When a custom slot is deleted, meals assigned to it remain in the plan and are shown under `Other Meals` because the database relationship is cleared rather than cascading the meal deletion.

### 2.8 Meal-time management

The `Meal Times` control opens a management dialog where the user can:

- Review all default and custom time ranges.
- Add a custom meal time.
- Delete a custom meal time after confirmation.
- Close the dialog without leaving the planner.

The planner updates its local meal-time list after successful create/delete operations and uses server revalidation so subsequent page loads receive fresh configuration.

## 3. End-to-End Technical Implementation

### 3.1 Frontend architecture

The frontend is implemented under `life-tracker-frontend/app/meal-plan` using the Next.js App Router.

- `page.tsx` is a server-rendered entry page. It forwards the incoming cookie header to session validation, loads initial meals and meal times in parallel, and computes today's date in `Asia/Kolkata`.
- `meal-plan-client.tsx` is the interactive planner. It owns week navigation, selected-day state, local meal and meal-time collections, modal state, optimistic consumption updates, confirmation dialogs, and loading transitions.
- `actions.ts` defines the shared TypeScript data contracts and server functions used by the planner.
- Shared design-system buttons, dialogs, dialog actions, and confirmation dialogs provide the interaction and accessibility primitives.
- Lucide icons communicate navigation, calendar, meal, clock, settings, completion, loading, and destructive actions.

The server page uses `cache: "no-store"` for its initial meal and meal-time requests. The client also requests each newly selected week with no-store semantics through `getMealPlansAction`, keeping calendar navigation aligned with current backend data.

### 3.2 Date and timezone handling

The UI uses date-only values in `YYYY-MM-DD` form for meal-plan records. The server page determines today with `Intl.DateTimeFormat` in `Asia/Kolkata`, while the client derives Monday, adds or subtracts days, and formats the selected week locally.

The backend validates date strings with Go's `2006-01-02` layout and stores them as PostgreSQL `DATE` values. This avoids treating a meal-plan date as an instant in time and prevents ordinary week navigation from drifting because of browser timezone offsets.

### 3.3 Mutations and cache refresh

The frontend uses Next.js Server Functions in `actions.ts` to forward authenticated requests to the Go API:

- `getMealTimesAction` loads available default and user-owned time slots.
- `createMealTimeAction` creates a custom slot and revalidates `/meal-plan`.
- `deleteMealTimeAction` deletes a custom slot and revalidates `/meal-plan`.
- `getMealPlansAction` retrieves plans by date, date range, or the backend's recent-plan fallback.
- `addMealPlanAction` creates a meal and revalidates `/meal-plan`.
- `deleteMealPlanAction` removes a meal and revalidates `/meal-plan`.
- `updateMealPlanConsumedAction` updates only the consumed state and revalidates `/meal-plan`.
- `updateMealPlanAction` forwards broader meal updates and revalidates `/meal-plan`.

The client also updates its local collections immediately after successful create/delete operations. Consumption changes are optimistic and explicitly revert if the server rejects the request. Server revalidation ensures a later server render does not rely on stale data.

### 3.4 Backend feature slice

The backend implementation is grouped under `go-backend/internal/mealplan`:

- `handler.go` authenticates requests, parses query parameters and JSON bodies, maps service errors to HTTP responses, and registers routes.
- `service.go` contains validation, user-scoped business rules, date-range selection, default initialization, meal-time ownership checks, and DTO construction.
- `mealplan.go` defines defaults, API input/output types, flexible time parsing, and DTO conversion.
- `repository.go` adapts SQLC query results into the service's repository-facing row types.
- `query/mealplan.sql` contains the hand-authored SQL used to generate the pgx query layer.
- `mealplan_test.go` covers SQLC behavior, unauthorized requests, time parsing, and route flows.

The feature is wired explicitly in `cmd/api/main.go`: the repository is built from the PostgreSQL pool, the service is created, defaults are ensured, and the handler is registered in the API router.

### 3.5 API surface

The meal-plan feature exposes both meal-plan and meal-time endpoints:

| Method   | Route                       | Purpose                                                       |
| -------- | --------------------------- | ------------------------------------------------------------- |
| `GET`    | `/meal-times`               | List default and current-user custom meal times               |
| `POST`   | `/meal-times`               | Create a custom meal time                                     |
| `DELETE` | `/meal-times/{id}`          | Delete a current-user custom meal time                        |
| `GET`    | `/meal-plans`               | List recent meals, meals for a date, or meals in a date range |
| `POST`   | `/meal-plans`               | Create a meal plan entry                                      |
| `PATCH`  | `/meal-plans/{id}/consumed` | Update consumed state                                         |
| `PATCH`  | `/meal-plans/{id}`          | Update meal fields                                            |
| `PUT`    | `/meal-plans/{id}`          | Update meal fields                                            |
| `DELETE` | `/meal-plans/{id}`          | Delete a meal plan entry                                      |

`GET /meal-plans` supports these query modes:

- `?date=YYYY-MM-DD` for one day.
- `?start_date=YYYY-MM-DD&end_date=YYYY-MM-DD` for an inclusive range.
- No date parameters for up to 100 recent meals, ordered by date descending.

The frontend uses the range mode for week navigation and the default mode for the server-rendered initial load.

### 3.6 Validation and ownership rules

The backend remains the final validation boundary:

- Meal-plan dates must use `YYYY-MM-DD`.
- Meal names are trimmed and cannot be empty.
- Meal-time names are trimmed and cannot be empty.
- Meal-time start and end values must be valid `HH:MM` or `HH:MM:SS` strings.
- A referenced meal time must be visible to the current user: either a system default or that user's custom slot.
- A custom meal-time name must be unique for that user, case-insensitively.
- Default meal times cannot be deleted through the delete query.
- Meal-plan reads, updates, consumed changes, and deletes include the authenticated `user_id` in their SQL predicates.
- Custom meal-time reads and deletes are scoped to the authenticated user, while default rows are globally readable.
- Missing records are translated to a meal-specific 404 response; validation failures become 400 responses; invalid sessions become 401 responses.

### 3.7 Persistence model

PostgreSQL stores the feature in two related tables:

- `meal_times` stores default or user-owned time-of-day ranges, including name, start time, end time, and `is_default`.
- `meal_plans` stores the user, calendar date, meal name, optional meal-time relationship, optional start/end snapshot, consumed state, and timestamps.

The meal-time foreign key uses `ON DELETE SET NULL`. This is a deliberate preservation rule: deleting a custom time does not delete the user's meals. The plan remains visible and can be represented as an unassigned meal.

The schema includes:

- A case-insensitive uniqueness index over global/default versus user-owned meal-time names.
- A user/date index for daily and weekly planner queries.
- A meal-time index for grouped lookups.
- A foreign key from meal plans to users with cascade deletion when a user is removed.
- A foreign key from meal plans to meal times with nullification on time-slot deletion.
- `is_consumed` with a non-null default of `false`.

The data access layer uses `pgx/v5` and SQLC-generated query code. The repository translates nullable PostgreSQL values into service-level rows and DTOs without introducing an ORM.

### 3.8 Meal-time snapshots and defaults

When a meal is created or updated with a meal-time ID, the service verifies that the time belongs to the user or is a default, then copies that time range into the meal's `start_time` and `end_time` columns. Query responses prefer the meal's stored range and fall back to the current meal-time range when necessary.

This gives the feature two useful properties:

- Meals retain a usable time range even if the configured slot changes later.
- Deleting a custom slot clears the relationship but does not erase the meal's scheduling information or history.

Default meal times are inserted by the migration and are also ensured at API startup through `EnsureDefaults`. The insertion is idempotent, so restarting the backend does not create duplicates.

## 4. Critical Implementation Notes

- **User isolation:** all meal-plan operations receive the authenticated user ID and use it in SQL filters. A user cannot read or mutate another user's meal plans by changing an ID in the URL.
- **Shared versus private configuration:** default meal times have a null user ID and are available to everyone; custom meal times have an owning user ID and are only visible to that user.
- **Data preservation:** deleting a custom meal time sets `meal_time_id` to null rather than deleting meals. The frontend's `Other Meals` section makes this orphaned assignment visible and recoverable as data.
- **Optimistic interaction:** consumed toggles feel immediate, but the client stores enough state to revert the UI when the backend request fails.
- **Range efficiency:** weekly navigation requests one inclusive date range instead of issuing seven daily requests. SQL ordering groups meals by date and time range before the client filters the selected day.
- **Consistent time handling:** date-only storage and explicit application timezone handling reduce timezone-related day-boundary errors. Time-of-day ranges are stored as PostgreSQL `TIME` values rather than browser timestamps.
- **Current UI/API parity:** the backend and `actions.ts` support updating a meal's name, date, meal time, and consumed state, but the current planner UI exposes consumed updates, creation, and deletion rather than a general edit dialog.
- **Failure behavior:** server actions return structured `{ ok, error }` results, while the planner keeps the page usable during loading and reports mutation failures inside the relevant modal or interaction flow.

## 5. Source Map

- [Meal Plan server page](../life-tracker-frontend/app/meal-plan/page.tsx)
- [Meal Plan client planner](../life-tracker-frontend/app/meal-plan/meal-plan-client.tsx)
- [Meal Plan server actions and types](../life-tracker-frontend/app/meal-plan/actions.ts)
- [Meal Plan handler](../go-backend/internal/mealplan/handler.go)
- [Meal Plan service](../go-backend/internal/mealplan/service.go)
- [Meal Plan models and DTOs](../go-backend/internal/mealplan/mealplan.go)
- [Meal Plan repository](../go-backend/internal/mealplan/repository.go)
- [Meal Plan SQL queries](../go-backend/internal/mealplan/query/mealplan.sql)
- [Meal Plan tests](../go-backend/internal/mealplan/mealplan_test.go)
- [Meal Plan schema](../go-backend/migrations/00012_meal_plans.up.sql)
- [Consumed-state migration](../go-backend/migrations/00016_add_is_consumed_to_meal_plans.up.sql)
- [Backend startup wiring](../go-backend/cmd/api/main.go)
- [API route registration](../go-backend/cmd/api/router.go)
