# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-networkmock` is the Compose UI layer for the network mocking feature. It depends on `devview-networkmock-core` (which owns the mock engine: OpenAPI spec parsing, request matching, DataStore state) and surfaces it as a DevView `Module` with two navigation screens.

## Public API

**`NetworkMock`** (`NetworkMock.kt`) — the sole entry point for integrators. Implements `Module` and `RequiresDataStore`. Registered via:
```kotlin
rememberModules {
    module(NetworkMock(
        resourceLoader = { path -> Res.readBytes(path) },
        specPaths = listOf("files/networkmocks/specs/my-backend.json")
    ))
}
```
`specPaths` lists every OpenAPI spec file to load — one per API group. There is no default; every spec must be listed explicitly.

**`NetworkMockDestination`** — sealed nav key interface with two destinations:
- `Main` — the full operation list screen
- `Endpoint(operationKey: OperationKey)` — the detail screen for one operation

**Public composable**: `NetworkMockScreen` (main list). `NetworkMockEndpointScreen` is `internal`.

**Public ViewModels**: `NetworkMockViewModel` and `NetworkMockEndpointViewModel` (both constructed by `NetworkMock.registerContent` via the `viewModel { }` factory, scoped to their navigation entry).

**Public UI states**: `NetworkMockUiState` (Loading / Error / Empty / Content) and `NetworkMockEndpointUiState` (Loading / Error / Content).

**Public UI models**: `ApiSpecUiModel` (one per spec tab) and `OperationUiModel` (pairs a static `OperationDescriptor` with a live `OperationMockState`).

**Naming note**: internal Compose component names (`EndpointCard`, `EndpointStateChip`, `EndpointHeaderCard`, `MockItem`) and their test tags intentionally keep "endpoint" vocabulary — they're DevView's own UI implementation detail, not one of the types renamed to OpenAPI vocabulary by the 0.2.0 migration (`ApiSpecUiModel`, `OperationUiModel`, `OperationKey`, etc.).

## Internal Architecture

### Initialization flow

`NetworkMock.initModule()` (called by `rememberModules` after `initDataStore`) delegates to `NetworkMockInitializer.initialize(...)` from the core module, which constructs `MockConfigRepository` and `MockStateRepository` once for the process lifetime. ViewModels retrieve them via `NetworkMockInitializer.requireConfigRepository()` / `requireStateRepository()`.

### Shared DataStore singleton

`NetworkMock.dataStoreDelegate` points to `NetworkMockDataStoreDelegate` — a process-level singleton declared in `devview-networkmock-core`. Both this module and `devview-networkmock-ktor` reference the same object, so there is exactly one DataStore instance without a direct dependency between those two modules.

### ViewModel state model

Both ViewModels combine two sources via `combine(...)` stateIn `WhileSubscribed(5000ms)`:
- A one-shot coroutine that loads config / discovers response variants from `MockConfigRepository`
- A live `Flow<NetworkMockState>` from `MockStateRepository.observeState()`

### "Reset to Network" toolbar action

Wired via a `MutableSharedFlow<Unit>` (capacity 1, `DROP_OLDEST`) created in `NetworkMock` and passed into `NetworkMockScreen`. `resetAllToNetwork()` resets every operation in the parsed config (not just those stored in DataStore) to avoid gaps for operations the user has never touched.

### Preview bottom sheet state machine

`PreviewSheetState` (in `NetworkMockEndpointScreen.kt`) is a sealed interface with three states: `Hidden`, `Single(response)`, `Compare(first, second)`. Toggling a response via long-press calls `transition(response)`, which cycles: Hidden → Single → Compare (second long-press) → back to Single (deselect one) → Hidden (deselect last). Selection identity is the whole `MockResponse` (effectively its `(statusCode, exampleName)` pair), not a file name.

### Diff rendering pipeline

When the sheet is in `Compare` state, responses are diffed:

1. `shouldUseInlineDiff(...)` — computes LCS length / max lines; uses inline diff if ratio ≥ `INLINE_DIFF_THRESHOLD` (0.4).
2. `computeLineDiff(...)` — LCS-based O(m×n) diff producing a `PersistentList<DiffLine>` (Unchanged / Different).
3. `List<DiffLine>.toDisplayLines()` — flattens to `DisplayLine` values and collapses runs of ≥ `COLLAPSE_THRESHOLD` (7) unchanged lines, keeping `CONTEXT_LINES` (3) on each side with a `Collapsed(count)` placeholder.

### Status code colors and icons

`ModelUtils.kt` provides internal extension properties (`OperationMockState.icon`, `.contentColor`, `.containerColor`) that map HTTP status families (1xx–5xx) to hardcoded `Color` and `ImageVector` values. These are the only place to update if chip colours need changing.

### Fake data for previews

`ModelUtils.kt` adds `fake(...)` functions on the `Companion` objects of `ApiSpecUiModel`, `OperationDescriptor`, `OperationUiModel`, and `MockResponse`. Compose `@Preview` parameters use these via `*PreviewParameterProvider` classes in the `preview/` package.

## Testing

Host tests (`androidHostTest`) use MockK to stub `MockConfigRepository` and `MockStateRepository`, and the `ViewModelTest` base class from `devview-test` for coroutine dispatcher setup. Device tests (`androidDeviceTest`) exercise composables with fake repository implementations in the `fixtures/` package.

The `tasks.withType<Test> { failOnNoDiscoveredTests.set(false) }` block exists because the `androidHostTest` source set may legitimately be empty for some configurations.
