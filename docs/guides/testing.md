# Testing

Install Go 1.25+, the Rust toolchain pinned in `rust-backend/rust-toolchain.toml`,
Node.js 24/npm, Python 3.12+, and Make, and start
Docker. Install frontend dependencies once with
`cd life-tracker-frontend && npm ci`, then run from the repository root:

```sh
make test
```

This runs the Go (including integration-tagged tests), Rust, frontend, Hermes
MCP, and seed/test tooling suites in parallel. Every suite finishes even if another
fails, and the command exits unsuccessfully if any suite fails. Tests run on
the host; Go and Rust start their own disposable database containers through
Docker. The development/production Compose stacks do not need to be running.

### HTML test report

`make test` (or `make test-report`) writes **`test-results/index.html`**.
Open that file in a browser after the command finishes. The report shows:

- Overall and per-module passed, failed, errored, and skipped counts.
- Individual test names and durations, searchable by name and filterable by status.
- Expandable failure messages, assertions, tracebacks/backtraces, and captured output.
- Full runner logs for each module and `test-results/results.json` for structured results.

The report is generated even if a suite fails or a runner cannot start, and the
command returns a nonzero exit code on failure. Expected error logs do not mark
a passing test as failed. Go tests use the CI race detector, shuffled order,
and 20-minute timeout. They are uncached and include parent tests and
subtests, both of which count as results. Python subtest failures appear under
their parent test. Rust uses stable Rust and runs cases serially within its
module to measure elapsed time from live test output; modules still run in
parallel. Its failure details include the runner's panic output with backtraces
enabled. Timings include test setup and cleanup; module times also include builds
and discovery. No test-reporting packages need to be installed.

The scripts suite includes seed exporter and report checks, plus seed restore
tests. Seed restore tests are reported as skipped unless `TEST_SEED_DATABASE_URL`
is set to a disposable PostgreSQL server and `psql` is installed. Browser flows
and production builds are separate. Report files are ignored by Git and
replaced by the next run. Individual module commands below retain their normal
console output; use `make test` for the combined HTML report.


Run a module independently from the repository root:

| Command | Suite |
| --- | --- |
| `make go-test` | Go backend, including integration-tagged tests. |
| `make rust-test` | Rust backend, including database tests. |
| `make frontend-test` | Frontend Node tests. |
| `make mcp-test` | Hermes MCP tests. |
| `make seed-test` | Seed exporter, seed restore, and report tests; restore tests skip without their test database URL. |

Equivalent direct module commands:

| Module | Command | Requirements |
| --- | --- | --- |
| Go backend | `make -C go-backend test-integration` | Go 1.25+ and a running Docker daemon. |
| Rust backend | `cd rust-backend && cargo test --locked --workspace --all-features` | Rust (the crate's stable toolchain) and a running Docker daemon. |
| Next.js frontend | `cd life-tracker-frontend && npm ci && npm test` | Node.js 24 and npm; no database or running backend. |
| Hermes MCP (`hermes-mcp/`) | `python3 -m unittest discover -v -s hermes-mcp -p 'test_*.py'` | Python 3.12; tests mock MCP and backend requests, so no credentials or running services are needed. |
| Seed exporter | `python3 -m unittest discover -v -s scripts -p 'test_export_dev_seed.py'` | Python 3.10+; no database. |

Go tests start temporary PostgreSQL 17.6 containers, apply migrations, and insert
fixtures as needed. Rust database tests also start temporary PostgreSQL 17.6
containers automatically for every database test. Each Rust database
test uses one connection with temporary tables and its own small fixtures;
containers are removed when the test finishes, including assertion failures.
Docker may pull the image on the first run. Database setup failures fail the
suite instead of silently skipping tests. No development database or shared
demo seed is needed for either backend suite.

For a single Go package, run `cd go-backend && go test ./internal/gym`.
For a single Rust test, run `cd rust-backend && cargo test summary_selects_latest_strength_run_and_ride`.
For a frontend test file, run `cd life-tracker-frontend && npm test -- app/profile/copy-api-key.test.mjs`.
Also run `cd life-tracker-frontend && npm run build` for TypeScript checking and
the production build.

`make test` covers the module suites above; browser flows and production builds
run separately because they need additional setup.
Maestro now runs two native Android smoke flows: the clean sign-in screen and
required-field validation. They use an installed debug APK on an API 35 emulator,
with no backend, account or database. Run `maestro test maestro/android/` using
Java 21; see [Maestro smoke tests](../../maestro/README.md) for build and report commands.
CI runs these flows after Android instrumentation on a fresh emulator and
uploads JUnit/debug evidence in `pulse-android-device-reports`.

Browser E2E coverage is paused in CI. Existing Maestro web flows are retained in
`maestro/web/`, and Playwright journeys remain available for later work. Frontend
Node unit tests, type checks and the production build continue to run. Select
`maestro/android/` explicitly so the paused browser flows are not discovered.

Seed restore checks separately require `psql`
and `TEST_SEED_DATABASE_URL`; see [Development seed data](development-seed.md).

## Continuous integration

[Tests](../../.github/workflows/tests.yml) runs on every push and pull request, and
can also be started manually from GitHub Actions. Its independent jobs run:

- Every Go package, including `integration`-tagged tests, with race detection
  and uncached, shuffled test runs. The existing test helpers start temporary
  PostgreSQL 17.6 containers and apply all migrations using Docker.
- All Rust workspace tests with all features enabled. Database tests start
  disposable PostgreSQL 17.6 containers and create their own temporary tables.
- All frontend Node tests through `npm test`, plus TypeScript checks and the
  Next.js production build.
- All Hermes MCP Python tests and seed exporter tests.
- Android unit tests, lint and formatting, the full instrumentation suite, and
  two small native Maestro smoke tests on a fresh API 35 emulator.
- Seed restoration, repeat runs, protection of existing data, date rebasing and
  rollback on error against disposable PostgreSQL databases, in a dedicated job.

The databases are disposable and removed with their jobs. No production
credentials or repository secrets are required. Jobs run independently so a
failure in one module does not cancel the other suites. Monitoring and Compose
configuration have no dedicated test suites; these checks cover the existing
code and browser suites, not every production integration (such as live S3,
Gemini, Cloudflare, or Hermes Telegram).

## Generated Go code

After editing migrations or SQL in `go-backend/internal/<feature>/query/`, run `make -C go-backend sqlc-generate` and commit the generated files. Use `make -C go-backend swagger` after changing Swagger annotations. Generation does not apply database migrations. Add `VERBOSE=1` to Go test/coverage targets to show commands.

See [Android CI verification](../development-history/android/ci-test-verification.md) for the recorded device-test investigation and results.
