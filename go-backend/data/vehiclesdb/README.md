# VehiclesDB snapshot

Release: **v2026.10.4** (2026-10-12), 15,068 model records.

Source: https://github.com/vehiclesdb/vehiclesdb/tree/v2026.10.4

`vehicles.csv`, `manifest.json`, and `ATTRIBUTION.md` are unmodified files from
that pinned release. `SHA256SUMS` records the CSV checksum. Vehicle identity and
search data are transformed into `seed.sql` and migration 28 by
`python scripts/build-vehicle-catalog.py`. Pulse makes no claim to supply model
specifications, country-specific completeness, or vehicle images.

The data is licensed under CC BY 4.0:
https://creativecommons.org/licenses/by/4.0/

Vehicle data by VehiclesDB: https://vehiclesdb.com

See `ATTRIBUTION.md` for all upstream register notices; preserve them when
redistributing. Pulse displays the notices at `/vehicle-data`.

See `docs/vehicle-catalog.md` in the repository root for the import lifecycle,
API contract, upgrade instructions, and tests. Do not regenerate an already
released migration to upgrade data; add a new versioned migration instead.
