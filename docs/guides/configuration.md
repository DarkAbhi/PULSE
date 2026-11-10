# Configuration and access

The root [development template](../../.env.dev.example) and [production template](../../.env.example) configure Compose. Keep `.env.dev` and `.env` private. Component-local `.env` files apply only when running those processes directly.

## Database

- Create an empty PostgreSQL database and a role that can connect, create tables, and apply migrations. PostgreSQL 17.6 is the version used by the test fixtures.
- Set `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_HOSTNAME`, `DB_PORT`, and `DB_SSLMODE`. Set `RUST_DB_SSLMODE` for the same database's TLS requirements.
- Both backends share the database; Go owns migrations. Use separate development and production databases. Compose does not run a PostgreSQL server.
- `localhost` inside a container means that container. Docker Desktop users can use `host.docker.internal` for a database on the host; on Linux use an address reachable from Docker and allow that connection in PostgreSQL/network rules.
- Rust builds a PostgreSQL URL from these variables. URL-reserved characters in credentials require percent encoding in its `DATABASE_URL`; see the [Rust guide](../../rust-backend/README.md).

## Local development database example

If you do not already have PostgreSQL, Docker Desktop users can create a separate local development server:

```sh
docker run -d --name pulse-dev-postgres -e POSTGRES_DB=pulse_dev -e POSTGRES_USER=pulse_dev -e POSTGRES_PASSWORD=pulse_dev_local -p 127.0.0.1:5432:5432 -v pulse-dev-postgres-data:/var/lib/postgresql/data postgres:17.6
```

Set `.env.dev` to `DB_NAME=pulse_dev`, `DB_USERNAME=pulse_dev`, `DB_PASSWORD=pulse_dev_local`, `DB_HOSTNAME=host.docker.internal`, `DB_PORT=5432`, `DB_SSLMODE=disable`, and `RUST_DB_SSLMODE=disable`. These example credentials are for local development only. Wait for PostgreSQL to report ready (`docker logs pulse-dev-postgres`) before `make dev`. On Linux, use a host address reachable from Docker and a suitable port binding instead of this Desktop-specific loopback setup. If port 5432 is occupied, choose another host port and update `DB_PORT`.

The database container runs separately from Compose. Stop it with `docker stop pulse-dev-postgres` and start it again with `docker start pulse-dev-postgres`; the named volume preserves its data.

## Upgrading an existing database

For the user ownership rollout, run the read-only audit and follow the [ownership migration guide](../development-history/features/user-ownership.md) before applying migrations 29–30. The guide also covers migration 31's removal of legacy tables, backfills, backup/rehearsal and rollback limitations. Fresh databases follow the normal setup without legacy owner mapping.

## Sessions and network access

Set `SESSION_COOKIE_SECURE=false` for direct HTTP, and `true` when serving the web app over HTTPS. Plain HTTP carries passwords and sessions without transport encryption; use HTTPS for public access. The web ports bind to all interfaces. Go, MCP, Prometheus, Jaeger and default Grafana ports bind to loopback.

For remote monitoring, forward the appropriate port, for example `ssh -N -L 3101:127.0.0.1:3101 <server>`, then open [Grafana](http://localhost:3101). See [monitoring](monitoring.md) for Tailscale binding and privileged collector details.

The first migration creates `admin` / `password`. Change that password before exposing a production installation. Set `MCP_BACKEND_USERNAME` and `MCP_BACKEND_PASSWORD` to the account Hermes should use; update them if that account's password changes.

## Optional integrations

| Integration | Settings | Setup |
| --- | --- | --- |
| PDF statement extraction | `GEMINI_API_KEY` | Server-side key used by Next.js; never expose through `NEXT_PUBLIC_*`. |
| Garage receipts | `S3_BUCKET`, `AWS_REGION`, standard AWS credentials or an IAM role | Optional `AWS_SESSION_TOKEN`; S3-compatible storage can use `S3_ENDPOINT` and `S3_FORCE_PATH_STYLE=true`. Backend only. |
| Rust production tunnel | `CLOUDFLARE_TUNNEL_TOKEN` | Configure the named hostname to route to `http://rust-backend:8083`, or the chosen `RUST_PORT`. |
| Rust development tunnel | None | Started by `make dev`; `make dev-tunnel-url` prints the temporary HTTPS address. |
| Hermes | `MCP_BACKEND_USERNAME`, `MCP_BACKEND_PASSWORD` | [MCP connection and tools](../../hermes-mcp/README.md). |

`make deploy` starts the full production profile, including MCP and Cloudflare. Without their configuration those integrations will not work; the core web/API configuration still requires only the database and Grafana password.

## Troubleshooting startup

- Check `make ps` or, for development, `docker compose --env-file .env.dev --project-name life-dev -f docker-compose.yml -f docker-compose.dev.yml --profile dev ps`.
- Use `make logs` / `make dev-logs` for application failures and `make dev-monitoring-logs` for monitoring failures.
- Database failures: check host reachability, credentials, TLS mode and migration logs before restarting.
- Source changes: Go and web reload in development; rebuild Rust with `make dev`. Apply frontend container configuration changes with `make dev-web`.
- `make down` and `make dev-down` retain named volumes. `docker compose down -v` deletes their stored monitoring history and development caches.
