---
name: pulse-deslop
description: >-
  Use when reviewing, refactoring, or generating Kotlin and Jetpack Compose code
  in the Pulse Android app (tablet-first personal dashboard). Finds and removes
  AI-generated slop: ghost interfaces, pass-through use cases, redundant mappers,
  legacy async code, service-locator DI, state bags and duplicated state in
  composables, hardcoded styling, and code that ignores the project's architecture. Use before merging any
  AI-assisted change, or when cleaning up existing code.
---

# Pulse De-Slop

## Overview

"Slop" is code that looks professional but adds no value: it follows a generic
template instead of this project's actual needs and architecture. AI tools produce
it by default because they pattern-match on "enterprise Android" examples.

This skill is a **removal and enforcement** skill. The goal is less code that is
more correct, not more code with better names. If a change makes the diff bigger
without making the app simpler, safer, or faster, it is probably slop.

**Stack assumed:** Kotlin, Jetpack Compose, Koin, Ktor, Coroutines/Flow.
**Product context:** Pulse (`dev.abhishekan.pulse`) is a tablet-first personal
dashboard with Home, Fitness, Financial Horizon, Garage, Meal Plan, Notifications, and
Profile areas. It is multi-module (`:app`, `:core:*`, `:feature:*`), has its own design
system (`PulseTheme`, `PulseTokens`, `Pulse*` components), runs in light and dark
themes, and includes an animated breathing orb on the home screen.

## When to Use

- Reviewing any AI-assisted PR or generated file before it lands
- Cleaning up a feature that "works but feels wrong"
- A file imports from a layer it should not know about
- Generating new code (apply the rules *while* writing, not after)

**Skip when:** the change is a one-line fix inside code that already follows these rules.

## Precedence: resolving rule conflicts

The two source documents (the de-slop rulebook and the `android-architecture`
skill) disagree in three places. For Pulse, resolve them like this:

| Topic | Rulebook says | Architecture skill says | **Pulse decision** |
|-------|---------------|-------------------------|--------------------|
| Repository interface | Delete; concrete class only | `interface` in domain, `Impl` in data | **Concrete class.** Add an interface only with a second real implementation (see Rule 1). |
| UseCases | Delete pass-throughs | Always use cases | **Only when they do real work** (coordinate repos, validate, compute). |
| Mappers / models | Share one model if schemas match | DTO → domain → UI | **Map at real boundaries** (backend API JSON to app model). Do not map identical shapes. |
| Module topology, layer direction, Koin constructor DSL, Flow, lifecycle-aware collection | Aligned | Aligned | **Both apply, no conflict.** |

**Rule of thumb:** the architecture skill defines *direction and boundaries*; the
rulebook defines *what is not worth creating*. A boundary can exist without an
interface, and a layer can exist without a use case.

Pulse is multi-module, so the module rules below apply as written. Two clarifications:

- `:app` may depend on features to wire navigation and Koin, but **no feature may depend on `:app`**, and `:app` should not reach into a feature's *internals* (its draft types, form kinds, or ViewModel state) to render shell UI. See the State Ownership Rules.
- Do not create, split, or merge modules as part of a de-slop pass. Propose that separately.

## Workflow

### Step 0: Scope the pass

1. List the files in the change (`git diff --name-only <base>`) or the target feature folder.
2. Do **not** touch unrelated files. A de-slop pass is a focused diff.
3. Read the nearest existing, human-written code in the same area first. The project's real conventions beat any generic rule here.

### Step 1: Detect

Run these from the repo root. Each hit is a *candidate*, not a verdict.

```bash
# Ghost interfaces
rg -n "^(class|object) \w+Impl\b" --glob '*.kt'
rg -n "^interface \w+(Repository|DataSource|Service|Manager|Provider)\b" --glob '*.kt'

# Pass-through use cases
rg -n "operator fun invoke" --glob '*UseCase.kt' -A2

# Legacy async / threading
rg -n "LiveData|MutableLiveData|GlobalScope|AsyncTask|Thread\(|Handler\(|runBlocking|suspendCancellableCoroutine" --glob '*.kt'

# Service-locator DI and framework leaks
rg -n "KoinComponent|by inject\(|\bget\(\)|getKoin\(\)|GlobalContext" --glob '*.kt'

# State bags, child-to-parent flag callbacks, string-route chrome
rg -c "by (rememberSaveable|remember) \{ mutableStateOf" --glob '*.kt' | awk -F: '$2>4'
rg -n "on(Draft|Detail|Title|EditorTitle|SensitiveEdits)Changed\s*=" --glob '*.kt'
rg -n 'route (in|==|!=)|listOf\("[a-z]+' --glob '*.kt'
rg -n 'launchSingleTop = true' --glob '*.kt' -c

# Compose state collection
rg -n "collectAsState\(\)|mutableStateOf\(.*\)\s*$" --glob '*.kt'

# Hardcoded styling
rg -n "Color\(0x|\.dp\b|\.sp\b|Color\.(Red|Blue|Green|White|Black|Gray)" --glob '*Screen.kt' --glob '*Composable*.kt'

# Unsafe and noisy code
rg -n '!!|catch \(e: Exception\)|catch \(e: Throwable\)|Log\.[dviwe]\(|println\(|TODO\(\)' --glob '*.kt'

# Comment slop
rg -n "^\s*// (Initialize|Set up|Create|Call|Return|Handle|Update|Get|Define)\b" --glob '*.kt'
```

### Step 2: Classify each candidate

For every hit, decide one of:

- **DELETE**: the code has no reason to exist
- **INLINE**: the abstraction forwards one call; move the call to the caller
- **FIX**: the code is needed but violates a rule
- **KEEP**: it looks like slop but is justified (see "Not slop" below)

Write the reason in one sentence. If you cannot, it is probably KEEP or you need more context.

### Step 3: Apply fixes in this order

Order matters because early deletions make later fixes smaller.

1. Delete dead code, unused parameters, unused imports, commented-out code
2. Remove ghost interfaces and pass-through use cases
3. Remove redundant mappers and duplicate models
4. Fix layer and dependency direction violations
5. Fix DI wiring (Koin)
6. Fix async and state (Flow, ViewModel)
7. Fix state ownership: classify every local state var, delete mirrors, move events out of flags (State Ownership Rules)
8. Fix Compose issues
9. Fix styling (tokens, strings, theming)
10. Remove comment and naming noise

### Step 4: Verify

```bash
./gradlew ktlintCheck detekt        # if configured
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Then run the Koin graph check (see Rule 9) and open the affected screens on a
tablet emulator in **both light and dark** themes.

### Step 5: Report

Use the output format at the end of this document.

---

## Architecture Rules

### Rule 1: Ghost Interface

**Slop:** an interface plus a single `Impl`, created by reflex.

```kotlin
interface WeatherRepository { fun observe(): Flow<Weather> }
class WeatherRepositoryImpl(private val api: WeatherApi) : WeatherRepository { ... }
```

**Fix:** keep only the class.

```kotlin
class WeatherRepository(private val api: WeatherApi) {
    fun observe(): Flow<Weather> = ...
}
```

**Introduce an interface only when at least one is true:**

- Two or more production implementations exist or are being built (e.g. a live backend vs. a local/demo data source)
- It is a public API of a module that other teams consume
- It wraps a platform class you cannot construct in tests and cannot make `open`

**For tests:** inject a Ktor `MockEngine` into the `HttpClient`, or pass a fake
collaborator through the constructor. Do not create an interface just so a mock library can implement it.

**Naming:** the suffix `Impl` is a smell on its own. If a name needs `Impl` to be
distinct, either the interface should not exist or the implementation deserves a
descriptive name (`HomeAssistantWebSocketSource`).

### Rule 2: Useless Mapper

**Slop:** `XDto → XEntity → XDomain → XUiState` where every field is copied unchanged.

**Fix:** collapse to the fewest models that serve a real boundary.

| Boundary | Map? |
|----------|------|
| Backend API / external JSON → app model | **Yes.** External schemas change and are messy (nullable, stringly typed, extra fields). |
| App model → local storage entity | Only if the stored shape differs (flattened, versioned, encrypted). |
| App model → UI state | Only if the UI needs derived or formatted values. A plain `List<Item>` inside `Success(items)` needs no UI model. |
| Identical shapes | **No.** Share one `@Serializable` data class. |

**Smell test:** if a mapper function body is only `Foo(a = a, b = b, c = c)`, delete it.

**Keep mapping out of Composables.** Parsing raw API values (stringly-typed numbers,
nullable fields, sentinel values like `"unknown"`) belongs in one place in the data
layer, never in UI code.

### Rule 3: Pass-Through UseCase

**Slop:**

```kotlin
class GetFitnessTodayUseCase(private val repo: FitnessRepository) {
    operator fun invoke() = repo.observeToday()
}
```

**Fix:** delete it, inject `FitnessRepository` into the ViewModel.

**Keep a use case only when it does at least one of:**

- Combines two or more repositories (e.g. "Your room to move" derived from budget data plus fitness data)
- Applies business rules or validation
- Does non-trivial computation worth unit testing in isolation
- Is reused by two or more ViewModels with the same logic

A use case that keeps one of these must have a name describing the *rule*, not just
`GetXUseCase`: `CalculateRoomToMove`, `MergeFitnessAndSleep`.

### Rule 4: Layer and dependency direction

Direction is always **UI → Domain → Data**, never upward.

Red flags (each is a FIX, not a style note):

- A Composable calls a repository, `HttpClient`, DAO, or `Koin` directly
- A ViewModel imports Ktor types (`HttpResponse`, `HttpClient`) or Room DAOs
- Domain/pure-Kotlin code imports `android.*`, Compose, Koin, or Ktor
- A feature package imports another feature's package (extract shared code to a common/core location instead)
- Business logic (filtering, formatting rules, thresholds) lives inside a `@Composable`

**If the project is multi-module**, additionally enforce:

- `:feature:*` → `:core:domain`, `:core:ui`, `:core:common` only
- `:core:data` → `:core:domain`, `:core:database`, `:core:network`
- `:core:domain` is pure Kotlin
- Never feature → feature, core → feature, domain → data

### Rule 5: Over-structure for the size of the feature

AI loves to scaffold a full `data/domain/presentation` tree for a three-field card.

- One screen with one data source does not need its own module, repository, use case, mapper, and DTO.
- Start from the smallest thing that preserves the dependency direction in Rule 4.
- Add structure when a *second* consumer or a *real* complexity appears, not before.

---

## Kotlin Rules

| Slop | Fix |
|------|-----|
| `!!` | Model nullability properly: `?.`, `?:`, `requireNotNull(x) { "reason" }`, or make the type non-null at the boundary |
| `catch (e: Exception)` / `Throwable` in suspend code | Catch specific exceptions. Always rethrow `CancellationException` |
| `if (x != null) x.foo()` | `x?.foo()` or smart cast |
| Java-style getters/setters, `private var` + `getX()` | Kotlin properties |
| `Utils` / `Helper` / `Manager` classes full of statics | Top-level functions or extension functions in a file named for the topic |
| `object` singletons holding mutable state | Koin-provided `single`, or remove the state |
| `when` without exhaustive handling on sealed types, with `else ->` swallowing new cases | Remove `else` so the compiler flags new subtypes |
| Stringly-typed state (`"loading"`, `"error"`) | Sealed interface or enum |
| Boolean flag soup (`isLoading`, `isError`, `isEmpty`, `hasData` as separate vars) | One sealed `UiState` (see State Rules) |
| Long `if/else` chains building strings or colors | `when`, a map, or a small pure function |
| Unused parameters, "for future use" fields | Delete (YAGNI) |
| `data class` with one `var` and no equality use | Reconsider; use `val`, copy via `copy()` |
| `lateinit var` for things set in the constructor | Constructor parameter |
| Excess `companion object { const val TAG = "..." }` | Delete unless used |
| `Log.d` noise at every function entry | Delete. Keep logs only at failure boundaries, via the project logger |

**Comments:**

- Delete comments that restate the next line (`// Create the view model`)
- Delete KDoc that repeats the function name (`/** Gets the user. */ fun getUser()`)
- Keep comments that explain **why** (a backend quirk, a workaround, a threshold's origin)
- Delete decorative section banners (`// ===== Helpers =====`) and emoji in code

**Naming:** remove filler (`Data`, `Info`, `Manager`, `Handler`, `Helper`, `Processor`)
unless it carries meaning. `FitnessManager` should be what it actually does.

---

## Coroutines and Flow Rules

1. **No `LiveData`.** Replace with `StateFlow` / `SharedFlow`.
2. **No `GlobalScope`, `runBlocking` in production code, raw `Thread`, `Handler`, or `AsyncTask`.**
3. **Scopes:** `viewModelScope` in ViewModels; injected `CoroutineScope` for app-wide work; never a hand-built `CoroutineScope(Dispatchers.IO)` inside a class that does not own its lifetime.
4. **Dispatchers:** do not hardcode `Dispatchers.IO` everywhere. Ktor, Room, and DataStore are already main-safe. Use `withContext` only for genuinely blocking work (file I/O, heavy parsing), and inject a dispatcher if tests need to control it.
5. **Expose `Flow` for observable data, `suspend` for one-shot calls.** Do not wrap a `Flow` in a callback or a callback in a `Flow` when a direct API exists.
6. **No unmanaged `suspendCancellableCoroutine`** for APIs that already have a coroutine/Flow equivalent. If truly needed, it must call `invokeOnCancellation`.
7. **ViewModels expose read-only `StateFlow`.** Back it with a private `MutableStateFlow`, or build it with `stateIn`.
8. **Cold to hot conversion:** `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` is the default. Do not use `Eagerly` without a reason.
9. **Error handling:** catch at the ViewModel or at the Flow (`.catch {}`). Do not scatter `try/catch` in every repository method, and never swallow `CancellationException`.
10. **No `delay()`-based polling in a ViewModel `init`** unless it is lifecycle-scoped (`WhileSubscribed`) and has a documented interval. For live data prefer a push/stream subscription from the backend over polling.

```kotlin
// Slop
private val _state = MutableLiveData<FitnessState>()
val state: LiveData<FitnessState> = _state
fun load() {
    GlobalScope.launch(Dispatchers.IO) {
        try { _state.postValue(repo.getToday()) } catch (e: Exception) { /* ignore */ }
    }
}

// Fixed
val uiState: StateFlow<FitnessUiState> = repo.observeToday()
    .map<FitnessToday, FitnessUiState> { FitnessUiState.Success(it) }
    .catch { emit(FitnessUiState.Error(it.message ?: "Couldn't load fitness data")) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FitnessUiState.Loading)
```

---

## Koin Rules

1. **Constructor DSL only:** `singleOf(::X)`, `factoryOf(::X)`, `viewModelOf(::X)`. Use `single { X(get(), get()) }` only when you need a custom lambda (third-party classes, configuration).
2. **No service locator:** `by inject()`, `get()`, `getKoin()`, and `KoinComponent` are not allowed in ViewModels, repositories, use cases, or Composables' business logic. Allowed at Android entry points only (`Application`, `Activity`).
3. **In Composables**, only `koinViewModel()` as a default parameter. Do not call `koinInject()` for repositories inside UI code.
4. **Classes stay DI-free:** no Koin imports in domain or in regular classes.
5. **Scope narrowly:** `single` only for truly app-wide state (the `HttpClient`, an authenticated session). `factory` for stateless helpers. `viewModel` for ViewModels. No `single` "just in case".
6. **No interface binding without an interface** (`singleOf(::X) bind X::class` is meaningless slop). `bind` is only for real abstractions (Rule 1).
7. **One module per area**, declared next to the code it wires. No 300-line `AppModule`.
8. **Verify the graph in a test:**

```kotlin
class KoinGraphTest {
    @Test
    fun verifyModules() {
        // Use the Koin version's verification API, e.g. Module.verify() / checkModules
        appModules.forEach { it.verify() }
    }
}
```

---

## Ktor / Networking Rules

(Applies where Pulse talks to its backend through Ktor.)

1. **One `HttpClient`** (Koin `single`). Never create a client per call or per ViewModel.
2. **Configure with plugins** (`ContentNegotiation`, `HttpTimeout`, `Auth`, `HttpRequestRetry`, `WebSockets` for live state), not ad hoc code in each call.
3. **`Json { ignoreUnknownKeys = true }`.** API payloads can contain fields the app does not model.
4. **Do not hardcode** the base URL, tokens, or IDs in code. They come from configuration/secure storage and are injected.
5. **Never log tokens or full request headers**, even at debug level.
6. **Ktor types stay in the data/network layer.** ViewModels and Composables never see `HttpResponse` or `HttpClient`.
7. **Response parsing is centralized.** One place converts API responses into typed app models and handles missing, null, and sentinel values.
8. **Turn non-2xx into exceptions** (`expectSuccess = true` or a response validator) and handle them once, high in the stack.
9. **Connection and session state are first-class:** model *loading / stale / offline / signed-out* explicitly. A dashboard that silently shows stale data as live is a correctness bug, not a UX nicety.

---

## State and ViewModel Rules

1. **One sealed `UiState` per screen** instead of several independent flags:

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val greeting: String,
        val roomToMove: RoomToMove,
        val fitnessToday: FitnessToday,
        val hasUnreadNotifications: Boolean,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
```

   For screens with independently loading sections (cards that load at different times), use a single `data class` with per-section sealed states instead of one giant `Loading`. Do not leave the whole screen blank because one card is slow.

2. **State is immutable.** `val` and `List` (consider `ImmutableList` / `@Immutable` only if the Compose compiler reports instability for that type).
3. **One-shot events** (snackbar, navigation) go through a `Channel`/`SharedFlow`, or are modeled as state that is consumed. Do not put them in `UiState` where they replay on rotation.
4. **ViewModels do not hold `Context`, `View`, `Activity`, or Composable references.**
5. **ViewModels contain presentation logic only.** Anything testable without Android and independent of the screen belongs in domain.
6. **UI events go in as function calls** (`onRefresh()`, `onCardClick(id)`). Use an event sealed class only for genuinely complex screens (many actions plus effects).
7. **No `init { load() }` plus a manual `_state.value =`** when a `stateIn` pipeline expresses the same thing.
8. **User-facing strings are not built in the ViewModel** from hardcoded literals. Use string resources or a UI text wrapper resolved in the UI.
9. **Time-dependent text** (greeting by time of day, date/time header) takes an injected `Clock` / time provider so it can be tested and does not call `System.currentTimeMillis()` inside a Composable on every recomposition.

---

## State Ownership Rules (shells and "state bag" composables)

Pulse's app shell (`SignedInApp`) is the canonical offender: one composable holding a
dozen local `remember` / `rememberSaveable` variables that mirror child state, carry
one-shot events, and feed a 60-line `when(route)` for titles. The cure is **not**
"put everything in one UiState". It is: **every piece of state has exactly one owner,
and nothing is copied upward.**

### Rule S1: Decide the owner before you write `remember`

Ask these questions in order for every state variable. Stop at the first "yes".

1. **Is it derived** from other state, the window size, or the current destination? -> It is not state. Compute it with a pure function (or `derivedStateOf` when derived from fast-changing state).
2. **Does a child, ViewModel, or repository already own it?** -> Do not copy it. Read it from the owner, or ask the owner to expose it.
3. **Is it a one-time instruction** ("open the editor on arrival", "show this snackbar")? -> Event or navigation argument, never a sticky flag.
4. **Is it purely visual**, read by one composable (dropdown open, scroll position, focus, a local confirm dialog)? -> Keep it local with `remember` / `rememberSaveable`.
5. **Does logic read it, or do several screens read or write it, or must it outlive the composable?** -> It belongs in a ViewModel (or a plain state-holder class) as part of a `UiState`.

| Smell | Owner |
|-------|-------|
| `hasXDraft`, `hasPasswordEdits` (copies of feature drafts) | The feature ViewModel, aggregated by a shell ViewModel |
| `xTitle` set by a child so a parent bar can show it | The screen itself (its own top bar) |
| `onboardingOffered` + `onboardingVisible` | One enum in a shell ViewModel (persisted if "once ever") |
| `editProfileName` ("open name editor when you arrive") | Navigation argument or one-shot event |
| `showBars`, `showTopBar`, `selectedDestination`, `isTablet` | Nobody: derived values |
| `pendingDestination` (a local confirm dialog) | Local, or the shell state if the dialog logic grows |

### Rule S2: The state bag

**Slop:** a single composable declaring more than ~4 `remember { mutableStateOf }` /
`rememberSaveable { mutableStateOf }` variables, especially nullable `String?` titles
and `has*` booleans.

```bash
# files with 5+ local state vars (review each)
rg -c "by (rememberSaveable|remember) \{ mutableStateOf" --glob '*.kt' | awk -F: '$2>4'
```

**Fix:** run Rule S1 on each variable. Only the survivors of questions 4 and 5 remain, and
those usually fit in one small `UiState` plus a handful of derived values. A shell
composable with more than ~4 local state variables, or more than ~150 lines, is a
review trigger on its own.

### Rule S3: Boolean pairs describe a state machine, so model it as one

**Slop:**

```kotlin
var onboardingOffered by rememberSaveable { mutableStateOf(false) }
var onboardingVisible by rememberSaveable { mutableStateOf(false) }
// offered = false + visible = true is representable, and meaningless
```

**Fix:**

```kotlin
enum class OnboardingPrompt { NotOffered, Showing, Dismissed }
```

Two or more booleans that are only meaningful in certain combinations should become an
enum or sealed interface. If "offered" must be remembered across launches, persist it
(DataStore) instead of hoping `rememberSaveable` does it. `rememberSaveable` does not
survive a fresh process start.

### Rule S4: No child-to-parent flag callbacks

**Slop:** `onDraftChanged = { hasGarageDraft = it }`, `onDetailChanged = { hasGarageDetail = it }`,
`onSensitiveEditsChanged = { hasPasswordEdits = it }`. The parent keeps a mirror of state
the child already owns. Two sources of truth drift apart, and a flag can be left `true`
after the screen leaves composition.

**Fix:** one aggregated, owned state in a shell ViewModel, fed through a single reporting
helper that **always clears itself on dispose**:

```kotlin
// :core:ui
@Composable
fun ReportUnsavedChanges(dirty: Boolean, onChange: (Boolean) -> Unit) {
    val latest by rememberUpdatedState(onChange)
    LaunchedEffect(dirty) { latest(dirty) }
    DisposableEffect(Unit) { onDispose { latest(false) } }
}

// feature Route (feature code never sees the shell, only a callback)
@Composable
fun FitnessRoute(onUnsavedChanges: (Boolean) -> Unit, viewModel: FitnessViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportUnsavedChanges(state.hasDraft, onUnsavedChanges)
    // ...
}

// :app
data class ShellUiState(
    val unsavedChanges: Set<TopLevelDestination> = emptySet(),
    val onboarding: OnboardingPrompt = OnboardingPrompt.NotOffered,
    val pendingLeave: TopLevelDestination? = null,
) {
    val hasAnyUnsavedChanges get() = unsavedChanges.isNotEmpty()
}

class ShellViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()

    fun setUnsaved(destination: TopLevelDestination, dirty: Boolean) =
        _uiState.update {
            it.copy(unsavedChanges = if (dirty) it.unsavedChanges + destination else it.unsavedChanges - destination)
        }
}
```

Five booleans become one `Set`, and the existing consumer (e.g. a "you have unsaved work"
warning on logout) reads `hasAnyUnsavedChanges`.

> **Behavior check:** the original shell only blocks tab changes for password edits on
> the profile screen, not for other drafts. Keep that policy exactly unless the user asks
> to change it. Moving state is a refactor, not a product decision.

### Rule S5: Screen chrome belongs to the screen

**Slop:**

- The shell computes the top-bar title, back-button label, and actions with a `when(route)`
- It reads feature internals to do so (`horizonState.draft.kind`, `garageState.draft?.kind`)
- Children push titles up through callbacks (`onTitleChanged`, `onEditorTitleChanged`)
- `showTopBar` is a multi-line boolean over route strings

That makes the shell depend on every feature's internals and guarantees a new feature
edits the shell.

**Fix (default):** each destination owns its top bar, using a shared scaffold from
`:core:ui`; the shell owns only navigation rail / bottom bar, insets, and the `NavHost`.

```kotlin
// :core:ui
@Composable
fun PulseScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) { /* PulseTopAppBar + content, built once */ }
```

- Dynamic titles ("Add budget" / "Edit budget") are decided **inside the feature**, next to the type that determines them: `@StringRes fun HorizonForm.titleRes(isNew: Boolean): Int`, not nested `if/else` in the shell.
- Feature-specific actions (`HorizonTopBarActions`, `NotificationsTopBarActions`) are passed into the screen's own scaffold, so the shell never fetches a feature ViewModel to render them.
- The shell reading `koinViewModel<HorizonViewModel>(viewModelStoreOwner = entry)` just to draw a bar is the clearest sign this rule is broken.
- Back-label logic that depends on the previous destination ("Back to notifications") goes through the screen's `onBack`/`backLabel` parameters, supplied by the navigation layer once.

Only keep a shared shell-level bar if you need a single bar to animate across destinations.
In that case, expose one typed `ScreenChrome(title, backLabel, actions)` that screens
provide, never a growing set of nullable `String?` variables.

### Rule S6: One-shot instructions are events, not flags

**Slop:** `editProfileName` is set `true` before navigating to Profile and set back to
`false` by the destination once consumed. If the reset is missed, it replays.

**Fix:** pass it as a navigation argument (`ProfileRoute(editName = true)`), or send it
through a `Channel` / `SharedFlow` consumed exactly once. Never encode "do this once" as
a boolean that someone must remember to clear.

### Rule S7: Derived values are not state

`isTablet`, `showBars`, `showTopBar`, `showBottomBar`, `selectedDestination`,
`garageTabsInAppBar` are functions of (window size, current destination, font scale).

- Compute them in one place from inputs, preferably as a small pure function that can be unit tested.
- Prefer `WindowSizeClass` to raw `LocalConfiguration` / `maxWidth >= 1100.dp` thresholds scattered through a composable.
- A boolean expression of more than ~3 clauses over route strings is a bug waiting to happen. Replace it with a property on a typed destination (`destination.showsTopBar`).

### Rule S8: Typed destinations instead of route strings

**Slop:** `route in listOf("garage", "vehicle/{id}")` repeated across the file;
`"workout/{id}"` compared as a string; `nav.navigate("workout/$id")`.

**Fix:**

- Navigation 2.8+: `@Serializable` route objects/classes, `destination.hasRoute<Garage>()`, `backStackEntry.toRoute<Vehicle>()`.
- Otherwise: a sealed class / enum with a single `route` constant and metadata:

```kotlin
enum class TopLevelDestination(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    Home("home", R.string.pulse_app_home, UiR.drawable.pulse_home),
    // ...
}
```

The same enum drives the rail, the bottom bar, and `selectedDestination`. This removes the
parallel `destinations` list and the matching `navigationItems` list that have to stay
in sync by index.

### Rule S9: Repeated navigation blocks

**Slop:** `nav.navigate("x") { launchSingleTop = true }` copy-pasted a dozen times.

```kotlin
// Fix: one helper
fun NavController.navigateSingleTop(route: String) =
    navigate(route) { launchSingleTop = true }
```

Also check that tab-style navigation (`popUpTo` + `saveState` + `restoreState`) lives in
exactly one function, which `navigateTo` already is; do not re-implement it per call site.

### Rule S10: Moving state changes its lifetime

Before moving `rememberSaveable` state into a ViewModel:

- A ViewModel survives configuration change but **not** process death. Use `SavedStateHandle` for state that must survive both.
- State that is deliberately per-session (reset on every launch) can stay in a ViewModel; "ever offered" flags need persistence.
- Keep real UI-only state (scroll, expansion, focus, a local confirm dialog) local. The goal is fewer owners per piece of state, not fewer `remember` calls.

### Worked example: the shell

**Before** (12 local variables, 4 feature mirrors, 2 title mirrors, string-route booleans)

```kotlin
var onboardingOffered by rememberSaveable { mutableStateOf(false) }
var onboardingVisible by rememberSaveable { mutableStateOf(false) }
var editProfileName by rememberSaveable { mutableStateOf(false) }
var hasPasswordEdits by rememberSaveable { mutableStateOf(false) }
var pendingDestination by remember { mutableStateOf<String?>(null) }
var workoutEditorTitle by remember { mutableStateOf<String?>(null) }
var hasFitnessDraft by rememberSaveable { mutableStateOf(false) }
var hasMealDraft by rememberSaveable { mutableStateOf(false) }
var hasGarageDraft by rememberSaveable { mutableStateOf(false) }
var hasGarageDetail by rememberSaveable { mutableStateOf(false) }
var garageVehicleTitle by remember { mutableStateOf<String?>(null) }
var hasHorizonDraft by rememberSaveable { mutableStateOf(false) }
```

**After**

| Was | Becomes |
|-----|---------|
| `onboardingOffered`, `onboardingVisible` | `ShellUiState.onboarding: OnboardingPrompt` |
| `editProfileName` | `ProfileRoute(editName = true)` navigation argument |
| `hasFitnessDraft`, `hasMealDraft`, `hasGarageDraft`, `hasHorizonDraft`, `hasPasswordEdits` | `ShellUiState.unsavedChanges: Set<TopLevelDestination>` fed by `ReportUnsavedChanges` |
| `pendingDestination` | `ShellUiState.pendingLeave` (or stays local) |
| `workoutEditorTitle`, `garageVehicleTitle`, `hasGarageDetail` | Deleted. Each screen's own `PulseScreenScaffold` sets its title and back action |
| `isTablet`, `showBars`, `showTopBar`, `selectedDestination` | Pure derived values from window size + `TopLevelDestination` |

Result: a shell that renders `NavHost`, the rail/bottom bar, and the discard dialog, and
nothing else.

---

## Jetpack Compose Rules

### State and lifecycle

| Slop | Fix |
|------|-----|
| `collectAsState()` | `collectAsStateWithLifecycle()` |
| ViewModel passed deep into child Composables | Collect state at the screen level; pass **values and lambdas** down |
| `remember { mutableStateOf(...) }` holding data that belongs in the ViewModel | Move to the ViewModel; keep only transient UI state (scroll, expanded, focus) in the Composable. For shells and screens with many local vars, see the State Ownership Rules |
| `rememberSaveable` missing for state that must survive rotation or process death | Add it (or move to a `SavedStateHandle`) |
| `remember` without keys when the value depends on a parameter | `remember(param) { ... }` |
| `derivedStateOf` missing for state derived from fast-changing state | Add it; and **do not** wrap simple derivations that change at the same rate as their input |
| Side effects in the Composable body (`viewModel.load()`, `Log`, network) | `LaunchedEffect(key)` / `DisposableEffect` / move to ViewModel |
| `LaunchedEffect(Unit)` that needs to restart on a parameter change | Use the actual key |
| `rememberCoroutineScope().launch` used for work that belongs in the ViewModel | Move to ViewModel |
| Captured stale lambdas in long-lived effects | `rememberUpdatedState` |

### Structure

1. **Stateful/stateless split:** `HomeRoute(viewModel)` collects state and wires navigation; `HomeScreen(state, onEvent...)` is a pure function of its parameters. Only the second one is previewed and tested directly.
2. **Every reusable Composable takes `modifier: Modifier = Modifier` as the first optional parameter**, applies it to the **root** element only, and does not hardcode its own size or position.
3. **Parameters are minimal:** pass what the Composable displays, not a whole `UiState` or entity it only reads one field from.
4. **No business logic in Composables**: no filtering, sorting, parsing, threshold checks, or unit conversion. Precompute in the ViewModel/domain.
5. **No giant Composables.** If a function exceeds ~60-80 lines or nests more than ~3 levels, extract named Composables that map to real UI concepts (`GreetingHeader`, `RoomToMoveCard`, `FitnessTodayCard`), not `Section1`.
6. **Avoid wrapper-itis:** a Composable that only renders another Composable with the same parameters is slop. Delete it.
7. **No `Box` with one child and no modifier**, no redundant nested `Column { Column { } }`.
8. **Modifier order matters:** padding/clickable/background order changes behavior. Check that touch targets and ripple bounds are what was intended.

### Lists and performance

- `LazyColumn` / `LazyVerticalGrid` items have **stable `key`s** (and `contentType` for mixed lists)
- No `.sortedBy`/`.filter`/`.map` inside the Composable body on every recomposition; compute in the ViewModel or `remember(input) { }`
- Do not create new lambdas, `Brush`, `Shape`, `TextStyle`, or `Color` objects in hot paths; hoist them or `remember` them
- Prefer **deferred reads** for fast-changing values (animations, scroll): read state inside `graphicsLayer {}` / `drawBehind {}` / `Modifier.offset { }` lambdas instead of the Composable body, so recomposition is skipped
- Avoid `Modifier.composed {}`; use `Modifier.Node` or plain modifier factories
- Do not nest scrollable containers in the same direction without fixed bounds

### Animation (the breathing orb and ripple rings)

Animated code is where AI slop hides most, because it "looks fine" while burning frames.

1. Use `rememberInfiniteTransition` with one transition driving **all** related values (orb scale, ring radius, ring alpha), not a separate `LaunchedEffect` loop per ring.
2. Read animated values **inside draw/graphicsLayer lambdas** (`Modifier.graphicsLayer { scaleX = scale }`, `drawBehind {}` / `Canvas`) so the screen does not recompose every frame.
3. Stagger the ripple rings with `StartOffset` on the same specs instead of copy-pasting N near-identical animation blocks. Drive rings from a list/`repeat(n)`.
4. Use `tween` with easing for a breathing feel (`FastOutSlowInEasing` or a custom `CubicBezierEasing`); avoid `spring` for infinite loops.
5. **Respect reduced motion:** if system animations are disabled, render a static orb.
6. Do not run the animation when the screen is not visible (it should stop with the lifecycle, e.g. by living in a Composable that leaves composition).
7. Do not allocate `Brush`/`Paint` per frame; build them once with `remember` or inside `drawWithCache`.

```kotlin
@Composable
fun BreathingOrb(modifier: Modifier = Modifier, ringCount: Int = 3) {
    val transition = rememberInfiniteTransition(label = "orb")
    val breath by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val rippleProgress = List(ringCount) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(6_000, easing = LinearEasing),
                initialStartOffset = StartOffset(index * 6_000 / ringCount),
            ),
            label = "ring$index",
        )
    }
    val orbColor = PulseTheme.colors.textPrimary   // swap for the orb's accent role
    val ringColor = orbColor

    Canvas(modifier.graphicsLayer { scaleX = breath; scaleY = breath }) {
        rippleProgress.forEach { progress ->
            val p = progress.value               // state read happens in the draw phase
            drawCircle(
                color = ringColor.copy(alpha = (1f - p) * 0.35f),
                radius = size.minDimension / 2f * (0.6f + p * 0.7f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        drawCircle(orbColor, radius = size.minDimension * 0.3f)
    }
}
```

### Styling, theming, resources

1. **No hardcoded colors** (`Color(0xFF...)`, `Color.White`, `Color.Red`) in screens. Use `PulseTheme.colors`.
2. **Every color must work in light and dark.** If a screen only looks right in one theme, it is a bug. Check both.
3. **No magic numbers.** Spacing, radii, and sizes come from `PulseTheme.spacing` and `PulseTokens`. Repeated `16.dp`, `24.dp` scattered across files signals a missing token (add it, do not inline it).
4. **Typography through `PulseText(style = PulseTextStyle.*)`**, not raw `Text` with ad hoc `fontSize = 17.sp`. Sizes in `sp`, layout in `dp`.
5. **Use the `Pulse*` components** (`PulseButton`, `PulseCard`, `PulseDialog`, `PulseTopAppBar`, ...) instead of raw Material equivalents restyled by hand. If a variant is missing, add it to the design system in `:core:ui`.
6. **No hardcoded user-facing strings.** Use `stringResource(R.string...)`. This includes the greeting, tagline, and card titles, and plurals use `pluralStringResource`.
7. **Dynamic color / brand palette** decisions are made once in the theme, not per screen.
8. **No `Color.Red` for errors.** Use the error role from `PulseTheme.colors`.
9. **Remove decorative noise** (extra gradients, shadows, borders, and animations that add no information). A minimal dashboard should look intentional, not decorated.

### Tablet / adaptive layout (Pulse is tablet-first)

1. Do not assume a phone-width single column. Use window size classes (`currentWindowAdaptiveInfo()` / `WindowSizeClass`) to choose layouts.
2. Prefer `weight`, `fillMaxWidth(fraction)`, `LazyVerticalGrid(GridCells.Adaptive(minSize))`, and `widthIn(max = ...)` over hardcoded pixel/dp widths.
3. Cap line length and card width on large screens; do not stretch a phone card edge to edge across a 12-inch display.
4. Handle rotation and split-screen without losing state or breaking layout.
5. Respect system insets (`WindowInsets.safeDrawing`, `enableEdgeToEdge()`), especially for an always-on wall or desk tablet.

### Accessibility

- Every interactive element has a meaningful `contentDescription` (or `null` if purely decorative, never an empty string slop)
- Touch targets at least 48dp
- Use `Modifier.semantics {}` / `clearAndSetSemantics {}` where merged semantics read badly (a card with a label and a value should read as one sentence)
- Do not convey information by color alone (fitness progress, error state)
- Animated/decorative graphics (orb, ripples) are hidden from accessibility services

### Previews

- One `@Preview` per meaningful state (loading, success, error), light and dark, using fake state data
- Previews call the **stateless** Composable only; they never need Koin or a ViewModel
- Delete preview functions with hardcoded giant fake data blocks and empty `Preview` stubs that preview nothing
- Prefer a `@PreviewLightDark` / `@PreviewScreenSizes` multipreview annotation over copy-pasting parameters

---

## Testing Rules

1. **Delete tests that only verify mocks:** a test that mocks the repository, calls the pass-through, and asserts the mock was called tests nothing.
2. **Test behavior:** state transitions in ViewModels, rules in use cases, parsing in API mappers, error paths.
3. **Fakes over mocks** for repositories and data sources. Use Ktor `MockEngine` for network.
4. **Coroutines:** `runTest`, `Dispatchers.setMain(StandardTestDispatcher())` via a rule, and Turbine for Flow assertions (if the project already uses it).
5. **No `Thread.sleep` or real `delay`** in tests.
6. **Do not generate one test file per class** to hit a coverage number.
7. **Compose UI tests** target semantics (`onNodeWithText`, `onNodeWithContentDescription`), not implementation details or tags that exist only for testing.

---

## Not Slop: Keep These

Do not remove or "simplify" the following just because a rule above pattern-matches:

- An interface with two real implementations (or a deliberate seam for a second data source such as demo mode)
- A use case that combines data sources or encodes a rule
- A mapper at the backend JSON boundary, even if it looks repetitive; it absorbs schema drift
- Local `remember` / `rememberSaveable` for purely visual state read by one composable (dropdown open, scroll, focus, a local confirm dialog)
- A shell that legitimately owns the `NavHost`, navigation rail/bottom bar, insets, and window-size decisions
- `Dispatchers` injection used for deterministic tests
- A comment explaining a backend quirk, a platform bug workaround, or why a magic value exists
- `@Suppress` or lint baseline entries that carry a reason
- Code that is verbose but required by an API (e.g. `Canvas` drawing math)
- Existing human-written code in the same area with a convention you disagree with. Match it, and raise the disagreement separately instead of rewriting it in a de-slop pass

**When unsure: leave it and flag it.** Over-deleting is as bad as over-generating.

---

## Rules for Generating New Code (prevention)

When writing new Pulse code, do this up front:

1. Start from the **smallest design** that satisfies the dependency direction. Do not create an interface, use case, mapper, or module until a rule above says it is justified.
2. Write constructor-injected classes, with no DI imports in them.
3. Expose `StateFlow<SealedUiState>`; collect with `collectAsStateWithLifecycle()`.
4. Split `Route` (stateful) and `Screen` (stateless) Composables; add a `modifier` parameter.
5. Take colors, shapes, spacing, text styles, and strings from theme tokens and resources.
6. Add previews for loading/success/error in light and dark.
7. Write only comments that explain *why*.
8. State assumptions in the PR description instead of silently inventing behavior (IDs, endpoints, thresholds).

---

## Review Output Format

When reporting a de-slop pass, use this structure:

```
## De-slop report: <feature / PR>

**Summary:** <N files changed, +A / -D lines, one-sentence outcome>

### Removed
- `FitnessRepositoryImpl` + `FitnessRepository` interface -> single `FitnessRepository` (Rule 1: one implementation)
- `GetFitnessTodayUseCase` -> deleted, ViewModel injects repository (Rule 3: pass-through)

### Fixed
- `HomeScreen`: `collectAsState` -> `collectAsStateWithLifecycle`
- `RoomToMoveCard`: hardcoded `Color(0xFF...)` -> theme tokens (works in light/dark)
- `BreathingOrb`: per-ring `LaunchedEffect` loops -> one `rememberInfiniteTransition`, reads in draw phase
- `SignedInApp`: 12 local state vars -> `ShellUiState` + per-screen top bars (S1, S4, S5)

### Kept (looks like slop, isn't)
- `FitnessApiMapper`: boundary mapper, absorbs schema drift

### Flagged for human decision
- `FitnessApi` has a second implementation planned? If not, remove the interface

### Verification
- [x] build  [x] unit tests  [x] Koin graph check  [x] light + dark checked on tablet emulator
```

---

## Final Checklist

**Architecture**
- [ ] No interface with a single implementation (and no `*Impl` names)
- [ ] No use case that only forwards a call
- [ ] No mapper that copies identical fields; mapping exists at real boundaries
- [ ] Dependency direction is UI → Domain → Data; no feature-to-feature imports
- [ ] Domain code has no Android, Compose, Koin, or Ktor imports

**Kotlin / async**
- [ ] No `!!`, no `GlobalScope`, no `LiveData`, no raw threads
- [ ] `CancellationException` is never swallowed
- [ ] ViewModels expose read-only `StateFlow` of a sealed UI state
- [ ] No comment that restates the code; no unused code

**DI / networking**
- [ ] Koin constructor DSL only; no `get()` / `by inject()` inside classes
- [ ] Koin graph verified in a test
- [ ] One `HttpClient`; Ktor types stay out of ViewModels and UI
- [ ] No secrets, tokens, or IDs hardcoded or logged

**State ownership**
- [ ] Every state variable has one owner; no composable mirrors a child's or ViewModel's state
- [ ] No composable with more than ~4 local state vars (shells especially)
- [ ] No `on*Changed = { hasX = it }` callbacks pushing flags or titles upward
- [ ] Boolean combinations modeled as an enum/sealed type; one-shot instructions are events or nav args
- [ ] Derived values (`showBars`, `showTopBar`, `selectedDestination`) are pure functions, not state
- [ ] Screens own their top bar; the shell does not read feature internals
- [ ] Destinations are typed; no `route in listOf(...)` string checks

**Compose**
- [ ] `collectAsStateWithLifecycle()` everywhere
- [ ] Route/Screen split; `modifier` parameter on reusable Composables
- [ ] No business logic, parsing, or side effects in Composable bodies
- [ ] Lazy lists have stable keys; fast-changing values are read in draw/layout phase
- [ ] No hardcoded colors, strings, or magic dp values; verified in light **and** dark
- [ ] Tablet layouts use window size classes, not fixed widths
- [ ] Accessibility semantics set; decorative animation hidden; reduced motion respected
- [ ] Previews cover loading/success/error states

**Process**
- [ ] Diff is focused; no unrelated rewrites
- [ ] Build and tests pass
- [ ] Anything uncertain is flagged, not deleted