# NetworkMock UI

The `devview-networkmock` module provides the Compose UI for the network mocking feature. It implements the `Module` interface and surfaces a single navigation screen — the operation list — backed by `devview-networkmock-core`. Picking a mock response happens in a bottom sheet over that same screen, not a second destination.

## Screens

### Main screen — operation list

The main screen shows a global mock toggle at the top, followed by a scrollable tab row with one tab per OpenAPI spec (e.g. "My Backend"). Each tab lists every operation declared in that spec with its current mock state — there is no environment axis, so a spec spanning multiple API versions shows all of its operations side by side in one tab.

- **Global toggle**: enables or disables all mocking globally. When off, all requests go to the real network regardless of per-endpoint settings. Shows a count of mocked operations (e.g. "3 of 47 mocked") computed across every spec, not just the visible tab.
- **Search & filter bar**: the shared pinned search field expands a bounded, scrollable panel with Sort by, State, Version, Method, and Tag groups. The badge counts active non-search selections, including a non-default sort. Filters combine with AND; multi-select chips within one group combine with OR. Search and every selection are independent per API group and survive navigation and screen recreation through navigation-owned saved state, without changing persisted mock settings. **Clear filters** resets that group’s selections and sort but keeps its search; an empty result offers **Clear search and filters** to reset both.
- **Search field**: filters the visible operations in the current tab by name, path, or operationId, live as you type.
- **Mock-state filter**: per-group "Mocked"/"Network" chips. Mocked includes response mocks, failures, and sequences; no selection shows both.
- **Version filter**: "All" or one version per API group (see [Version Tags](networkmock-core.md#version-tags)). Hidden when there are no versioned operations or retained version selection. Selections absent after a spec reload remain visible so they can be cleared.
- **Method filter**: a per-tab row of chips, one per distinct `Operation.method` present among that tab's operations, ordered `GET`/`POST`/`PUT`/`PATCH`/`DELETE`/`HEAD`/`OPTIONS`. Multi-select — no chip selected shows every method; selecting one or more narrows the list to operations using any of the selected methods.
- **Tag filter**: a per-tab row of chips, one per distinct `Operation.tag` present among that tab's operations (see [Tags](networkmock-core.md#tags)), alphabetically ordered. Multi-select, same semantics as the method filter. Hidden when a spec has no tagged operations and no retained tag selection; selections absent after a reload remain available to clear.
- **Endpoint rows**: a leading colour rail (the state chip's colour, transparent when not mocked) followed by the endpoint name, a colour-coded HTTP method badge, the path (wraps instead of truncating — never cut off), and the current state (quiet Network text / a semantic status chip with a bare code, e.g. "404"). There is no separate version badge — `Operation.version` is parsed from the path shown right next to it, so it would only repeat what's already visible. Tap a row to open its operation sheet.
- **Reset to Network**: toolbar action that resets every endpoint to `Network` state in one tap.
- **Reload Config**: toolbar action that re-reads and re-parses every configured OpenAPI spec from disk, picking up edits to a spec file without restarting the app. Operations added, removed, or renamed in the spec appear immediately; per-operation mock selections are untouched. See [Caching & Reload](networkmock-core.md#caching--reload).
- **Sort**: selected chips inside the filter panel, not a separate toolbar menu. Choose Default (spec document order), Path (A-Z), Method (`HttpMethod.DefaultMethods` order), Tag (first declared tag, untagged first), or Mocked first (Mock, Failure, and Sequence before Network, stable within each group). Sort is saved per API group alongside its filters.

### Operation sheet

A bottom sheet over the operation list — opened by tapping a row, not a navigation destination — with a fixed 65% height (`fillMaxHeight(0.65f)`). Below the operation header, **Responses**, **Sequence**, and **Failure** tabs separate the three workflows. The eye opens a separate Preview page; switching tabs or opening Preview does not resize the sheet.

- **Responses tab** (opens first): the operation's name, method, and wrapping path, followed by rounded grouped cards for "No mock" and status-code families (2xx, 4xx, 5xx, etc.). Sticky headers use the same `surfaceContainerLow` background as the sheet; cards use `surfaceContainerHighest` with inset dividers and footer hints. Tapping "No mock" or a response activates that choice and dismisses the sheet. The active choice has a checkmark. Response rows show media types when needed to distinguish variants. An eye icon opens that response's preview directly — there is no marking step or bottom Preview/Compare button. The Sequence tab holds sequence controls, rather than an isolated button above the responses.
- **Failure tab**: Timeout and Connection Refused appear as grouped choices; tapping one applies the failure and dismisses the sheet. A configured `x-devview.failureRate` appears as a read-only footer below that group, not under its header. See [Simulating failures](networkmock-core.md#simulating-failures).
- **Preview page**: shows the response opened with the eye icon. Choose another response from the **"Compare with…" dropdown** in the header to compare it with the original, using an inline or side-by-side LCS diff with long unchanged runs collapsed. Valid JSON is pretty-printed before display and diffing; malformed JSON and non-JSON bodies remain unchanged. Logical lines stay unwrapped. Each preview pane scrolls horizontally as one section, and a lazy vertical list creates only visible rows. Comparisons exceeding 1,000,000 LCS cells use stacked previews instead, keeping diff memory bounded. Media-type labels distinguish otherwise identical variants. Previewing never activates a mock or changes its served body. Back returns to Responses.
- **Sequence tab**: tap responses in serving order to append numbered steps; repeated responses are allowed, and individual steps can be removed. Editing an active sequence starts with its saved steps. **Save Sequence (n)** requires at least two steps, applies the sequence, and closes the sheet. Cancel returns to Responses without applying draft changes. The last step repeats after the sequence finishes; Reset restarts the active sequence at step 1. See [Sequential mocks](networkmock-workflows.md#simulating-a-stagedpolling-flow-sequential-mocks).

List search, filtering, and sorting are client-side operations over loaded metadata. Their internal ViewModel state uses `SavedStateHandle`; the mock engine, repositories, `OperationSheetState`, Ktor behavior, persisted mock settings, and public API are unchanged by the list update.

## Theming

Status-family colors (2xx green, 4xx/5xx red, etc.) are a fixed, hand-tuned palette per
theme, not derived from `MaterialTheme.colorScheme` — see the
[Theming guide](../guides/theming.md#network-mock-status-colors) for how to provide and
override them.

## Registration

```kotlin
val modules = rememberModules {
    module(NetworkMock(
        resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) },
        specPaths = listOf("files/networkmocks/specs/my-backend.json")
    ))
}
```

`specPaths` lists every OpenAPI spec file to load — one per API group. There is no default; pass every spec your app should mock.

## Related Modules

- [NetworkMock Core](networkmock-core.md): Shared config and state.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor client plugin.
- [NetworkMock Workflows](networkmock-workflows.md): Step-by-step integration guide.
