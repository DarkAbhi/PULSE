# Rust review: entire rust-backend

Implementation follow-up: findings 1–9 have been addressed, along with the CI/toolchain
changes in 11 and the error/diagnostic cleanup in 12. All 18 tests pass, including
real-migration HTTP ownership/boundary checks, concurrent imports, shared-name
protection, corrected workout dates, bounded metric labels, and container cleanup
after a test task panics. Formatting, standard Clippy with warnings denied, and
documentation generation pass. The original review below is retained as a record
of the findings and their rationale; its line numbers refer to the reviewed version.
The intentionally public, all-user summary in finding 10 is unchanged. Broad
documentation polish and splitting long functions remain optional; no dependency
vulnerability scan or production deployment was performed.

Reviewed on 31 October 2026 using the repository's `rust-review` and `rust-best-practices` skills. This is an **application**, not a reusable library: one binary crate, edition 2024, no `lib.rs`, no declared MSRV, and Rust 1.98.1 installed locally. The backend authenticates fitness imports, writes to shared PostgreSQL tables, exports a public fitness summary, and serves operational metrics. The structure is appropriately small, but it does not yet meet the requested best-practice standard. The most important correction is preventing one user's import from renaming activity types used by other users. Five correctness or resource-safety issues need attention before stylistic cleanup.

This is a review, with suggested patches rather than application changes. Snippets show the relevant change; accompanying text identifies additional callers, tests, and compatibility decisions needed for a complete implementation.

## Verification and scope

Read all 13 Rust source files, 1,779 lines including tests, plus Cargo metadata and lockfile, toolchain configuration, Dockerfile, README, example environment, Docker ignore file, and relevant external integration points: release metadata, Compose configuration, CI, Go fitness/gym migrations, gym writes, and API-key generation/storage.

| Check | Result |
| --- | --- |
| `cargo fmt --check` | Passed |
| `cargo clippy --all-targets --all-features -- -W clippy::pedantic` | Completed; 14 production warnings; test target reported 17, including repeated production warnings |
| `cargo clippy --locked --all-targets --all-features -- -D warnings` | Passed; default Clippy does not enable pedantic checks |
| `cargo test --locked --all-features` | All 10 tests passed with local Docker and socket access |
| `cargo doc --locked --no-deps` | Passed; this alone does not enforce documentation coverage |
| `cargo tree --locked --duplicates` | Inspected; multiple dependency generations and host/target builds exist; no unsupported upgrade recommended |
| Temporary regression probes | All 12 tests passed in a separate copy, with extra assertions reproducing findings 1, 2, 3, and 5 |

The initial restricted test attempt passed five tests and failed five because local socket binding and Docker access were unavailable. The subsequent unrestricted local run passed all ten; those initial failures are not backend defects. Regression probes changed only a temporary copy, not repository application or test code.

### File coverage

| File under rust-backend | Assessment |
| --- | --- |
| `src/main.rs` | Startup checks, listeners, shutdown, and errors reviewed; see 11 and 12 |
| `src/config.rs` | Required settings and defaults reviewed; see 12 |
| `src/state.rs` | Cheap pool cloning is appropriate; no additional abstraction needed |
| `src/auth.rs` | Hash lookup, revocation, header parsing, and middleware reviewed; strengthen route-level tests under 8 |
| `src/error.rs` | Error derivation, source chaining, status mapping, and safe client messages are appropriate; see 12 |
| `src/metrics.rs` | Cancellation guard and route labels are good; method cardinality needs fixing under 5 |
| `src/db/mod.rs` | Explicit module declarations; no issue found |
| `src/db/activity_rings.rs` | Parameterized writes and conflict keys reviewed; see 2 and 10 |
| `src/db/workouts.rs` | Selection, joins, ordering, serialization, and tests reviewed; see 1 and 10 |
| `src/routes/mod.rs` | Route protection, CORS, health checks, release metadata, summary reviewed; see 8 and 10 |
| `src/routes/fitness_rings.rs` | Aliases, key trimming, numeric strings, validation and responses reviewed; see 2 and 8 |
| `src/routes/fitness_activity_rings.rs` | Entire validation/import/transaction path and fixtures reviewed; see 1–4 and 6–8 |
| `src/testhelper.rs` | Disposable database isolation and cleanup reviewed; see 9 |

## Must fix

### 1. Imports can rename other users' existing activity types

**Location:** `rust-backend/src/routes/fitness_activity_rings.rs:274`, with consumers at `rust-backend/src/db/workouts.rs:31` and `:41`.

**Rule:** Validate trust boundaries and enforce domain invariants (`rust-best-practices` §§1, 4; Microsoft `M-STRONG-TYPES-GUARD`). Ownership must remain consistent across application boundaries.

**Problem:** `workout_activity_types` is global, but every authenticated import executes `ON CONFLICT (raw_value) DO UPDATE SET name = EXCLUDED.name` using the caller's name. User 2 can submit raw type 50 with a different name, and user 1's existing workouts immediately acquire that name. Summary selection also uses these mutable names, so the rename can remove legitimate strength/run/ride entries from the public summary. This was reproduced against disposable PostgreSQL.

**Fix:** Preserve existing shared names on conflict. The smallest change that retains `RETURNING id` is:

```sql
ON CONFLICT (raw_value) DO UPDATE
SET name = workout_activity_types.name
RETURNING id
```

This prevents overwriting existing types; it does not authenticate the name of a previously unseen type. If names drive business logic, seed/validate canonical raw-value mappings and select by those values instead of user-controlled display names. Do not invent the supported raw-value list during this patch. Add a two-user regression test proving an import cannot rename an existing type or change another user's summary classification.

### 2. Numeric validation accepts values the database cannot store

**Locations:** `rust-backend/src/routes/fitness_activity_rings.rs:47`, `:51`, `:62`, `:438`; `rust-backend/src/routes/fitness_rings.rs:131`, `:140`; schema in `go-backend/migrations/00017_fitness_workouts.up.sql:18`.

**Rule:** Make invalid states unrepresentable and reject invalid input at the boundary (`rust-best-practices` §§1, 4, 5); `clippy::cast_precision_loss`, `clippy::cast_possible_truncation`.

**Problem:** Both routes accept i64-sized whole values while ring counters and activity raw values are PostgreSQL `INTEGER`/i32. Thus `2147483648` validates and fails during SQL execution with HTTP 500. Numeric columns are bounded too: `NUMERIC(8,2)` for calories, `NUMERIC(10,3)` for duration, and `NUMERIC(10,2)` for distance; finite values such as `1e100` pass validation but cannot be stored. Additionally, integer conversion through f64 changes `9007199254740993` to `9007199254740992`, and the flat parser accepts the string `9223372036854775808` and saturates it to `i64::MAX`. Temporary assertions confirmed these parser behaviors. The database usually rejects these large values, so this is not evidence of persisted integer corruption.

**Fix:** Validate against each storage column's actual range before opening a transaction. For the existing whole-number path, tighten the upper bound:

```rust
n.is_finite()
    && n >= 0.0
    && n.fract() == 0.0
    && n <= f64::from(i32::MAX)
```

Keep validated counters as i32 or bounded integers through the SQL binds; eliminate the i64 → f64 → i64 round trip in `metric`/`save_rings`. Preserve accepted whole floats such as `7500.0` and the flat route's numeric strings. Add per-column decimal limits with awareness of rounding at the declared SQL scale. Cover the maximum accepted value, first rejected value, fractions, negatives, and non-finite numeric strings. Update the existing overflow/rollback test at `fitness_activity_rings.rs:593`: it deliberately expects a 500 today. Keep a separate database-failure rollback test when invalid input moves to HTTP 400.

### 3. Correcting a workout's date can fail the entire import

**Location:** `rust-backend/src/routes/fitness_activity_rings.rs:358`.

**Rule:** Expected input and operational outcomes use fallible control flow; preserve resource and domain invariants (`rust-best-practices` §§4–5; Microsoft `M-PANIC-ON-BUG`).

**Problem:** An initial strength import creates a gym visit with a unique `fitness_workout_id`. Reimporting the same UUID with a start date on another India calendar day updates the workout, then tries to insert another visit because no visit exists on the new day. The existing visit still owns that workout ID, causing a unique-constraint error and rolling back the entire import. Reproduced: moving the example workout from 26 July to 27 July returns HTTP 500, `Unable to save gym visit.`

**Fix:** Under the existing policy of preserving saved visits and their exercises, add the specific conflict target to the gym insertion:

```sql
ON CONFLICT (fitness_workout_id) DO NOTHING
```

This lets the workout correction succeed while retaining the original gym visit and its date. If corrected dates should move gym visits too, implement that explicitly, preserving exercises, sets, and any existing visit on the destination day; the one-line change does not implement that policy. Test a changed date, repeated import, and preservation of linked exercise data.

### 4. Unordered activity upserts can deadlock concurrent imports

**Location:** `rust-backend/src/routes/fitness_activity_rings.rs:254` and `:272`.

**Rule:** Keep concurrency invariants explicit and acquire shared resources consistently (`rust-best-practices` §7). HashMap order must not determine externally significant behavior (§6).

**Problem:** Separate HashMaps choose independent iteration orders. Two uploads containing types A and B can lock/update A then B versus B then A. PostgreSQL retains conflict-update locks until transaction completion, so this permits a deadlock and one otherwise valid upload returns 500. The later per-user advisory lock does not protect these earlier writes; different users share these rows as well. This is a static lock-order finding, not a reproduced load-test result.

**Fix:** Sort only the collection that determines write order; the lookup map can remain a HashMap:

```rust
use std::collections::{BTreeMap, HashMap};

let mut activity_types = BTreeMap::new();
```

Add a concurrent multi-connection test with overlapping types in opposite input orders. Keep all shared row acquisition in a consistent order as the importer evolves; this change addresses the activity-type cycle identified here.

### 5. Arbitrary HTTP methods create unbounded metric series

**Location:** `rust-backend/src/metrics.rs:108`.

**Rule:** Validate untrusted input before using it in long-lived state (`rust-best-practices` §§1, 4); bound resource use at trust boundaries.

**Problem:** Paths are safely normalized, but the method label uses arbitrary request text. HTTP permits extension method tokens. Each distinct token creates retained counter, histogram, and gauge children even when the route rejects it, allowing unauthenticated callers to grow application memory and scrape size. A temporary HTTP probe confirmed `REVIEW_CUSTOM_METHOD` appears in exported metric labels. Actual exploit throughput depends on any upstream method filtering; none is enforced here.

**Fix:** Use a fixed label set for standard methods plus one fallback:

```rust
let method = match request.method().as_str() {
    method @ ("GET" | "HEAD" | "POST" | "PUT" | "PATCH"
        | "DELETE" | "OPTIONS" | "CONNECT" | "TRACE") => method,
    _ => "OTHER",
};
let method = method.to_owned();
```

Retain a test sending multiple distinct custom methods and asserting they share `OTHER`, with no raw tokens in the exporter.

## Should fix

### 6. Validation produces no typed result, so persistence reparses and unwraps

**Locations:** `rust-backend/src/routes/fitness_activity_rings.rs:170`, `:257`, `:345`, `:413`.

**Rule:** Parse into valid types once (`rust-best-practices` §§1, 4); Microsoft `M-STRONG-TYPES-GUARD`, `M-PANIC-MESSAGE`; avoid ambiguous boolean modes (§4).

**Problem:** Validation returns `()`, while the importer continues carrying raw JSON and repeats parsing with numerous `unwrap()` calls. The current immutable payload and matching checks protect most of these unwraps; I did not establish a malformed-request panic. The real cost is maintaining duplicated assumptions across validation and storage, including a handwritten UUID validator followed by another parser and the `metric(..., whole: bool)` integer/float conversion path.

**Fix:** Have the existing parser return concrete validated data, not a new service hierarchy. Reuse the flat route's `Rings` idea and introduce one concrete workout input containing parsed UUID/timestamps and bounded numeric fields:

```rust
fn parse_payload(payload: &Value) -> Result<ValidatedImport, String>;

let import = match parse_payload(&payload) {
    Ok(import) => import,
    Err(details) => return invalid(details),
};
// SQL binds use import.workouts[i].uuid and parsed timestamps directly.
```

`ValidatedImport` is a proposed type, not an existing helper. Keep the current response JSON, aliases, unit checks, error messages, and numeric-string acceptance specific to each endpoint. Separate integer and decimal metric parsing instead of introducing a more general enum solely to replace a private bool. Move nested persistence into `src/db/` in line with the README when extracting it; avoid duplicating the flat and nested upsert SQL further. Clippy's `too_many_lines` is supporting evidence, not the reason to split every function arbitrarily.

### 7. Duplicate UUIDs are reported as internal server failures

**Location:** `rust-backend/src/routes/fitness_activity_rings.rs:315`.

**Rule:** Expected invalid input returns an actionable validation error (`rust-best-practices` §5).

**Problem:** Duplicate UUIDs, including case variants, are rejected only after activity-type writes and transaction creation. The response is a generic HTTP 500 despite a known client-side validation error. Rollback preserves atomicity, but clients may retry an input that cannot succeed.

**Fix:** Detect duplicates during validation using parsed UUIDs, before database work:

```rust
if !seen.insert(workout.uuid) {
    return Err(format!("workouts[{index}] uuid must be unique within the request."));
}
```

This snippet belongs in the typed parser proposed in finding 6. Return the existing invalid-payload response with HTTP 400. Add same-case and mixed-case duplicate tests. Treat the status/message change as an API correction and check shortcut callers that retry 5xx responses.

### 8. Existing tests miss several important request and concurrency boundaries

**Locations:** `rust-backend/src/routes/fitness_activity_rings.rs:444` (test module), `rust-backend/src/routes/fitness_rings.rs:145` (test module), `rust-backend/src/auth.rs:49` (test module), `rust-backend/src/testhelper.rs:50`.

**Rule:** Test observable behavior, boundary cases, and external interactions (`rust-best-practices` §11; Effective Rust item 30).

**Problem:** The ten tests provide useful happy-path, revocation, rollback, and gym-preservation coverage, but all database helpers restrict the pool to one connection and fixtures are temporary tables. They cannot exercise competing transactions. The flat route has no successful persistence test, and auth parsing plus hash lookup are tested separately without verifying the middleware-to-handler ownership path. Findings 1–5 were not caught by the current suite.

**Fix:** Retain current fast tests and add focused cases to their existing modules. A route-level check should assert observable status and persisted ownership, for example:

```rust
assert_eq!(unauthenticated.status(), StatusCode::UNAUTHORIZED);
assert_eq!(response.status(), StatusCode::CREATED);
assert_eq!(saved_user_id, key_owner_id);
```

These variables represent results from the proposed test setup, not existing fixture APIs. Cover numeric bounds, repeated/corrected workouts, duplicate UUIDs, shared-name protection, malformed auth, and custom method labels. For concurrency, use regular tables inside an already disposable database plus at least two connections; temporary tables are connection-local. At least one schema-compatibility test should use actual migrations, because handwritten fixtures omit production foreign keys and check constraints. No need to convert this binary into a library merely to relocate its tests.

### 9. Docker subprocesses block async workers and can hang cleanup

**Location:** `rust-backend/src/testhelper.rs:13`, `:37`, and `:72`.

**Rule:** Move blocking work off async workers and do not block in destructors (`rust-best-practices` §§5, 7).

**Problem:** `std::process::Command::output()` runs synchronously inside async test setup, and `Drop` synchronously calls Docker again. The 60-second readiness timeout only surrounds database connection attempts; it cannot interrupt hung Docker setup, port discovery, or removal. This is test reliability and diagnostics risk, not a production request-path defect.

**Fix:** Use Tokio's process support for setup and discovery and apply a command timeout:

```rust
use tokio::process::Command;

let output = tokio::time::timeout(
    Duration::from_secs(30),
    command.kill_on_drop(true).output(),
)
.await
.expect("Docker command exceeded 30 seconds")
.expect("Docker command could not run");
```

`command` is the existing configured command. Give cleanup an explicit awaited path with bounded execution; keep an unwind-safe best-effort fallback without waiting synchronously on a Tokio worker. Preserve cleanup on assertion failure. This needs care because merely spawning an async removal from `Drop` can lose cleanup as the test runtime shuts down. Do not replace the current guard with that weaker pattern.

## Consider

### 10. The intentionally public summary combines every user's data

**Locations:** `rust-backend/src/routes/mod.rs:47`, `rust-backend/src/db/activity_rings.rs:35`, `rust-backend/src/db/workouts.rs:29`.

**Rule:** Preserve documented intent, but make ownership and disclosure policies explicit (`rust-review` §4; `rust-best-practices` §§1, 4).

**Problem:** The README explicitly says this endpoint is public, so lack of authentication is not classified here as an accidental regression. However, the queries are also unscoped: the rings may belong to user A, the run to user B, and strength data to user C. Rings tied on date have no deterministic user/timestamp tie-breaker. This matters if anyone beyond the intended portfolio owner can import data.

**Fix:** If the intended feed belongs to one portfolio owner, resolve that owner from trusted server configuration and scope every summary query:

```sql
WHERE user_id = $1
ORDER BY summary_date DESC
LIMIT 1
```

```sql
WHERE w.user_id = $1 AND ...
```

Update `latest`, `latest_relevant`, the handler, and multi-user tests together. Do not accept an arbitrary public query parameter as the privacy boundary. If the intended feed really includes all users, document that disclosure clearly and add deterministic ordering. Authentication and narrower CORS should follow the chosen product policy, not be added blindly.

### 11. Lint gates and the toolchain contract are incomplete

**Locations:** `rust-backend/Cargo.toml:1`, `rust-backend/rust-toolchain.toml:2`, `rust-backend/Dockerfile:1`, `.github/workflows/tests.yml:124`.

**Rule:** Microsoft `M-STATIC-VERIFICATION`, `M-MSRV`; Effective Rust items 29 and 32.

**Problem:** Cargo has no lint table or `rust-version`. Development/CI track floating `stable`, while the Docker build explicitly uses 1.98.1. CI runs tests but no formatter or Clippy gate, allowing mechanical regressions and a future CI/compiler mismatch with deployment. Pedantic warnings are concentrated in casts and the importer; semicolon and exact-float test warnings are minor and should not outrank actual bugs.

**Fix:** Declare and test the chosen minimum version and either align the toolchain with the Docker version or deliberately test both. For the currently verified compiler:

```toml
[package]
rust-version = "1.98.1"

[lints.rust]
unsafe_op_in_unsafe_fn = "deny"
```

```yaml
- run: cargo fmt --check
- run: cargo clippy --locked --all-targets --all-features -- -D warnings
```

If supporting an earlier MSRV is desirable, determine it from dependencies and a real build rather than copying this version blindly. Add selected pedantic/restriction lints once the actionable warnings are resolved. Do not add `#![deny(warnings)]` or globally suppress pedantic diagnostics. No need to upgrade dependencies solely because multiple versions occur in `cargo tree`.

### 12. Small documentation and diagnostic cleanups would help maintenance

**Locations:** `rust-backend/src/error.rs:8`, `rust-backend/src/config.rs:3`, `:15`, `rust-backend/src/main.rs:31`, `rust-backend/src/metrics.rs:128`, `rust-backend/src/routes/fitness_activity_rings.rs:38`.

**Rule:** Microsoft `M-LINT-OVERRIDE-EXPECT`, `M-PUBLIC-DEBUG`, `M-CANONICAL-DOCS`; Google Rust Style Guide's application simplicity rule; `clippy::needless_pass_by_value`.

**Problem:** A broad `#[allow(dead_code)]` appears unnecessary for the currently used error variants. Public internal types and functions generally lack API/module docs. Config's PORT error discards the offending value and parse cause, and startup wraps SQLx errors into strings rather than preserving their sources. `invalid(String)` only reads its input. The idle-connection cast triggers Clippy, although SQLx's u32 pool capacity already bounds realistic values; it is not a demonstrated overflow bug.

**Fix:** Remove the broad allow if it is no longer needed; otherwise use narrow `#[expect(..., reason = "...")]`. Borrow the validation message:

```rust
fn invalid(details: &str) -> Reply {
    reply(
        StatusCode::BAD_REQUEST,
        json!({"error": "The activity-rings payload is invalid.", "details": details}),
    )
}
```

Update callers to borrow owned messages and remove unnecessary `.into()` calls for literals. Add concise module docs and document actual side effects and validation contracts, rather than filling every internal field with redundant prose. Derive Debug where useful for ordinary internal data, but **do not derive Debug on Config without redacting the credential-bearing database URL**. Retain SQLx errors as causes if introducing startup context, without adding an error-handling dependency just for these two messages. Keep the metrics conversion checked if changing it, and retain a descriptive failure message for any invariant assertion.

## What's already good

- SQL writes bind user-supplied values; workout/ring conflict keys include the authenticated owner. API keys use hashed lookup, revoked keys stop working, and Go generates keys from cryptographic randomness.
- Nested imports are transactional. The existing test verifies rollback after a later failure and preservation of gym exercises, catalogue links, and sets during repeated imports. Keep these guarantees through refactoring.
- Metrics use matched route templates instead of raw paths, keep the exporter on a separate listener in Compose, and use a Drop guard to balance in-flight counts on cancellation. Readiness has a timeout and is separate from liveness.
- The application uses concrete state and SQLx types, cheap pool/shared-handle clones, `thiserror`, structured tracing, edition 2024, a committed lockfile, and a non-root runtime image. No first-party unsafe code, manual ordering/hash implementations, poisoned-lock recovery, or lock guards held across await were found. No extra traits, workspace split, new allocator, or blanket library API redesign is justified by this review.

## Lint configuration suggestion

Because the project lacks a lint table, review the skill-recommended [BeautifulRust configuration](https://github.com/morganlinton/BeautifulRust), specifically `lints/lints.toml` and `lints/clippy.toml`, and adopt only rules appropriate to this application. The skill notes that its suppression-related rules require Rust 1.81 or later. Relevant lint categories would flag the numeric casts in finding 2, the unreasoned allow in finding 12, and the needless owned message in finding 12. Lints will not detect cross-user data mutation, SQL lock-order cycles, or the gym-date conflict; those need the regression tests above. The external preset was suggested as required by the review skill, not fetched or audited during this local review.

Not checked: dependency vulnerability databases (`cargo audit`/`deny` unavailable), production deployment, exhaustive fuzzing, performance benchmarks, Linux execution, or a live concurrency reproduction of finding 4. Miri was unnecessary because no first-party unsafe code was found.

Historical review risk: the original suite passed despite the reproduced defects.
The implementation follow-up at the top records the fixes and new regression coverage.
