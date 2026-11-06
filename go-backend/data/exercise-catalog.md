# Exercise catalogue

Migration `00023_exercise_catalog` installs 876 exercises from the public-domain
[free-exercise-db](https://github.com/yuhonas/free-exercise-db) dataset at revision
[`f00c92c7dcf1216a928a52c3706c7ce8e2f71ed5`](https://github.com/yuhonas/free-exercise-db/tree/f00c92c7dcf1216a928a52c3706c7ce8e2f71ed5).
The downloaded `dist/exercises.json` SHA-256 is
`5bb747e3fc658f095a60dcbf6d53c96627acdcc6ffb6fffde86f7e26995d40bf`.
The source licence is preserved in `exercise-catalog-LICENSE.md`.

## Deploy

1. Apply the existing development migration command from the repository root:
   `make dev-migrate-up`. The database migration role needs permission to install
   PostgreSQL's `pg_trgm` extension. A database administrator can install it first
   if the migration role cannot.
2. Restart/build the application with `make dev`. Production's `make deploy`
   already applies pending migrations before starting Go and Rust.
3. Add `search_exercises` to Hermes's existing tool allowlist, restart the gateway,
   and send `/reload-mcp` in Telegram (see `hermes-mcp/README.md`).
4. Search for "dumbbell shoulder press" in the exercise form. Select the matching
   variant, save a set, and verify the catalogue name and instructions in history.

The migration contains the data; deployment does not fetch GitHub. It preserves
all original fields in `exercise_catalog.data`, including relative image paths.
Image binaries are not stored in Postgres or required by the form. Resolve paths
against `https://raw.githubusercontent.com/yuhonas/free-exercise-db/f00c92c7dcf1216a928a52c3706c7ce8e2f71ed5/exercises/`
if images are added later.

## API

`GET /api/exercise-catalog?q=<exercise name>` requires the existing Go session
authentication (cookie or bearer session token). It returns:

```json
{
  "query": "dumbbell shoulder press",
  "match_type": "exact",
  "exercise_catalog_id": "Dumbbell_Shoulder_Press",
  "candidates": [{
    "id": "Dumbbell_Shoulder_Press",
    "name": "Dumbbell Shoulder Press",
    "equipment": "dumbbell",
    "match_type": "exact",
    "data": {"primaryMuscles": ["shoulders"], "instructions": ["..."]}
  }]
}
```

Search accepts 1–100 characters and returns up to eight candidates. Match types
are `exact`, `alias`, `ambiguous`, `suggested`, or `none`. Only a unique exact name
or personal alias supplies an automatic ID. Case and whitespace are normalized;
equipment, posture, and movement variants are not collapsed. Similarity ranks
candidates and is not a probability or permission to save a match.

Both existing exercise POST endpoints accept optional `exercise_catalog_id` and
`remember_alias` fields. Without an ID, unique exact/remembered names resolve
automatically; other names remain unlinked. A supplied ID must exist. Remembering
an alias requires an explicit ID and is opt-in, scoped to the authenticated user.
A later confirmed selection can replace that user's alias; it cannot override a
unique exact catalogue name for another exercise. Batch writes include sets and
aliases in one transaction.

Exercise GET/POST responses include nullable `exercise_catalog_id` and
`catalog_exercise` (ID, name, equipment, complete source data), alongside the
original name and sets. Existing clients can keep sending `name` and `sets`.
Existing history is backfilled only for unique exact normalized names. Unmatched
exercises remain valid, with null catalogue fields.

Go owns the shared migrations and gym exercise APIs. Rust's Apple fitness import
uses explicit columns and preserves existing gym visits and their exercises on
repeated imports; it does not implement a second exercise-write API.

## Updates and rollback

For future dataset updates, add a new migration that upserts by the existing
source ID and records its pinned source revision. Keep IDs and referenced entries
stable; do not truncate the catalogue. Review changed exercise definitions before
updating metadata for linked history.

Rolling back migration 23 removes catalogue links and aliases, while keeping
workout names, visits, and sets. It leaves `pg_trgm` installed because other
applications may use the extension. Roll back the application code as well.
