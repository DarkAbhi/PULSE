# Dependency usage audit — November 7, 2026

The audit checked source imports, configuration, build scripts, tests, and required runtime support. A missing application import alone was not treated as proof that a dependency was unused: framework support, build plugins, test tools, and transitive dependencies were checked separately.

## Web frontend

All 13 declared dependencies are used. No packages or lockfile entries were removed.

- Google GenAI powers statement extraction called by the financial horizon action.
- Lucide supplies UI icons; Next.js and React supply the application framework.
- React DOM is imported directly for portals in dialogs and the exercise form.
- next-themes supplies the theme provider and settings.
- Tailwind is imported by the stylesheet, and its PostCSS plugin is configured.
- Playwright supplies the browser tests; TypeScript and the Node/React type packages support compilation.

Validation: TypeScript checking and all 26 unit tests passed. No frontend changes were necessary.

## Rust backend

All 15 direct libraries have active uses. Authentication uses SHA-256 and hex; database/response types use Chrono, UUID, Serde, and SQLx; middleware uses CORS/tracing; metrics use Prometheus; startup uses dotenv and logging.

Unused optional features were removed:

- Axum form/query extraction and original-URI tracking.
- Chrono default features, retaining `serde` and `std` for stored and parsed date/time values.
- SQLx query-macro/offline support, retaining `derive` for `FromRow`.
- Tokio's broad `full` feature, replaced by the required runtime capabilities. Process execution and I/O helpers are enabled only for tests.

The refreshed lockfile removes `serde_urlencoded` and `ryu`. No application source changes were needed.

Validation: production compilation, formatting, strict Clippy for all targets/features, and all 19 tests, including database tests, passed. Temporary test resources were cleaned up.

## Go backend

No unused dependencies were found. `go mod tidy -diff` produced no changes, and `go mod why -m` confirmed a dependency path for all 114 explicitly required modules.

Testcontainers and Moby are used by database test setup. Packages such as Testify, purego, Docker connections, and x/tools remain necessary indirect dependencies. Swagger and sqlc pins are active generator commands in the Makefile and documented workflows.

No Go files changed. The expensive database suite was not repeated for this audit because the module graph and source were unchanged.

## Hermes MCP server

The only external requirement, `mcp`, is used directly: `server.py` imports `MCPServer`, registers seven tools, and starts the Streamable HTTP server. Other imports are Python standard-library modules. No dependency removal or code changes were warranted.

## Pulse Android

The module audit removed seven redundant dependency declarations:

- One duplicate coroutine-test declaration in core UI.
- Three preview dependencies in app, profile, and notifications, which have no preview sources.
- Two redundant UI tooling declarations in profile and notifications; the app keeps its explicit debug UI tooling dependency for Layout Inspector.
- One app lifecycle-runtime-ktx declaration with no direct KTX API use. Lifecycle runtime Compose retains the same runtime dependency/version. Its now-unused catalog alias was also removed.

Seven modules use previews only in debug sources, so their preview dependencies now use `debugImplementation` rather than being included in release builds. Modules with previews retain debug UI tooling.

The data module now declares coroutine core rather than coroutine Android; it uses only core APIs. The app retains the Android main-dispatcher dependency.

Android test framework and coroutine-test declarations were retained where the Compose test runner needs them or their explicit versions prevent older transitive defaults. Koin's ViewModel support/version alignment, the app's FileProvider support, and runtime debugging dependencies were also retained.

Validation: `test lint ktlintCheck :app:assembleDebug :app:assembleDebugAndroidTest :app:compileReleaseKotlin` passed. All 253 unit tests across 44 suites passed without skips. Both debug APKs assembled, including the instrumentation test APK, and release Kotlin compilation passed. Instrumentation tests were compiled but not run on a device.
