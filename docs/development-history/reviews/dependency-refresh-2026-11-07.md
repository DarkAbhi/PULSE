# Dependency refresh — November 7, 2026

Direct dependencies were checked against their official package registries for the latest stable releases. Prerelease versions were excluded. npm, Cargo, and Go lock/checksum files were refreshed alongside their manifests.

## Web frontend

| Dependency | Previous | Updated |
| --- | --- | --- |
| Google GenAI | 2.13.0 | 3.0.0 |
| Lucide React | 1.25.0 | 1.70.0 |
| Next.js | 16.3.5 | 16.4.5 |
| React / React DOM | 19.2.7 | 19.3.1 |
| Playwright | 1.64.1 | 1.65.0 |
| Tailwind / PostCSS plugin | 4.3.2 | 4.3.5 |
| Node types (resolved) | 26.1.1 | 26.6.6 |
| React types | 19.2.17 | 19.3.0 |
| React DOM types | 19.2.3 | 19.3.0 |

TypeScript 7.0.2 and next-themes 0.4.6 were already current. Docker now uses Node 26 LTS rather than Node 20, satisfying GenAI 3's Node >=22 requirement. Frontend CI also uses Node 26.

Validation: production build, TypeScript, 26 unit tests, and six production browser tests passed. `npm outdated` reported no outdated dependencies, `npm ls` was valid, and npm audit reported zero vulnerabilities.

## Rust backend

| Dependency | Previous | Updated |
| --- | --- | --- |
| serde_json | 1.0.151 | 1.0.154 |
| sha2 | 0.10 | 0.11.1 |
| Tokio | 1.53.1 | 1.54.1 |
| UUID | 1.26.1 | 1.29.0 |

Other original direct dependencies were already current. The lockfile refresh updated 44 transitive packages. `hex` 0.4.3, previously transitive, is now direct: sha2 0.11 removed digest `LowerHex`, so token hashing now uses `hex::encode` to preserve lowercase hexadecimal hashes.

Validation: formatting, strict Clippy for all targets/features, and all 19 tests passed, including six database tests. The database checks used explicitly approved temporary Docker socket access and a temporary port-discovery adapter for Docker Desktop networking; no adapter changes remain in the repository, and test containers were removed.

## Go backend

| Dependency | Previous | Updated |
| --- | --- | --- |
| AWS SDK core | 1.45.1 | 1.47.7 |
| AWS SDK config | 1.33.1 | 1.33.15 |
| AWS SDK S3 | 1.109.1 | 1.114.8 |
| Chi | 5.2.3 | 5.5.0 |
| pgx | 5.7.6 | 5.12.0 |
| Prometheus client | 1.24.1 | 1.25.0 |
| OpenTelemetry | 1.46.0 | 1.47.0 |
| gRPC | 1.83.1 | 1.85.0 |
| golang-migrate | 4.19.0 | 4.20.1 |
| testcontainers-go | 0.39.0 | 0.45.0 |
| x/crypto | 0.55.0 | 0.58.0 |

The dependency graph raises the minimum Go version from 1.25 to 1.26. The Docker builder uses Go 1.27.3. Existing Swagger and sqlc generator pins were checked and are already at their latest stable releases; Swagger v2 prereleases were excluded.

Testcontainers' SQL wait callback now uses Moby's `network.Port`. Database test migrations use the filesystem source adapter instead of a `file://` URL. The production migration runner also normalizes the file URL so Windows drive letters stay in the path rather than being interpreted as a host. Migration files and database behavior are unchanged.

The vulnerability scan found zero imported-package or reachable vulnerabilities. [GO-2026-5932](https://pkg.go.dev/vuln/GO-2026-5932) is reported only at module level for the unused `golang.org/x/crypto/openpgp` package; the scanner identifies a prerelease pseudo-version as the fix, so the requested latest stable x/crypto 0.58.0 was retained.

Validation: production build, integration-tagged vet, and module checksum verification passed. All 114 top-level tests were covered across the initial full unit/integration run and targeted retries: 18 tests initially failed during Docker setup, then passed with an explicit Docker connection and reduced package concurrency. This includes all six migration tests. No test failures remain. No explicitly required module has a newer stable version available. Test resources created by this upgrade run were cleaned up.

## Pulse Android

| Dependency | Previous | Updated |
| --- | --- | --- |
| Android Gradle plugin | 9.4.1 | 9.5.1 |
| Gradle | 9.6.0 | 9.9.0 |
| Ktor | 3.6.0 | 3.6.1 |
| Kotlin serialization | 1.11.0 | 1.12.0 |
| DataStore | 1.2.1 | 1.3.0 |

The Gradle distribution checksum was updated from the official release metadata. Other version-catalog libraries and plugins were already current, including Kotlin 2.4.21, Koin 4.2.2, Compose BOM 2026.09.00, and coroutines 1.11.0.

Validation: `test lint ktlintCheck :app:assembleDebug` passed, with 691 tasks executed. All 253 unit tests across 44 suites passed with no skips. The debug APK assembled successfully. Device instrumentation tests were not run.

## Hermes MCP server

Raised the SDK requirement from `mcp>=2.0,<3` to `mcp>=2.3.0,<3`. Installed SDK 2.3.0 in an isolated environment; dependency consistency, the existing unit test, server import, and real in-memory client discovery of all seven tools passed.

## Release sources

- Web: [npm registry](https://registry.npmjs.org/), [Node releases](https://nodejs.org/en/about/previous-releases).
- Rust: [crates.io](https://crates.io/), [sha2 0.11.1 API](https://docs.rs/sha2/0.11.1/sha2/).
- Go: [official module proxy](https://proxy.golang.org/), [Go releases](https://go.dev/dl/).
- Android: [Google Maven](https://maven.google.com/), [Maven Central](https://central.sonatype.com/), [Gradle release metadata](https://services.gradle.org/versions/current), [Ktor 3.6 changelog](https://ktor.io/changelog/3.6/).
- MCP: [PyPI release 2.3.0](https://pypi.org/project/mcp/2.3.0/).
