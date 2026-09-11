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

### Operation sheet: one sheet, two pages

`NetworkMockOperationSheet.kt` renders `NetworkMockViewModel.sheetState` as a `ModalBottomSheet`
with two pages switched by `AnimatedContent`, both plain local `remember` state in the sheet
composable (not the ViewModel — see `PreviewSheetState` below):

- **Picker page** (`OperationPickerPage`, `internal` rather than `private` specifically so
  device tests can exercise it without going through `ModalBottomSheet`'s chrome/animation,
  which has no established testing pattern in this codebase): `OperationPickerHeader` mirrors
  `EndpointCard`'s row anatomy (see "Endpoint row anatomy" above) for the name/method/path —
  no version badge, wrapping instead of truncating path — since it renders the same operation
  identity, just above the response list instead of in a list row. Unlike `EndpointCard`, the
  header has **no** state chip: the currently-active response is already marked with a
  checkmark, its full `displayName`, and a family icon/colour by `MockItemContent` in the list
  right below, so a second, bare-status-code chip in the header would only repeat it — the same
  "don't show it twice" reasoning as the deleted version badge. Below the header,
  `NetworkItem` + `MockItem` rows grouped by `StatusCodeFamily`. Tapping a row calls
  `onSelectResponse` and dismisses the sheet. Each `MockItem` also has an eye-icon preview
  toggle (`isMarkedForPreview`/`onToggleMarkedForPreview`) that marks it *without* dismissing;
  once ≥1 response is marked, a "Preview .../Compare 2 responses" button appears and switches
  to the preview page.
- **Preview page** (`MockResponsePreviewPage.kt`, replaces the pre-sheet `NetworkMockEndpointPreviewBottomSheet.kt`):
  same diff-rendering body as before, now reached via a back arrow instead of a close button —
  going back returns to the picker page without clearing the marks.

Marking is driven by `combinedClickable`'s replacement, plain per-row `clickable`s — the sheet
used to require a *long-press* to mark a response for preview (invisible enough that the old
detail screen carried a permanent hint card explaining it); the eye toggle is a visible
affordance, so the hint card is gone along with the long-press gesture.

`PreviewSheetState` (`PreviewSheetState.kt`, unchanged by the sheet redesign) is a sealed
interface with three states: `Hidden`, `Single(response)`, `Compare(first, second)`. Toggling a
response calls `transition(response)`, which cycles: Hidden → Single → Compare (second toggle)
→ back to Single (untoggle one) → Hidden (untoggle the last). Selection identity is the whole
`MockResponse` (effectively its `(statusCode, exampleName)` pair), not a file name. What changed
is *what triggers* a transition (an explicit eye-icon tap, not a long-press) and what the state
*means* (which responses are marked, not "is a second sheet open" — that's now a separate
`showingPreviewPage: Boolean` alongside it).

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
