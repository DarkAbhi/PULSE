# Pulse Android

Native Kotlin/Compose app for `dev.abhishekan.pulse`. Phases 1–6 implement the design system, username/password authentication through the existing Go API, encrypted cookie restoration, the dashboard, Fitness, Financial Horizon, Garage, and Meal Plan. Product screens fetch API data; preview and test fixtures are confined to debug/test source sets.

## Setup

Open this directory in Android Studio. Keep `local.properties` local and point `sdk.dir` at the Android SDK. Install platform 37, accept SDK licenses, and use the checked-in wrapper. The daemon configuration selects JDK 25; Java/Kotlin bytecode targets Java 11. Minimum Android API remains 28.

```sh
./gradlew build
./gradlew :app:connectedDebugAndroidTest # emulator/device required
./gradlew :app:installDebug
```

The original AGP 9.4.1, Gradle 9.6.0, Kotlin/Compose compiler 2.2.10, SDK 37, Compose BOM 2026.02.01, application ID, and daemon toolchain are preserved. AGP supplies built-in Kotlin for Android modules; only domain applies the Kotlin JVM plugin. Do not add the legacy Kotlin Android plugin.

All versions live in `gradle/libs.versions.toml`. Compatible stable additions remain Koin 4.1.1, Ktor 3.2.3, coroutines 1.10.2, serialization 1.9.0, DataStore 1.1.7, Lifecycle/Navigation 2.9.3, and Activity 1.10.1. Phase 2 uses the existing dependency set; HTTP cookies require no additional Ktor library.

## Action feedback

Use the design system's `PulseSnackbar` for transient action results. The app scaffold owns the host above bottom navigation; `PulseSnackbarProvider` shares it with feature routes. Routes pass their pending `UiText` to `PulseSnackbarEffect` and clear it with `consumeMessage` when queued. Feedback uses the current language, dismisses automatically, supports manual dismissal, and does not occupy screen content space. Keeping the queue at the app host allows feedback to survive navigation without replaying when returning to a route.

Keep validation errors beside fields and uncertain-change review instructions beside their recovery controls. Do not send those persistent instructions to a transient snackbar.

## Kotlin formatting

Ktlint is applied to every module and the root Gradle scripts. It checks production, debug, unit-test and instrumentation-test Kotlin sources using the shared `.editorconfig`, with Compose function naming supported. Module `check` tasks include ktlint validation.

```sh
./gradlew ktlintFormat # format Kotlin sources and Gradle scripts
./gradlew ktlintCheck  # verify formatting without changing files
```

## API addresses

Debug defaults to **`http://10.0.2.2:18081/api/`**, the emulator-accessible address for this repository's Go development container. Start the backend using the repository's development instructions. Android's localhost is the Android device, not the development computer.

On Android 17/API 37 and newer, debug builds declare and request `ACCESS_LOCAL_NETWORK` before starting the app's API requests. Approve the first system prompt; Android remembers the grant. If denied, the development access screen offers retry and app settings, and detects a grant when returning from settings. Older Android versions skip this step. The permission and request implementation live only in the debug source set; release builds include neither. Android's [permission reference](https://developer.android.com/reference/android/Manifest.permission#ACCESS_LOCAL_NETWORK) excludes VPN connections, so the release endpoint routed through Tailscale's Android VPN does not require this permission. A future release endpoint accessed directly over a local Wi-Fi/Ethernet network would need permission handling.

For unattended emulator development, install the debug APK with `adb -s <emulator> install -r -g <debug.apk>` to grant its runtime permissions during installation, or use `adb -s <emulator> shell pm grant dev.abhishekan.pulse android.permission.ACCESS_LOCAL_NETWORK` after installation. The app cannot silently grant itself runtime permissions.

```sh
./gradlew :app:assembleDebug -Ppulse.debugApiUrl=https://your-development-host.example/api/
./gradlew :app:assembleRelease -Ppulse.releaseApiUrl=https://your-production-host.example/api/
```

Release defaults to **`http://100.115.134.90:3100/api/`**, the production web proxy to Go over Tailscale. The phone must have Tailscale connected and permission to reach that host and port. Port 80 does not route the Pulse API, and Go's port 18080 remains bound to the server's localhost. Addresses must end in `/api/` and cannot contain credentials, query parameters, or fragments. HTTP is allowed only for this exact host/port and, in debug, the emulator host `10.0.2.2`. Android's host exception is limited to `100.115.134.90`; the client additionally restricts the port. Other production hosts require HTTPS. No account credentials are supplied or persisted by the app.

HTTP relies on Tailscale for transport encryption. The backend must issue non-Secure session cookies for this HTTP endpoint (`SESSION_COOKIE_SECURE=false`); Secure cookies cannot authenticate HTTP requests. Do not publish this HTTP service to the public internet.

## Signed personal release

For a GitHub release build delivered to Telegram on every push to `migration`, see
[Get a production APK in Telegram](docs/release-apk.md), including the four required
repository secrets and optional production API URL override.

Local signing uses the ignored `.signing/release.properties` file:

```properties
storeFile=.signing/pulse-release.p12
storePassword=<local-keystore-password>
keyAlias=pulse-release
keyPassword=<local-key-password>
```

Keep this file and the keystore private, and back up the entire `.signing/` directory securely outside the checkout. The same key is required for future in-place updates. Without local signing properties, CI can still compile unsigned release artifacts.

```sh
./gradlew :app:assembleRelease
```

The signed APK is `app/build/outputs/apk/release/app-release.apk`. Increment `versionCode` for subsequent releases. A debug install uses a different signing key and must be uninstalled before installing the first release; uninstalling removes its local session and preferences.

Always install and cold-launch the final signed APK before distributing it; debug/device tests and JVM unit tests do not exercise R8's transformed release code. Release 1.0 crashed during Koin initialization because R8 9.4.24 moved a Kotlin text subclass outside its package while its parent remained package-private. The targeted `-keeppackagenames kotlin.text` rule preserves that package while leaving release optimization enabled.

Release 1.0.1 (versionCode 2) was verified on 19 October 2026: signed release build, release lint and formatting checks passed; updating the crashing APK succeeded; three cold launches on an isolated Android 15/API 35 emulator reached the login screen with no process crashes. The original APK's startup crash was reproduced on the same emulator before the fix. Physical-phone startup and authenticated live-backend workflows remain unverified.

Backend contracts inspected:

| Operation | Endpoint | Handling |
| --- | --- | --- |
| Login | `POST /api/auth/login` | Username/password JSON; username response and `life_session` cookie |
| Restore/validate | `GET /api/auth/session` | Validated username; 401 confirms an invalid session |
| Logout | `POST /api/auth/logout` | Server revocation and local cookie deletion |
| Greeting | `GET /api/profile` | `has_profile`, optional/null `name`; username fallback |
| Workout | `GET /api/workout/today` | Read-only `visited` status; backend India date boundary |
| Notifications | `GET /api/notifications?limit=5` | Latest five, optional body/date, no mutations or deferred links |
| Financial Horizon | `GET /api/horizon` | Backend currency, starting pool, uncommitted amount, fixed obligations |

Source of truth: Go auth/profile/gym/notification handlers and Horizon summary DTO; web dashboard and financial summary component. Backend code has not been modified.

## Architecture

| Module | Owns | Project dependencies |
| --- | --- | --- |
| `:app` | Application/Koin startup, session gate, appearance, Compose navigation, environment configuration | All ten modules |
| `:core:domain` | Pure models, outcomes, repository contracts and use cases | None; coroutines only |
| `:core:data` | Repository implementations, mapping, DataStore preferences, Keystore encrypted storage | Domain, network |
| `:core:network` | One Ktor/OkHttp client, API services/DTOs, cookie policy and storage contract | None |
| `:core:ui` | Theme/tokens, adaptive helper, individually organized components, failure messages; debug gallery | Domain |
| `:feature:auth` | Login screen, immutable state, ViewModel, feature DI | Domain, UI |
| `:feature:home` | Dashboard/notification presentation, independent section state, ViewModel, feature DI | Domain, UI |
| `:feature:fitness` | Activity calendar, workout details, exercise forms, ViewModels/state and feature DI | Domain, UI |
| `:feature:horizon` | Financial overview, paged ledger, budgets, obligations, subscriptions, planned purchases, forms and feature DI | Domain, UI |
| `:feature:garage` | Vehicle collection/detail, fuel, tire pressure/air, maintenance, receipt actions, drafts and feature DI | Domain, UI |
| `:feature:mealplan` | Weekly planner, meal/consumption actions, custom meal times, saved drafts and feature DI | Domain, UI |

Features never import data/network or one another. Domain has no Android, Koin, Ktor, or persistence dependencies. Boundary tests enforce the module edges and import restrictions. No database/common/testing module, offline sync, or additional architecture framework was introduced.

MVVM uses constructor injection and immutable `StateFlow`; Compose collects lifecycle-aware state. Use cases register with `factoryOf`, owned repositories with `singleOf`, and ViewModels with `viewModelOf`. Infrastructure converts HTTP, timeout, connection, decoding, and storage failures into domain outcomes. Coroutine cancellation propagates. Only confirmed local session cleanup finishes in `NonCancellable`, so disposing an expired navigation flow cannot interrupt revocation.

## Architecture skill review

Reviewed against [android-architecture](../.agents/skills/android-architecture/SKILL.md) after Phase 2. The skill separates responsibilities and dependencies; it does not mandate one declaration per file (its own domain example groups a repository, model, and use case). Pulse now separates these roles for navigation and maintenance:

```text
core/domain/.../domain/
  model/       # Focused models/outcomes; related feature models may share a file
  repository/  # One repository contract per file
  usecase/     # One application operation per file
core/data/.../data/di/DataModule.kt
core/network/.../network/
  di/NetworkModule.kt
  dto/PulseDtos.kt
  NetworkConfig.kt
  PulseHttpClient.kt
  PulseApi.kt
feature/{auth,home,fitness,horizon,garage}/.../
  di/{Auth,Home,Fitness,Horizon,Garage}Module.kt
  # Screens, ViewModels and immutable UI state in separate files
app/.../pulse/di/ApplicationModule.kt
```

Related transport DTOs and UI state variants can share a focused file. Repository implementations remain in data; use cases remain in pure Kotlin domain and are registered by app/feature DI modules. Shared UI depends only on domain to translate failure codes into user-facing text; domain contains no presentation messages. Preference writes now return domain outcomes instead of exposing disk exceptions to the ViewModel. The financial overcommitment rule lives in the domain model; composables only choose its presentation. Storage formats, preference keys, cookie handling, and session encryption are unchanged.

| Skill requirement | Implementation and verification |
| --- | --- |
| Acyclic layer/feature dependencies; pure domain | `ArchitectureTest` checks Gradle edges and forbidden imports |
| Immutable StateFlow; lifecycle-aware UI | Private mutable flows, read-only state, `collectAsStateWithLifecycle`; ViewModel and device tests |
| Constructor injection and correct lifetimes | `singleOf` repositories, `factoryOf` use cases, `viewModelOf` ViewModels; DI files contain composition |
| Explicit complete Koin graph verification | `KoinGraphTest` combines all production modules with a test API address and calls `Module.verify()`; only Android-provided Context/SavedStateHandle and factory-supplied configuration/engine arguments are exempted |
| One shared client; infrastructure boundaries | Singleton Ktor/OkHttp client; API DTOs in network, mappings/failures in data; device test checks client identity |
| Structured concurrency and cancellation | ViewModel scopes, disk work on IO, cancellation-preserving failure mapping; boundary and cancellation tests |
| Observable repository contracts | Session StateFlow and theme Flow; dashboard HTTP reads remain suspend snapshots |
| Independently testable domain rules | Framework-free `LoginTest` exercises validation, normalization, failure forwarding, and cancellation; `HorizonTest` checks commitment at precise negative/zero/positive boundaries |
| Lifecycle/process behavior | Navigation/recreation/restoration device tests; only username enters saved state |

The original product requirements take precedence over conflicting skill examples: HTTP exceptions are translated **inside infrastructure**, never imported into ViewModels; credential headers stay redacted and release HTTP logging stays disabled; coroutines are allowed in domain. The skill's illustrative database/common/testing modules and empty Koin modules for domain/shared UI are unnecessary for current functionality.

This review changes architecture organization and error boundaries only. No later-phase workflows are included. Verification results for this review are recorded at the end of this document.

## Authentication and navigation

Startup stays behind a session gate while encrypted material is read and a saved session is validated. A missing/expired cookie opens Login. Network/timeouts/server failures show a retry screen **without deleting the saved session or exposing authenticated content**. A separate explicit Forget action can remove an unreadable/device-local session; it does not revoke other devices' sessions.

Login trims only the username and preserves the exact password. Required fields validate inline; credentials/connection/timeout/server failures use safe messages. Duplicate submissions are blocked. Username is saved in `SavedStateHandle`; passwords stay only in the ViewModel's memory, surviving configuration changes but not process death. The Compose String text-field implementation also keeps its editing value in ordinary `remember`, not saved-state storage. Password visibility, autofill semantics, Next/Done IME actions, and keyboard insets are supported.

One activity hosts Compose navigation. Successful login disposes the Login flow. Signed-in tabs are **Home**, **Fitness**, **Horizon**, **Garage**, **Meals**, and **You**, using Navigation's saved/restored destination state. Notifications open within Home. Fitness lives in a nested Spaces navigation flow; Home shortcuts enter or restore the owning feature directly. Logout or confirmed expiry disposes authenticated destinations. Expiry is checked by a lifecycle-bound timer, on foreground resume, and on authenticated 401 responses. A stale request cannot expire a newer session generation.

Logout is not retried automatically. A connection failure keeps the active session and reports a Snackbar; a confirmed logout removes access. Storage-cleanup failures are surfaced and cannot silently restore a cookie in memory. The client disables redirects and connection retries.

Debug builds log API request/response headers and bodies, methods, URLs, and response status to Logcat under the `PulseNetwork` tag using Ktor's Logging plugin at `ALL` level. Filter Logcat with `tag:PulseNetwork`. Release builds do not install the plugin. Authorization, Cookie, Set-Cookie, and Location headers are redacted; Location can contain signed storage URLs. Logging is restricted to the configured API origin (protocol, host, and port), excluding external storage requests. Bodies and API query parameters remain visible, including passwords and API keys, so use disposable test accounts and data in debug builds.

## Session storage

`PersistentSessionCookies` implements Ktor `CookiesStorage`. It retains only `life_session`, restricted to the configured API's scheme/host/port, and honors path boundaries, Secure, expiry, and Max-Age precedence. The backend currently uses a host-only cookie with path `/`; parent-domain sharing is deliberately not accepted. Cookies without explicit expiry are not persisted. The envelope contains only the cookie value and required scope/expiry metadata, never username/password.

`EncryptedSessionStore` writes an authenticated AES-256-GCM envelope with randomized IVs, a versioned format, and a non-exportable Android Keystore key. Atomic writes and mutexes protect the file in `noBackupFilesDir`. Backup and device transfer exclude app data. DataStore contains theme preferences only.

Read/corruption/missing-key failures do not silently erase material. Failed deletions remain pending and must complete before a subsequent request proceeds. Login starts from a cleared signed-out cookie state, so an incomplete older response cannot authenticate a different account accidentally.

## Dashboard and design system

Profile, workout, finance, and notifications load in separate jobs. Each section has loading, empty, error and retry presentation. A failed section cannot erase successful sections. Missing profile information falls back to username. Workout status refreshes on resume using the account's selected time zone definition of today.

Financial numbers are parsed directly from JSON numeric text into `BigDecimal`, never through `Double`. Display pads short fractions to two places and preserves every supplied decimal place; balances and progress are not fabricated or recomputed. All-zero backend values remain actual zero values.

Home uses a sunlit illustrated room, a live India-time clock, a personalized greeting, and illustrated cards for the exact remaining balance and today’s workout status. Finance opens Financial Horizon; fitness opens the existing Fitness flow; the header opens Notifications. Cards stack on phones and at large font scales, with two columns on wider screens. The dark appearance dims the room and retains readable card colors. Generated artwork, prompts and asset paths are documented in [Dashboard artwork](docs/dashboard-artwork.md). You provides username, persisted System/Light/Dark appearance, and working logout. Spaces lists Fitness and Financial Horizon, the currently implemented spaces. Deferred space workflows, profile editing, notification mutations, and onboarding have no clickable placeholders.

Shared controls now live in **`core/ui/.../components/`**, one component per file; related models sit with the component that owns them. Themes/tokens and adaptive helpers remain beside that package. There are four type sizes, two weights, tabular/monospace metrics, 4dp spacing increments, 48dp targets, labeled selection semantics, native focus behavior, and tested contrast pairs. Error/retry cards use neutral surfaces so action text remains accessible.

Layouts measure available width: compact below 600dp, medium from 600dp, expanded from 840dp. Phones stack cards; larger windows use readable columns. Tablet login puts branding beside a constrained form. Phone/landscape forms scroll with the keyboard open. Orientation is not locked. Tablets use a left navigation rail with Home, Fitness, Horizon, Garage, Meals, and You. Phones use bottom navigation with Home, Fitness, Horizon, Garage, and Meals; a Profile icon beside Notifications in the Home header opens You. Root tab pages use the full content height without an empty header; detail pages and destinations opened from Notifications retain back navigation. The rail highlights the owning feature on workout and vehicle detail screens and scrolls when vertical space is limited.

The debug-only component gallery is reachable from You. Gallery and auth/home previews cover phone/tablet, light/dark, loading/disabled/error/empty states, landscape, and large text. Preview amounts and test backend credentials are not part of release sources.

## Verification

`build` runs compilation, unit tests, release packaging, and lint. Android tests use production networking, encrypted persistence, repository mapping, use cases and navigation against a test-only HTTP engine; they do not log in to a personal backend account.

Coverage includes:

- Module direction/domain isolation, full Koin foundation graph, configuration and backup policies.
- Encryption/randomization/tamper rejection; Android Keystore reopening and corruption retention.
- Cookie origin/path/Secure/expiry/Max-Age rules, storage failure handling, and pending deletion.
- Exact credentials, missing-cookie rejection, invalid input, offline restoration, cancellation, logout and confirmed expiry.
- Session generation races and cleanup when a session observer cancels the caller.
- Decimal precision, optional profile data, malformed responses, and independent section retries.
- Login ViewModel validation, duplicate submission, configuration/process-state rules; expiry and logout ViewModels.
- Android login/dashboard, back-stack removal, recreation, session reopening, expiry, offline retry, notifications, partial failure recovery, IME actions and touch targets.

Phase 1 originally passed its build, 17 unit tests and five device tests. Phase 2 results are recorded below.

## Official compatibility references

- [AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Koin 4.1](https://insert-koin.io/docs/4.1/support/releases/), [Ktor releases](https://ktor.io/docs/releases.html), [Ktor cookies](https://ktor.io/docs/client-cookies.html)
- [Serialization 1.9.0](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle), [Navigation](https://developer.android.com/jetpack/androidx/releases/navigation)

## Phase 2 results

- `./gradlew build`: passed, including debug and optimized unsigned release APKs.
- **40 unit tests passed**; module boundaries, pure domain and Koin wiring checks passed.
- **10 Android tests passed**, none skipped, on Android 15/API 35.
- All **five authentication/dashboard scenarios** also passed at 1024×900dp in dark mode, and at 360×800dp with 200% text scaling in light mode.
- The short **800×360dp landscape** login check passed, including Next/Done focus/submission and invalid-credential feedback with the keyboard flow.
- Native screenshots were inspected for phone/tablet, light/dark and large-text layouts. Captured account names, balances and notifications are test fixtures, not personal/live backend values.
- Lint passed with zero errors. Remaining advisories concern dependency update availability and existing template resources/labels; dependency versions were not changed just to silence these notices.
- Emulator display, density, font and appearance settings were restored; test captures on the emulator were removed and the task's emulator was shut down.

No personal backend login was performed: API integration is verified against source-derived fixtures through the production stack. Physical-device TalkBack and external-keyboard traversal remain unverified. The real release HTTPS API origin still needs configuration. At the Phase 2 boundary, later feature workflows had not been started. Phase 3 results follow below.

## Architecture review results — 17 October 2026

- `./gradlew build`: passed, including debug/release compilation, optimized release packaging, unit tests, and lint.
- **47 unit tests passed**, zero failures/errors/skips, including complete `Module.verify()` coverage and boundary/domain tests.
- **10 Android regression tests passed**, zero failures/errors/skips, on the existing Android 15/API 35 emulator.
- Lint: zero errors; 35 existing app advisories (dependency updates and template resources/labels), zero warnings in the core/feature module reports.
- Device regression fixtures went through production API/repository/session/navigation code. Temporary fixture screenshots were removed and the emulator started for this review was stopped.
- No dependency versions, encrypted storage formats, preference keys, backend contracts, or later-phase scope changed. Ktor core was added only to the app's unit-test classpath for graph verification; it was already a production infrastructure dependency.
- The prior tablet/landscape/large-text matrix, personal/live backend login, and physical-device accessibility were not repeated for this architecture refactor. Configure the release HTTPS API origin before production use.

## Phase 3 — Fitness

Fitness adds a month calendar, a selected-day workout list, server-provided metrics, workout details, catalogue search/instructions, exercise creation/editing/deletion, and workout deletion. Each exercise supports 1–20 sets, 1–1,000 repetitions per set, optional non-negative kilogram weights, an optional catalogue match, and the backend’s remembered-name alias setting. Imported start/end times, duration and calories are displayed read-only; gym-visit exercises remain editable because the API supports them.

| API | Use |
| --- | --- |
| `GET /fitness/overview` | Authoritative today, summary values and calendar groups |
| `GET /gym-visits` | Complete visit history, including future dates omitted by overview statistics |
| `GET /workout/today` | Existing Home status; refreshed after fitness mutations |
| `POST /workout/today` | Record today, using the backend’s India date boundary |
| `POST /gym-visits` | Record an explicit calendar date as `YYYY-MM-DD` |
| `GET /fitness-workouts/{visitId}` | Visit details and supported imported workout information |
| `GET /exercise-catalog?q=...` | Debounced catalogue search; explicit candidate selection |
| `GET/POST /gym-visits/{id}/exercises` | Read and create exercises |
| `PUT/DELETE /gym-visits/{id}/exercises/{exerciseId}` | Replace an exercise’s name/match/sets, or delete it |
| `DELETE /gym-visits/{id}` | Delete visit and cascade its exercises/sets |

All paths above are relative to the configured `/api/` base. No Go/backend or web contracts were changed. The backend accepts future-dated visits: Android retains them in the calendar through the history endpoint while displaying statistics exactly as returned by overview. “Today” follows the profile time zone (UTC by default); Material date-picker UTC milliseconds are converted to calendar dates without applying a device-zone shift. No additional past-only restriction was invented.

Deleting a visit removes its exercises and sets through the database’s cascade. A linked Apple Watch import and catalogue aliases remain. Confirmation dialogs explain those consequences; exercise deletion removes only that exercise and its sets.

Shared repository revisions coordinate updates across screens. Successful and uncertain mutations invalidate Fitness and Home workout reads. Home cancels any older in-flight workout read before fetching current status. No feature imports another feature, and no database, offline mutation queue, background synchronization, or new dependency version was added.

Draft fields live in the workout ViewModel and SavedStateHandle, including set rows, catalogue choice and alias choice. Rotation, resizing and tab changes preserve them. Back/Cancel warns before discarding edits; logout from You also warns when an unsaved Fitness draft exists. Restoration keeps the draft visible even when its server read fails. Password handling is unchanged.

Mutations are never automatically retried. Synchronous submission guards reject duplicate taps. An interrupted or ambiguous write retains the draft, refreshes server state, and disables another mutation until refreshed records are explicitly reviewed. If refresh fails, only refreshing is offered. A persisted pending-operation marker enforces the same rule after process recreation. A confirmed workout deletion, including a missing record after an uncertain deletion, returns to the calendar.

Weight input and JSON mapping use BigDecimal and unquoted numeric serialization, avoiding an Android-side Double conversion. The existing Go API stores exercise weights as float64, so server-side numeric precision remains limited by that existing contract. Imported durations use the web’s whole-minute presentation without floating-point conversion. Financial decimal handling remains unchanged.

The calendar and selected-day pane sit side by side at suitable widths and stack for narrow/large-text windows. Calendar controls keep 48dp targets; narrow windows can scroll dates horizontally. Day semantics identify full dates, today, selection and recorded activity. Exercise number fields stack when their available width/text scale calls for it, and IME Next explicitly moves from repetitions to weight. Dialog body text can scroll. Shared calendar/date controls are included in the debug gallery; fitness previews cover phone/tablet, light/dark, landscape, loading/error and large text.

The picker and tab navigation follow the installed libraries’ documented APIs: [Compose date pickers](https://developer.android.com/develop/ui/compose/components/datepickers), [multiple navigation back stacks](https://developer.android.com/guide/navigation/backstack/multi-back-stacks). Existing versions remain unchanged.

Verification covers domain date/validation boundaries; exact JSON round-tripping; authentication expiry and cancellation; malformed/optional responses; duplicate submissions; uncertain mutations and refreshed-state review; process-state drafts; calendar navigation; exercise CRUD; deletion consequences; Home refresh; saved tab state; keyboard focus; live resizing; and large-text controls. Device tests use source-derived HTTP fixtures through the production Ktor/session/repository/navigation stack, without accessing a personal account.

Fitness history currently uses the backend’s unpaginated visits endpoint. A server range/pagination contract would be needed for substantially larger histories. Physical-device TalkBack/external-keyboard testing and personal/live-backend login remain separate checks. Later feature phases remain outside this delivery.

### Phase 3 verification results — 17 October 2026

- `./gradlew build :app:assembleDebugAndroidTest`: passed, including optimized unsigned release packaging and lint.
- **77 unit tests passed**, zero failures/errors/skips; module boundaries, pure-domain imports and the complete Koin graph passed.
- **17 Android tests passed**, zero failures/errors/skips, on Android 15/API 35.
- All **seven Fitness device scenarios** also passed at 1024×900dp in dark mode, 360×800dp with 200% text in light mode, and 800×360dp landscape. These cover live resizing, keyboard traversal, draft restoration, CRUD, deletion and uncertain-save recovery.
- Native phone/tablet, dark/light, large-text, landscape, editor and confirmation screenshots were inspected. Captured content comes from test fixtures, not a personal account.
- After the window-dimension correction, the date-picker integration test also passed again in the landscape and 200% text configurations.
- Emulator display, density, text and appearance settings were restored; temporary captures were removed and the task’s emulator was stopped.
- Lint reports zero errors and zero core/feature warnings. The app retains 35 existing dependency-update/template advisories. Toolchain and dependency versions remain unchanged.

At the Phase 3 boundary, Phase 4 had not started. Personal/live-backend login, physical-device TalkBack and external-keyboard traversal remain unverified; configure the release HTTPS API origin before production use.

## Phase 4 — Financial Horizon

The `:feature:horizon` module adds native secondary navigation for Overview, Transactions, Budgets, Obligations, Subscriptions and Planned purchases. Home and Spaces open the working finance destination; the existing bottom navigation remains on phones and tablets. No backend or web code was changed. PDF statement extraction, bulk import, Garage and Meal Plan workflows remain outside this phase.

| Workflow | Existing API, relative to `/api/` |
| --- | --- |
| Summary, budget usage and projections | `GET /horizon` |
| Starting pool and currency | `PUT /horizon/config` |
| Transactions | `GET/POST /horizon/transactions`, `PUT/DELETE /horizon/transactions/{id}` |
| Categories | Summary categories and `POST /horizon/categories` |
| Budgets | `POST /horizon/budgets`, `PUT/DELETE /horizon/budgets/{id}` |
| Fixed obligations | `POST /horizon/deductions`, `PUT/DELETE /horizon/deductions/{id}` |
| Subscriptions | Summary subscriptions, `POST /horizon/subscriptions`, `PUT/DELETE /horizon/subscriptions/{id}` |
| Linked payments | `GET/POST /horizon/subscriptions/{id}/transactions` |
| Planned purchases | `GET/POST/DELETE /next-month-purchases`, `DELETE /next-month-purchases/{id}` |

Transactions request one 20-item page at a time with the supported `search` and `type` filters, honoring server page clamping and totals. Only unlinked debits are offered when linking an existing payment, with confirmation before applying the association. Categories support the API’s name/icon/color metadata; no unsupported category update/delete operation is exposed.

Forms support all exposed budget, category, subscription and obligation associations, including explicit JSON nulls to clear a link. Obligations include category, due day and active state. Subscriptions include monthly/yearly cycles, billing day, optional renewal date, active/paused/cancelled status and notes. Transaction timestamps retain their seconds/precision when editing an unrelated field; native date/time controls change the local date/time and send the corresponding instant. Renewal dates remain calendar dates. Planned purchases use the server’s next-month boundary.

Money stays in `BigDecimal` through domain, mapping, display and unquoted JSON numeric serialization. Inputs respect the existing `NUMERIC(12,2)` database limits: at most two fractional digits, 0–9,999,999,999.99, with strictly positive transactions. Backend totals, projections, renewal dates and budget usage are displayed rather than recalculated in Android. The existing Go finance DTOs/calculations use float64, so Android cannot recover precision already lost on the server. Summary transaction totals and budget usage use the backend’s recent 50 transactions; the UI labels that limitation rather than presenting it as an all-history balance.

Deletion confirmations describe the actual foreign-key behavior: deleting a budget clears budget links while keeping related records; deleting an obligation clears subscription obligation links; deleting a subscription clears transaction subscription links while keeping transactions and fixed obligations. Individual purchase deletion and whole-list clearing require confirmation and do not affect transactions.

Overview, transaction pages, planned purchases and linked payments load independently with retry states. Financial mutations invalidate shared repository revisions, refreshing Home’s financial card without coupling feature modules. Home cancels an older in-flight financial read before loading the current result.

Drafts and pending-operation markers use SavedStateHandle. Rotation, resizing, tab changes and process-state restoration preserve input; leaving a dirty form or logging out requires confirmation. A lost/failed mutation response never triggers an automatic retry: the UI refreshes server state, retains the draft and blocks further mutations until current records are available and explicitly reviewed. Review includes searching/checking additional pages for a possibly committed transaction.

New shared controls are thin Pulse wrappers for selection fields, filter chips and the native time input dialog; the existing date picker and confirmation dialogs are reused. They appear in the debug component gallery. Debug-only finance previews cover light/dark, phone/tablet, landscape, large text and loading/error/disabled/saving states. The overview uses a warm balance hero with a supporting commitment/projection column at suitable widths, and stacks for compact or large-text windows. Forms stay width-constrained. Debit/credit rows use labels and signs as well as visual styling.

The integration reuses the installed versions and documented [Compose menu APIs](https://developer.android.com/develop/ui/compose/components/menu) and [Ktor request parameters](https://ktor.io/docs/client-requests.html). No toolchain or dependency version changed.

### Phase 4 verification results — 17 October 2026

- `./gradlew build`: passed, including debug/release compilation, optimized unsigned release packaging, unit tests and lint.
- Lint: zero errors, zero core/feature warnings; 35 existing app advisories for dependency updates/template resources. Toolchain and dependency versions are unchanged.
- **97 unit tests passed**, zero failures/errors/skips. Coverage includes decimal JSON round-tripping, numeric/database limits, exact zero boundaries, pagination/filtering/retry, associations, cancellation/authentication expiry, independent reads, duplicate submissions, uncertain saves, saved-state draft restoration, deleted subscription selection, module boundaries and the full Koin graph.
- **25 Android tests passed**, zero failures/errors/skips, on Android 15/API 35 in the 1024×900dp dark tablet configuration. This includes all prior authentication, Fitness, storage and gallery regressions plus eight finance scenarios.
- All **eight finance scenarios** also passed on the normal phone, at 360×800dp with 200% text, and in 800×360dp landscape. They cover configuration, category creation, transaction paging/filtering/CRUD, budget/obligation CRUD, subscription CRUD/payment linking, purchase deletion/clearing, Home refresh, keyboard focus, rotation, tab restoration, native pickers, live resizing and uncertain-response recovery.
- Focused final device checks at 200% text passed for excluding credit/already-linked payments, closing an open payment panel after subscription deletion, opening new forms at the top, and keeping transaction history readable when currency/summary data is unavailable. The corrected currency label was captured and visually inspected.
- Native phone/tablet, light/dark, landscape, large-text, form, picker and recovery screenshots were inspected. Preview/fixture amounts remain isolated from production sources.
- Emulator size/density/text/appearance settings were restored; task-created emulator screenshots were removed and the task’s emulator was stopped.

API workflows were tested through production Ktor, encrypted session persistence, repositories, use cases, Koin and navigation against test-only source-derived HTTP fixtures. No personal/live backend account was accessed. Process-state restoration was checked through SavedStateHandle tests; native tests exercise rotation/resizing/tab restoration rather than killing a live personal session. Physical-device TalkBack, external-keyboard traversal and other Android runtime versions remain unverified. The release HTTPS API origin still needs configuration.

Known server limits remain the existing float64 finance calculations, recent-50 summary transaction/budget usage, and unpaginated linked-payment lists. The main transaction ledger stays paginated. After an uncertain mutation, review waits for successful overview, current ledger page and planned-purchase reads (plus an open payment list), so unresolved read failures intentionally keep writes blocked. Phase 4 stopped here; Garage is delivered separately below.


## Phase 5 — Garage

Garage is available from Home and Spaces. Its native vehicle collection opens a vehicle overview, fuel history, maintenance history, and tire/air section. Vehicle details occupy a separate page on phones and tablets, with a top-bar back arrow returning to the collection. Forms stay width-constrained, use Pulse controls, preserve unsaved fields with SavedStateHandle, and warn before discard/logout. Tab navigation preserves destination state. Only Garage was added in this phase; Meal Plan is deferred.

| Workflow | Existing API, relative to `/api/` |
| --- | --- |
| Vehicle collection/create/delete | `GET/POST /vehicles`, `DELETE /vehicles/{id}` |
| Histories and backend efficiency | `GET /vehicles/{id}/history` |
| Fuel create/edit/delete | `POST /vehicles/{id}/fuel-fillups`, `PUT/DELETE /vehicles/{id}/fuel-fillups/{fillupId}` |
| Air fill now/delete | `POST /vehicles/{id}/air-fills`, `DELETE /vehicles/{id}/air-fills/{airFillId}` |
| Target tire pressure | `PUT /vehicles/{id}/tire-pressure` |
| Maintenance create/edit/delete | `POST /vehicles/{id}/maintenance-records`, `PUT/DELETE /vehicles/{id}/maintenance-records/{recordId}` |
| Receipt upload | `POST /vehicles/{id}/maintenance-records/{recordId}/attachments`, multipart field `file` |
| Receipt download/remove | `GET/DELETE /vehicles/{id}/maintenance-records/{recordId}/attachments/{attachmentId}` |

No backend/API or web changes. The vehicle-history endpoint remains unpaginated; Android uses the returned history and backend summaries without introducing a second cache or offline queue. Vehicle creation includes name, active flag and optional solo/pillion target pressures. Tire targets use PSI and explicit nulls clear them. Air fills use server time; the API does not accept backdating. Fuel supports one or two tanks, petrol/diesel/LPG/CNG/electric categories, full/partial/missed flags, timestamp, odometer, station and notes. Enter any two of quantity/unit price/total; absent operands are omitted and calculated by the server. Creation enforces the API’s highest-existing-odometer restriction; editing follows its supported replacement operation. Maintenance includes the five supported categories, amount, date/time, optional odometer/provider/notes. Byte-length title/provider limits match the Go validators. Validation also respects the database’s 48-character vehicle names, 160-character stations, NUMERIC(5,1) pressures, NUMERIC(12,1) odometers, NUMERIC(10,3) quantities, NUMERIC(10,2) unit prices and NUMERIC(12,2) costs, preventing silent fractional rounding or numeric overflow.

Decimal JSON numbers map directly to BigDecimal and serialize as exact unquoted literals. Android does not calculate fuel economy or round-trip money through Double. The server supplies total cost/volume and average/latest/minimum/maximum efficiency; missing intervals show “Not enough history.” Partial/missed intervals retain the backend behavior. The web/API currently label all fuel quantities L and costs ₹, including CNG/electric; Android states that limitation instead of inventing conversions or using Horizon’s unrelated currency setting. The existing Go float64 fields still limit server-side precision.

Receipt actions are available only on saved maintenance records. The system OpenDocument picker grants access to the selected file without broad storage permission. Infrastructure reads on the I/O dispatcher, checks declared and streamed lengths, rejects empty/unreadable/over-10-MiB files, sanitizes names, and uploads multipart with actual transfer progress. Failures keep the maintenance record. Passwords and receipt bytes never enter SavedStateHandle. A process-interrupted write persists a review marker; duplicate taps are blocked and no mutations automatically retry. Successful/uncertain writes refresh server state and invalidate repository revisions. An uncertain result blocks another write until fresh collection/history reads are explicitly reviewed.

Downloads authorize against the Pulse API with the existing session. Redirects remain disabled globally. Garage follows exactly one explicit absolute HTTPS receipt location, rejects embedded credentials/insecure locations/redirect chains, and bounds the downloaded body to 10 MiB. The shared persistent cookie storage only supplies the Pulse session to the configured API origin, never to another storage origin. A storage-server 401 does not expire the Pulse session. Download uses CreateDocument to save where the user chooses; Open uses a private cached file shared through a non-exported FileProvider with a temporary read grant. Its paths expose only the receipts cache subtree. Private previews expire on subsequent receipt opens after a day. No storage keys, cloud credentials or signed-link logging are introduced. See the official [Android document picker guidance](https://developer.android.com/training/data-storage/shared/documents-files), [FileProvider setup](https://developer.android.com/training/secure-file-sharing/setup-sharing), and [Ktor multipart/progress APIs](https://ktor.io/docs/client-requests.html).

Deletion dialogs match existing backend consequences. Fuel deletion removes its tank items and changes server efficiency. Air deletion leaves target pressures intact. Receipt deletion removes its hosted object and metadata. Maintenance deletion cleans hosted objects before deleting the record; failed partial cleanup can leave some files missing while keeping the record. Vehicle deletion cascades database history and receipt metadata **but the current backend does not clean hosted objects**; its confirmation explicitly says those objects may remain. Server-side storage cleanup would require a separate backend change.

Scaffold padding is consumed before feature content handles keyboard insets. When Android resizes the available root below 240dp in height, app bars temporarily yield space to the form; they return when the window grows or the keyboard closes. This is based on available dimensions, without an orientation lock or a change to the bottom-navigation model. The native landscape test checks visible text input and discard with the software keyboard open.

The existing versions/toolchain are unchanged. The only added library dependency is core:data using the already-catalogued AndroidX Core library for FileProvider. A small PulseTransferProgress wrapper joins the component gallery. Debug-only Garage previews cover phone, dark tablet and 200% text; fixture records are confined to debug/test sources.


### Phase 5 verification results — 17 October 2026

- `./gradlew build`: passed, including debug/release compilation, optimized unsigned release packaging, unit tests and lint.
- Lint: zero errors and zero core/feature warnings; the app retains 35 existing dependency-update/template advisories. Dependency versions and the toolchain are unchanged.
- **120 unit tests passed**, zero failures/errors/skips. Coverage includes exact decimal JSON, database limits, malformed histories, odometer rules, dates/DST, duplicate submissions, failed saves, deletion/selection, saved drafts and pending writes, multipart headers and the exact 10-MiB boundary, bounded downloads, redirect/cookie isolation, cancellation, module boundaries and the complete Koin graph.
- **31 Android tests passed**, zero failures, on Android 15/API 35 at 1024×900dp in dark mode. This includes prior authentication, Fitness, Financial Horizon, storage and gallery regressions plus six Garage scenarios.
- All **six Garage scenarios passed** on the normal phone, at 360×800dp with 200% text, and at 800×360dp in landscape. They cover vehicle/fuel/maintenance CRUD, tire pressure, air fills, deletion consequences, Home/Spaces entry, rotation/tab draft retention, keyboard/discard, native date/time pickers, live resizing, uncertain-save review, receipt transfer/removal and failure recovery.
- Native light/dark, phone/tablet, large-text, landscape, form, keyboard, picker and recovery captures were inspected. All shown records are isolated test fixtures.
- Device tests invoke AndroidJUnitRunner directly because the Gradle connected-test launcher stalled before test execution in this environment. The APKs were built with Gradle; the complete direct-runner result was `OK (31 tests)` and the final phone result was `OK (6 tests)`.
- Emulator size/density/text/appearance/autofill settings were restored, task-created emulator screenshots removed, and the task emulator stopped.

Integration tests exercise production Ktor, encrypted cookie/session persistence, repositories, use cases, Koin, navigation and Android content-URI file I/O against source-derived test-only HTTP fixtures. Picker/chooser results are intercepted in instrumentation: real provider selection interfaces, external PDF viewers and live hosted-storage/account behavior remain unverified. SavedStateHandle tests cover process-state reconstruction; native tests cover rotation/resizing rather than killing a live personal session. Physical-device TalkBack, external hardware keyboard and other Android versions remain unverified. Configure the release HTTPS API origin before production use.

Known backend limits remain float64 precision, fixed ₹/L fuel labels, unpaginated history, and hosted receipt objects left behind by vehicle deletion. No backend changes were included. Phase 5 stopped here; Meal Plan is delivered separately below.


## Phase 6 — Meal Plan

Home and Spaces open the working Meal Plan destination. The native planner provides Monday-based week navigation, a Today action, an automatically visible selected-day chip, and a date/meal-time agenda. Each illustrated agenda card groups items by meal time and saved time range, with independent consumption, edit and delete controls for each item. The selected-day agenda uses two card columns on tablets and one on phones or with enlarged text. The weekday strip scrolls on narrow windows, and an illustrated empty state offers Add meal. Consumption uses a labeled checkbox and an accessible Planned/Consumed description, with confirmation feedback after the server responds. No nutritional values or meal fixtures are supplied to production UI.

| Workflow | Existing API, relative to `/api/` |
| --- | --- |
| Selected week | `GET /meal-plans?start_date=YYYY-MM-DD&end_date=YYYY-MM-DD` |
| Add/edit/delete meal | `POST /meal-plans`, `PATCH/DELETE /meal-plans/{id}` |
| Consumption | `PATCH /meal-plans/{id}/consumed` with `is_consumed` |
| Meal times | `GET/POST /meal-times`, `DELETE /meal-times/{id}` |

No backend, web, toolchain, or dependency-version changes. Dates stay LocalDate values through DTO mapping and ISO date serialization, without conversion through midnight instants. Today follows the profile time zone (UTC by default). Week/month/year/leap-day boundaries use java.time calendar arithmetic. Forms use native Pulse date/time picker wrappers, meaningful labels, constrained widths and keyboard actions.

Meal names allow up to the database's 255 Unicode characters; custom meal-time names allow 100. Blank names, invalid dates/times and invalid IDs are blocked. The API accepts both HH:mm and HH:mm:ss; it imposes no start-before-end rule. Android therefore accepts equal endpoints and overnight ranges, labeling overnight ranges explicitly. Default meal times are protected from deletion in both the UI and domain use case. Meal-time editing is not exposed because no update endpoint exists.

A new meal may omit its time. The current Go update contract cannot clear an assigned time: omitted and JSON-null pointers both mean “keep the existing link.” Existing assigned meals can switch to another supported time; the form explains why removal is unavailable. Meal edits omit consumption, preserving the server's current confirmed value. Deleting a custom time keeps associated meals, clears their time association and retains their stored time range under Other meals. A later meal edit follows the backend's existing behavior, which can clear that detached range. Deleting a meal removes the plan and its consumption status, with confirmation.

Meal and meal-time reads load independently and expose loading/error/retry states. Last confirmed meal values remain visible during refresh and failed toggles. No optimistic consumption change occurs: the response supplies the confirmed value. Failed/uncertain writes refresh server records, retain the draft and block another mutation until successful reads are explicitly reviewed. A meal saved for another week moves review to that target week before submission, persisting the selection so process reconstruction reviews the correct date. No mutation retries automatically; duplicate submissions are blocked synchronously.

SavedStateHandle stores small editor fields, selected date, management mode and an interrupted-write marker. Scroll state uses Compose saved state; dirty drafts require discard confirmation and participate in the app's logout warning. Server records are reloaded rather than saved in a Bundle or database. Debug previews demonstrate phone/large text, tablet meal cards, illustrated empty states, saving and validation errors. Asset prompts are recorded in [meal-plan-design-assets.md](../docs/meal-plan-design-assets.md). The implementation reuses the installed [Compose saved-state APIs](https://developer.android.com/develop/ui/compose/state-saving) and [Ktor request APIs](https://ktor.io/docs/client-requests.html).


### Phase 6 verification results — 17 October 2026

- `./gradlew build :app:assembleDebugAndroidTest`: passed, including debug/release compilation, optimized unsigned release packaging, unit tests and lint.
- Lint: zero errors, zero core/feature warnings; 35 existing app dependency/template advisories remain. Toolchain and dependency versions are unchanged.
- **138 unit tests passed**, zero failures/errors/skips. New coverage includes India date boundaries, Monday weeks across months/years/leap days, empty weeks, range-query parameters, optional/detached meal times, validation/database name limits, protected defaults, correct consumption responses, failed toggles, duplicate submission blocking, independent read failures/retries, saved-state reconstruction, interrupted writes and reviewing the target week after an uncertain save.
- All **six Meal Plan phone scenarios passed** on Android 15/API 35, including meal CRUD, both consumption states, custom time creation/deletion, native pickers, retained meals after time deletion, dirty-draft rotation/tab/logout behavior, uncertain saves, failed toggles and live resizing.

- **37 Android tests passed** in the 1024×900dp dark tablet configuration: all 31 existing regressions and all six Meal Plan scenarios.
- All **six Meal Plan scenarios also passed** at 360×800dp with 200% text and in 800×360dp landscape. Native resize checks additionally cover the 1280dp weekly board and large-text agenda fallback, as well as restoring the agenda's scroll position after tab navigation.
- Phone/tablet, light/dark, weekly board, large text, landscape/keyboard, native date/time input, forms, deletion and recovery captures were visually inspected. Device tests ran through AndroidJUnitRunner directly using Gradle-built APKs.
- Emulator display/density/text/appearance/autofill settings were restored, task-created emulator screenshots removed, and the task emulator stopped.

Tests use production Ktor, encrypted session persistence, repositories, use cases, Koin and navigation against isolated source-derived HTTP fixtures. No personal/live backend account was accessed. Native captures contain test records only. SavedStateHandle tests reconstruct process state; native tests exercise rotation and resizing rather than killing a live personal session. Physical-device TalkBack, external hardware keyboards, other Android versions and live account behavior remain unverified. Configure the release HTTPS API origin before production use.

Phase 6 stops here. Phase 7 remains outside this work.

## String resources and translations

App-owned text lives in each presentation module’s `src/main/res/values/strings.xml`: `:app`, `:core:ui`, and each `:feature:*` module. Shared control labels and domain-failure presentation belong to `:core:ui`; feature-specific messages stay with their feature. The debug component gallery has a separate `core/ui/src/debug/res/values/strings.xml`. Each module uses a distinct `pulse_<module>_` resource prefix because Android merges library resource names into the application.

Add translated files in the corresponding module’s `src/main/res/values-<language>/strings.xml`. Preserve numbered format arguments (such as `%1$s`) and translate complete phrases rather than assembling English fragments. Use `<plurals>` for count-dependent messages and supply the quantity categories required by the target language. Default English resources remain the fallback. API identifiers, navigation routes, saved-state keys, dates and user/server-provided content are data, not translatable app copy.

Text defined in Composables uses `stringResource()` and `pluralStringResource()` directly, including labels, hints, accessibility descriptions and formatted summaries. Pulse controls accept resolved strings. Static tab/editor metadata carries string resource IDs; UI-only option and theme labels resolve in composition.

`UiText` in `:core:ui` remains for deferred validation, error and operation messages, retained confirmation content, and pure formatting helpers that prepare localized values outside composition. It retains a string/plural resource and arguments until display. Resolve deferred values with `.asString()` before passing them into `stringResource()` format arguments; do not pass a `UiText` object to the standard formatter. `PulseText`, `PulseErrorState` and field errors can also render deferred messages. This keeps `Context` out of ViewModels and resource IDs out of domain models. No `StringProvider` is injected into ViewModels. User/server text stays literal. Native file chooser titles resolve during composition before Android launches the chooser. Session storage, monetary calculations, API payloads and dependency versions are unchanged.

`LocalizationResourcesTest` checks merged resource-name collisions, numbered format arguments and direct hardcoded copy in Pulse controls. `LocalizationDeviceTest` uses test-only English/French resources to check direct Compose string/plural resources alongside retained messages, language changes, nested formatting, singular/plural selection and unchanged user text. Those fixtures do not add a production French translation. Run the existing build/unit/lint checks and Android instrumentation suite described above.

Material 3 remains the implementation underneath Pulse’s thin Compose components. `PulseTheme` supplies the project’s own palette, typography and shapes; Material 3 was an implementation choice, not an explicit requirement in the original product brief.

### Localization verification — 18 October 2026

- `./gradlew build :app:assembleDebugAndroidTest` passed for the final code, including debug/release packaging, release shrinking, 141 unit tests, module boundaries, and lint.
- Lint: zero errors; no issues in infrastructure, shared UI or feature modules. The app retains its 35 existing manifest/toolchain/dependency advisories. Dependency versions were not changed.
- Android instrumentation: all 39 distinct test cases passed across the full regression and focused reruns on an API 35 emulator with dark tablet dimensions. The full run initially passed 38/39 and exposed subpixel rounding in a strict 48dp assertion at fractional density; the assertion now allows 0.001dp rounding and includes measured bounds on failure. The final 11-test rerun passed authentication/logout, affected Horizon and Meal Plan workflows, resizing, keyboard/draft preservation, and the two language-resource checks.
- Reviewed screenshots of Home, exercise and fuel editors, Horizon and Meal Plan. Instrumentation uses isolated test backend responses, not a personal account or production data.
- Actual translations, expanded translated-text layouts, physical devices and a manual TalkBack walkthrough remain unverified. No backend or Phase 7 functionality was added.

### Direct Compose resource APIs — 18 October 2026

Static UI labels and quantities now use `stringResource()` and `pluralStringResource()` across the app, all feature screens, shared components and debug gallery. Unused `UiText` label overloads were removed. Deferred validation/operation messages, retained confirmation text and pure localized formatting helpers still use `UiText`; resolution happens at display time. ViewModel behavior, business rules, resource keys and XML text are unchanged.

Verification: `./gradlew build :app:assembleDebugAndroidTest` passed, including debug/release packaging, release shrinking and all 141 unit tests. Lint has zero errors and the same 35 existing app advisories; infrastructure, UI and feature modules report no issues. All 39 Android tests passed in one full run on API 35 with a dark 1024×900dp window, including resizing/keyboard checks. The expanded English/French test verifies direct string/plural calls and a retained nested `UiText` message together. Reviewed login with the keyboard open, Home and exercise-editor screenshots. Tests use isolated backend fixtures. Physical devices, manual TalkBack and actual translated production layouts were not verified in this change.

## Phase 7 — Profile and in-app notifications

The You tab now lives in `:feature:profile`; the notification center lives in `:feature:notifications`. Both depend only on domain contracts and shared UI. App navigation owns notification target routing, while data repositories publish dashboard changes through the domain contract so account names and notification previews refresh without restarting, including after a successful explicit account/notification refresh. Koin assembles the new repositories, one-operation use cases and screen-scoped ViewModels. Existing authentication, encrypted session cookies, exact financial values, appearance persistence and dependency versions are preserved. There are no backend/API changes.

You supports display-name creation/editing, read-only username, appearance, password changes, API key management, About Pulse and confirmed server-backed logout. A successful `has_profile: false` response offers optional setup once per signed-in session; a failed request never triggers onboarding. Names match Go's surrounding Unicode whitespace trimming and 1–120 code-point limit. Profile and API-key reads fail independently.

Passwords remain exact, in-memory values, survive activity recreation, and clear on success, editor dismissal, leaving the screen or session exit. They are never placed in saved state. Validation matches the existing server's minimum six UTF-8 bytes and bcrypt's maximum 72 bytes; confirmation is local. The server does **not** invalidate existing sessions after a password change. Unknown password outcomes explicitly explain that the value cannot be read back; they are not retried automatically.

Key metadata is server-backed. The creation token is shown once in memory, survives rotation, and clears when closed or the flow/session ends. Password and token windows use `FLAG_SECURE`. Copying uses Android's [sensitive clipboard hint](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling), with success only after clipboard readback; that hint suppresses supported system previews, not access by every possible clipboard consumer. No full token, password or credentials are stored in preferences or saved state. Release HTTP logging is disabled; debug body logging can expose these values in Logcat. If a creation response or token is lost, refreshed metadata and an explicit review guide revocation before creating another key. Only a nonsecret interrupted-operation marker survives process recreation. Revocation confirms integration access loss; an already-absent key is handled successfully.

The center fetches the backend's default maximum of 100 notifications without invented pagination. It displays title, optional body, source, date and nonzero priority with text cues. Dismissal and confirmed Clear all refresh Home; Clear all explicitly includes notifications outside the displayed limit. Confirmed values survive failures. Unknown mutation outcomes trigger a read and require explicit review before another mutation. No mutation retries automatically, and already-dismissed items are handled successfully.

Supported native targets are exactly `/fitness`, `/financial-horizon`, `/garage`, `/meal-plan`, `/fitness/{positive-id}` and `/garage/{positive-id}`. Identifiers must fit a positive signed Long; external URLs, query strings, fragments, encoded/unknown paths and malformed IDs remain informational. Routing selects the owning Spaces graph and retains the notification center underneath. Opening an item does not dismiss it. Deleted vehicle records return to the collection with feedback; workout details retain their existing unavailable-record state.

Profile uses stacked phone cards and a two-column identity/appearance and account/integrations layout when width and font size permit. Forms are constrained and scroll with the keyboard. The notification list is width-constrained and lazy. Shared Pulse controls preserve 48dp targets, field/autofill semantics and visible focus; validation and operation feedback use polite live regions. All new app-owned copy is in the two feature modules' `strings.xml`; static labels resolve with `stringResource()`, deferred ViewModel messages with `UiText`. Name and key-name drafts use saved state; passwords and full tokens deliberately do not. Repeated editor-open requests preserve an existing draft. Dirty account forms require confirmation before discard, and password edits require confirmation before tab navigation.

No push messaging, device notification permission/channels, background polling, new notification-generation rules, offline storage or release/distribution work was added. Phase 8 remains deferred.

### Phase 7 verification — 18 October 2026

- `./gradlew build :app:assembleDebugAndroidTest` passed, including debug/release compilation, optimized unsigned release packaging, formatting checks, unit tests and lint. The expanded cross-feature import check was also rerun with `:app:testDebugUnitTest :app:ktlintCheck`.
- **168 distinct unit tests passed**, with no failures/errors/skips. New coverage includes Go-compatible Unicode names, UTF-8/bcrypt boundaries, exact passwords, independent reads, stale-read cancellation after saves, repeated editor-open draft preservation, duplicate submission blocking, secret clearing, interrupted operations, one-time-token loss, notification reconciliation, safe path/ID parsing, HTTP request contracts, already-absent records and cancellation propagation. Module directions and the complete production Koin graph are verified.
- Lint: zero errors; no core or feature issues. The app reports nine template/toolchain advisories (redundant label, a Gradle update suggestion and unused template colors). The removed You/notification screen resources were removed as well. No dependency/toolchain versions changed.
- **All 52 distinct Android tests passed across the regression run and focused reruns** on Android 15/API 35. The first full run passed 49/52. The final 14-case run passed all 13 account/notification scenarios plus the receipt regression: resize was rerun after display configuration settled, revocation now waits for confirmed asynchronous feedback, and the receipt fixture accepts valid multipart header quoting/casing like the existing unit test. The app's receipt upload implementation was unchanged. After the final draft-preservation fix, all 13 account/notification cases passed together on a dark phone, and the name/rotation case passed five consecutive focused runs. Both light-tablet capture scenarios passed across the combined run and an isolated rerun; the combined run still had an intermittent sign-in startup timeout, so device-test orchestration is not claimed to be fully stable.
- New device coverage includes missing-profile setup/skip, fetch failure, Unicode name save and live Home refresh, password errors/exact values/rotation/clearing, confirmed logout, one-time token rotation/copy/revocation, interrupted key creation, notification dismissal and clear beyond 100, lost responses, safe routing/back selection, deleted targets after rotation and session expiry while a token is visible.
- Visual/device checks cover phone and tablet widths, light/dark, landscape, live resizing, 200% text, and a keyboard-open landscape editor. The keyboard test checks bounds against the visible window and taps Save through touch input. Verification corrected IME padding placement, secure-window flag cleanup after rotation, stale-read cancellation and the light-theme hero action's contrast. Profile and notification screenshots were reviewed at both phone/tablet widths in both themes. Shared design-system contrast and 48dp semantics/focus checks remain passing. Screen captures contain isolated fixtures only.

Run the focused device class using the existing Gradle-built test APK and an emulator:

```sh
adb shell am instrument -w -r \
  -e class dev.abhishekan.pulse.AccountNotificationsDeviceTest \
  dev.abhishekan.pulse.test/androidx.test.runner.AndroidJUnitRunner
```

Tests use production session persistence, Ktor, repositories, use cases, Koin and navigation with source-derived HTTP fixtures. No personal/live backend account was accessed. Saved-state reconstruction is unit tested; device tests exercise recreation and resizing, not an OS process kill during an in-flight account write. Physical devices, manual TalkBack, external hardware keyboards, other Android versions and actual translated production layouts remain unverified. The endpoint's 100-item limit, one-time key tokens and unchanged password/session behavior are backend constraints, not offline guarantees.

Phase 7 stops here. Phase 8 has not started.


## Custom visual foundation

See [Design system](docs/design-system.md) for the Pulse-owned token architecture,
light/dark colors, typography, shared components, font licenses and debug showcase.
The [pre-implementation audit](docs/design-system-audit.md) records the previous
Material styling dependencies and migration scope. Validation results are recorded
in [design-system validation](docs/design-system-validation.md).

## Garage vehicle classification

Adding a vehicle requires an explicit two-wheeler/four-wheeler selection and a fuel type. Dual tank reveals a required second, distinct fuel type. New vehicle drafts do not guess these values; selections survive activity recreation, and choosing the current secondary fuel as the primary clears the secondary selection for review.

The Android domain/network models and create payload now carry `vehicle_type` and `fuel_types`, matching the Go API introduced with migration 00024. Vehicle cards show the saved classification and fuel types. Use this app with that backend migration and API deployed; missing or invalid classifications are reported as malformed responses rather than replaced with guesses.

Verification: debug app/test APKs built; 98 unit tests across Garage, data, domain, and app passed; affected formatting checks and Garage debug lint passed with no issues. Two targeted Garage instrumentation tests passed on the API 35 tablet emulator using isolated HTTP fixtures, covering required selections, duplicate-fuel handling, restored drafts, create payloads, and the existing garage CRUD flow. The vehicle classification screenshot was visually reviewed. Physical devices and a live backend were not exercised.
