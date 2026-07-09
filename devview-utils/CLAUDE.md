# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-utils` is the lowest-level module in the DevView dependency graph. It provides the DataStore creation infrastructure and related contracts used by every module that needs persistent preferences storage. It must never import from `devview` (enforced by Konsist architecture tests).

## Public API

| Symbol | Location | Purpose |
|---|---|---|
| `createDataStore(producePath)` | `commonMain` | Low-level factory that wraps `PreferenceDataStoreFactory.createWithPath`. Accepts a path-producing lambda so it can be reused by both platform actuals. |
| `rememberDataStore(dataStoreName)` | `commonMain` (expect) | Composable that creates and remembers a `DataStore<Preferences>`. Must be called inside a composition. |
| `DataStoreDelegate` | `commonMain` | Holder class that owns a single `DataStore` instance for a module. Lazily initialised via `init()`, retrieved via `get()`. |
| `RequiresDataStore` | `commonMain` | Interface a `Module` implements to signal it needs a DataStore. `rememberModules` calls `initDataStore()` automatically before `initModule()`. |
| `BooleanPreviewParameterProvider` | `commonMain/preview` | Compose `PreviewParameterProvider<Boolean>` emitting `true` then `false`, used for two-state Composable previews. |

## expect/actual Pattern

`rememberDataStore` is declared as `expect` in `commonMain` with two actuals:

- **Android** (`androidMain`): reads `LocalContext.current`, resolves the path as `context.filesDir.resolve(dataStoreName).absolutePath`, wraps in `remember(dataStoreName)`.
- **iOS** (`iosMain`): resolves the path via `NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, …)` (requires `@OptIn(ExperimentalForeignApi::class)`), wraps in `remember(dataStoreName)`.

Both actuals delegate path resolution to the internal `createDataStore(context, dataStoreName)` / `createDataStore(dataStoreName)` overloads (both `internal`), which in turn call the public `createDataStore(producePath)` in `commonMain`.

## Key Conventions

- **DataStore filename convention:** `"<module_name>_datastore.preferences_pb"`. Each module must use a unique name to prevent accidental state sharing.
- **Single initialisation guarantee:** `DataStoreDelegate.init()` is a no-op if the instance already exists (`if (instance != null) return`), making it safe to call on every recomposition.
- **Shared delegates:** When a DataStore must be shared between two modules (e.g. a Ktor plugin and its UI counterpart), declare a top-level `val` of `DataStoreDelegate` in the shared core module. Only one of the two modules should implement `RequiresDataStore` (the one that drives initialisation); the other reads from the delegate directly via `.get()`.
- **`@Suppress("ComposableNaming")`:** Applied to `DataStoreDelegate.init` and `RequiresDataStore.initDataStore` because they are `@Composable` functions that follow an imperative naming style (no return value, side-effectful). This is intentional — do not rename them to match the Compose convention.
- **`rememberDataStore` must not be called outside a composition.** Non-Composable code always accesses the DataStore through `DataStoreDelegate.get()` after the delegate has been initialised by `rememberModules`.
