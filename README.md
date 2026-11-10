# PULSE

<img src="docs/logo.png" alt="PULSE logo" width="96" />

A personal-data hub for fitness, meals, finances, and vehicle care, with web and native Android apps.

[Setup](#setup) · [Service URLs](#service-urls) · [Commands](#commands) · [Documentation](docs/README.md)

## Project structure

| Directory | Purpose |
| --- | --- |
| [go-backend/](go-backend/README.md) | Main REST API, authentication, PostgreSQL migrations and reminders. |
| [rust-backend/](rust-backend/README.md) | Apple fitness rings and workout imports; shares the Go database. |
| [life-tracker-frontend/](life-tracker-frontend/README.md) | Next.js web app; forwards `/api` requests to Go. |
| [pulse-android/](pulse-android/README.md) | Native Kotlin/Compose Android app. |
| [hermes-mcp/](hermes-mcp/README.md) | MCP server connecting Hermes to gym and garage tools. |
| [monitoring/](monitoring/) | Grafana, Prometheus, Jaeger, Loki and Alloy configuration. |
| [maestro/](maestro/README.md) | Android smoke tests and retained browser flows. |
| [scripts/](scripts/) | Test reports, seed export, vehicle catalog and CI helpers. |
| [docs/](docs/README.md) | Operating guides and categorized development history. |

## Setup

### Requirements

- Git, Docker with Linux containers, [Docker Compose **2.24.4+**](https://docs.docker.com/reference/compose-file/merge/#replace-value) (`!override` support), and GNU Make.
- A Bash-compatible shell for Make commands. On Windows, use WSL2 with Docker Desktop integration.
- An existing PostgreSQL server reachable from Docker, with an empty database and a role allowed to run migrations. The stacks **do not create PostgreSQL**; tests use PostgreSQL 17.6. Need a local database? Use the [Docker example](docs/guides/configuration.md#local-development-database-example).

Go, Rust, Node.js and Python are only required on the host for direct development or tests; Docker builds the web/API services.

```sh
git clone https://github.com/DarkAbhi/PULSE.git
cd PULSE
```

### Development

1. Copy the template:

   ```sh
   cp .env.dev.example .env.dev
   chmod 600 .env.dev
   ```

2. Edit `.env.dev`: fill `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_HOSTNAME`, and `DB_PORT`; set `DB_SSLMODE` and `RUST_DB_SSLMODE` to match your database. Keep `APP_ENV=development` and set `GRAFANA_ADMIN_PASSWORD`. For a database on the Docker Desktop host, use `DB_HOSTNAME=host.docker.internal`, not `localhost`. See [configuration](docs/guides/configuration.md) for Linux/network details and optional integrations.
3. Start the stack:

   ```sh
   make dev
   ```

   Migrations run automatically; a pristine development database receives demo data. Go and web reload on source changes; Rust changes require rebuilding with `make dev`.

4. Open [PULSE](http://localhost:3102). Sign in with `admin` / `password`. Use `make dev-logs` to inspect startup and `make dev-down` to stop.

Development uses `.env.dev` and Compose project `life-dev`. It also starts monitoring and a temporary public Rust tunnel; `make dev-tunnel-url` prints its changing HTTPS address.

### Production

1. On your deployment host, copy the production template:

   ```sh
   cp .env.example .env
   chmod 600 .env
   ```

2. Edit `.env`: keep `APP_ENV=production`, fill the `DB_*` values for a **separate production database**, set `RUST_DB_SSLMODE` for its TLS requirements, and choose a strong `GRAFANA_ADMIN_PASSWORD`. Set `SESSION_COOKIE_SECURE=true` when serving over HTTPS (`false` for direct HTTP).
3. For the full stack, set `MCP_BACKEND_USERNAME` / `MCP_BACKEND_PASSWORD` to the intended PULSE account. Set `CLOUDFLARE_TUNNEL_TOKEN` and route its hostname to `http://rust-backend:8083` (or your `RUST_PORT`). See [configuration and optional integrations](docs/guides/configuration.md).
4. Build and start:

   ```sh
   make deploy
   make ps
   ```

   Pending migrations run before the APIs start. Open [PULSE](http://localhost:3100) on the host. On a fresh database, log in with `admin` / `password` and change the password before exposing the app; update MCP credentials to match. Use HTTPS for public access. See [MCP setup](hermes-mcp/README.md) to connect Hermes.

Production uses `.env` and Compose project `life-prod`. Use `make logs` for startup failures and `make down` to stop. Public hostnames are deployment-specific; none are supplied by this repository.

### Android

1. Start the development stack above. Open `pulse-android/` in Android Studio, install SDK platform 37, set `sdk.dir` in local `local.properties`, and use JDK 25 through the checked-in Gradle wrapper.
2. Start an emulator (Android API 28+), then build and install:

   ```sh
   cd pulse-android
   ./gradlew :app:installDebug
   ```

   Debug uses `http://10.0.2.2:18081/api/`. On API 37+, approve the local-network permission prompt. For a physical phone, configure an accessible HTTPS API address ending in `/api/`:

   ```sh
   ./gradlew :app:assembleDebug -Ppulse.debugApiUrl=https://your-dev-host.example/api/
   ./gradlew :app:assembleRelease -Ppulse.releaseApiUrl=https://your-prod-host.example/api/
   ```

See [Android setup](pulse-android/README.md#setup) for device access and [release signing](docs/guides/android-release.md) for distribution. Windows PowerShell users can use `gradlew.bat` in place of `./gradlew`.

## Service URLs

Defaults below are reachable on the Docker host unless noted. Remote loopback services need port forwarding; Grafana can also use a [configured Tailscale binding](docs/guides/monitoring.md#grafana-over-tailscale).

| Application / endpoint | Development | Production |
| --- | --- | --- |
| Web app | [localhost:3102](http://localhost:3102) | [localhost:3100](http://localhost:3100) |
| Web API proxy | [localhost:3102/api/](http://localhost:3102/api/) | [localhost:3100/api/](http://localhost:3100/api/) |
| Go API | [localhost:18081](http://localhost:18081) | [localhost:18080](http://localhost:18080) |
| Go health / readiness | [healthz](http://localhost:18081/healthz) / [readyz](http://localhost:18081/readyz) | [healthz](http://localhost:18080/healthz) / [readyz](http://localhost:18080/readyz) |
| Go Swagger UI (health endpoints) | [Swagger](http://localhost:18081/swagger/index.html) | [Swagger](http://localhost:18080/swagger/index.html) |
| Rust API | [localhost:18084](http://localhost:18084); HTTPS via `make dev-tunnel-url` | Configured Cloudflare HTTPS hostname; no published host port. |
| Rust health / readiness | [healthz](http://localhost:18084/healthz) / [readyz](http://localhost:18084/readyz) | `/healthz` / `/readyz` on the configured Rust hostname. |
| Hermes MCP | Not started by `make dev`. | [localhost:18082/mcp](http://localhost:18082/mcp) |
| Grafana | [localhost:3103](http://localhost:3103) | [localhost:3101](http://localhost:3101) |
| Prometheus | [localhost:19090](http://localhost:19090) | [localhost:9090](http://localhost:9090) |
| Jaeger | [localhost:16687](http://localhost:16687) | [localhost:16686](http://localhost:16686) |
| Android API base | `http://10.0.2.2:18081/api/` (emulator) | Set `pulse.releaseApiUrl`; existing default is `http://100.115.134.90:3100/api/`, accessible only on the maintainer's tailnet. |
| Loki / Alloy | Internal only; view logs in Grafana. | Internal only; view logs in Grafana. |
| Rust metrics | Internal `http://rust-backend-api:9091/metrics` | Internal `http://rust-backend-api:9091/metrics` |

Grafana login is `admin` with that environment's `GRAFANA_ADMIN_PASSWORD`. Android has no browser URL. The Go API root and `/api/` are base addresses; use the linked health checks to verify startup.

## Commands

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
| `make monitoring-logs` | Follow the last 100 log lines from production Prometheus, Grafana, Jaeger, Loki, and Alloy. |

### Production migrations

For upgrades of existing databases, first read the [migration guidance](docs/guides/configuration.md#upgrading-an-existing-database).

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
| `make dev-web` | Apply frontend container/config changes without restarting other services. |
| `make dev-seed` | Migrate and seed a pristine development database; skip populated databases. |
| `make dev-seed-export` | Refresh the anonymized demo snapshot; requires Python 3.10+ and `psql`. |
| `make dev-down` | Stop and remove development containers, including orphaned containers. |
| `make dev-logs` | Follow the last 200 log lines from the development Go and Rust APIs and web app. |
| `make dev-web-logs` | Follow the last 200 log lines from the development web app. |
| `make dev-backend-logs` | Follow the last 200 log lines from the development Go API. |
| `make dev-rust-backend-logs` | Follow the last 200 log lines from the development Rust API. |
| `make dev-cloudflared-logs` | Follow the last 200 log lines from the development Cloudflare tunnel. |
| `make dev-tunnel-url` | Show the active Cloudflare tunnel URL for the development Rust API. |
| `make dev-monitoring-logs` | Follow the last 100 log lines from development Prometheus, Grafana, Jaeger, Loki, and Alloy. |
| `make dev-prometheus-logs` | Follow the last 100 log lines from development Prometheus. |
| `make dev-grafana-logs` | Follow the last 100 log lines from development Grafana. |
| `make dev-jaeger-logs` | Follow the last 100 log lines from development Jaeger. |
| `make dev-loki-logs` | Follow the last 100 log lines from development Loki. |
| `make dev-alloy-logs` | Follow the last 100 log lines from development Alloy. |

### Development migrations

These commands run the one-shot `migrate-dev` service with `.env.dev` and the
`life-dev` Compose project.

| Command | Description |
| --- | --- |
| `make dev-migrate-up` | Apply all pending database migrations. |
| `make dev-rollback` | Roll back the most recent database migration. |
| `make dev-steps n=N` | Apply `N` migration steps, or roll back with a negative value, such as `make dev-steps n=-2`. |
| `make dev-version` | Display the current database migration version and dirty state. |

### Tests and component commands

All root Make targets are listed here. Component commands below run from the named directory unless stated otherwise. Test prerequisites: Go 1.25+, Rust from `rust-backend/rust-toolchain.toml`, Node.js 24/npm, Python 3.12+, Make and a running Docker daemon. Run `npm ci` in `life-tracker-frontend/` once first. Android checks also need the Android setup above.

| Command (repository root) | Purpose |
| --- | --- |
| `make test` / `make test-report` | Run Go, Rust, frontend, MCP and scripts suites; write `test-results/index.html`. |
| `make go-test` | Go tests including integration-tagged tests. |
| `make rust-test` | Rust workspace tests with all features. |
| `make frontend-test` | Frontend Node tests. |
| `make mcp-test` | Hermes MCP Python tests. |
| `make seed-test` | Script tests; database restore checks need `TEST_SEED_DATABASE_URL` and `psql`. |
| `make -C go-backend test` | Go tests. |
| `make -C go-backend test-integration` | Verbose Go tests with integration tag. |
| `make -C go-backend cover` | Go coverage report. |
| `make -C go-backend sqlc-generate` | Regenerate SQL query code. |
| `make -C go-backend swagger` | Regenerate Swagger documentation. |

| Directory | Command | Purpose |
| --- | --- | --- |
| `life-tracker-frontend/` | `npm ci` | Install locked dependencies. |
| `life-tracker-frontend/` | `npm run dev` | Local Turbopack server on port 3000; configure its backend separately. |
| `life-tracker-frontend/` | `npm run dev:docker` | Webpack server with external binding; used by Compose. |
| `life-tracker-frontend/` | `npm test` | Node unit tests. |
| `life-tracker-frontend/` | `npm run build` | Type check and production build. |
| `life-tracker-frontend/` | `npm start` | Serve the built app on port 3000. |
| `life-tracker-frontend/` | `npm run test:e2e` | Retained Playwright journeys; browser E2E is paused in CI. See [testing](docs/guides/testing.md). |
| `go-backend/` | `go run ./cmd/api` | Run Go directly after [local configuration](go-backend/README.md#getting-started). |
| `rust-backend/` | `cargo run` | Run Rust directly after [local configuration](rust-backend/README.md). |
| `rust-backend/` | `cargo test --locked --workspace --all-features` | Run all Rust tests. |
| `rust-backend/` | `cargo fmt --all -- --check` | Check Rust formatting. |
| `rust-backend/` | `cargo clippy --locked --workspace --all-targets --all-features -- -D warnings` | Check Rust lints. |
| `pulse-android/` | `./gradlew build` | Build and run module checks. |
| `pulse-android/` | `./gradlew testDebugUnitTest lintDebug ktlintCheck` | Unit tests, debug lint and formatting checks. |
| `pulse-android/` | `./gradlew ktlintFormat` | Format Kotlin and Gradle files. |
| `pulse-android/` | `./gradlew :app:assembleDebug` | Build a debug APK. |
| `pulse-android/` | `./gradlew :app:installDebug` | Install on a running emulator/device. |
| `pulse-android/` | `./gradlew :app:assembleRelease` | Build a release APK; [signing setup](docs/guides/android-release.md). |
| `pulse-android/` | `./gradlew :app:assembleDebugAndroidTest` | Build the instrumentation test APK. |
| `pulse-android/` | `./gradlew :app:connectedDebugAndroidTest` | Run device tests on an emulator/device. |
| Repository root | `maestro test maestro/android/` | Native smoke flows; installed debug APK and Java 21 required. See [Maestro](maestro/README.md). |

See [testing and CI](docs/guides/testing.md), [seed data](docs/guides/development-seed.md), and [pre-commit setup](docs/guides/pre-commit.md) for detailed workflows. CI-only emulator helpers and catalog maintenance are described in [maintenance commands](docs/guides/maintenance-commands.md).

## Documentation

- [Documentation index](docs/README.md): configuration, monitoring, testing, pre-commit, seeds and releases.
- [Development history](docs/development-history/README.md): categorized feature plans, design rationale, reviews and verification evidence across backend, web and Android.
- [Development timeline](docs/development-history/timeline.md): dated index of the existing build records.
