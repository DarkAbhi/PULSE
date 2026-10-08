# PULSE

<img src="docs/logo.png" alt="PULSE logo" width="96" />

PULSE is a private personal-data hub for keeping the moving parts of everyday life in one place.
It pairs a Go API with a Next.js web app for fitness, meals, finances, vehicle care, and more.

## Project structure

The root modules and shared files are:

| Module or file | Purpose |
| --- | --- |
| [`go-backend/`](go-backend/README.md) | Main REST API, authentication, domain services, PostgreSQL migrations, and background reminders. |
| [`rust-backend/`](rust-backend/README.md) | Rust API for Apple fitness rings and workout imports, using the shared PostgreSQL database. |
| [`life-tracker-frontend/`](life-tracker-frontend/README.md) | Next.js web app and server-side forwarding of browser API requests to the Go backend. |
| [`hermes-mcp/`](hermes-mcp/README.md) | MCP server that connects Hermes to gym and garage data and workout tools through the Go API. |
| [`monitoring/`](monitoring/) | Prometheus metrics and alerts, Grafana dashboards, and provisioning configuration. |
| [`maestro/`](maestro/) | UI test flows for login, profile, and garage screens. |
| [`docs/`](docs/) | Shared feature documentation and branding assets. |
| [`Makefile`](Makefile) | Commands to build, deploy, inspect, and migrate the Docker Compose stacks. |
| [`docker-compose.yml`](docker-compose.yml), [`docker-compose.dev.yml`](docker-compose.dev.yml) | Production services and development overrides, including Jaeger tracing. |
| [`.env.example`](.env.example), [`.env.dev.example`](.env.dev.example) | Configuration templates for production and development. |
| [`release.json`](release.json) | Shared platform version and optional release timestamp used by the frontend, Go and Rust version endpoints, and MCP metadata; update it once per release and rebuild the services together. |
| [`.pre-commit-config.yaml`](.pre-commit-config.yaml) | Secret scanning and component test checks before commits. |

## Setup to run

Install Docker with Docker Compose and Make. Start the Docker daemon and provide
an external PostgreSQL instance reachable from the containers; these stacks do
not start PostgreSQL. Use separate databases for development and production.
Run the following commands from the repository root.

### 1. Deploy on development

1. Create a private development configuration file:

   ```sh
   cp .env.dev.example .env.dev
   chmod 600 .env.dev
   ```

2. Edit `.env.dev`. Keep `APP_ENV=development`, fill in the `DB_*` values for
   your development database, and set `RUST_DB_SSLMODE` for its TLS configuration.
   Set `GRAFANA_ADMIN_PASSWORD`; add `GEMINI_API_KEY` if you need statement
   extraction and the optional storage values described below if you need receipts.
   When PostgreSQL runs on the Docker host, use a hostname reachable from
   containers rather than `localhost`.

3. Apply pending migrations, then start the development services:

   ```sh
   make dev-migrate-up
   make dev
   ```

4. Open `http://localhost:3102`. Go and Next.js reload as their source files
   change. Use `make dev-logs` to inspect application logs and `make dev-down`
   to stop the stack. The first migration creates `admin` / `password`; change
   that password after the first login.

Development uses `.env.dev` and the `life-dev` Compose project. It also starts
monitoring and a temporary Cloudflare tunnel for Rust; `make dev-tunnel-url`
prints the generated HTTPS URL, which can change when the tunnel restarts.

### 2. Deploy on production

1. On the production host, create a private configuration file:

   ```sh
   cp .env.example .env
   chmod 600 .env
   ```

2. Edit `.env`. Set `APP_ENV=production`, fill in the production `DB_*` values
   and `RUST_DB_SSLMODE`, and set `GRAFANA_ADMIN_PASSWORD`. Set
   `MCP_BACKEND_USERNAME` and `MCP_BACKEND_PASSWORD` to the PULSE account Hermes
   should use. Set `CLOUDFLARE_TUNNEL_TOKEN` for the named Rust tunnel and route
   its hostname to `http://rust-backend:8083` (or your configured `RUST_PORT`).
   Add the optional Gemini and storage settings below as needed.

3. Build and start the production stack:

   ```sh
   make deploy
   ```

   Pending Go migrations run before the APIs start. Production uses `.env`
   and the `life-prod` Compose project.

4. Check the containers and open the web app on the host:

   ```sh
   make ps
   ```

   Open `http://localhost:3100`. Browser API requests use `/api` on the same
   host; Next.js forwards them to the Go backend inside Docker. Use `make logs`
   to inspect startup failures. On a new database, log in with `admin` /
   `password` and change the password immediately. See the
   [MCP guide](hermes-mcp/README.md) to connect Hermes.

### Shared configuration and access

- `SESSION_COOKIE_SECURE`: use `false` for direct HTTP access and `true` when
  serving the app over HTTPS.
- `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_HOSTNAME`, `DB_PORT`, and
  `DB_SSLMODE`: PostgreSQL connection settings for Go. `RUST_DB_SSLMODE` sets
  database TLS behavior for Rust.
- `GEMINI_API_KEY`: used by the Next.js server action for PDF statement
  extraction; the key stays out of the browser bundle.
- Optional Garage receipt storage: set `S3_BUCKET` and `AWS_REGION`, plus
  standard AWS credentials or run the backend with an IAM role.
  `S3_ENDPOINT` and `S3_FORCE_PATH_STYLE=true` support compatible storage such
  as MinIO. These settings are backend-only; do not use `NEXT_PUBLIC_` names
  for credentials.

Plain HTTP does not encrypt passwords or session cookies. Restrict access to
trusted clients and use HTTPS for public access. Monitoring, the Go API, and
MCP host ports bind to loopback. To view production Grafana remotely, forward
its port with `ssh -L 3101:127.0.0.1:3101 <server>` and open
`http://localhost:3101` locally.

## Service URLs

These URLs are for access on the Docker host. For remote access to loopback
ports, use SSH port forwarding.

| Service | Development | Production |
| --- | --- | --- |
| Next.js web app | `http://localhost:3102` | `http://localhost:3100` |
| Go API | `http://localhost:18081` | `http://localhost:18080` |
| Rust API | `http://localhost:18084` | No localhost port; accessed through the configured Cloudflare tunnel. |
| Hermes MCP | Not started by `make dev`. | `http://localhost:18082/mcp` |
| Prometheus | `http://localhost:19090` | `http://localhost:9090` |
| Grafana | `http://localhost:3103` | `http://localhost:3101` |
| Jaeger UI | `http://localhost:16687` | `http://localhost:16686` |

## Make commands

Run these commands from the repository root. Production commands use the
`prod` Compose profile with `.env`; development commands combine both Compose
files with `.env.dev`.

### Production

| Command | Description |
| --- | --- |
| `make build` | Build the production Docker images. |
| `make up` | Build and start production services in the background, applying pending migrations and removing orphaned containers. |
| `make down` | Stop and remove production containers, including orphaned containers. |
| `make restart` | Stop the production stack and start it again with rebuilt images. |
| `make deploy` | Build the production images and start the production stack. |
| `make ps` | Show the status of production containers. |
| `make logs` | Follow the last 200 log lines from all production services. |
| `make backend-logs` | Follow the last 200 log lines from the Go backend. |
| `make web-logs` | Follow the last 200 log lines from the web app. |
| `make monitoring-logs` | Follow the last 100 log lines from production Prometheus, Grafana, and Jaeger. |

### Production migrations

These commands run the one-shot migration service using the production Go image
and database configuration.

| Command | Description |
| --- | --- |
| `make docker-migrate-up` | Apply all pending database migrations. |
| `make docker-rollback` | Roll back the most recent database migration. |
| `make docker-steps n=N` | Apply `N` migration steps, or roll back with a negative value, such as `make docker-steps n=-2`. |
| `make docker-version` | Display the current database migration version and dirty state. |

### Development

| Command | Description |
| --- | --- |
| `make dev` | Build and start the Go and Rust APIs, web app, Rust tunnel, and monitoring in the background; remove orphaned containers. |
| `make dev-down` | Stop and remove development containers, including orphaned containers. |
| `make dev-logs` | Follow the last 200 log lines from the development Go and Rust APIs and web app. |
| `make dev-tunnel-url` | Show the active Cloudflare tunnel URL for the development Rust API. |
| `make dev-monitoring-logs` | Follow the last 100 log lines from development Prometheus, Grafana, and Jaeger. |

### Development migrations

These commands run the one-shot `migrate-dev` service with `.env.dev` and the
`life-dev` Compose project.

| Command | Description |
| --- | --- |
| `make dev-migrate-up` | Apply all pending database migrations. |
| `make dev-rollback` | Roll back the most recent database migration. |
| `make dev-steps n=N` | Apply `N` migration steps, or roll back with a negative value, such as `make dev-steps n=-2`. |
| `make dev-version` | Display the current database migration version and dirty state. |

### Go backend

These commands invoke the [Go backend Makefile](go-backend/Makefile) from the
repository root. For local Go setup and configuration, see the
[backend guide](go-backend/README.md).

| Command | Description |
| --- | --- |
| `make -C go-backend test` | Run the Go test suite. |
| `make -C go-backend test-integration` | Run tests verbosely with the `integration` build tag. |
| `make -C go-backend cover` | Run tests and print function coverage. |
| `make -C go-backend sqlc-generate` | Regenerate SQL query code from migrations and feature SQL files. |
| `make -C go-backend swagger` | Regenerate Swagger documentation from handler annotations. |

Go database tests require a running Docker daemon. Add `VERBOSE=1` to show
commands for the test and coverage targets. After editing SQL under
`go-backend/internal/<feature>/query/` or migrations, regenerate and commit
the sqlc files. Generation does not change the database; apply migrations
separately before running the backend.

## Continuous integration

[Tests](.github/workflows/tests.yml) runs on every push and pull request, and
can also be started manually from GitHub Actions. Its independent jobs run:

- Every Go package, including `integration`-tagged tests, with race detection
  and uncached, shuffled test runs. The existing test helpers start temporary
  PostgreSQL 17.6 containers and apply all migrations using Docker.
- All Rust workspace tests with all features enabled. A healthy PostgreSQL
  17.6 service supplies `TEST_DATABASE_URL`, so database tests execute instead
  of silently returning. These tests create their own temporary tables.
- All frontend Node tests through `npm test`, plus TypeScript checks and the
  Next.js production build.
- All Hermes MCP Python tests and seed exporter tests.
- Every Maestro browser flow in headless Chromium, against a production-built
  frontend and Go API with a separate, migrated PostgreSQL service. This job
  loads the shared anonymized development snapshot, including the bootstrap
  account's profile. It also tests seed restoration, repeat runs, protection
  of existing data, date rebasing, and rollback on error against temporary
  databases.

The databases are disposable and removed with their jobs. No production
credentials or repository secrets are required. Jobs run independently so a
failure in one module does not cancel the other suites. Monitoring and Compose
configuration have no dedicated test suites; these checks cover the existing
code and browser suites, not every production integration (such as live S3,
Gemini, Cloudflare, or Hermes Telegram).

## Development seed data

[`go-backend/data/dev-seed.sql`](go-backend/data/dev-seed.sql) is a shareable
snapshot derived from the development database. It contains 165 anonymized
records with the original relationships and relative days. Names, free text,
workout UUIDs, financial amounts, fitness measurements, and passwords are
replaced with fictional values. Sessions, API keys, attachment records, and
workout/notification metadata are not copied. The public exercise catalogue
continues to come from migration 23.

Configure an existing, separate development PostgreSQL database in `.env.dev`
and run `make dev`. Compose applies migrations, seeds a pristine database,
then starts both backends. The demo login is `admin` / `password`. An existing
database with user data is skipped; seeding does not merge or replace it.
Dates are shifted on the first restore so the latest demo gym visit falls
on the current day, and planned purchases target next month. Later restarts
leave those records alone.

Useful commands from the repository root:

| Command | Purpose |
| --- | --- |
| `make dev-seed` | Apply migrations and seed a pristine development database; skip a populated database. |
| `make dev-seed-export` | Refresh the anonymized snapshot from `.env.dev` using Python 3.10+ and `psql`. The source database is read only. |
| `python3 -m unittest discover -s scripts -p 'test_export_dev_seed.py'` | Check anonymization and export guards without a database. |

The exporter reads all selected tables in one consistent SQL snapshot and
writes only sanitized data. It rejects unrecognized tables, numeric fields,
and enum values so schema changes require review. Refresh the fixture alongside
migration changes: restoring into a pristine database with a different schema
version fails rather than applying an incompatible snapshot. Do not manually
load an unsanitized dump into the shared seed file.

GitHub's browser job uses this same fixture in a disposable database. Go and
Rust unit/integration tests keep their existing controlled fixtures: many
assert empty results or insert fixed IDs, so adding a month of records to every
test database would invalidate those tests. Seed restore tests can be run
locally with `TEST_SEED_DATABASE_URL` pointing to a disposable PostgreSQL server
whose role can create databases:

```sh
python3 -m unittest discover -s scripts -p 'test_seed_database.py'
```

Production Compose does not include the seed service. Direct execution of the
SQL also requires `psql -v seed_environment=development` (or `test`); it rejects
other environment values. The seed is demo data and retains record counts and
relationships from the source, so it is not a backup or an exact reproduction
of the original activity and financial values.

## Pre-commit checks

Install [pre-commit](https://pre-commit.com/#install), Go, Rust, Node.js 24,
Python 3, and Docker, then install the hooks from the repository root:

```sh
pre-commit install
```

Each commit scans staged changes with Gitleaks and runs the Go, Rust, Next.js,
Hermes MCP, and seed exporter test suites. To run the checks without committing:

```sh
pre-commit run --all-files
```

Go tests need a running Docker daemon. Rust's database tests need
`TEST_DATABASE_URL` set to a disposable PostgreSQL database or they skip
themselves.
