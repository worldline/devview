# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-featureflip` provides feature-flag management for the DevView developer menu: a runtime API for checking flag state and a Compose UI for toggling flags at development time. It supports two flag kinds — device-local booleans and remote-config flags with local overrides.

## Public API Surface

| Symbol | Kind | Description |
|---|---|---|
| `Feature` | sealed class | Base type; two concrete subclasses below |
| `Feature.LocalFeature(name, description, isEnabled)` | data class | Simple on/off flag stored as a boolean in DataStore |
| `Feature.RemoteFeature(name, description, defaultRemoteValue, state)` | data class | Flag backed by remote config; `state` controls whether the remote value or a local override wins |
| `FeatureState` | enum | `REMOTE` / `LOCAL_OFF` / `LOCAL_ON` — ordinals are persisted; **do not reorder** |
| `FeatureType` | enum | `REMOTE` / `LOCAL` — used only for UI filtering |
| `FeatureHandler` | class | Runtime handler; constructed via `rememberFeatureHandler` |
| `FeatureHandler.isFeatureEnabledFlow(name)` | `Flow<Boolean>` | Reactive read, safe for ViewModels |
| `FeatureHandler.isFeatureEnabled(name)` | `@Composable State<Boolean>` | Lifecycle-aware Compose read |
| `FeatureHandler.addFeatures(List<Feature>)` | `suspend fun` | Register additional flags at runtime (e.g. from remote config) |
| `rememberFeatureHandler(features)` | `@Composable` | Creates and remembers a `FeatureHandler` backed by the module's DataStore |
| `LocalFeatureHandler` | `CompositionLocal<FeatureHandler>` | Provides the handler to the composition tree |
| `FeatureFlipScreen(modifier, bottomPadding)` | `@Composable` | Full-screen management UI; requires `LocalFeatureHandler` to be provided |
| `FeatureFlip` | `object : Module, RequiresDataStore` | Module entry point; registered with `rememberModules` in the host app |
| `FeatureFlipDestination.Main` | `@Serializable data object` | Only navigation destination; title "Feature Flip" |

`setFeatureState` is `internal` — only the DevView UI can write flag state. External code is read-only.

## Internal Architecture

```
Host app
  └─ rememberModules { }          // calls FeatureFlip.dataStoreDelegate.init(context)
  └─ rememberFeatureHandler(features)
       └─ FeatureHandler(dataStore, initialFeatures)
            └─ featureRegistry: MutableMap<Feature, Preferences.Key<*>>
                 LocalFeature  → booleanPreferencesKey(name)
                 RemoteFeature → intPreferencesKey(name)   // stores FeatureState.ordinal

FeatureFlipScreen
  ├─ reads featureHandler.features  (internal @Composable property → getFeatures() Flow)
  ├─ derives filteredFeatures from search + FeatureFilter chips
  └─ per item → FeatureItem
       ├─ LocalFeature  → Switch  (checked ↔ LOCAL_ON / LOCAL_OFF)
       └─ RemoteFeature → Material3 SingleChoiceSegmentedButtonRow
            labels Remote / Off / On, paired host-theme colours
            supporting text shows the remote value
  └─ DevViewFilterBar: pinned search, collapsible Type/State groups and active count
       query, selected filter names and expanded state use rememberSaveable
```

State-change requests enter one remembered channel, consumed in click order by a `LaunchedEffect`. DataStore owns I/O dispatching. An `IOException` keeps the persisted selection and shows a Retry action; cancellation and invalid requests still propagate. Restored type filters remain active when the registered feature types change.

`rememberFeatureHandler` obtains the DataStore via `FeatureFlip.dataStoreDelegate.get()`. DataStore initialisation (platform-specific file path) is handled by `devview-utils`; this module has no `androidMain`/`iosMain` source sets and no `expect`/`actual` declarations.

DataStore file: `feature_flip_datastore.preferences_pb`

## How Feature Flags Are Defined and Toggled

**Define flags** (host app, before the composition):
```kotlin
val features = listOf(
    Feature.LocalFeature(
        name = "dark_mode",
        description = "Enable dark theme",
        isEnabled = false
    ),
    Feature.RemoteFeature(
        name = "new_checkout",
        description = "New checkout flow",
        defaultRemoteValue = remoteConfig.getBoolean("new_checkout"),
        state = FeatureState.REMOTE
    )
)
val featureHandler = rememberFeatureHandler(features)
CompositionLocalProvider(LocalFeatureHandler provides featureHandler) { … }
```

**Read flags** (anywhere in the composition tree or a ViewModel):
```kotlin
// Compose
val isEnabled by LocalFeatureHandler.current.isFeatureEnabled("dark_mode")

// ViewModel / non-Compose
featureHandler.isFeatureEnabledFlow("dark_mode").collect { … }
```

**Toggle flags** — via the `FeatureFlipScreen` UI only (write path is internal).

## Non-Obvious Conventions

- **`FeatureState` ordinals are load-bearing.** They are persisted as raw integers in DataStore (`REMOTE=0`, `LOCAL_OFF=1`, `LOCAL_ON=2`). Reordering the enum entries is a breaking data migration.
- **Segment order mirrors the enum order.** Selection compares FeatureState values, not a separate index. Keep the persisted ordinals unchanged.
- **`FeatureFilter` is adaptive.** If all registered features are the same type (all `RemoteFeature` or all `LocalFeature`), the LOCAL/REMOTE filter chips are hidden; only ON/OFF chips appear.
- **Filter chips use OR within a dimension, AND across dimensions.** Selecting LOCAL+REMOTE shows all features; selecting LOCAL+ON shows only enabled local features.
- **`Poko` annotation** is applied via `com.worldline.devview.core.Poko` (project-local alias), not the default `@Poko`. The `Feature` sealed class members use standard `data class`, so `Poko` affects other model classes in the wider project.
- **`FeatureHandler.featureRegistry` keyed by `Feature` instance.** Lookups for reads/writes use `firstOrNull { it.key.name == featureName }` — feature `name` is the stable identity; the `Feature` object itself (including `isEnabled`/`state`) changes as DataStore emits updates.
- **Use Material3 segmented controls directly.** No copied measure policy or custom selected-colour logic remains.
