# PULSE API

Go service for the PULSE application. It exposes the REST API used by
the web app and Telegram bot, persists data in PostgreSQL, runs reminder jobs,
and publishes health, metrics, tracing, and optional profiling endpoints.

## Getting started

Requirements:

- Go 1.25 or newer
- PostgreSQL
- Docker to run the full test suite (database tests use temporary containers)

From this directory, create a local configuration file and point it at a
PostgreSQL database:

```sh
cp ../.env.example .env
# Set APP_ENV=development and edit the DB_* values in .env.
go run ./cmd/api --migrate-up
go run ./cmd/api
```

The server listens on `http://localhost:8080` by default. Useful endpoints:

- `GET /healthz` — process liveness
- `GET /readyz` — database readiness
- `GET /metrics` — Prometheus metrics
- `/swagger/index.html` — Swagger UI
- `/swagger/doc.json` — generated Swagger document

The Swagger document currently covers the health endpoints. Treat the Chi
route registrations in `cmd/api/router.go` and `internal/*` as the source of
truth until the remaining handlers have annotations.

## Configuration

The service loads `.env` from its working directory and then reads the
environment. Explicit environment variables take precedence over `.env`.

| Variable | Default | Purpose |
| --- | --- | --- |
| `APP_ENV` | `development` | Use `production` for secure session cookies and production logging defaults. |
| `PORT` | `8080` | HTTP listen port. |
| `DB_HOSTNAME` | `localhost` | PostgreSQL host. |
| `DB_PORT` | `5432` | PostgreSQL port. |
| `DB_NAME` | `life` | PostgreSQL database. |
| `DB_USERNAME` | `user` | PostgreSQL user. |
| `DB_PASSWORD` | `pass` | PostgreSQL password. |
| `DB_SSLMODE` | `disable` | PostgreSQL SSL mode. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated browser origins. |
| `S3_BUCKET` | unset | Enables vehicle maintenance attachments. |
| `AWS_REGION` | unset | AWS region used for attachment storage. |
| `S3_ENDPOINT` | unset | Optional S3-compatible endpoint. |
| `S3_FORCE_PATH_STYLE` | `false` | Enables path-style URLs for compatible stores such as MinIO. |
| `LOG_LEVEL` | environment-dependent | `debug`, `info`, `warn`, or `error`. |
| `LOG_FORMAT` | environment-dependent | `text` or `json`. |
| `OTEL_SERVICE_NAME` | `life-backend` | OpenTelemetry service name. |
| `OTEL_SERVICE_VERSION` | `1.0.0` | OpenTelemetry service version. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | unset | OTLP gRPC collector endpoint. |
| `OTEL_EXPORTER_OTLP_INSECURE` | `true` | Set to `false` to require TLS. |
| `PPROF_ENABLED` | `false` | Enables `/debug/pprof`; configure authentication before exposing it outside a trusted environment. |
| `PPROF_AUTH_USER` | unset | Basic-auth username for profiling endpoints. |
| `PPROF_AUTH_PASS` | unset | Basic-auth password for profiling endpoints. |

Standard AWS credential variables and IAM roles are supported when S3 storage
is enabled. Never expose those credentials through `NEXT_PUBLIC_*` variables.

## Development commands

Run these commands from `go-backend/`:

| Command | Purpose |
| --- | --- |
| `make test` | Run the Go test suite; database tests use temporary PostgreSQL containers. |
| `make test-integration` | Run the suite verbosely with the `integration` build tag. |
| `make cover` | Run tests and print function coverage. |
| `make sqlc-generate` | Regenerate query code from migrations and feature SQL files. |
| `make swagger` | Regenerate `docs/` from handler annotations. |

Database-backed tests require a running Docker daemon. The current tests are
not separated by build tags, so both test commands require Docker. Generated
SQL code and Swagger files are committed; regenerate and commit them with their
source changes.

## Database migrations

The API binary also manages migrations:

```sh
go run ./cmd/api --migrate-up  # apply all pending migrations
go run ./cmd/api --rollback    # roll back one migration
go run ./cmd/api --steps=-2    # roll back two migrations
go run ./cmd/api --version     # show version and dirty state
```

Migration commands expect `migrations/` in the current working directory, so
run them from `go-backend/`. Production and development Docker equivalents are
listed in the [repository README](../README.md).

## Architecture

```text
cmd/api/                    process startup, dependency wiring, and top-level routing
internal/<feature>/handler.go
                            HTTP validation and responses
internal/<feature>/service.go
                            domain rules
internal/<feature>/repository.go
                            database adapter
internal/<feature>/query/   handwritten SQL and generated sqlc code
internal/db/                migration runner
internal/observability/     logs, metrics, tracing, and profiling
migrations/                 ordered PostgreSQL schema changes
docs/                       generated Swagger files
```

Feature packages own their handlers, services, repositories, and SQL. The API
entry point wires them together; shared HTTP behavior lives in `internal/webutil`.

Most `/api` routes require a session token. Clients may send the token in the
`life_session` cookie, `Authorization: Bearer <token>`, or the
`X-Session-Token` header. Obtain a token through `POST /api/auth/login`.

## Adding or changing a feature

1. Add the migration when the schema changes.
2. Update the feature's SQL under `internal/<feature>/query/`.
3. Run `make sqlc-generate`.
4. Update the service and handler, including Swagger annotations.
5. Run `make test`; the suite includes database-backed tests.
6. Run `make swagger` when API annotations changed.
