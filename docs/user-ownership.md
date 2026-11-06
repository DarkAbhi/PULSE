# User ownership rollout

The web keeps `life_session` in an HTTP-only cookie. Next.js forwards only that token as `Authorization: Bearer …` to Go, with private fetches uncached. The browser API proxy also streams attachments and preserves login/logout cookies. A supplied body/query `user_id` does not select an owner. Go derives it from a valid, unexpired session.

Rust fitness imports and `GET /api/fitness-summary` use active profile-generated API keys as bearer credentials. The summary now requires authentication; anonymous callers receive 401. Session tokens and API keys remain different credential types.

## Database changes

- Migration 29 assigns ownership to meditations, credit cards, and sports. It infers an owner only when exactly one user exists. Empty tables can migrate with any user count. Explicitly pre-mapped owner columns are preserved.
- Migration 30 rejects cross-owner private relationships between vehicles, expenses, attachments, notifications, workouts, and Financial Horizon records. Original cascade and optional-reference deletion behavior is retained.
- Migration 31 drops the unused `meditations`, `credit_cards`, and `sports` tables and their data. Apply earlier pending migrations in order, including 29 and 30; do not drop the tables manually before migration 29. Its down migration restores empty schema only, so retain a backup if those records are needed.
- Shared meal times and financial categories must have `user_id IS NULL` and `is_default=true`; private entries must have an owner and `is_default=false`. API validation and scoped joins enforce access to these mixed shared/private references.
- Exercise sets, visit exercises, and fuel items inherit ownership through their parent. Exercise/vehicle/workout-type catalogs and catalog import metadata remain shared.

No live database has been migrated by this change. Production owner mappings and data volumes must be checked before rollout. Existing migrations 19 and 20 also require a single-user backfill; installations older than version 28 need their own reviewed mapping before applying those historical migrations. Do not edit previously applied migration files or blindly force their version.

## Before deployment

1. Record the deployed version and dirty state. For schema 28 or later, run `scripts/audit-user-ownership.sql` against the target database. It is read-only. Review all nonzero mismatch counts; the migrations intentionally reject invalid private relationships instead of silently reassigning or deleting data.
2. Back up the database and verify restoring the backup into a disposable database. Capture record counts and financial totals by user before and after a rehearsal.
3. If multiple accounts exist and any of the three legacy tables contains unowned rows, explicitly map each record from known ownership evidence. In a reviewed transaction, add nullable BIGINT `user_id` columns to those legacy tables and populate them from the mapping. Migration 29 accepts existing columns and preserves assigned values. Do not choose the first/minimum user or duplicate data into every account.
4. Resolve existing cross-owner links and invalid shared defaults through reviewed data corrections. Preserve an audit of the original values; the migrations do not invent a repair policy.
5. Rehearse all pending migrations and seed restoration against the restored database. The supplied development seed is anonymized, requires schema 31, and is never a production migration. The ownership audit skips the three retired tables once migration 31 has removed them.

## Deployment order

Use a short maintenance window with writes paused for this installation. Index creation and constraint validation take locks; measure the rehearsal duration against actual data before selecting the window. Large tables require a separately reviewed online migration rather than assuming these transactional migrations will be fast.

1. Stop writers, including Go, Rust imports, jobs, and external integrations.
2. Apply migrations using the existing API migration command (`go run ./cmd/api --migrate-up` from `go-backend`, with the target database configured securely).
3. Deploy the Go API and Rust API, then the web app. Do not re-enable writers still using the pre-ownership schema.
4. Run the read-only audit again. Verify two separate users can create/list/update/delete their own records, cannot access each other's IDs, and cannot link to each other's budgets/categories/subscriptions/meal times. Check attachments, pagination totals, bulk imports, and fitness summaries.
5. Compare counts and financial totals with the rehearsal baseline, then resume writes and watch errors.

If a transactional migration fails, inspect and repair the underlying cause first. `golang-migrate` can leave its bookkeeping dirty even when PostgreSQL rolled the transaction back. Verify the schema really matches the last successful version before an operator repairs that bookkeeping and retries. Never clear a dirty flag solely to bypass an ownership error.

## Rollback

Prefer a forward fix or temporary feature shutdown after accounts have private data. Do not deploy an older backend that reads across users. The migration down files exist for disposable test/rehearsal databases; migration 29 down removes ownership information and is unsuitable for production rollback after multi-user writes. A database restore is only appropriate with an explicit plan for writes since the backup.

## Validation

Existing CI already covers Go, Rust, frontend tests, and browser flows. Extend those checks with the new two-user tests; no new test framework is required.

- Go: `go test -race -count=1 -shuffle=on -tags=integration -timeout=20m ./...`
- Rust: `cargo test --locked --workspace --all-features` and `cargo clippy --locked --all-targets --all-features -- -D warnings`
- Web: `npm test` and `npm run build`
- Seed: run `scripts/test_seed_database.py` with `TEST_SEED_DATABASE_URL` pointing only to a disposable PostgreSQL server.

Mobile authentication changes, account registration, and account sharing are outside this rollout. Existing cookie/session credentials remain supported by Go for client compatibility.
