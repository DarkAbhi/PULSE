# Development seed data

[`go-backend/data/dev-seed.sql`](../../go-backend/data/dev-seed.sql) is a shareable
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

GitHub's seed-database job checks this same fixture in disposable databases. Go and
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
