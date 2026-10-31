# Garage vehicle catalog

Pulse bundles VehiclesDB release **2026.10.4**, 15,068 models, for an offline
Kind → Make → Model picker. The Go API and web client use `kind`: `car`,
`motorcycle`, `moped`, `van`, `truck`, or `bus`. The Android picker and identity editor use this contract too; see
[Android vehicle catalog](../pulse-android/docs/garage-vehicle-catalog.md).

## Install and upgrade

Run the normal `go run ./cmd/api --migrate-up` from `go-backend`, or the existing
Docker migration service. Migration 27 creates the catalog and vehicle identity
columns; migration 28 inserts the bundled snapshot. Both development and
production Compose already wait for migrations before starting the API.

`--migrate-up` also reruns the conflict-safe bundled seed after migrations. This
repairs missing reference rows on existing databases, without duplicating rows,
editing garage vehicles, or requiring network access. A failed import rolls back
and returns an error to the startup dependency. Running the API directly still
requires running migrations first, as before.

The production reference catalog is independent of the development demo seed.
The latter accepts schema version 28 and leaves the reference catalog intact.

Existing vehicles keep their nickname, fuel choices, pressures, and history. They
receive `kind: "unknown"` and no catalog selection, because a two/four-wheeler
grouping cannot identify a precise kind. Use **Edit vehicle** to classify or link
them. `unknown` is a response-only migration state, not a valid new selection.
The historical `vehicle_type` database column is retained for schema rollback and
other backends; the Go/web API no longer reads or writes that field.

## API contract

All endpoints require the existing authenticated session/API authentication.

* `GET /api/vehicle-catalog/kinds`
* `GET /api/vehicle-catalog/makes?kind=car&q=honda`
* `GET /api/vehicle-catalog/models?kind=car&make_slug=honda&q=civic`

Each returns `[{"id":"…","name":"…"}]`. Makes return make slugs; models return
the stable `kind/make_slug/model_slug` ID. Searches are literal, case-insensitive,
limited to 100 characters and 100 results, ordered by name then ID. Search again
to narrow a full result set. Model searches also include published aliases.

Create with `POST /api/vehicles`:

```json
{
  "name": "My Civic",
  "kind": "car",
  "catalog_id": "car/honda/civic",
  "fuel_types": ["petrol"]
}
```

The server resolves canonical make/model names and validates the model's kind.
Submitted display labels cannot override catalog names. Personal nicknames remain
independent. Manual creation omits `catalog_id` (or sends `""`) and may include
`make_name` and `model_name`, up to 200 characters each. Fuel configuration and
tire pressure are entered by the user; this dataset does not contain these specs.

Vehicle list, detail, update, tire-pressure update, and history responses include
`kind`, nullable `catalog_id`, `make_name`, and `model_name`. The old
`vehicle_type` request/response contract has been removed. Fuel and ownership
rules are unchanged.

For `PATCH`/`PUT /api/vehicles/{id}`, omitted identity fields preserve their values.
Send `catalog_id: ""` to clear a link and optionally supply manual make/model
names in the same request. JSON null has the same meaning as omission. Changing
kind while retaining an incompatible catalog selection is rejected. Switching to
another catalog ID resolves its canonical names. Existing `unknown` vehicles may
receive unrelated partial updates without forced classification.

## Source and maintenance

Source files, manifest, attribution and SHA-256 are in
`go-backend/data/vehiclesdb/`. `python scripts/build-vehicle-catalog.py` generates
the initial seed migration and embedded repair seed from the pinned CSV.
The importer only inserts missing records; it never deletes referenced models.

Do not rewrite released migrations for a future dataset update. Pin and verify a
new snapshot, create a new migration for explicit catalog metadata updates and
new records, reconcile any published `former_ids`, and update the repair seed,
import version and attribution together. Keep old referenced IDs or migrate their
references transactionally before retiring them. Catalog updates must never
silently relink personal vehicles.

The source notices are published at `/vehicle-data`; the picker and garage pages
include visible VehiclesDB credit. India is not a source country in this release;
global search and manual entry remain available without country filtering.

## Verification

* Backend: `go test -race -count=1 -shuffle=on -tags=integration -timeout=20m ./...`
  from `go-backend` with Docker running. New integration cases cover an upgrade
  from schema 26, repeated seeding and repair, catalog search and validation,
  canonical labels, manual unlinking, partial updates and ownership.
* Web: `npm test` and `npm run build` from `life-tracker-frontend`.
* Browser: `maestro test --headless maestro/garage-add-vehicle.yaml` against the
  disposable browser-test stack; this preserves manual-entry coverage.
* Seed exporter: `python -m unittest discover -v -s scripts -p test_export_dev_seed.py`.

Rolling back migration 28 retains reference data deliberately. Rolling back 27
removes the new identity fields and catalog; do that only when removing the
feature and reverting its clients. Personal vehicle history remains intact.
