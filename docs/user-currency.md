# Account currency

Users choose one currency in Profile on web and Android. The same preference applies to Financial Horizon, its dashboard card, statement previews, and Garage monetary labels. Monetary displays and amount labels use currency symbols, with codes as a fallback when the platform has no symbol. Currency selectors retain codes and names for clarity. Changing currency relabels existing amounts without converting them; no exchange rates or per-transaction currencies are introduced.

## API and storage

`GET /api/profile` includes `currency` even before display-name onboarding. `PUT /api/profile/currency` accepts `{ "currency": "INR" }` and returns the saved code. It uses the authenticated account and rejects unsupported, lowercase, blank, or symbol values with 400. Name and time zone updates preserve currency. The supported codes are defined in `go-backend/internal/currency/currency.go`, with matching frontend choice sets.

Migration `00026_profile_currency` adds a non-null currency preference with a supported-code constraint and GBP default. It backfills recognized legacy Horizon ISO codes and the £, ₹, $, €, and ¥ symbols; unrecognized or missing settings fall back to GBP. Monetary amounts, display names, time zones, and onboarding state are preserved. The down migration copies the profile preference to existing legacy Horizon configs before removing the column.

Apply the migration before deploying the updated backend, then deploy the web and Android clients. The existing Horizon `currency` response remains available and always reflects the profile. Legacy currency fields sent to the Horizon config endpoint are ignored; they cannot override the profile. The legacy config column is retained for rollback compatibility and is no longer authoritative.

## Shared frontend code

Web: `app/lib/currency.mjs` defines choices, labels, validation, and amount formatting; `app/lib/user-currency.ts` reads the account preference during server rendering; `app/components/currency-provider.tsx` exposes reactive reads and confirmed updates to client components. It refreshes on navigation and window focus, guards against stale responses, and resets for signed-out users.

Android: `core:domain` owns `UserCurrency`, including the reactive preference, supported choices, labels, validation, precision-preserving formatting, and reset. `SaveProfileCurrency` validates before calling the shared repository. The network DTO and profile repository implement persistence; authentication loads the preference before showing authenticated screens and resets it on logout or expiry. `core:ui` exposes the shared `currencyText` adapter for Compose. Feature modules consume this shared implementation without depending on each other.

The preference changes display units, not storage precision: existing amount entry and database precision limits are unchanged.

## Verification

- Backend profile, Horizon, and currency tests pass, including the currency endpoint and migration integration test. All backend packages compile with the regenerated sqlc bindings.
- All 14 web tests and TypeScript checks pass. The production build passes in Linux; the Windows Turbopack build hit a CSS worker startup failure.
- Android `build :app:assembleDebugAndroidTest` passes, including unit tests, formatting, lint, debug/release packaging, and test-app compilation. The debug/domain result set contains 207 passing tests.
- The focused API 37 tablet test passes: search for INR, save the preference, recreate the activity, and verify the saved currency is restored. Reviewed the [picker](currency/android-picker.png) and [saved preference](currency/android-saved.png) screenshots.

The migration has been exercised against temporary test databases. Apply it to the deployment database before releasing the backend.
