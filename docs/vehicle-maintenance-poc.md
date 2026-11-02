# Vehicle maintenance proof of concept

The feature supports manual structured work entry and per-vehicle reminder settings on Android, web and
Hermes MCP. The same PostgreSQL records power component history and upcoming
maintenance estimates. Existing invoice attachments remain private evidence
linked to the service; no automated extraction is enabled.

## Try it

1. Apply migration `00033_vehicle_maintenance_schedules` (after migration 32) using the existing migration
   workflow, then deploy the Go API before the updated clients.
2. Open a vehicle's maintenance tab, create/edit a service, and choose **Add work
   performed**. Record each component and action. Amount can be zero for free/DIY
   work. Date and odometer describe the visit, not the upload.
3. Use **Engine oil / Replaced** for an oil change and **General service /
   Completed** for a full service. A top-up, inspection, adjustment, or oil-only
   visit does not reset either unrelated schedule. Custom components are allowed.
4. Save, then attach the invoice through the existing attachment controls. Original
   wording belongs in the action description. Requested/estimated work belongs in
   notes until completion is confirmed.
5. Search work in the maintenance tab, or inspect **Coming up** in Overview.
   Tire pressure targets/actions remain in **Tires & air**.
6. Add `get_vehicle_maintenance` and `record_vehicle_maintenance` to the deployed
   Hermes tool allowlist and reload the connection (see `hermes-mcp/README.md`).
   Ask “When did I last replace the clutch cable?” or “What is due next?” Hermes
   resolves the vehicle ID, uses an exact component/action filter, and cites the
   returned service date, odometer and record/attachment. Writes require explicit
   confirmation of the full entry. Do not retry an uncertain create blindly.

## Contract

Maintenance create/update accept `actions`, an array of
`{component, action, description}`. At most 100 actions, no duplicate
component/action pairs. Component keys match `[a-z][a-z0-9_]{0,63}`; descriptions
are at most 500 UTF-8 bytes. Actions are `replaced`, `inspected`, `adjusted`,
`repaired`, `topped_up`, `cleaned`, `lubricated`, and `completed`.

Omitted/null actions preserve work during updates by old clients; `[]` explicitly
clears work. An omitted service date is preserved during edits. Parent and children save atomically. Action rows cascade with their
parent and have an owner-safe foreign key. Existing records are not classified or
backfilled from titles. Dates, odometers, providers and invoice attachments are
inherited from the service; action amounts and separate invoice line tables are
not needed for this POC.

`GET /api/vehicles/{id}/maintenance-history?component=engine_oil&action=replaced`
returns up to 50 matching services, newest service date then record ID first,
including their actions and attachment metadata. A missing match means **no
recorded work**, not that it never happened. Future-dated services are excluded
from this lookup and upcoming baselines.

The existing history endpoint adds `coming_up`. Its entries contain kind, status,
due date, signed remaining days/km, elapsed interval progress (0..1), last action
date, last recorded mileage, and active interval/source. Status is `no_history`, `needs_odometer`,
`upcoming`, `due`, or `overdue`. The more elapsed of time and recorded mileage
determines progress. No travel-rate prediction is made. Missing/inconsistent
mileage falls back to time only. Calendar calculations use India time and clamp
month ends. Editing/deleting records recalculates estimates on refresh.

## Initial estimates

| Task | Motorcycle/moped (including scooters) | Car |
| --- | --- | --- |
| Oil | 3,000 km or 4 calendar months | 10,000 km or 9 calendar months |
| General service | 5,000 km or 6 calendar months | 10,000 km or 12 calendar months |
| Pressure | 14 days after last air fill | 30 days after last air fill |

Whichever threshold comes first applies. Electric-only vehicles omit engine oil.
Other/unknown kinds have only a pressure app default; personal/manual oil and service intervals can still be configured. Existing air notifications use the selected pressure schedule (14/30 days when App default is active) and ignore fills superseded by newer fills. Oil/service
push notifications are not added. The UI labels the active source and mileage as based on the last recorded reading. Only the source selected by the user applies. These defaults do not automatically override personal or manual values.

## Personal and manual reminder schedules

Each vehicle/task has an **App default**, **My intervals**, or **Vehicle manual**
active source. Both personal and manual sets stay saved when switching. They are
never combined: if manual is active, an earlier personal limit does not affect the
reminder. Within the active set, either threshold being reached makes work due.

Use **Reminder settings** below Coming up. Enter a positive whole distance (km),
a duration in days or calendar months, or both. Leave an unused threshold blank.
The active personal/manual set needs at least one threshold. The optional manual
reference can name the booklet/page. Manual values are user-entered and are not
independently verified by Pulse. Defaults are read-only and remain available as a
fallback selection. Engine-oil settings are hidden for electric-only vehicles.

`PUT /api/vehicles/{id}/maintenance-schedules/{kind}` accepts:

```json
{
  "active_source": "manual",
  "personal": {"kilometers": 3000, "duration": 4, "unit": "months"},
  "manual": {"kilometers": 5000, "duration": 6, "unit": "months"},
  "manual_reference": "Owner manual, page 42"
}
```

These are illustrative inputs, not a recommendation for a specific model. Zero
represents an unused threshold in the API. Send both complete sets on a PUT.
Tasks are `engine_oil`, `general_service`, and `tire_pressure`. Owner-scoped saved
settings are included in `maintenance_schedules` on the existing history response.
Bounds are 1,000,000 km, 1,200 months or 36,500 days.

Air fills now accept an optional `odometer_km` and return it in history. Android
and web allow entering it when recording a fill. A distance-only rule with no
usable task baseline/current reading shows **Record an odometer** instead of an
invented due date. With a time threshold, the time estimate remains available and
the UI explains that the distance limit cannot yet be evaluated. No current
odometer or travel rate is inferred from elapsed time.

Existing tire notifications use the selected source's thresholds. Changing an
effective pressure interval dismisses old active pressure alerts and clears only
the latest fill's sent marker so it can be reevaluated. Saving unchanged active
intervals or editing only an inactive set does not restart notification delivery.
Oil and general-service push notifications remain out of scope.

Hermes can read the selected intervals using `get_vehicle_maintenance` and update
both saved sets using `configure_vehicle_reminder`, after explicit confirmation.
Add this tool to existing allowlists and reload Hermes.

## Next increment

Add invoice import drafts and extraction review on top of these action rows:
embedded PDF text first, OCR for scans, original page/line evidence, and explicit
review before saving. The per-vehicle schedule table is already implemented. Neither schedules nor extracted work require a column for each component. Durable import
jobs, file deduplication, audit revisions, Telegram file transfer and AI provider
configuration belong to that increment, not this manual POC. Invoice content must
always be treated as untrusted data, never instructions.

The September Himalayan pre-invoice has not been imported into any account.
Its service completion/date must be confirmed before it becomes recorded history.

## Verification

- Vehicle and notification Go package tests pass against temporary PostgreSQL,
  including migration 32, ownership, action roundtrips, omitted-action/date
  preservation, explicit clearing, free maintenance, and deletion cascade.
- Reminder tests cover top-up versus replacement, oil-only versus full service,
  mileage/time thresholds, missing/future/regressing data, electric vehicles,
  unsupported kinds, capped progress, and calendar month ends.
- 102 Android unit tests pass across domain, data and garage modules. The garage
  lint check and debug app/test APK builds pass.
- Three focused device tests pass on an isolated API 35 phone emulator: Coming up
  and tire-tab navigation, maintenance action entry with activity recreation, and
  the existing vehicle/fuel/pressure/air/maintenance workflow. The Coming up screen
  was visually checked in light and dark mode.
- Web TypeScript and all 19 existing Node tests pass. The production build passes
  with `next build --webpack`; the default Turbopack build hit a local worker-port
  restriction in this execution environment.
- MCP tests verify read filters, input validation, confirmation, service dates,
  and tool registration before server startup.

No production deployment, live Telegram session, physical-device/TalkBack testing,
or invoice extraction was performed.


## Configurable schedules verification

Migration 33, selected-source isolation, retained inactive values, invalid inputs,
owner isolation, mileage-only and time-only calculations, stale-alert dismissal,
and selected tire notification thresholds are covered by passing Go tests.
The updated Android modules pass 103 unit tests, garage lint, and debug app/test
builds. Three focused API 35 emulator checks pass, including both-source entry and
active-source restoration through activity recreation, and recording an air-fill
odometer. Web TypeScript, the 19 existing web tests, and the Webpack production
build pass. MCP tests cover confirmation and preservation of both source sets.
These changes have not been deployed or tested in a live Telegram session.
