# Pulse Android

Native Kotlin/Compose app for `dev.abhishekan.pulse`. Phase 2 implements username/password authentication through the existing Go API, encrypted cookie restoration, and a real dashboard. Product screens fetch API data; preview and test fixtures are confined to debug/test source sets.

## Setup

Open this directory in Android Studio. Keep `local.properties` local and point `sdk.dir` at the Android SDK. Install platform 37, accept SDK licenses, and use the checked-in wrapper. The daemon configuration selects JDK 25; Java/Kotlin bytecode targets Java 11. Minimum Android API remains 28.

```sh
./gradlew build
./gradlew :app:connectedDebugAndroidTest # emulator/device required
./gradlew :app:installDebug
```

The original AGP 9.4.1, Gradle 9.6.0, Kotlin/Compose compiler 2.2.10, SDK 37, Compose BOM 2026.02.01, application ID, and daemon toolchain are preserved. AGP supplies built-in Kotlin for Android modules; only domain applies the Kotlin JVM plugin. Do not add the legacy Kotlin Android plugin.

All versions live in `gradle/libs.versions.toml`. Compatible stable additions remain Koin 4.1.1, Ktor 3.2.3, coroutines 1.10.2, serialization 1.9.0, DataStore 1.1.7, Lifecycle/Navigation 2.9.3, and Activity 1.10.1. Phase 2 uses the existing dependency set; HTTP cookies require no additional Ktor library.

## API addresses

Debug defaults to **`http://10.0.2.2:18081/api/`**, the emulator-accessible address for this repository's Go development container. Start the backend using the repository's development instructions. Android's localhost is the Android device, not the development computer.

```sh
./gradlew :app:assembleDebug -Ppulse.debugApiUrl=https://your-development-host.example/api/
./gradlew :app:assembleRelease -Ppulse.releaseApiUrl=https://your-production-host.example/api/
```

Use an Android-reachable HTTPS address on real devices. Addresses must end in `/api/` and cannot contain credentials, query parameters, or fragments. Only debug permits HTTP to `10.0.2.2`; release rejects cleartext. The release fallback **`https://pulse.invalid/api/`** is deliberately non-routable. Supply the real HTTPS origin before using a release build. No account credentials are supplied or persisted by the app.

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
| `:app` | Application/Koin startup, session gate, appearance, Compose navigation, environment configuration | All six modules |
| `:core:domain` | Pure models, outcomes, repository contracts and use cases | None; coroutines only |
| `:core:data` | Repository implementations, mapping, DataStore preferences, Keystore encrypted storage | Domain, network |
| `:core:network` | One Ktor/OkHttp client, API services/DTOs, cookie policy and storage contract | None |
| `:core:ui` | Theme/tokens, adaptive helper, individually organized components; debug gallery | None |
| `:feature:auth` | Login screen, immutable state, ViewModel, feature DI | Domain, UI |
| `:feature:home` | Dashboard/notification presentation, independent section state, ViewModel, feature DI | Domain, UI |

Features never import data/network or one another. Domain has no Android, Koin, Ktor, or persistence dependencies. Boundary tests enforce the module edges and import restrictions. No database/common/testing module, offline sync, or additional architecture framework was introduced.

MVVM uses constructor injection and immutable `StateFlow`; Compose collects lifecycle-aware state. Use cases register with `factoryOf`, owned repositories with `singleOf`, and ViewModels with `viewModelOf`. Infrastructure converts HTTP, timeout, connection, decoding, and storage failures into domain outcomes. Coroutine cancellation propagates. Only confirmed local session cleanup finishes in `NonCancellable`, so disposing an expired navigation flow cannot interrupt revocation.

## Authentication and navigation

Startup stays behind a session gate while encrypted material is read and a saved session is validated. A missing/expired cookie opens Login. Network/timeouts/server failures show a retry screen **without deleting the saved session or exposing authenticated content**. A separate explicit Forget action can remove an unreadable/device-local session; it does not revoke other devices' sessions.

Login trims only the username and preserves the exact password. Required fields validate inline; credentials/connection/timeout/server failures use safe messages. Duplicate submissions are blocked. Username is saved in `SavedStateHandle`; passwords stay only in the ViewModel's memory, surviving configuration changes but not process death. The Compose String text-field implementation also keeps its editing value in ordinary `remember`, not saved-state storage. Password visibility, autofill semantics, Next/Done IME actions, and keyboard insets are supported.

One activity hosts Compose navigation. Successful login disposes the Login flow. Signed-in tabs are **Home** and **You**, using Navigation's saved/restored destination state. Notifications open within Home. Logout or confirmed expiry disposes authenticated destinations. Expiry is checked by a lifecycle-bound timer, on foreground resume, and on authenticated 401 responses. A stale request cannot expire a newer session generation.

Logout is not retried automatically. A connection failure keeps the active session and reports a Snackbar; a confirmed logout removes access. Storage-cleanup failures are surfaced and cannot silently restore a cookie in memory. The client disables redirects, connection retries, and HTTP logging.

## Session storage

`PersistentSessionCookies` implements Ktor `CookiesStorage`. It retains only `life_session`, restricted to the configured API's scheme/host/port, and honors path boundaries, Secure, expiry, and Max-Age precedence. The backend currently uses a host-only cookie with path `/`; parent-domain sharing is deliberately not accepted. Cookies without explicit expiry are not persisted. The envelope contains only the cookie value and required scope/expiry metadata, never username/password.

`EncryptedSessionStore` writes an authenticated AES-256-GCM envelope with randomized IVs, a versioned format, and a non-exportable Android Keystore key. Atomic writes and mutexes protect the file in `noBackupFilesDir`. Backup and device transfer exclude app data. DataStore contains theme preferences only.

Read/corruption/missing-key failures do not silently erase material. Failed deletions remain pending and must complete before a subsequent request proceeds. Login starts from a cleared signed-out cookie state, so an incomplete older response cannot authenticate a different account accidentally.

## Dashboard and design system

Profile, workout, finance, and notifications load in separate jobs. Each section has loading, empty, error and retry presentation. A failed section cannot erase successful sections. Missing profile information falls back to username. Workout status refreshes on resume using the backend's Asia/Kolkata definition of today.

Financial numbers are parsed directly from JSON numeric text into `BigDecimal`, never through `Double`. Display pads short fractions to two places and preserves every supplied decimal place; balances and progress are not fabricated or recomputed. All-zero backend values remain actual zero values.

Home uses a warm financial hero, supporting activity/notification cards, and a read-only overview of the four spaces. You provides username, persisted System/Light/Dark appearance, and working logout. Deferred space workflows, profile editing, notification mutations, and onboarding have no clickable placeholders.

Shared controls now live in **`core/ui/.../components/`**, one component per file; related models sit with the component that owns them. Themes/tokens and adaptive helpers remain beside that package. There are four type sizes, two weights, tabular/monospace metrics, 4dp spacing increments, 48dp targets, labeled selection semantics, native focus behavior, and tested contrast pairs. Error/retry cards use neutral surfaces so action text remains accessible.

Layouts measure available width: compact below 600dp, medium from 600dp, expanded from 840dp. Phones stack cards; larger windows use readable columns. Tablet login puts branding beside a constrained form. Phone/landscape forms scroll with the keyboard open. Orientation is not locked. Bottom navigation is retained on large windows.

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

No personal backend login was performed: API integration is verified against source-derived fixtures through the production stack. Physical-device TalkBack and external-keyboard traversal remain unverified. The real release HTTPS API origin still needs configuration. Phase 3 and later feature workflows have not been started.
