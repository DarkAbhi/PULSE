# Vehicle maintenance proof of concept

The first version supports manual structured work entry on Android, web and
Hermes MCP. The same PostgreSQL records power component history and upcoming
maintenance estimates. Existing invoice attachments remain private evidence
linked to the service; no automated extraction is enabled.

## Try it

1. Apply migration `00032_maintenance_actions` using the existing migration
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
date, last recorded mileage, and default interval. Status is `no_history`,
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
Other/unknown kinds get pressure reminders only. Existing air notifications use
the same 14/30-day cadence and ignore fills superseded by newer fills. Oil/service
push notifications are not added. The UI labels defaults as estimates and mileage
as based on the last recorded reading. Manufacturer schedules take precedence.

## Next increment

Add invoice import drafts and extraction review on top of these action rows:
embedded PDF text first, OCR for scans, original page/line evidence, and explicit
review before saving. Add per-vehicle manufacturer intervals as a separate
schedule table. Neither requires a column for each component. Durable import
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
