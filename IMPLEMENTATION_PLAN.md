# Implementation Plan: DevView Module Architecture Refactor

This document captures the full plan agreed upon before implementation begins.
Work through sections in order — later sections depend on earlier ones.

---

## Context & Problem Statement

The current `devview-networkmock` module bundles two distinct concerns:

1. **Data layer** — the Ktor plugin (`NetworkMockPlugin`) and shared models/repositories
2. **UI layer** — the DevView screen, ViewModel, components

This forces `:sample:network` (a production data-layer module) to depend on
`devview-networkmock`, pulling the entire Compose/DevView/ViewModel stack into
the data layer. That is the architectural violation being fixed.

A generalisation opportunity was also identified: `devview-featureflip` has the
same DataStore bootstrapping pattern duplicated internally, and future modules
will face the same issue. So we are introducing a general DataStore mechanism
at the `devview-utils` level at the same time, driven by a lifecycle hook on
the `Module` interface.

---

## Part 1 — `initModule()` hook on `Module`

### What and why

Each module may need to perform Composable-context initialisation when it is
first registered — setting up repositories, observing state, or anything else
that requires a Composable context. `initModule()` is the general-purpose hook
for this. It is called automatically by `rememberModules` for every registered
module.

`RequiresDataStore` modules get their DataStore initialised **automatically and
separately** by `rememberModules` (via `initDataStore()`) before `initModule()`
is called — so `initModule()` can safely access `dataStoreDelegate.get()` if
needed. This clean separation means `initDataStore()` is never called manually
inside `initModule()`.

This hook is also useful for integrators writing custom modules — it provides
a clear, well-named entry point for any module-level Composable initialisation
beyond DataStore.

#### `devview/core/Module.kt`

Add an optional `@Composable` lifecycle hook with a default no-op. Existing
modules are unaffected.

```kotlin
public interface Module {
    // ...existing code...

    /**
     * Called once when the module is first registered in a Composable context
     * via [rememberModules]. Override to perform any Composable-context
     * initialisation this module requires (e.g. repository setup, state
     * observation). DataStore is guaranteed to be initialised before this
     * is called for modules that implement [RequiresDataStore].
     *
     * Default implementation is a no-op.
     */
    @Composable
    public fun initModule() {}
}
```

#### `devview/core/ModuleRegistry.kt`

`rememberModules` is already `@Composable`. After building the remembered module
list, iterate over modules:
1. Call `initDataStore()` for any module implementing `RequiresDataStore` —
   automatic, no module action needed
2. Call `initModule()` on every module — general-purpose hook

```kotlin
@Composable
public fun rememberModules(block: ModuleRegistry.() -> Unit): ImmutableList<Module> {
    val modules = remember { buildModules(block) }
    modules.forEach { module ->
        if (module is RequiresDataStore) module.initDataStore()
        module.initModule()
    }
    return modules
}
```

#### `devview/build.gradle.kts`

No dependency changes needed — `initModule()` is `@Composable` and `devview`
already applies `convention.compose.multiplatform`.

---

## Part 2 — `DataStoreDelegate` and `RequiresDataStore` in `devview-utils`

### Why `devview-utils` and not `devview`

`devview-utils` already depends on `devview` (`api(projects.devview)`), already
has DataStore (`convention.datastore`), and already owns `rememberDataStore`.
Putting `RequiresDataStore` in `devview` would require `devview` to depend on
`devview-utils`, creating a circular dependency. `devview-utils` is the correct
home.

### New file: `devview-utils/DataStoreDelegate.kt`

A minimal reusable holder. Owns the single `DataStore` instance per module.
`init()` is `@Composable` — called automatically by `rememberModules` — so
`LocalContext` is always available on Android. The `null` guard ensures
exactly-once initialisation regardless of recompositions.

```kotlin
// devview-utils — commonMain
public class DataStoreDelegate {
    private var instance: DataStore<Preferences>? = null

    @Composable
    public fun init(dataStoreName: String) {
        if (instance != null) return
        instance = rememberDataStore(dataStoreName = dataStoreName)
    }

    public fun get(): DataStore<Preferences> = instance
        ?: error(
            "DataStore not initialised. " +
            "Ensure the module is registered via rememberModules { }."
        )
}
```

### New file: `devview-utils/RequiresDataStore.kt`

Interface that any `Module` can implement. Module authors specify only the
DataStore filename and provide a `DataStoreDelegate` instance. The default
`initDataStore()` implementation is called automatically by `rememberModules` —
module authors never call or override it.

```kotlin
// devview-utils — commonMain
public interface RequiresDataStore {
    public val dataStoreName: String
    public val dataStoreDelegate: DataStoreDelegate

    // Called automatically by rememberModules — never override or call manually
    @Composable
    public fun initDataStore() {
        dataStoreDelegate.init(dataStoreName = dataStoreName)
    }
}
```

### `devview-utils/build.gradle.kts` — no changes needed

Already has DataStore and Compose via convention plugins.

### Usage for custom integrator modules

Two lines of boilerplate for DataStore. `initModule()` is available for any
additional initialisation — or omitted entirely if nothing else is needed.

```kotlin
object MyModule : Module, RequiresDataStore {
    override val dataStoreName = "my_module.preferences_pb"
    override val dataStoreDelegate = DataStoreDelegate()

    // initModule() override is optional — only needed for extra initialisation
    // DataStore is ready via dataStoreDelegate.get() once rememberModules has run
}
```

If the module also needs non-DataStore Composable initialisation:

```kotlin
object MyModule : Module, RequiresDataStore {
    override val dataStoreName = "my_module.preferences_pb"
    override val dataStoreDelegate = DataStoreDelegate()

    @Composable
    override fun initModule() {
        // DataStore is already initialised here — safe to call dataStoreDelegate.get()
        // Add any other Composable-context initialisation here
    }
}
```

---

## Part 3 — Split `devview-networkmock` into three modules

### New module structure

```
devview-networkmock-core    — shared models + repositories + DataStore delegate
devview-networkmock-ktor    — Ktor plugin only, depends on core
devview-networkmock         — DevView UI panel only, depends on core + devview
```

### Dependency graph after the split

```
devview               — Module, initModule()
      ↑
devview-utils         — DataStoreDelegate, RequiresDataStore, rememberDataStore()
      ↑                              ↑
devview-networkmock-core             devview-featureflip
(NetworkMockDataStoreDelegate,       (DataStoreDelegate on FeatureFlip object,
 NetworkMockInitializer,              no initModule() override needed)
 models, repositories)
      ↑                    ↑
devview-networkmock-ktor   devview-networkmock
(Ktor plugin only)         (UI panel — initModule() drives NetworkMockInitializer)
      ↑                          ↑
:sample:network            :sample:shared
(data layer — no UI baggage)       (UI/app layer)
```

No circular dependencies. Data layer has zero UI baggage. UI layer has zero
Ktor coupling. Both share models and repositories through `core`.

---

## Part 4 — `devview-networkmock-core` contents

### Files moved from `devview-networkmock`

| File | Notes |
|---|---|
| `model/MockConfiguration.kt` | No changes needed |
| `model/MockResponse.kt` | No changes needed |
| `model/NetworkMockState.kt` | No changes needed |
| `repository/MockConfigRepository.kt` | No changes needed |
| `repository/MockStateRepository.kt` | No changes needed |

### Files removed

| File | Reason |
|---|---|
| `model/rememberDataStore.kt` | Replaced by `DataStoreDelegate` in `devview-utils` |

### New: `NetworkMockDataStoreDelegate` top-level val

The single process-level delegate that both `devview-networkmock-ktor` and
`devview-networkmock` reference. Lives in `core` so neither UI nor Ktor module
needs to depend on each other to share it.

```kotlin
// devview-networkmock-core — commonMain
public val NetworkMockDataStoreDelegate = DataStoreDelegate()

internal const val NETWORK_MOCK_DATASTORE_NAME = "network_mock_datastore.preferences_pb"
```

### New: `NetworkMockInitializer`

Owns both repositories. Initialised once from `NetworkMock.initModule()`.
Read by both `devview-networkmock-ktor` and `devview-networkmock` UI.

The DataStore instance is passed explicitly as a parameter — `NetworkMockInitializer`
has no implicit dependency on `NetworkMockDataStoreDelegate`. The caller
(`NetworkMock.initModule()`) retrieves it from the delegate and passes it in,
keeping data flow explicit and the initializer testable.

```kotlin
// devview-networkmock-core — commonMain
public object NetworkMockInitializer {
    private var stateRepository: MockStateRepository? = null
    private var configRepository: MockConfigRepository? = null

    @Composable
    internal fun initialize(
        dataStore: DataStore<Preferences>,
        resourceLoader: suspend (String) -> ByteArray
    ) {
        if (stateRepository != null) return
        stateRepository = remember { MockStateRepository(dataStore = dataStore) }
        configRepository = remember { MockConfigRepository(resourceLoader = resourceLoader) }
    }

    public fun requireStateRepository(): MockStateRepository =
        stateRepository ?: error(
            "NetworkMockInitializer not initialised. " +
            "Ensure NetworkMock is registered via rememberModules { }."
        )

    public fun requireConfigRepository(): MockConfigRepository =
        configRepository ?: error(
            "NetworkMockInitializer not initialised. " +
            "Ensure NetworkMock is registered via rememberModules { }."
        )
}
```

### `build.gradle.kts` for `devview-networkmock-core`

No Ktor. No Compose UI. No `devview` core UI. Compose runtime only (for
`@Composable` on `initialize()`).

```kotlin
plugins {
    alias(libs.plugins.convention.multiplatform.library)
}

sourceSets {
    commonMain.dependencies {
        implementation(projects.devviewUtils)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.jetbrains.compose.runtime)
    }
}
```

---

## Part 5 — `devview-networkmock-ktor` contents

### Files moved from `devview-networkmock`

| File | Notes |
|---|---|
| `plugin/NetworkMockPlugin.kt` | Updated — see below |
| `plugin/NetworkMockConfig.kt` | Updated — see below |
| `plugin/RequestMatcher.kt` | No changes needed |

### Modified: `NetworkMockConfig.kt`

Repositories become optional — resolved from `NetworkMockInitializer` if not
explicitly set. Explicit setting remains supported for testing.

```kotlin
public class NetworkMockConfig {
    public var mockRepository: MockConfigRepository? = null
    public var stateRepository: MockStateRepository? = null

    internal fun resolvedMockRepository(): MockConfigRepository =
        mockRepository ?: NetworkMockInitializer.requireConfigRepository()

    internal fun resolvedStateRepository(): MockStateRepository =
        stateRepository ?: NetworkMockInitializer.requireStateRepository()
}
```

### Modified: `NetworkMockPlugin.kt`

Uses `resolvedMockRepository()` / `resolvedStateRepository()` instead of
direct field access. No other logic changes.

### `build.gradle.kts` for `devview-networkmock-ktor`

```kotlin
plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.ktor)
}

sourceSets {
    commonMain.dependencies {
        implementation(projects.devviewNetworkmockCore)
    }
}
```

### Integrator usage in `:sample:network` after the change

```kotlin
// Before
install(NetworkMockPlugin) {
    mockRepository = networkMock.configRepository
    stateRepository = mockStateRepository
}

// After — zero configuration needed
install(NetworkMockPlugin)
```

---

## Part 6 — `devview-networkmock` (UI module) contents

### Files that stay

| File | Notes |
|---|---|
| `NetworkMock.kt` | Updated — see below |
| `NetworkMockScreen.kt` | Minor update — repositories from initializer |
| `components/` | No changes needed |
| `viewmodel/NetworkMockViewModel.kt` | No changes needed |
| `preview/` | No changes needed |

### Files removed

| File | Reason |
|---|---|
| `model/rememberDataStore.kt` | Replaced by `DataStoreDelegate` in `devview-utils` |
| `plugin/*` | Moved to `devview-networkmock-ktor` |
| `repository/*` | Moved to `devview-networkmock-core` |
| `model/MockConfiguration.kt` | Moved to `devview-networkmock-core` |
| `model/MockResponse.kt` | Moved to `devview-networkmock-core` |
| `model/NetworkMockState.kt` | Moved to `devview-networkmock-core` |

### Modified: `NetworkMock.kt`

- Implements both `Module` and `RequiresDataStore`
- `dataStoreDelegate` points to `NetworkMockDataStoreDelegate` from `core` —
  the UI module does not own the DataStore, it drives its initialisation
- `initModule()` is where NetworkMock-specific initialisation happens: it
  retrieves the DataStore from the delegate (already initialised by
  `rememberModules` before `initModule()` is called) and passes it explicitly
  to `NetworkMockInitializer.initialize()`. This is a clear example of what
  `initModule()` is for — module-specific Composable init beyond DataStore.
- Constructor takes only `resourceLoader` — no DataStore, no repositories

```kotlin
public class NetworkMock(
    private val resourceLoader: suspend (String) -> ByteArray
) : Module, RequiresDataStore {

    // Points to the core singleton — ktor and UI share one DataStore
    override val dataStoreName = NETWORK_MOCK_DATASTORE_NAME
    override val dataStoreDelegate = NetworkMockDataStoreDelegate

    @Composable
    override fun initModule() {
        // DataStore is guaranteed initialised before initModule() is called
        NetworkMockInitializer.initialize(
            dataStore = dataStoreDelegate.get(),
            resourceLoader = resourceLoader
        )
    }

    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    ) {
        entry<NetworkMockDestination.Main> {
            Scaffold { paddingValues ->
                NetworkMockScreen(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    configRepository = NetworkMockInitializer.requireConfigRepository(),
                    stateRepository = NetworkMockInitializer.requireStateRepository()
                )
            }
        }
    }
}
```

### `build.gradle.kts` for `devview-networkmock`

```kotlin
plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
}

sourceSets {
    commonMain.dependencies {
        api(projects.devview)
        implementation(projects.devviewNetworkmockCore)
        implementation(projects.devviewUtils)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
    }
}
```

### Integrator usage in `:sample:shared` after the change

```kotlin
// Before
val networkMock = NetworkMock(
    resourceLoader = { Res.readBytes(it) },
    stateRepository = mockStateRepository
)

// After
val networkMock = NetworkMock(
    resourceLoader = { Res.readBytes(it) }
)
```

---

## Part 7 — `devview-featureflip` DataStore alignment

`FeatureFlip` adopts `RequiresDataStore` for consistency. The internal
`rememberDataStore.kt` file is removed. Because `FeatureFlip` has no external
module needing its DataStore (no ktor equivalent), the `DataStoreDelegate`
lives directly on the `FeatureFlip` object itself.

`FeatureFlip` does **not** need to override `initModule()` — DataStore is
handled automatically by `rememberModules` calling `initDataStore()`, and there
is no other Composable-context initialisation needed.

```kotlin
// devview-featureflip — FeatureFlip.kt
public object FeatureFlip : Module, RequiresDataStore {
    override val dataStoreName = FEATURE_FLIP_DATASTORE_NAME
    override val dataStoreDelegate = DataStoreDelegate()

    // No initModule() override needed — DataStore init is fully automatic
    // Access dataStoreDelegate.get() inside FeatureFlipScreen / ViewModel
}
```

### Files removed from `devview-featureflip`

| File | Reason |
|---|---|
| `model/rememberDataStore.kt` | Replaced by `DataStoreDelegate` on `FeatureFlip` object |

### `devview-featureflip/build.gradle.kts`

Add `implementation(projects.devviewUtils)` to get `DataStoreDelegate` and
`RequiresDataStore`.

---

## Part 8 — Sample app cleanup

### `:sample:network/build.gradle.kts`
- Replace `implementation(projects.devviewNetworkmock)` with
  `implementation(projects.devviewNetworkmockKtor)`
- Remove `implementation(projects.devviewUtils)` if no longer needed directly

### `:sample:shared/build.gradle.kts`
- `devviewNetworkmock` dependency stays (UI panel)

### `DevViewApp.kt`
- Remove `mockStateRepository` and `mockConfigRepository` threading
- `NetworkMock` constructed with only `resourceLoader`
- Remove `rememberMockStateRepository()` call — no longer needed

### `App.kt`
- Remove `mockConfigRepository` and `mockStateRepository` parameters
- `rememberHttpClientWithMocking` requires no repository arguments

### `BaseHttpClientConfig.kt`
- Remove `mockStateRepository` and `mockConfigRepository` parameters
- `install(NetworkMockPlugin)` with no configuration block

### `RememberHttpClient.kt`
- Remove `mockConfigRepository` and `mockStateRepository` parameters

### `RememberMockStateRepository.kt`
- **Delete** — DataStore is now owned by `NetworkMockDataStoreDelegate` in core

---

## Part 9 — `settings.gradle.kts` update

```kotlin
include(
    ":devview",
    ":devview-analytics",
    ":devview-featureflip",
    ":devview-networkmock",
    ":devview-networkmock-core",   // new
    ":devview-networkmock-ktor",   // new
    ":devview-utils",
    ":internal:dokka",
    ":sample:androidApp",
    ":sample:network",
    ":sample:shared"
)
```

---

## Part 10 — `KNOWN_ISSUES.md` updates

- Mark NM-001 as superseded — the shared repository problem is now solved
  structurally: `NetworkMockDataStoreDelegate` in `core` is the single source
  of truth, shared by delegation rather than by explicit instance passing
- DataStore duplication issue resolved — each module owns one `DataStoreDelegate`
  instance; `devview-utils` provides the mechanism but holds no state

---

## Summary — what each party writes

### Lifecycle in `rememberModules` — order of calls per module

```
rememberModules runs
    │
    ├─ for each module:
    │   ├─ if RequiresDataStore → initDataStore()   (automatic, never manual)
    │   └─ initModule()                             (no-op unless overridden)
    │
    └─ returns module list
```

### What module authors write

**Module with DataStore only — zero extra boilerplate:**
```kotlin
object MyModule : Module, RequiresDataStore {
    override val dataStoreName = "my_module.preferences_pb"
    override val dataStoreDelegate = DataStoreDelegate()
    // Nothing else needed — DataStore ready via dataStoreDelegate.get()
}
```

**Module with DataStore + extra init (like NetworkMock):**
```kotlin
class MyModule(...) : Module, RequiresDataStore {
    override val dataStoreName = "my_module.preferences_pb"
    override val dataStoreDelegate = DataStoreDelegate()

    @Composable
    override fun initModule() {
        // DataStore already initialised — safe to call dataStoreDelegate.get()
        MyInitializer.initialize(dataStore = dataStoreDelegate.get(), ...)
    }
}
```

**Module with no DataStore — no RequiresDataStore, initModule() optional:**
```kotlin
object MyModule : Module {
    @Composable
    override fun initModule() {
        // Any Composable-context init that doesn't involve DataStore
    }
}
```

### For `devview-networkmock` specifically

| Who | What they own |
|---|---|
| `devview-networkmock-core` | `NetworkMockDataStoreDelegate` (top-level val) + `NetworkMockInitializer` + repositories + models |
| `devview-networkmock` | `NetworkMock` points delegate to core singleton, calls initializer in `initModule()` |
| `devview-networkmock-ktor` | Calls `NetworkMockInitializer.require*()` — no Compose needed |

### Full module dependency graph

```
devview               — Module, initModule()
      ↑
devview-utils         — DataStoreDelegate, RequiresDataStore, rememberDataStore()
      ↑                              ↑
devview-networkmock-core             devview-featureflip
(NetworkMockDataStoreDelegate,       (DataStoreDelegate on FeatureFlip object,
 NetworkMockInitializer,              no initModule() override needed)
 models, repositories)
      ↑                    ↑
devview-networkmock-ktor   devview-networkmock
(Ktor plugin only)         (UI panel — initModule() drives NetworkMockInitializer)
      ↑                          ↑
:sample:network            :sample:shared
(data layer)               (UI/app layer)
```
