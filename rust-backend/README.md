# Rust backend scaffold

This crate is a small backend using the existing PostgreSQL tables. It has no migrations.

The shared exercise catalogue is installed by Go migration 23. Gym exercise
search and saves remain in the Go API; Rust's Apple workout importer preserves
existing visits, catalogue links, names, and sets during repeated imports. Its
database-backed import test covers that compatibility. See
[exercise catalogue setup](../go-backend/data/exercise-catalog.md).

Use `make dev` from the repository root to run the development backend through Docker Compose. For an optional direct `cargo run`, copy `rust-backend/.env.example` to `rust-backend/.env` and fill in `DATABASE_URL`. Docker Compose ignores that file and reads the repository root `.env.dev` instead. The process checks the database with `SELECT 1` before listening on `PORT` (default `8083`). `RUST_LOG` defaults to `info`.

The Rust container listens on `RUST_PORT` (default `8083`); Compose publishes it on `127.0.0.1:18084` for dev. Production does not publish a Rust host port. Set `CLOUDFLARE_TUNNEL_TOKEN` in the root `.env` and route only the Rust hostname to `http://rust-backend:8083` in Cloudflare (or to the configured `RUST_PORT`). Compose builds `DATABASE_URL` from the existing `DB_*` fields and uses `RUST_DB_SSLMODE` for Rust's TLS setting. If database credentials later contain URL-reserved characters, provide a percent-encoded URL for the Rust service instead.

`make dev` starts a temporary Cloudflare tunnel for Rust. Run `make dev-tunnel-url` to print its current random HTTPS URL. It needs no tunnel token and can change after a restart.

`GET /healthz` reports process liveness with `{"status":"ok"}`. `GET /readyz` checks PostgreSQL within two seconds and returns `{"status":"ready"}` or HTTP 503 with `{"status":"db not ready"}`. Neither route requires a bearer token.

Prometheus metrics are available internally at `http://rust-backend-api:9091/metrics`
in Compose. This listener has no published host port and is separate from the
public API. See [monitoring setup](../README.md#backend-and-resource-metrics).

`GET /api/fitness-summary` requires no bearer token. It returns `{"snapshot": null | {...}, "workouts": [...]}` with the latest daily rings and up to three workouts: the latest traditional or functional strength workout first, then the latest run with distance and latest cycling workout with distance.

`POST /api/shortcut/fitness-rings` requires `Authorization: Bearer <API key>` using a key created in the Go app. It accepts the flat fitness rings payload used by the portfolio shortcut, including trimmed keys, numeric strings, and the exercise, stand, and step aliases. It upserts by the API key's user and `summary_date` and returns HTTP 201 with `{"saved":true}`.

`POST /api/fitness-activity-rings` requires the same bearer API key. It accepts the portfolio's nested activity rings payload, including optional workouts. It upserts workout activity types by `raw_value`, workouts by the API key's user and `uuid`, and rings by the user and `summary_date`. On success it returns HTTP 201 with `{"rings": {...}, "workouts": [{"id": ..., "uuid": ...}], "saved": true}`.

Both import routes reject numbers outside the storage ranges with HTTP 400:
whole-number counters and activity type values must fit a non-negative PostgreSQL
integer (at most 2,147,483,647). Calories are at most 999,999.99, workout duration
9,999,999.999 seconds, and distance 99,999,999.99 meters. Whole floats remain valid;
the flat route also retains numeric-string and alias support. The nested route
rejects duplicate workout UUIDs, including case variants, before database writes.

Imports preserve the names of existing shared activity types. Shared types are
written in ascending raw-value order to avoid conflicting lock orders. Correcting
a workout's dates updates the workout while retaining its existing gym visit,
visit date, exercises, and sets. New activity types still take their initial names
from the import; existing names are not repaired automatically.

To add an endpoint, add a raw SQL function in `src/db/`, a handler that calls it, and register the route in `src/routes/mod.rs`. The models use `sqlx::FromRow` with `query_as`, so compilation does not need a live database.

## Tests

From this directory:

```sh
cargo test --locked --workspace --all-features
# Run one test by name:
cargo test summary_selects_latest_strength_run_and_ride
```

Install Rust using the pinned 1.98.1 toolchain in `rust-toolchain.toml`, matching the
Docker build. CI checks formatting and Clippy as well as tests. Start Docker;
no database URL, migrations, or manual seeding are needed. Each database test
starts a PostgreSQL 17.6 container on a random loopback port, creates isolated
tables, and inserts only its own fixtures. The container is removed on normal
completion or assertion failure. If Docker cannot start PostgreSQL, the test
fails rather than silently passing. Unit tests without database access do not
start containers.

Tests always use disposable containers and ignore application database settings;
there is no external test database option. Tests run on the host and communicate
with PostgreSQL through its random loopback port. `docker run --detach --rm
--publish 127.0.0.1::5432` starts each container; the helper discovers the assigned
port with `docker port`, waits up to 60 seconds for a connection, and runs
`docker rm --force` when the database guard is dropped, including on assertion
failure. No volume is mounted, so test data disappears with the container.

Docker startup and port discovery run asynchronously with a 30-second command
timeout. Cleanup runs on a blocking worker, including during test failure, with a
30-second limit; the test runtime waits for that worker at shutdown. Containers
carry the `life-backend-rust-tests` label for inspection. Concurrency tests use
multiple connections and regular tables inside a disposable database. The HTTP
ownership/bounds test applies the actual baseline, fitness, API-key, and gym
migrations instead of handwritten table definitions.

A forcibly terminated test process can leave a container behind; remove it
with `docker rm -f <container-id>`. Running the test process inside another
container is not configured by these commands: that would require access to
the Docker daemon and networking to reach the database on the Docker host.
