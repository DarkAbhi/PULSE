# Garage vehicle selection

The Android garage uses the same backend-owned VehiclesDB catalog as the web
client. It sends `kind`, `catalog_id`, `make_name`, and `model_name`; it no longer
uses the old two/four-wheeler API contract. Backend migrations 27â€“28 must be
applied before using the updated app. Android does not import or maintain its own
copy of the vehicle catalog.

## Product flow and design

The full-page editor guides users through three steps: Vehicle, Fuel, and optional
Tyre pressure. Each step uses Pulse components and theme tokens. Tablet layouts
keep the illustrated introduction and live vehicle summary on the left; compact
and large-text layouts stack the content. Existing scooter/motorcycle/car artwork
continues cycling until the user explicitly selects a kind. Kind and fuel start
unselected. New vector icons, a shared `PulseChoiceCard`, and a theme-aware axle
illustration support the flow without additional raster assets or dependencies.

Users choose **Choose a vehicle** or **Enter manually**, then select one of six
kinds: car, motorcycle, moped, van, truck, or bus. Catalog selection suggests a
vehicle name; manual make/model edits also suggest a name while preserving personal
nicknames. Make and model appear side by side where space permits. The searchable
picker sheets show monograms, highlighted selection, bounded result counts,
loading/retry/empty states, and Close. They deliberately omit Popular and the
alphabet rail.

Step progression requires a kind, a name, and a catalog model in catalog mode.
Fuel requires one type, or two distinct types when dual fuel is enabled. Pressure
fields stay blank until entered, support direct decimal entry and +/- controls,
and show front/rear targets for larger vehicles plus solo/pillion targets for
motorcycles and mopeds. Existing pillion values remain editable after reclassification.
The final review links back to vehicle details. Step state survives recreation;
Back moves to the preceding step, while Cancel and Back to garage retain discard
protection. Moving between steps never sends a write.

Year is intentionally omitted because the API does not store it.

Changing kind clears the selected make and model. Changing make clears the model.
Search does not silently change a selection or nickname. Tapping the Make or
Model field opens a Pulse-styled bottom sheet with its search input above a
scrollable list of options. Filtering uses the existing debounced backend search;
up to 100 matches appear, with guidance to narrow a full list. Selecting a row
closes the sheet. Close, Back, or scrim dismissal leaves the selection unchanged,
and search text is retained when reopening. Sheet visibility also survives
Activity recreation. Loading, retry, and no-results states keep manual entry
available. Catalog selection is
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

## Manual catalogue matching

`GET /api/vehicle-catalog/match?kind=car&make_name=Honda&model_name=Civic`
requires the existing authenticated session and returns
`{"match":{"id":"car/honda/civic","make_slug":"honda","make_name":"Honda","model_name":"Civic"}}`.
A unique exact match ignores case and surrounding whitespace. No match or ambiguous
results return `{"match":null}`. Names must contain 1–200 characters; invalid kind
or inputs return 400. Queries are parameterized, bounded to two candidates, and do
not change the vehicle. No new database migration is needed.

Android debounces manual matching by 350ms, cancels superseded requests, clears
stale suggestions, and verifies the request key before accepting results. The
suggestion changes nothing until **Use match** is tapped; acceptance keeps the
nickname and links canonical make/model values. Matching failure offers Retry and
allows manual continuation. Deploy the new Go endpoint with the Android update;
older APIs still permit manual entry but cannot provide suggestions. Existing
migrations 27–28 remain prerequisites. OpenAPI files are regenerated from annotations.
