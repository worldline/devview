# NetworkMock UI

The `devview-networkmock` module provides the Compose UI for the network mocking feature. It implements the `Module` interface and surfaces a single navigation screen — the operation list — backed by `devview-networkmock-core`. Picking a mock response happens in a bottom sheet over that same screen, not a second destination.

## Screens

### Main screen — operation list

The main screen shows a global mock toggle at the top, followed by a scrollable tab row with one tab per OpenAPI spec (e.g. "My Backend"). Each tab lists every operation declared in that spec with its current mock state — there is no environment axis, so a spec spanning multiple API versions shows all of its operations side by side in one tab.

- **Global toggle**: enables or disables all mocking globally. When off, all requests go to the real network regardless of per-endpoint settings. Shows a count of mocked operations (e.g. "3 of 47 mocked") computed across every spec, not just the visible tab.
- **Search & filter bar**: pinned to the bottom of the screen (a `Scaffold` `bottomBar`, reachable one-handed). The search field is always visible; a chevron button expands/collapses the mock-state, version, HTTP method, and tag filter rows below it. All filters are plain client-side filters over already-loaded data and combine with AND (multi-select chips within one row combine with OR).
- **Search field**: filters the visible operations in the current tab by name, path, or operationId, live as you type.
- **Mock-state filter**: a row of "Mocked"/"Network" chips, not scoped to the current tab — selecting one persists across tab switches, since "what's mocked" is a question about every spec.
- **Version filter**: a per-tab row of chips — "All" plus one per distinct `Operation.version` present among that tab's operations (see [Version Tags](networkmock-core.md#version-tags)). Hidden entirely when a spec has no versioned operations.
- **Method filter**: a per-tab row of chips, one per distinct `Operation.method` present among that tab's operations, ordered `GET`/`POST`/`PUT`/`PATCH`/`DELETE`/`HEAD`/`OPTIONS`. Multi-select — no chip selected shows every method; selecting one or more narrows the list to operations using any of the selected methods.
- **Tag filter**: a per-tab row of chips, one per distinct `Operation.tag` present among that tab's operations (see [Tags](networkmock-core.md#tags)), alphabetically ordered. Multi-select, same semantics as the method filter. Hidden entirely when a spec has no tagged operations.
- **Endpoint rows**: a leading colour rail (the state chip's colour, transparent when not mocked) followed by the endpoint name, a colour-coded HTTP method badge, the path (wraps instead of truncating — never cut off), and the current state chip (Network / bare status code, e.g. "404"). There is no separate version badge — `Operation.version` is parsed from the path shown right next to it, so it would only repeat what's already visible. Tap a row to open its operation sheet.
- **Reset to Network**: toolbar action that resets every endpoint to `Network` state in one tap.
- **Reload Config**: toolbar action that re-reads and re-parses every configured OpenAPI spec from disk, picking up edits to a spec file without restarting the app. Operations added, removed, or renamed in the spec appear immediately; per-operation mock selections are untouched. See [Caching & Reload](networkmock-core.md#caching--reload).
- **Sort**: toolbar dropdown — `NetworkMock` exposes a `sortSharedFlow`, and `NetworkMockScreen` collects it in `ContentState`. Tapping the icon opens an anchored menu with one entry per sort key: spec order (default, document order), path (A-Z), method (`HttpMethod.DefaultMethods` order), tag (an operation's first declared tag, untagged operations first), and "Mocked first" (Mock, Failure, and Sequence before Network, preserving spec order within each group); picking an entry sets that sort directly. The menu always offers "Tag" even when the current tab has no tagged operations — it's a fixed list built once at module-construction time — but picking it in that case is a no-op. Sort selection is per-tab, like the version and method filters, and — like every filter here — pure client-side state, not persisted or round-tripped through the ViewModel.

### Operation sheet

A bottom sheet over the operation list — opened by tapping a row, not a navigation destination — with a fixed 65% height (`fillMaxHeight(0.65f)`). Below the operation header, **Responses**, **Sequence**, and **Failure** tabs separate the three workflows. The eye opens a separate Preview page; switching tabs or opening Preview does not resize the sheet.

- **Responses tab** (opens first): the operation's name, method, and wrapping path, followed by rounded grouped cards for "No mock" and status-code families (2xx, 4xx, 5xx, etc.). Sticky headers use the same `surfaceContainerLow` background as the sheet; cards use `surfaceContainerHighest` with inset dividers and footer hints. Tapping "No mock" or a response activates that choice and dismisses the sheet. The active choice has a checkmark. Response rows show media types when needed to distinguish variants. An eye icon opens that response's preview directly — there is no marking step or bottom Preview/Compare button. The Sequence tab holds sequence controls, rather than an isolated button above the responses.
- **Failure tab**: Timeout and Connection Refused appear as grouped choices; tapping one applies the failure and dismisses the sheet. A configured `x-devview.failureRate` appears as a read-only footer below that group, not under its header. See [Simulating failures](networkmock-core.md#simulating-failures).
- **Preview page**: shows the response opened with the eye icon. Choose another response from the **"Compare with…" dropdown** in the header to compare it with the original, using an inline or side-by-side LCS diff with long unchanged runs collapsed. Valid JSON is pretty-printed before display and diffing; malformed JSON and non-JSON bodies remain unchanged. Long lines wrap, and content scrolls vertically rather than each line scrolling sideways. Comparisons exceeding 1,000,000 LCS cells use stacked previews instead, keeping diff memory bounded. Media-type labels distinguish otherwise identical variants. Previewing never activates a mock or changes its served body. Back returns to Responses.
- **Sequence tab**: tap responses in serving order to append numbered steps; repeated responses are allowed, and individual steps can be removed. Editing an active sequence starts with its saved steps. **Save Sequence (n)** requires at least two steps, applies the sequence, and closes the sheet. Cancel returns to Responses without applying draft changes. The last step repeats after the sequence finishes; Reset restarts the active sequence at step 1. See [Sequential mocks](networkmock-workflows.md#simulating-a-stagedpolling-flow-sequential-mocks).

This revamp is UI-only: the mock engine, repositories, ViewModel, `OperationSheetState`, Ktor behavior, persistence, and public API are unchanged.

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
