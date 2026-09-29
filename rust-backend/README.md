# Rust backend scaffold

This crate is a small backend using the existing PostgreSQL tables. It has no migrations.

Use `make dev` from the repository root to run the development backend through Docker Compose. For an optional direct `cargo run`, copy `rust-backend/.env.example` to `rust-backend/.env` and fill in `DATABASE_URL`. Docker Compose ignores that file and reads the repository root `.env.dev` instead. The process checks the database with `SELECT 1` before listening on `PORT` (default `8083`). `RUST_LOG` defaults to `info`.

The Rust container listens on `RUST_PORT` (default `8083`); Compose publishes it on `127.0.0.1:18084` for dev. Production does not publish a Rust host port. Set `CLOUDFLARE_TUNNEL_TOKEN` in the root `.env` and route only the Rust hostname to `http://rust-backend:8083` in Cloudflare (or to the configured `RUST_PORT`). Compose builds `DATABASE_URL` from the existing `DB_*` fields and uses `RUST_DB_SSLMODE` for Rust's TLS setting. If database credentials later contain URL-reserved characters, provide a percent-encoded URL for the Rust service instead.

`make dev` starts a temporary Cloudflare tunnel for Rust. Run `make dev-tunnel-url` to print its current random HTTPS URL. It needs no tunnel token and can change after a restart.

`GET /healthz` reports process liveness with `{"status":"ok"}`. `GET /readyz` checks PostgreSQL within two seconds and returns `{"status":"ready"}` or HTTP 503 with `{"status":"db not ready"}`. Neither route requires a bearer token.

`GET /api/fitness-summary` requires no bearer token. It returns `{"snapshot": null | {...}, "workouts": [...]}` with the latest daily rings and up to three workouts: the latest traditional or functional strength workout first, then the latest run with distance and latest cycling workout with distance. Set `TEST_DATABASE_URL` to a disposable PostgreSQL database when running the database-backed test.

`POST /api/shortcut/fitness-rings` requires `Authorization: Bearer <API key>` using a key created in the Go app. It accepts the flat fitness rings payload used by the portfolio shortcut, including trimmed keys, numeric strings, and the exercise, stand, and step aliases. It upserts by the API key's user and `summary_date` and returns HTTP 201 with `{"saved":true}`.

`POST /api/fitness-activity-rings` requires the same bearer API key. It accepts the portfolio's nested activity rings payload, including optional workouts. It upserts workout activity types by `raw_value`, workouts by the API key's user and `uuid`, and rings by the user and `summary_date`. On success it returns HTTP 201 with `{"rings": {...}, "workouts": [{"id": ..., "uuid": ...}], "saved": true}`.

To add an endpoint, add a raw SQL function in `src/db/`, a handler that calls it, and register the route in `src/routes/mod.rs`. The models use `sqlx::FromRow` with `query_as`, so compilation does not need a live database.
