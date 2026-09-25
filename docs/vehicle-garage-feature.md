# Vehicle Garage Feature

## 1. Feature Overview

The Vehicle Garage is a personal vehicle-management area inside Life Tracker. It gives a user one place to register vehicles, record recurring vehicle-care events, and review the operating and financial history of each vehicle.

The feature is organized around two screens:

- **Garage inventory** at `/garage`: a list of vehicles with a quick air-fill status and shortcuts to record air or fuel events.
- **Vehicle history** at `/garage/[id]`: a detailed record for one vehicle, including fuel economy, fuel fill-ups, tire-pressure targets, air-fill history, and maintenance expenses.

The feature is designed for lightweight, repeated logging rather than a full vehicle-identity registry. A vehicle currently has a name and operational tracking data; richer metadata such as make, model, registration, or odometer profile is not part of the visible product flow.

## 2. Product Perspective

### 2.1 Garage inventory

A signed-in user can:

- Open the Garage from the dashboard.
- See all registered vehicles as individual vehicle cards.
- Add a vehicle through a modal using a required name.
- Open a vehicle to view its history.
- See the latest recorded air-fill time for each vehicle.
- Mark that air was filled immediately.
- Add a fuel entry without leaving the vehicle list.
- See a useful empty state when no vehicles exist.
- See an error state when the vehicle list or server cannot be loaded.

The garage list keeps frequent actions close to the vehicle card. Marking air filled uses a confirmation dialog because it records an event immediately. Adding fuel opens a form so that the user can capture the odometer reading, date and time, station/vendor, notes, and one or two fuel tanks.

### 2.2 Vehicle history

The vehicle history page brings all important records for one vehicle together:

- Vehicle name and navigation back to the Garage.
- Fuel fill-up history, ordered newest first.
- Average mileage in km/L by fuel type.
- Air-fill history, ordered newest first.
- Target tire pressures for solo riding and riding with a pillion.
- Maintenance and expense history, ordered newest first.

The page provides empty states for each history type, so a newly created vehicle remains useful before any records have been entered.

### 2.3 Fuel tracking

A fuel entry captures:

- Odometer in kilometres.
- Date and time of the fill.
- Fuel station or vendor, optionally.
- Notes, optionally.
- One or two fuel items.
- Fuel type: petrol, diesel, LPG, CNG, or electric.
- Fill type: full tank, partial fill-up, or missed fill-up.
- Any two of quantity, price per litre, and total cost.

The missing third value is calculated automatically. This lets the user enter the values available on a receipt without doing the arithmetic manually.

Existing fuel records can be edited or deleted from the vehicle history page. The displayed record includes the odometer, timestamp, station, fuel details, quantity, and total cost.

### 2.4 Mileage insight

The history page displays average mileage per fuel type when enough reliable data exists. The product intentionally avoids presenting a misleading number when there are not two comparable full-tank readings.

The calculation model is:

- A full fill establishes a baseline odometer reading.
- Partial fills between full fills contribute their quantities to the interval.
- The next full fill closes the interval and contributes distance divided by fuel consumed.
- Missed fills reset the calculation because the consumed fuel is unknown.
- Multiple completed intervals are combined into a weighted average by fuel type.

When no complete interval exists, the UI tells the user to add two full-tank entries.

### 2.5 Air-fill tracking and reminders

Air tracking is deliberately quick:

- The user can mark air as filled from the garage card.
- The timestamp is recorded immediately.
- The garage card displays the latest fill time.
- The vehicle history page lists every air-fill event.
- Individual air-fill records can be deleted from vehicle history.
- The system schedules a reminder after 30 days.

The reminder points back to `/garage` and includes the vehicle name, allowing the user to return to the general garage view and decide what to do next.

### 2.6 Tire-pressure targets

A vehicle can store target pressure values for two riding contexts:

- Front and rear pressure for solo riding.
- Front and rear pressure when carrying a pillion.

The vehicle history page displays the configured values in PSI and allows them to be set or edited in a dedicated modal. Each value is optional, but negative values are rejected.

### 2.7 Maintenance and expenses

A user can add, edit, and delete vehicle maintenance or expense records. Supported categories are:

- Service
- Repair
- Insurance
- Washing
- Tyres

Each record can include:

- Category.
- Title.
- Amount in Indian rupees.
- Date of occurrence.
- Optional odometer reading.
- Optional provider or service centre.
- Optional notes.

The history page displays the category, date, odometer when available, amount, provider, and notes. This makes the vehicle page useful both as a service log and as a running cost history.

### 2.8 Receipts and service bills

A saved maintenance record can have file attachments. The current flow requires the record to be saved first, after which the user can:

- Upload a receipt or service bill.
- View/download an attachment.
- Remove an attachment.

Attachments are private, have a maximum size of 10 MB, and are presented by file name in the maintenance modal.

## 3. End-to-End Technical Implementation

### 3.1 Frontend architecture

The frontend is a Next.js App Router implementation under `life-tracker-frontend/app/garage`.

- `app/garage/page.tsx` is a server-rendered page. It validates the incoming session by forwarding the request cookies to `/api/auth/session`, then loads vehicles and their latest air-fill timestamps.
- `app/garage/vehicles-list.tsx` is a client component for card interactions, confirmation dialogs, fuel entry state, pending states, and local optimistic display of the newly recorded air-fill timestamp.
- `app/garage/[id]/page.tsx` is a server-rendered detail page. It loads `/api/vehicles/{id}/history`, derives the selected edit state from URL search parameters, and renders the history sections.
- Small client components own focused interactions such as tire-pressure editing, fuel editing, maintenance editing, attachment uploads, and record deletion.
- Shared design-system controls provide buttons, dialogs, confirmation dialogs, and form presentation.
- `LocalDate` is used for browser-local date/time presentation while API payloads use ISO timestamps.

### 3.2 Mutations and cache refresh

Most mutations use React Server Functions in `actions.ts` files:

- Vehicle creation calls `POST /api/vehicles` and revalidates `/garage`.
- Air-fill creation calls `POST /api/vehicles/{id}/air-fills` and revalidates `/garage`.
- Fuel creation calls `POST /api/vehicles/{id}/fuel-fillups` and revalidates both `/garage` and `/garage/{id}`.
- Fuel update/delete, maintenance create/update/delete, and tire-pressure updates call their corresponding detail endpoints and revalidate `/garage/{id}`.
- After a successful mutation, dialogs close or the local form state resets.
- Server errors are converted into user-facing form or alert messages rather than being silently ignored.

Attachment upload and deletion currently use browser-side `fetch` with `credentials: "include"`, followed by `router.refresh()` and local attachment state updates. This is separate from the Server Function pattern used by most JSON mutations because attachments are multipart or file-download operations.

### 3.3 Backend feature slice

The backend implementation is grouped under `go-backend/internal/vehicle`:

- `routes.go` registers the vehicle API surface with Chi.
- `vehicle.go` handles vehicle CRUD, history assembly, DTO conversion, tire-pressure payloads, and air/fuel record deletion.
- `service.go` contains vehicle validation and the service/store boundary for vehicle CRUD and pressure updates.
- `fuel.go` handles fuel input normalization, transactional writes, validation, editing, and mileage calculations.
- `air_fill.go` handles air-fill creation and latest-air-fill listing.
- `maintenance.go` handles maintenance record validation and CRUD.
- `maintenance_attachments.go` handles multipart upload, private object storage, download redirects, and cleanup.
- `reminder.go` runs the periodic air-fill reminder job.
- `query/` contains SQLC-generated query code and the hand-authored SQL query definitions.
- `vehicle_test.go` and `fuel_mileage_test.go` cover API flows and mileage behavior.

The feature is wired explicitly in `cmd/api/router.go` and `cmd/api/main.go`, where the vehicle handler, attachment configuration, and hourly reminder job are initialized.

### 3.4 API surface

The vehicle routes are registered below the API router and include:

| Method         | Route                                                                      | Purpose                                  |
| -------------- | -------------------------------------------------------------------------- | ---------------------------------------- |
| `GET`          | `/vehicles`                                                                | List vehicle IDs and names               |
| `POST`         | `/vehicles`                                                                | Create a vehicle                         |
| `GET`          | `/vehicle-air-fills/latest`                                                | Get the latest air fill for each vehicle |
| `GET`          | `/vehicles/{id}/`                                                          | Get a vehicle                            |
| `PUT`, `PATCH` | `/vehicles/{id}/`                                                          | Update vehicle properties                |
| `DELETE`       | `/vehicles/{id}/`                                                          | Delete a vehicle                         |
| `PUT`, `PATCH` | `/vehicles/{id}/tire-pressure`                                             | Update tire-pressure targets             |
| `POST`         | `/vehicles/{id}/air-fills`                                                 | Record an air fill                       |
| `DELETE`       | `/vehicles/{id}/air-fills/{airFillID}`                                     | Delete an air-fill record                |
| `POST`         | `/vehicles/{id}/fuel-fillups`                                              | Create a fuel fill-up                    |
| `PUT`          | `/vehicles/{id}/fuel-fillups/{fillupID}`                                   | Edit a fuel fill-up                      |
| `DELETE`       | `/vehicles/{id}/fuel-fillups/{fillupID}`                                   | Delete a fuel fill-up                    |
| `POST`         | `/vehicles/{id}/maintenance-records`                                       | Create a maintenance record              |
| `PUT`          | `/vehicles/{id}/maintenance-records/{recordID}`                            | Edit a maintenance record                |
| `DELETE`       | `/vehicles/{id}/maintenance-records/{recordID}`                            | Delete a maintenance record              |
| `POST`         | `/vehicles/{id}/maintenance-records/{recordID}/attachments`                | Upload an attachment                     |
| `GET`          | `/vehicles/{id}/maintenance-records/{recordID}/attachments/{attachmentID}` | Download an attachment                   |
| `DELETE`       | `/vehicles/{id}/maintenance-records/{recordID}/attachments/{attachmentID}` | Delete an attachment                     |
| `GET`          | `/vehicles/{id}/history`                                                   | Load the complete vehicle history        |

The frontend currently consumes the list, create, history, tire-pressure, air-fill, fuel, maintenance, and attachment routes. Vehicle update and vehicle delete are available in the backend API but are not currently exposed as controls in the visible Garage UI.

### 3.5 Validation and consistency rules

The backend is the final authority for validation even when the frontend provides HTML constraints:

- Vehicle names are required when creating a vehicle.
- Tire pressures and odometers cannot be negative.
- Fuel entries require one or two items.
- Fuel type and fill type must be from the supported enumerations.
- At least two of quantity, unit price, and total cost must be supplied for each fuel item.
- All calculated fuel values must be positive.
- A new fuel odometer cannot be lower than the vehicle's previous fuel odometer.
- Maintenance categories must be supported.
- Maintenance titles and provider names are limited to 160 characters.
- Maintenance amounts must be greater than zero.
- Maintenance attachment files must be between 1 byte and 10 MB and must have a valid file name.
- Record mutations include vehicle, record, and user identifiers in their SQL predicates where ownership is relevant.

### 3.6 Persistence model

The PostgreSQL schema uses separate tables for the different kinds of vehicle history:

- `vehicles` stores the vehicle name, active flag, and timestamps.
- `vehicle_air_fills` stores air-fill events and the notification ID used to prevent duplicate reminders.
- `vehicle_fuel_fillups` stores the event-level odometer, timestamp, station, and notes.
- `vehicle_fuel_items` stores one or two fuel-type line items for each fill-up.
- `vehicle_maintenance_records` stores categorized expenses and service history.
- `vehicle_maintenance_attachments` stores attachment metadata and the private object-storage key.

Foreign keys cascade vehicle deletion into related records. Query indexes support latest air fills, chronological fuel history, due reminders, maintenance history, and attachment listing.

The data access layer uses `pgx/v5` and SQLC-generated query methods. There is no ORM in the vehicle slice. Fuel creation and editing use database transactions so the parent fill-up and its child fuel items are committed together.

### 3.7 Air reminders and notifications

When an air-fill record becomes due, the background job:

1. Finds air fills older than 30 days that do not yet have a reminder notification.
2. Locks the due record in a transaction.
3. Creates a notification through the notification feature's consumer-defined interface.
4. Stores the notification ID on the air-fill row.
5. Commits both operations together.

The job runs once at startup and then hourly. The notification uses the `Garage` source, includes the vehicle name, points to `/garage`, and records the vehicle ID in metadata.

### 3.8 Attachment storage and security

Maintenance files are stored outside PostgreSQL in an S3-compatible bucket. PostgreSQL retains the ownership-linked metadata and storage key.

The backend protects the attachment lifecycle by:

- Checking the signed-in user.
- Verifying that the maintenance record belongs to the requested vehicle and user.
- Generating a namespaced storage key containing the user, vehicle, and maintenance record IDs.
- Removing the object if metadata insertion fails after upload.
- Deleting the object before deleting its metadata.
- Returning a five-minute presigned download URL rather than exposing the bucket directly.

Attachment support is optional at startup: if no bucket is configured, the upload/download capability reports that S3 uploads are not configured.

## 4. Critical Implementation Notes

- **Authentication boundary:** the frontend validates the session on both garage pages and forwards the browser cookie to the backend. Record-specific handlers perform explicit session checks. The vehicle list/create service itself is not visibly user-scoped in the current schema: `vehicles` has no `user_id`, and `ListVehicles` queries all rows. This is important if the application will support multiple users with private vehicle inventories.
- **Product/API parity:** the backend supports vehicle update and deletion, but the current frontend does not provide controls for those operations. The documented product flow should therefore be understood as the currently shipped UI, while the route table describes the broader API capability.
- **Transactional integrity:** fuel parent and child rows are written in one transaction, and reminder creation plus reminder marking are also atomic. This prevents partially saved fuel records and duplicate reminder processing under normal concurrent execution.
- **Calculation correctness:** mileage is intentionally based on complete full-to-full intervals and is reset by missed fills. The result is more conservative than a simple average of displayed fill-ups and is appropriate for mixed partial/full records.
- **Data deletion behavior:** database foreign keys cascade related history when a vehicle is deleted. Maintenance deletion additionally removes object-storage files before removing the database record.
- **Frontend freshness:** route revalidation keeps server-rendered pages authoritative after mutations. The air-fill card also updates its local timestamp immediately after success, while the detail page is refreshed through its normal server-rendered data path.

## 5. Source Map

- [Garage page](../life-tracker-frontend/app/garage/page.tsx)
- [Vehicle list and quick actions](../life-tracker-frontend/app/garage/vehicles-list.tsx)
- [Vehicle history page](../life-tracker-frontend/app/garage/[id]/page.tsx)
- [Garage server actions](../life-tracker-frontend/app/garage/actions.ts)
- [Vehicle history actions](../life-tracker-frontend/app/garage/[id]/actions.ts)
- [Fuel form](../life-tracker-frontend/app/components/fuel-form.tsx)
- [Maintenance modal and attachments](../life-tracker-frontend/app/garage/[id]/maintenance-record-modal.tsx)
- [Vehicle routes](../go-backend/internal/vehicle/routes.go)
- [Vehicle handlers and history assembly](../go-backend/internal/vehicle/vehicle.go)
- [Fuel handlers and mileage calculation](../go-backend/internal/vehicle/fuel.go)
- [Maintenance handlers](../go-backend/internal/vehicle/maintenance.go)
- [Attachment storage](../go-backend/internal/vehicle/maintenance_attachments.go)
- [Air-fill reminder job](../go-backend/internal/vehicle/reminder.go)
- [Vehicle SQL queries](../go-backend/internal/vehicle/query/vehicles.sql)
- [Fuel SQL queries](../go-backend/internal/vehicle/query/vehicle_fuel.sql)
- [Initial vehicle schema](../go-backend/migrations/00001_init.up.sql)
- [Maintenance schema](../go-backend/migrations/00010_vehicle_maintenance_records.up.sql)
- [Attachment schema](../go-backend/migrations/00011_vehicle_maintenance_attachments.up.sql)
