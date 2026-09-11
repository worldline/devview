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

**Public theming**: `MockColorScheme` / `StatusColors` (`theme/MockColorScheme.kt`) and the `LocalMockColorScheme` CompositionLocal (`theme/LocalMockColorScheme.kt`) — see "Status colour theming" below.

**Naming note**: internal Compose component names (`EndpointCard`, `EndpointStateChip`, `EndpointHeaderCard`, `MockItem`) and their test tags intentionally keep "endpoint" vocabulary — they're DevView's own UI implementation detail, not one of the types renamed to OpenAPI vocabulary by the 0.2.0 migration (`ApiSpecUiModel`, `OperationUiModel`, `OperationKey`, etc.).

## Internal Architecture

### Initialization flow

`NetworkMock.initModule()` (called by `rememberModules` after `initDataStore`) delegates to `NetworkMockInitializer.initialize(...)` from the core module, which constructs `MockConfigRepository` and `MockStateRepository` once for the process lifetime. ViewModels retrieve them via `NetworkMockInitializer.requireConfigRepository()` / `requireStateRepository()`.

### Shared DataStore singleton

`NetworkMock.dataStoreDelegate` points to `NetworkMockDataStoreDelegate` — a process-level singleton declared in `devview-networkmock-core`. Both this module and `devview-networkmock-ktor` reference the same object, so there is exactly one DataStore instance without a direct dependency between those two modules.

### ViewModel state model

Both ViewModels combine two sources via `combine(...)` stateIn `WhileSubscribed(5000ms)`:
- A one-shot coroutine that loads data from `MockConfigRepository`
- A live `Flow<NetworkMockState>` from `MockStateRepository.observeState()`

`NetworkMockViewModel` (main list) only calls `loadConfiguration()` — spec metadata, no response
bodies. `NetworkMockEndpointViewModel` (detail screen) additionally calls
`discoverResponseFiles(operationKey)` for its one operation, since that's the only place a
response body is actually read; its `NetworkMockEndpointUiState.Content.responses` carries the
result alongside `operationUiModel`.

### Search and filters live in the composable, not the ViewModel

`NetworkMockScreen`'s search query, per-spec selected version, per-spec selected methods, and
the mock-state filter are plain `remember`/`mutableStateMapOf` state in `ContentState` — all are
pure client-side filters over data the ViewModel already loaded, so there's no reason to
round-trip them through `NetworkMockUiState`. The version and method selections are keyed by
`ApiSpec.id` (`mutableStateMapOf<String, ...>`) rather than living inside the pager's per-page
scope, because the filter chip rows themselves render in the screen's `Scaffold` `bottomBar` —
outside the `HorizontalPager` — and need to read/write the *current* tab's selection from there.
The mock-state filter (`MockStateFilter`: `MOCKED`/`NETWORK`) is deliberately **not** keyed by
spec — mocked-ness is a question about every operation, not just the visible tab's, so unlike
version/method it stays selected across tab switches.

### Bottom bar: search always visible, filters collapse behind a chevron

Only the search field and a chevron `IconButton` are visible by default (mirrors
`devview-analytics`'s `AnalyticsScreen` bottom bar). Tapping the chevron toggles `filtersExpanded`,
revealing — top to bottom — the mock-state filter row, the version filter row (if the current
spec has versioned operations), then the method filter row (if it has more than one method) inside
an `AnimatedVisibility`. The chevron rotates via `graphicsLayer(rotationX = ...)` driven by
`animateFloatAsState`, identical to the Analytics pattern.

### Global mocked-count header

The `GlobalMockToggle` row (above the tab row, not inside the `HorizontalPager`) shows
`"$mockedCount of $totalCount mocked"` computed by summing `OperationMockState.Mock` occurrences
across **every** spec via `remember(key1 = uiState.specs) { derivedStateOf { ... } }` — global,
not per-tab, so it answers "what have I left mocked anywhere" without needing to visit every
tab. This replaced the two-line "Mock responses enabled/disabled" explainer text.

### Endpoint row anatomy

`EndpointCard` is two lines (name; method badge + path + version badge) plus a leading 3.dp
colour rail — the state chip's container colour for `Mock`, transparent for `Network` — following
`devview-analytics`'s `AnalyticsLogItem` rail pattern. There used to be a third line duplicating
the state chip's status code (`OperationMockState.displayName`); `EndpointStateChip`'s label now
defaults to the full `displayName` (`"$statusCode - $exampleName"`) instead of a bare status code,
so the chip alone carries what the deleted line used to — see #115. HTTP method badges are
coloured via `HttpMethod.badgeContainerColor`/`.badgeContentColor` (`ModelUtils.kt`), mapped onto
`MaterialTheme` colour-scheme roles rather than a fixed palette — a method is a navigational aid,
not a status signal, so it should track the host app's brand colours the way status colours
deliberately don't (see "Status code colors and icons" below).

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

`ModelUtils.kt` provides internal extension properties (`OperationMockState.icon`, `.contentColor`, `.containerColor`) that map HTTP status families (1xx–5xx) to `ImageVector` values and — via `theme/MockColorScheme.kt` — theme-aware `Color` values. `iconForStatusCode`/`contentColorForStatusCode`/`containerColorForStatusCode` are the free-function equivalents used where there's no `OperationMockState` to hang the extension off of (e.g. `MockItem`'s per-response rows). The icon mapping is a plain `when` on numeric ranges; colours resolve through `theme/LocalMockColorScheme.kt`'s `rememberMockColorScheme()` — see "Status colour theming" below.

### Status colour theming

Mirrors `devview-consolelogger`'s `LogColorScheme` pattern exactly: `MockColorScheme` (`theme/MockColorScheme.kt`) holds two complete, hand-tuned palettes (`Light`/`Dark`) — one `StatusColors(container, content)` pair per `StatusCodeFamily` plus one for the `Network` pass-through state — deliberately not derived from `MaterialTheme.colorScheme`, since a 2xx chip needs to read as "success" regardless of the host's brand palette. Hosts provide a palette via the public `LocalMockColorScheme` CompositionLocal at their `MaterialTheme` site; `rememberMockColorScheme()` (internal, in `theme/LocalMockColorScheme.kt`) resolves it, falling back to a luminance-based guess against the ambient `MaterialTheme.colorScheme.surface` (not `isSystemInDarkTheme()` — DevView's theme may not track the system setting) with a one-time Kermit warning if the CompositionLocal was never provided. Because the colour extensions in `ModelUtils.kt` are themselves `@Composable @ReadOnlyComposable`, their test coverage for anything beyond the pure `MockColorScheme.get()`/`copy()` (`MockColorSchemeTest.kt`, `commonTest`) lives in `androidDeviceTest` (`ModelUtilsColorTest.kt`, `MockColorSchemeResolutionTest.kt`), not `commonTest`.

### Fake data for previews

`ModelUtils.kt` adds `fake(...)` functions on the `Companion` objects of `ApiSpecUiModel`, `OperationDescriptor`, `OperationUiModel`, and `MockResponse`. Compose `@Preview` parameters use these via `*PreviewParameterProvider` classes in the `preview/` package.

## Testing

Host tests (`androidHostTest`) use MockK to stub `MockConfigRepository` and `MockStateRepository`, and the `ViewModelTest` base class from `devview-test` for coroutine dispatcher setup. Device tests (`androidDeviceTest`) exercise composables with fake repository implementations in the `fixtures/` package.

The `tasks.withType<Test> { failOnNoDiscoveredTests.set(false) }` block exists because the `androidHostTest` source set may legitimately be empty for some configurations.
