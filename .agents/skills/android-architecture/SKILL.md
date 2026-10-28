---
name: android-architecture
description: >-
  Use when designing module structure, choosing architecture patterns
  (MVVM/MVI), setting up dependency injection with Koin, or defining layer
  boundaries in an Android project.
---

# Android Architecture

## Overview

Good architecture makes Android apps testable, maintainable, and scalable. This skill guides architectural decisions: module structure, layer separation (UI → Domain → Data), dependency injection with Koin, networking with Ktor, state management with Coroutines and Flow, and patterns that survive configuration changes and process death.

## When to Use

- Starting a new Android project or module
- Adding a feature module to an existing multi-module project
- Choosing between MVVM and MVI for a screen
- Setting up or refactoring dependency injection
- Defining boundaries between layers
- Debugging lifecycle or state management issues

**Skip when:** Making a small change within an already-architected feature.

## Core Process

### Step 1: Module Structure

1. **Organize by layer and feature:**

```
:app                          # Application, navigation, Koin startup
:feature:home                 # Home screen (UI + ViewModel + Koin module)
:feature:profile              # Profile screen (UI + ViewModel + Koin module)
:feature:settings             # Settings screen (UI + ViewModel + Koin module)
:core:data                    # Repositories, data sources, API services
:core:domain                  # Use cases, domain models (pure Kotlin)
:core:database                # Room database, entities, DAOs
:core:network                 # Ktor HttpClient, API services, API models, plugins
:core:ui                      # Shared composables, theme, design tokens
:core:common                  # Utilities, extensions, constants
:core:testing                 # Shared test utilities, fakes
```

2. **Dependency rules:**
   - `:feature:*` depends on `:core:domain`, `:core:ui`, `:core:common`
   - `:core:data` depends on `:core:domain`, `:core:database`, `:core:network`
   - `:core:domain` depends on **nothing** (pure Kotlin module, no Koin dependency)
   - `:app` depends on all `:feature:*` and `:core:data` (for Koin module wiring)
   - **Never:** feature → feature, core → feature, domain → data

### Step 2: Layer Separation

3. **Three layers, strict boundaries:**

| Layer | Contains | Depends On |
|-------|----------|-----------|
| **UI** | Composables, ViewModels, UiState | Domain |
| **Domain** | Use cases, domain models, repository interfaces | Nothing |
| **Data** | Repository implementations, data sources, mappers | Domain (interfaces) |

4. **Data flows one direction:** UI ← Domain ← Data

```kotlin
// Domain layer: pure Kotlin, no Android or Koin dependencies
interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun addTask(task: Task)
}

data class Task(val id: String, val title: String, val completed: Boolean)

// Plain constructor: no DI annotations in the domain layer
class GetTasksUseCase(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> = repository.getTasks()
}
```

### Step 3: ViewModel and State Management

5. **MVVM with UiState pattern:**

```kotlin
// Sealed interface for exhaustive state handling
sealed interface TaskListUiState {
    data object Loading : TaskListUiState
    data class Success(val tasks: List<Task>) : TaskListUiState
    data class Error(val message: String) : TaskListUiState
}

// No DI annotation needed: registered in the feature's Koin module
class TaskListViewModel(
    private val getTasks: GetTasksUseCase
) : ViewModel() {

    val uiState: StateFlow<TaskListUiState> = getTasks()
        .map<List<Task>, TaskListUiState> { TaskListUiState.Success(it) }
        .catch { emit(TaskListUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TaskListUiState.Loading
        )
}
```

6. **MVI for complex screens** (multiple user actions, side effects):

```kotlin
// Events from UI → ViewModel
sealed interface TaskListEvent {
    data class ToggleTask(val taskId: String) : TaskListEvent
    data class DeleteTask(val taskId: String) : TaskListEvent
    data object RefreshTasks : TaskListEvent
}

// One-time effects (navigation, snackbar)
sealed interface TaskListEffect {
    data class ShowSnackbar(val message: String) : TaskListEffect
    data class NavigateToDetail(val taskId: String) : TaskListEffect
}
```

### Step 4: Dependency Injection with Koin

7. **Module setup:** each module exposes one Koin `module { }`. Use the constructor DSL (`singleOf`, `factoryOf`, `viewModelOf`) so classes stay free of DI code.

```kotlin
// In :core:network
val networkModule = module {
    single<HttpClient> {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            install(Logging) {
                level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
            }
            defaultRequest {
                url(BuildConfig.API_BASE_URL)
                contentType(ContentType.Application.Json)
            }
        }
    }
    singleOf(::TaskApiService)
}

// In :core:data
val dataModule = module {
    singleOf(::TaskRepositoryImpl) bind TaskRepository::class
}

// In :feature:tasks (use cases and ViewModels live with the feature,
// because :core:domain stays free of Koin)
val tasksFeatureModule = module {
    factoryOf(::GetTasksUseCase)
    viewModelOf(::TaskListViewModel)
}

// In :app
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(networkModule, dataModule, tasksFeatureModule)
        }
    }
}
```

8. **Using injected dependencies in the UI:**

```kotlin
@Composable
fun TaskListScreen(viewModel: TaskListViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

9. **Koin rules:**
   - Use constructor injection (`singleOf` / `factoryOf` / `viewModelOf`), not `by inject()` or `get()` inside classes
   - Avoid `KoinComponent` outside Android entry points (Application, Activity); it turns Koin into a service locator and hides dependencies
   - Scope to the narrowest lifecycle: `single` only for truly app-wide instances, `factory` for stateless use cases, `viewModel` for ViewModels, `scope<T> { }` for screen- or flow-bound objects
   - Use `bind` for interface→implementation, and a `single { }` lambda for third-party classes
   - Koin resolves at runtime, so verify the graph in tests (see Verification)
   - Test with `KoinTestRule` / `koinApplication` and override definitions with test modules (`loadKoinModules`)

### Step 5: Networking with Ktor

10. **Keep Ktor behind the data layer.** API services live in `:core:network`, take the shared `HttpClient`, and return API models; repositories map them to domain models.

```kotlin
// In :core:network
class TaskApiService(private val client: HttpClient) {
    suspend fun getTasks(): List<TaskDto> =
        client.get("tasks").body()

    suspend fun addTask(task: TaskDto) {
        client.post("tasks") { setBody(task) }
    }
}

@Serializable
data class TaskDto(val id: String, val title: String, val completed: Boolean)
```

11. **Ktor guidelines:**
   - Create **one** `HttpClient` per app (singleton); it owns a connection pool and threads
   - Use the `OkHttp` engine on Android for HTTP/2, certificate pinning and interceptor reuse
   - Configure behavior with plugins (`ContentNegotiation`, `HttpTimeout`, `Logging`, `Auth`, `HttpRequestRetry`) rather than ad hoc code in each call
   - Use `expectSuccess = true` (or a `HttpResponseValidator`) to turn non-2xx responses into exceptions
   - Never expose Ktor types (`HttpResponse`, `HttpClient`) outside `:core:network` / `:core:data`

### Step 6: Coroutines and Flow

12. **Structured concurrency:**

```kotlin
// Use viewModelScope — never GlobalScope
viewModelScope.launch {
    repository.syncTasks()
}

// Use withContext for thread switching
suspend fun readFromDisk(): Data = withContext(Dispatchers.IO) {
    file.readText().parseData()
}

// Expose Flow from repositories, collect in ViewModels
fun getTasks(): Flow<List<Task>> = taskDao.observeAll()
    .map { entities -> entities.map { it.toDomain() } }
```

13. **Error handling:**

```kotlin
// Catch in the ViewModel, not in the repository
viewModelScope.launch {
    try {
        repository.syncTasks()
    } catch (e: CancellationException) {
        throw e // never swallow cancellation
    } catch (e: HttpRequestTimeoutException) {
        _uiState.update { it.copy(error = "Request timed out") }
    } catch (e: ResponseException) { // non-2xx with expectSuccess = true
        _uiState.update { it.copy(error = "Server error (${e.response.status.value})") }
    } catch (e: IOException) {
        _uiState.update { it.copy(error = "Network unavailable") }
    }
}
```

## Common Rationalizations

| Shortcut | Why It Fails |
|----------|-------------|
| "We don't need modules, it's a small app" | Modularization pays off at ~10 screens. Build times and code coupling grow fast. |
| "Use cases are unnecessary boilerplate" | Use cases make ViewModels testable without mocking repositories. They also compose. |
| "I'll put the logic in the ViewModel" | Fat ViewModels become untestable. Domain logic belongs in use cases. |
| "Koin is simple, I'll just call `get()` / `by inject()` everywhere" | That's a service locator: dependencies become hidden and tests need a running Koin. Use constructor injection. |
| "Manual DI is enough" | Manual DI doesn't scale across a multi-module project. Koin modules per layer keep wiring explicit and small. |
| "I'll create an `HttpClient` per request" | Each client owns its own pool and threads. Share one singleton. |
| "GlobalScope is fine for fire-and-forget" | GlobalScope survives activity destruction, leaking resources. Use structured concurrency. |

## Red Flags

- Feature module depends on another feature module
- Domain layer imports Android framework classes, Koin, or Ktor
- ViewModel directly accesses Room DAOs or Ktor `HttpClient`
- `GlobalScope.launch` anywhere in production code
- `by inject()` / `get()` / `KoinComponent` in ViewModels, repositories or use cases instead of constructor injection
- Multiple `HttpClient` instances created at runtime
- Catching `Exception` in a coroutine without rethrowing `CancellationException`
- Mutable state exposed from ViewModel (`MutableStateFlow` made public)
- Business logic in Composables

## Verification

- [ ] Module dependency graph follows layer rules (no upward dependencies)
- [ ] Domain layer has zero Android, Koin and Ktor dependencies
- [ ] ViewModels expose `StateFlow`, not `MutableStateFlow` or `LiveData`
- [ ] All dependencies provided via Koin constructor injection (`singleOf`, `factoryOf`, `viewModelOf`)
- [ ] Koin graph verified in a test (`Module.verify()` or `checkModules`)
- [ ] A single shared `HttpClient`; Ktor types do not leak past the data layer
- [ ] No `GlobalScope` usage in production code
- [ ] Repositories return `Flow` for observable data
- [ ] Use cases are independently testable
- [ ] `./gradlew build` succeeds with no circular dependency errors