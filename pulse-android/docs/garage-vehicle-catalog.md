# Garage vehicle selection

The Android garage uses the same backend-owned VehiclesDB catalog as the web
client. It sends `kind`, `catalog_id`, `make_name`, and `model_name`; it no longer
uses the old two/four-wheeler API contract. Backend migrations 27–28 must be
applied before using the updated app. Android does not import or maintain its own
copy of the vehicle catalog.

## Product flow and design

The existing full-page editor retains its compact phone layout, tablet artwork,
optional tire pressures, fuel selection, and centered cancel action. Users choose
**Choose a vehicle** or **Enter manually**, then select one of six kinds: car,
motorcycle, moped, van, truck, or bus. Catalog entry progressively reveals make
and model searches. Selecting a model suggests a vehicle name; personal nicknames
remain editable. Manual entry has optional make/model fields.

Changing kind clears the selected make and model. Changing make clears the model.
Search does not silently change a selection or nickname. Up to 100 matches appear
in the shared selection menu, with guidance to narrow a full list. Loading,
retry, and no-results states keep manual entry available. Catalog selection is
required before saving in catalog mode.

Vehicle cards show make/model information. The overview includes a full-width
vehicle identity card and **Edit vehicle**. Existing `kind: "unknown"` vehicles
still load and retain their names/history; editing lets the user classify them.
Switching to manual entry clears the catalog reference explicitly on save.

All new controls use the existing Pulse components, colors, typography, spacing,
and adaptive editor. No new UI framework, theme, navigation destination, or
runtime dependency was introduced. Optional pressures, fuel, maintenance, and
receipt flows remain in the existing garage screens.

## Implementation boundaries

* `core:network`: `GarageApi.catalog` and `VehicleCatalogOptionDto` use the shared
  authenticated Ktor client. Vehicle/history DTOs carry the new identity fields.
* `core:domain`: `VehicleCatalogRepository` and `SearchVehicleCatalog` expose
  catalog searches without Android/Ktor dependencies. `GarageChange.SaveVehicle`
  supports both creation and identity edits.
* `core:data`: `PulseGarageRepository` maps catalog options and serializes vehicle
  writes. Updates use the existing PUT endpoint, explicitly clear a manual
  vehicle's `catalog_id` with an empty string, and omit `is_active` so editing
  identity does not reactivate a vehicle.
* `feature:garage`: the ViewModel debounces searches by 250ms, cancels replaced
  searches, and verifies their request keys before accepting results. Search
  terms and selected IDs/labels live in the existing saved draft. Recreation
  reloads options without replaying writes. Existing uncertain-write review and
  discard protection remain in place.

List/history mapping accepts `unknown` solely for migrated vehicles; new writes
require one of the six supported kinds. The backend remains authoritative for
catalog membership and canonical make/model names.

## Data credits

The editor and identity card visibly credit VehiclesDB. **Sources and credits**
opens the bundled release attribution notices without a network request. The
snapshot version is 2026.10.4, licensed under CC BY 4.0. The notices are copied
from `go-backend/data/vehiclesdb/ATTRIBUTION.md` into the garage raw resources.
Update both copies and the displayed version together when upgrading the data.
Fuel configuration and tire specifications remain user-entered.

## Verification

Run from `pulse-android` with the configured Android/JDK toolchains:

```sh
./gradlew :core:domain:test :core:network:testDebugUnitTest :core:data:testDebugUnitTest :feature:garage:testDebugUnitTest :app:testDebugUnitTest
./gradlew :core:domain:ktlintCheck :core:network:ktlintCheck :core:data:ktlintCheck :feature:garage:ktlintCheck :app:ktlintAndroidTestSourceSetCheck
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.abhishekan.pulse.GarageDeviceTest
```

Unit coverage includes query encoding, response identity mapping, migrated
vehicles, explicit unlinking, preserving active state, debouncing, retry/manual
fallback, dependent-field resets, and saved catalog drafts. Device coverage adds
selection and nickname editing across recreation, failed-search recovery, manual
entry, classifying existing vehicles, and the existing garage regression flows.
Test screenshots use the existing `pulse-phase5-*` naming in device Downloads.

See the repository's `docs/vehicle-catalog.md` for backend seeding, provenance,
rollback behavior, and API details.
