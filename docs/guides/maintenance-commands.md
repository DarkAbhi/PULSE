# Maintenance commands

Routine build, run, migration and test commands are listed in the [root README](../../README.md#commands). These helpers have narrower maintenance or CI purposes.

| Command | Purpose / prerequisites |
| --- | --- |
| `python3 scripts/export-dev-seed.py --env-file .env.dev` | Export the anonymized fixture; default env file is `.env.dev`. Requires `psql`; see [seed data](development-seed.md). |
| `python3 scripts/build-vehicle-catalog.py` | Regenerate the bundled vehicle catalog from the vendored VehiclesDB source. Review the generated data before committing; see [catalog provenance and maintenance](../development-history/features/vehicle-catalog.md#source-and-maintenance). |
| `timeout 90s bash scripts/wait-android-device.sh` | Run from root with `ANDROID_SERIAL` set; wait for stable transport/boot readiness. Requires GNU `timeout`. |
| `bash ../scripts/prepare-android-emulator.sh [report-directory]` | Run from `pulse-android/` with `ANDROID_SERIAL` set; changes test-device settings. |
| `bash ../scripts/run-android-instrumentation.sh` | Run from `pulse-android/`; requires the workflow's emulator, `ANDROID_SERIAL` and environment. |
| `bash ../scripts/run-android-maestro.sh` | Run from `pulse-android/`; requires installed APK, Maestro, Java 21, `ANDROID_SERIAL` and workflow environment. |

Catalog generation rewrites the bundled seed and its existing migration. Use it only to reproduce the current snapshot; a data upgrade needs a new versioned migration. See [VehiclesDB maintenance](../../go-backend/data/vehiclesdb/README.md).

The authoritative helper arguments/environment are in [scripts](../../scripts/) and the [Tests workflow](../../.github/workflows/tests.yml). These are CI utilities, not required setup steps. For regular local device/smoke testing, use the Gradle and Maestro commands in the [Android README](../../pulse-android/README.md) and [Maestro guide](../../maestro/README.md).
