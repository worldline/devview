# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-networkmock` is the Compose UI layer for the network mocking feature. It depends on `devview-networkmock-core` (which owns the mock engine: OpenAPI spec parsing, request matching, DataStore state) and surfaces it as a DevView `Module` with a single navigation screen — the operation list. Picking a mock response happens in a bottom sheet over that screen, not a second destination; see "Operation sheet" below.

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

**`NetworkMockDestination`** — sealed nav key interface with exactly one destination, `Main` (the operation list). Prior to the sheet-based redesign there was also an `Endpoint(operationKey: OperationKey)` destination pushing a full detail screen — removed as a breaking change; see the operation sheet section below.

**Public composable**: `NetworkMockScreen` (the operation list; internally also hosts `NetworkMockOperationSheet` when one is open). `NetworkMockOperationSheet`, `MockResponsePreviewPage`, and every component in `components/` are `internal`.

**Public ViewModel**: `NetworkMockViewModel` (constructed by `NetworkMock.registerContent` via the `viewModel { }` factory, scoped to the navigation entry). Owns both the operation list (`uiState`) and the operation sheet (`sheetState`, `openOperation()`, `closeSheet()`) — there is no separate ViewModel per screen since there's no separate screen.

**Public UI states**: `NetworkMockUiState` (Loading / Error / Empty / Content) for the operation list, and `OperationSheetState` (Hidden / Loading / Error / Content) for the operation sheet.

**Public UI models**: `ApiSpecUiModel` (one per spec tab) and `OperationUiModel` (pairs a static `OperationDescriptor` with a live `OperationMockState`) — the latter is reused as `OperationSheetState.Content.operationUiModel`.

**Public theming**: `MockColorScheme` / `StatusColors` (`theme/MockColorScheme.kt`) and the `LocalMockColorScheme` CompositionLocal (`theme/LocalMockColorScheme.kt`) — see "Status colour theming" below.

**Naming note**: internal Compose component names (`EndpointCard`, `EndpointStateChip`, `EndpointHeaderCard`, `MockItem`) and their test tags intentionally keep "endpoint" vocabulary — they're DevView's own UI implementation detail, not one of the types renamed to OpenAPI vocabulary by the 0.2.0 migration (`ApiSpecUiModel`, `OperationUiModel`, `OperationKey`, etc.).

## Internal Architecture

### Initialization flow

`NetworkMock.initModule()` (called by `rememberModules` after `initDataStore`) delegates to `NetworkMockInitializer.initialize(...)` from the core module, which constructs `MockConfigRepository` and `MockStateRepository` once for the process lifetime. ViewModels retrieve them via `NetworkMockInitializer.requireConfigRepository()` / `requireStateRepository()`.

### Shared DataStore singleton

`NetworkMock.dataStoreDelegate` points to `NetworkMockDataStoreDelegate` — a process-level singleton declared in `devview-networkmock-core`. Both this module and `devview-networkmock-ktor` reference the same object, so there is exactly one DataStore instance without a direct dependency between those two modules.

### ViewModel state model: two independent `combine` chains, one ViewModel

`NetworkMockViewModel` exposes two `StateFlow`s, each its own `combine(...).stateIn(WhileSubscribed(5000ms))`
chain, re-emitting independently — opening the sheet doesn't touch `uiState`, and picking a
response doesn't re-derive the whole operation list:

- **`uiState`** (operation list): `privateConfiguration` (loaded once via `loadConfiguration()` —
  spec metadata only, no response bodies) combined with `MockStateRepository.observeState()`.
- **`sheetState`** (operation sheet): `privateOpenOperationKey` (`null` when hidden),
  `privateLoadedOperation`, `privateSheetError`, and the *same* `observeState()` flow — four
  sources, since `combine` supports up to 5. `openOperation(key)` is the only place
  `discoverResponseFiles(key)` is called — this is where response bodies are actually read,
  once per sheet-open, not eagerly for the whole list.

`privateLoadedOperation` is a `LoadedOperation(key, descriptor, responses)`, not just the
discovered data — `sheetState`'s combine checks `loaded.key == openKey` before treating it as
current. Without that check, closing operation A's sheet and immediately opening operation B's
could briefly show A's stale content while B is still discovering (a real race once one
ViewModel handles every operation, not one ViewModel per opened operation).

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
spec has versioned operations), the method filter row (if it has more than one method), then the
tag filter row (if the current spec has any tagged operations — see
[Tags](../docs/modules/networkmock-core.md#tags)) inside an `AnimatedVisibility`. The chevron
rotates via `graphicsLayer(rotationX = ...)` driven by `animateFloatAsState`, identical to the
Analytics pattern.

### Global mocked-count header

The `GlobalMockToggle` row (above the tab row, not inside the `HorizontalPager`) shows
`"$mockedCount of $totalCount mocked"` computed by summing `OperationMockState.Mock` occurrences
across **every** spec via `remember(key1 = uiState.specs) { derivedStateOf { ... } }` — global,
not per-tab, so it answers "what have I left mocked anywhere" without needing to visit every
tab. This replaced the two-line "Mock responses enabled/disabled" explainer text.

### Endpoint row anatomy

`EndpointCard` is two lines (name; method badge + path) plus a leading 3.dp colour rail — the
state chip's container colour for `Mock`, transparent for `Network` — following
`devview-analytics`'s `AnalyticsLogItem` rail pattern. There used to be a third line duplicating
the state chip's status code (`OperationMockState.displayName`, see #115); that line is gone and
`EndpointStateChip`'s default label is the bare status code (`"404"`), not the full
`"$statusCode - $exampleName"` — a chip carrying both the code and the example name (e.g.
`"404 - default"`) was wide enough to squeeze the path into truncation. The example name is
still shown in full where it's actually chosen (`EndpointStateChip(label = ...)` call sites in
the operation preview sheet). The path itself never truncates — it has no `maxLines`/`overflow`
and simply wraps, since a cut-off URL segment can hide the difference between two similar
operations; the method badge row uses `verticalAlignment = Alignment.Top` so the badge stays on
the path's first line when it wraps. There is no version badge in the row: `Operation.version` is
parsed out of the very path segment rendered next to it (see
[Version Tags](../docs/modules/networkmock-core.md#version-tags)), so a separate badge could
never show anything the path wasn't already showing — it only crowded the trailing state chip.
The version *filter* in the bottom bar is unaffected; it still needs `Operation.version` to group
operations, it's just not repeated as a badge on every row. HTTP method badges are coloured via
`HttpMethod.badgeContainerColor`/`.badgeContentColor` (`ModelUtils.kt`), mapped onto
`MaterialTheme` colour-scheme roles rather than a fixed palette — a method is a navigational aid,
not a status signal, so it should track the host app's brand colours the way status colours
deliberately don't (see "Status code colors and icons" below).

### "Reset to Network" toolbar action

Wired via a `MutableSharedFlow<Unit>` (capacity 1, `DROP_OLDEST`) created in `NetworkMock` and passed into `NetworkMockScreen`. `resetAllToNetwork()` resets every operation in the parsed config (not just those stored in DataStore) to avoid gaps for operations the user has never touched.

### "Sort" toolbar dropdown

Registered via `DestinationMetadataBuilder.menu` (the shared toolbar's dropdown-menu action —
see `devview/DevView.kt` and `ModuleDestinationAction.menuItems`), not the plain single-tap
`action` used by "Refresh"/"Reset to Network" — a sort key is a discrete choice among several, so
it gets an anchored dropdown instead of a cycling single-tap icon. One menu entry per
`OperationSort` value (`Default`, `Path (A-Z)`, `Method`, `Tag`, `Mocked first`), built once at module-construction
time from `OperationSort.entries`; because the shared `menu` DSL takes a fixed list of items with
no access to the currently-visible spec, "Tag" is always offered even when the current spec has no
tagged operations — picking it in that case is a harmless no-op (`sortedByOption` sorts by every
operation's absent first tag, i.e. an equal empty string for all, so the list order doesn't
change). "Mocked first" uses `sortedBy { it.currentState is OperationMockState.Network }`:
Mock, Failure, and Sequence precede Network, with stable spec order within both groups. List
items use `animateItem()` so filter and sort changes animate.

The flow crossing `NetworkMock` → `NetworkMockScreen` carries an `OperationSort.label: String`,
not `OperationSort` itself: `NetworkMockScreen` is public API but `OperationSort` is deliberately
`internal` (see "Search and filters live in the composable, not the ViewModel" above), and a
public composable can't expose an internal type in its signature. `ContentState` maps the label
back to the enum entry via `OperationSort.entries.firstOrNull { it.label == label }`; both ends of
this flow live in this module and share the same `OperationSort.label` values, so the label is a
safe, internal-only protocol despite the public parameter type. Sort selection itself is stored
the same way as the version/method/tag filters — plain `mutableStateMapOf<String, OperationSort>`
in `ContentState`, keyed by spec ID, never round-tripped through `NetworkMockViewModel`.

### Operation sheet: a fixed-height tabbed sheet and preview

`NetworkMockOperationSheet.kt` renders `NetworkMockViewModel.sheetState` as a
`ModalBottomSheet` with `surfaceContainerLow` and a fixed `fillMaxHeight(0.65f)` root.
An operation identity header precedes Responses / Sequence / Failure tabs. Tab choice and
preview state are local UI details, not new ViewModel state or navigation destinations.
Switching tabs or opening Preview leaves the height unchanged.

- **Responses**: Network and status-family cards with inset dividers, matching sticky headers,
  and animated items. Tapping a row applies it and closes the sheet; the current choice is
  checked. The eye opens Preview directly without a marking toggle or conditional bottom button.
- **Sequence**: the existing builder seeds active steps, preserves order/repetitions, allows
  removal, and saves at least two available steps through `onCommitSequence` before closing.
  Active sequence position and Reset are here. Cancel returns to Responses without applying
  the draft. The last response continues to repeat once the sequence finishes.
- **Failure**: grouped Timeout / Connection Refused choices close the sheet when applied.
  Spec-authored `x-devview.failureRate` remains read-only in a footer.
- **Preview**: the eye-selected response body and Compare with dropdown of alternatives.
  Comparing retains the original first response; Back returns to Responses. Neither preview
  nor comparison activates a mock. Keep `previewToggleTestTag` despite the direct-open behavior.

`PreviewSheetState` holds `Hidden`, `Single(response)`, or `Compare(first, second)`.
It no longer models marked rows or a toggle cycle: `transition` and `isInPreviewMode` are
removed. Response variant identity includes `(statusCode, mediaType, exampleName)`.
The ViewModel, `OperationSheetState`, repositories, mock engine, Ktor plugin, persistence,
and public API are unchanged by this revamp.

### Diff rendering pipeline

`MockResponse.displayContent()` (`utils/DiffLineUtils.kt`) pretty-prints valid JSON when
`contentType` contains "json" (case-insensitive), falling back to the raw body for malformed
JSON or non-JSON content. `ResponseContentPane` uses it for display, and
`PreviewSheetState.Compare` uses it before similarity checking and diffing. This is display-only:
served response bodies do not change. `DiffLineRow` keeps logical lines unwrapped (`softWrap = false`); each pane has one
horizontal scroll around a finite-width `LazyColumn`, so all rows move together and only
visible rows are composed.

When the sheet is in `Compare` state, formatted responses are diffed:

1. `PreviewSheetState.Compare` formats and caches one diff per pair, falling back to split previews above 1,000,000 LCS cells; shared-line ratio ≥ `INLINE_DIFF_THRESHOLD` (0.4) uses inline diff.
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
