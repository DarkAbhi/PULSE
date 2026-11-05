# Meal onboarding verification

Verified on 2026-11-05 against the implemented web frontend and Go API.

- Frontend unit tests: passing, including questionnaire validation, fresh drafts, explicit terminal states, selected-meal times, immutable editing, and standard-slot mapping.
- Production build: passing (`npm run build`).
- Production browser suite: all six Playwright journeys passing (`PULSE_E2E_PRODUCTION=1 npm run test:e2e`). Uses the actual Next.js page and server actions with an isolated HTTP contract fixture; it does not use production user data.
- Full Go regression suite, including PostgreSQL and migration tests: passing (`go test -tags=integration -p=4 ./... -count=1 -timeout=10m` inside the development container). Database transactions, first-write races, rollback, identity scoping, and persisted decisions are exercised against real disposable PostgreSQL databases.
- Seed exporter unit tests and disposable-database restore tests: passing. New setup/answer tables are included in restore safety guards and excluded from copied personal data.
- SQLC and Swagger outputs regenerated with the repository-pinned tools.

The database tests required Docker Desktop's `host.docker.internal` override from the test container. The full suite also required the repository's release manifest mounted above the backend. Older migration tests were updated to target explicit historical versions.

Screenshots are captured from the production build, without the development overlay:

- [Desktop, light](meal-onboarding-desktop.png)
- [Tablet](meal-onboarding-tablet.png)
- [Phone, 320px](meal-onboarding-phone.png)
- [Phone, long avoidance list](meal-onboarding-avoidances-phone.png)
- [Desktop, dark](meal-onboarding-desktop-dark.png)

The narrow-screen footer follows content to avoid covering answers; desktop retains a sticky action area. No application database was migrated or modified during verification. Apply migration 36 before deploying the new API and frontend.
