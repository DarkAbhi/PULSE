# Life tracker

Life Tracker is a personal-data application with a Go API and a Next.js web app.
See the [backend guide](go-backend/README.md) for local Go
development, configuration, architecture, migrations, tests, and API docs.

### Pre-commit checks

Install [pre-commit](https://pre-commit.com/#install), Go, Rust, Node.js 24,
Python 3, and Docker, then run `pre-commit install` from the repository root.
Each commit scans staged changes with Gitleaks and runs the Go, Rust,
Next.js, and Hermes MCP test suites. Run `pre-commit run --all-files` to run
the checks without committing. The Go tests need a running Docker daemon;
Rust's database tests also need `TEST_DATABASE_URL` set to a disposable
PostgreSQL database or they skip themselves.

### Setup to run

On the production host, copy `.env.example` to `.env` and fill in the values.
For development, copy `.env.dev.example` to `.env.dev` and use a different
database. The Makefile loads the matching file explicitly
and uses separate Compose project names (`life-prod` and `life-dev`).
Keep both files private (`chmod 600 .env` or `chmod 600 .env.dev`).

- `APP_ENV`: `production` in `.env` and `development` in `.env.dev`.
- `SESSION_COOKIE_SECURE`: set to `false` for direct HTTP access by LAN or
  Tailscale IP. Set to `true` if you later add HTTPS.
- `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_HOSTNAME`, `DB_PORT`, and
  `DB_SSLMODE`: PostgreSQL database connection details.
- `RUST_DB_SSLMODE`: database TLS setting for the Rust backend.
- `GEMINI_API_KEY`: Gemini key used only by the Next.js server action. PDF
  statements are uploaded to the server for extraction; the key is never put in
  the browser bundle.
- Optional Garage receipt storage: set `S3_BUCKET` and `AWS_REGION`, plus
  standard AWS credentials (`AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`) or
  run the backend with an IAM role. `S3_ENDPOINT` and `S3_FORCE_PATH_STYLE=true`
  support S3-compatible storage such as MinIO. These values are backend-only;
  do not use `NEXT_PUBLIC_` names for credentials.

Run the production version using

```
make deploy
```

Open `http://<server-LAN-or-Tailscale-IP>:3100`. Browser API requests use
`/api` on that same host, and Next.js forwards them to the backend inside
Docker. No browser URL or CORS origin needs to be built into the image. See
[Service URLs](#service-urls) for the other local endpoints.

The Compose stacks expect PostgreSQL outside this repository. Use separate
development and production databases. Run `make dev-migrate-up` before
`make dev`. Production runs pending migrations when `make up` starts; you can
also run `make docker-migrate-up` explicitly. The first migration creates
`admin` / `password`; change that password after the first login.

Plain HTTP on a LAN does not encrypt your password or session cookie in
transit. Tailscale traffic is encrypted; restrict access to trusted clients
and do not forward port 3100 from your internet router. Prometheus, Grafana,
and Jaeger listen only on the server's loopback interface. Set
`GRAFANA_ADMIN_PASSWORD` in `.env` before starting production. To view Grafana
from another computer, forward its port with
`ssh -L 3101:127.0.0.1:3101 <server>` and open `http://localhost:3101` in your
browser.

## Service URLs

Use `localhost` when running Docker on your computer. When running the
production stack on another host, replace `localhost` with the server's LAN or
Tailscale IP where the service is exposed. Monitoring ports are bound to
loopback and therefore require SSH port forwarding when accessed remotely.

| Service           | Development              | Production                                 | Browser access                                                 |
| ----------------- | ------------------------ | ------------------------------------------ | -------------------------------------------------------------- |
| Next.js web app   | `http://localhost:3102`  | `http://<server-LAN-or-Tailscale-IP>:3100` | Open this URL.                                                 |
| Go API            | `http://localhost:18081` | `http://localhost:18080` on the server     | Loopback only; browser API requests use the web app at `/api`. |
| Rust API scaffold | `http://localhost:18084` | Cloudflare Rust hostname                   | Tunnel targets `http://rust-backend:8083` by default.          |
| Prometheus        | `http://localhost:19090` | `http://localhost:9090` on the server      | Open locally, or use SSH port forwarding.                      |
| Grafana           | `http://localhost:3103`  | `http://localhost:3101` on the server      | Open locally, or use SSH port forwarding.                      |
| Jaeger UI         | `http://localhost:16687` | `http://localhost:16686` on the server     | Open locally, or use SSH port forwarding.                      |

The migration services are one-shot command-line services and do not expose a
browser URL.

Development also starts a temporary Cloudflare tunnel for the Rust API. Run
`make dev-tunnel-url` after `make dev` to see its generated HTTPS URL. It can
change when the development tunnel restarts; production uses the hostname
configured on its named Cloudflare tunnel.

## Make commands

Run these commands from the repository root. The production commands use the
`prod` Docker Compose profile with `.env`. Development commands combine
`docker-compose.yml` and `docker-compose.dev.yml` with `.env.dev`.

### Production

| Command                | Description                                                                                   |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| `make build`           | Build the production Docker images.                                                           |
| `make up`              | Build, start, and run the production services in the background. Removes orphaned containers. |
| `make down`            | Stop and remove the production containers, including orphaned containers.                     |
| `make restart`         | Stop the production stack and start it again with rebuilt images.                             |
| `make deploy`          | Build the production images and start the production stack.                                   |
| `make ps`              | Show the status of production containers.                                                     |
| `make logs`            | Follow the last 200 log lines from all production services.                                   |
| `make backend-logs`    | Follow the last 200 log lines from the backend service.                                       |
| `make web-logs`        | Follow the last 200 log lines from the frontend web service.                                  |
| `make monitoring-logs` | Follow production Prometheus, Grafana, and Jaeger logs.                                       |

### Production migrations

These commands run the migration service using the production Docker image.

| Command                  | Description                                                                                                                                                    |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `make docker-migrate-up` | Apply all pending database migrations.                                                                                                                         |
| `make docker-rollback`   | Roll back the most recent database migration.                                                                                                                  |
| `make docker-steps n=N`  | Apply or roll back `N` migration steps. Use a positive value to apply migrations and a negative value to roll them back, for example `make docker-steps n=-2`. |
| `make docker-version`    | Display the current database migration version.                                                                                                                |

### Development

| Command                    | Description                                                                                    |
| -------------------------- | ---------------------------------------------------------------------------------------------- |
| `make dev`                 | Start the development backend and web services in the background. Removes orphaned containers. |
| `make dev-down`            | Stop and remove the development services, including orphaned containers.                       |
| `make dev-logs`            | Follow the last 200 log lines from the development backend and web services.                   |
| `make dev-tunnel-url`      | Show the active Cloudflare tunnel URL for the development Rust API.                            |
| `make dev-monitoring-logs` | Follow development Prometheus, Grafana, and Jaeger logs.                                       |

### Development migrations

These commands run the `migrate-dev` service using `.env.dev` and the
`life-dev` Compose project. Production migration commands use `.env` and
`life-prod`.

| Command               | Description                                                                                                                                                 |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `make dev-migrate-up` | Apply all pending database migrations.                                                                                                                      |
| `make dev-rollback`   | Roll back the most recent database migration.                                                                                                               |
| `make dev-steps n=N`  | Apply or roll back `N` migration steps. Use a positive value to apply migrations and a negative value to roll them back, for example `make dev-steps n=-2`. |
| `make dev-version`    | Display the current database migration version.                                                                                                             |

### Monitoring and service status

| Command                    | Description                                              |
| -------------------------- | -------------------------------------------------------- |
| `make ps`                  | Show the status of the production containers.            |
| `make monitoring-logs`     | Follow production Prometheus, Grafana, and Jaeger logs.  |
| `make dev-monitoring-logs` | Follow development Prometheus, Grafana, and Jaeger logs. |

### Backend SQL queries

Backend queries and generated sqlc code live together under
`go-backend/internal/<feature>/query/`. After editing a query or migration, run
`cd go-backend && make sqlc-generate` and commit the generated files. Generation
uses the migration files as the schema and does not change the database. Apply
migrations separately before running the backend.

## Project structure

```
life-backend/
├── .dockerignore
├── .env.example
├── .gitignore
├── LICENSE
├── Makefile
├── README.md
├── docker-compose.dev.yml
├── docker-compose.yml
├── go-backend/
│   ├── .air.toml
│   ├── Dockerfile
│   ├── Makefile
│   ├── cmd/
│   │   └── api/
│   │       └── main.go
│   ├── coverage.out
│   ├── go.mod
│   ├── go.sum
│   ├── internal/
│   │   ├── auth/
│   │   ├── db/
│   │   ├── gym/
│   │   ├── health/
│   │   ├── horizon/
│   │   ├── mealplan/
│   │   ├── notification/
│   │   ├── profile/
│   │   ├── purchase/
│   │   ├── testhelper/
│   │   ├── timeutil/
│   │   ├── vehicle/
│   │   └── webutil/
│   ├── migrations/
│   ├── sqlc.yaml
├── life-tracker-frontend/
│   ├── .DS_Store
│   ├── .gitignore
│   ├── .next/
│   ├── Dockerfile
│   ├── README.md
│   ├── app/
│   │   ├── components/
│   │   ├── dashboard/
│   │   ├── dialog-preview/
│   │   ├── favicon.ico
│   │   ├── financial-horizon/
│   │   ├── garage/
│   │   ├── globals.css
│   │   ├── gym-visits/
│   │   ├── layout.tsx
│   │   ├── meal-plan/
│   │   ├── notifications/
│   │   ├── page.tsx
│   │   └── profile/
│   ├── next-env.d.ts
│   ├── next.config.ts
│   ├── node_modules/
│   ├── package-lock.json
│   ├── package.json
│   ├── postcss.config.mjs
│   ├── public/
│   ├── services/
│   ├── tsconfig.json
│   └── tsconfig.tsbuildinfo
```
