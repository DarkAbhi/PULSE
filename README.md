# Life tracker

This is the backend that I use to track some of my personal data using a telegram bot.

Yes, Django is a bit too much at this stage. I do plan to scale this at some point, where I think Django would better serve my purpose.

### Setup to run

Copy `.env.example` to `.env`, then fill in the values:

- `APP_ENV`: application environment (`production` or `development`).
- `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_HOSTNAME`, `DB_PORT`, and
  `DB_SSLMODE`: PostgreSQL database connection details.
- `BOT_API_KEY`: Telegram bot token obtained from BotFather.
- `NEXT_PUBLIC_GEMINI_API_KEY`: Gemini key used in the browser for statement
  extraction. It is included in the public frontend bundle, so restrict it by
  allowed origins and APIs.
- Optional Garage receipt storage: set `S3_BUCKET` and `AWS_REGION`, plus
  standard AWS credentials (`AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`) or
  run the backend with an IAM role. `S3_ENDPOINT` and `S3_FORCE_PATH_STYLE=true`
  support S3-compatible storage such as MinIO. These values are backend-only;
  do not use `NEXT_PUBLIC_` names for credentials.

Run the production version using

```
make deploy
```

## Make commands

Run these commands from the repository root. The production commands use the
`prod` Docker Compose profile. The development commands combine
`docker-compose.yml` with `docker-compose.dev.yml`.

### Production

| Command             | Description                                                                                   |
| ------------------- | --------------------------------------------------------------------------------------------- |
| `make build`        | Build the production Docker images.                                                           |
| `make up`           | Build, start, and run the production services in the background. Removes orphaned containers. |
| `make down`         | Stop and remove the production containers, including orphaned containers.                     |
| `make restart`      | Stop the production stack and start it again with rebuilt images.                             |
| `make deploy`       | Build the production images and start the production stack.                                   |
| `make ps`           | Show the status of production containers.                                                     |
| `make logs`         | Follow the last 200 log lines from all production services.                                   |
| `make backend-logs` | Follow the last 200 log lines from the backend service.                                       |
| `make web-logs`     | Follow the last 200 log lines from the frontend web service.                                  |
| `make bot-logs`     | Follow the last 200 log lines from the Telegram bot service.                                  |

### Production migrations

These commands run the migration service using the production Docker image.

| Command                  | Description                                                                                                                                                    |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `make docker-migrate-up` | Apply all pending database migrations.                                                                                                                         |
| `make docker-rollback`   | Roll back the most recent database migration.                                                                                                                  |
| `make docker-steps n=N`  | Apply or roll back `N` migration steps. Use a positive value to apply migrations and a negative value to roll them back, for example `make docker-steps n=-2`. |
| `make docker-version`    | Display the current database migration version.                                                                                                                |

### Development

| Command         | Description                                                                                          |
| --------------- | ---------------------------------------------------------------------------------------------------- |
| `make dev`      | Start the development backend, web, and bot services in the background. Removes orphaned containers. |
| `make dev-down` | Stop and remove the development services, including orphaned containers.                             |
| `make dev-logs` | Follow the last 200 log lines from the development backend, web, and bot services.                   |

### Development migrations

These commands run migrations against the development Compose environment.

| Command               | Description                                                                                                                                                 |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `make dev-migrate-up` | Apply all pending database migrations.                                                                                                                      |
| `make dev-rollback`   | Roll back the most recent database migration.                                                                                                               |
| `make dev-steps n=N`  | Apply or roll back `N` migration steps. Use a positive value to apply migrations and a negative value to roll them back, for example `make dev-steps n=-2`. |
| `make dev-version`    | Display the current database migration version.                                                                                                             |

The Makefile currently lists `go-test`, `go-test-integration`, and `go-cover`
in `.PHONY`, but does not define targets for them. They are therefore not
available commands until corresponding recipes are added.

### Backend SQL queries

Backend queries live in `go-backend/queries/*.sql`. The generated Go package is
`go-backend/internal/db/sqlc`; application code calls its methods. After editing
a query or a migration, run `cd go-backend && make sqlc-generate` and commit the
generated files. Generation uses the migration files as the schema and does not
change the database. Apply migrations separately before running the backend.

## Project structure

```
life-backend/
├── docker-compose.dev.yml
├── docker-compose.yml
├── Dockerfile (per-service in subfolders)
├── LICENSE
├── Makefile
├── README.md
├── go-backend/
│   ├── Dockerfile
│   ├── go.mod
│   ├── go.sum
│   ├── Makefile
│   ├── cmd/
│   │   └── main.go
│   ├── internal/
│   │   ├── db/
│   │   │   ├── migrations.go
│   │   │   └── postgres.go
│   │   └── handlers/
│   │       ├── api.go
│   │       ├── auth.go
│   │       ├── auth_test.go
│   │       ├── dates.go
│   │       ├── fuel.go
│   │       ├── gym_exercises.go
│   │       ├── gym_reminders.go
│   │       ├── gym_visits.go
│   │       ├── handlers.go
│   │       ├── handlers_test.go
│   │       ├── health.go
│   │       ├── helpers.go
│   │       ├── next_month_purchases.go
│   │       ├── notifications.go
│   │       ├── profile.go
│   │       ├── vehicle_air_fills.go
│   │       └── vehicle_history.go
│   ├── migrations/
│   │   ├── 00001_init.down.sql
│   │   ├── 00001_init.up.sql
│   │   ├── 00002_auth.down.sql
│   │   ├── 00002_auth.up.sql
│   │   ├── 00003_user_profiles.down.sql
│   │   ├── 00003_user_profiles.up.sql
│   │   ├── 00004_gym_exercises.down.sql
│   │   ├── 00004_gym_exercises.up.sql
│   │   ├── 00005_notifications.down.sql
│   │   ├── 00005_notifications.up.sql
│   │   ├── 00006_vehicle_air_fills.down.sql
│   │   ├── 00006_vehicle_air_fills.up.sql
│   │   ├── 00007_vehicle_fuel.down.sql
│   │   ├── 00007_vehicle_fuel.up.sql
│   │   ├── 00008_next_month_purchases.down.sql
│   │   ├── 00008_next_month_purchases.up.sql
│   │   ├── 00009_gym_reminders.down.sql
│   │   └── 00009_gym_reminders.up.sql
│   └── tmp/
│       ├── build-errors
│       └── main
├── life-tracker-frontend/
│   ├── Dockerfile
│   ├── next-env.d.ts
│   ├── next.config.ts
│   ├── package.json
│   ├── postcss.config.mjs
│   ├── README.md
│   ├── tsconfig.json
│   ├── app/
│   │   ├── globals.css
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   ├── components/
│   │   │   ├── fuel-form.tsx
│   │   │   └── notification-list.tsx
│   │   ├── dashboard/
│   │   │   └── page.tsx
│   │   ├── garage/
│   │   │   ├── page.tsx
│   │   │   └── [id]/
│   │   │       └── page.tsx
│   │   ├── gym-visits/
│   │   │   ├── page.tsx
│   │   │   └── [id]/
│   │   │       └── page.tsx
│   │   ├── next-month/
│   │   │   └── page.tsx
│   │   └── notifications/
│   │       └── page.tsx
│   └── public/
├── telegram-bot/
│   ├── Dockerfile
│   ├── api_constants.py
│   ├── constants.py
│   ├── main.py
│   ├── requirements.txt
│   └── utils.py
```
