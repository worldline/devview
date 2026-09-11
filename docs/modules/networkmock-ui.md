# NetworkMock UI

The `devview-networkmock` module provides the Compose UI for the network mocking feature. It implements the `Module` interface and surfaces a single navigation screen — the operation list — backed by `devview-networkmock-core`. Picking a mock response happens in a bottom sheet over that same screen, not a second destination.

## Screens

### Main screen — operation list

The main screen shows a global mock toggle at the top, followed by a scrollable tab row with one tab per OpenAPI spec (e.g. "My Backend"). Each tab lists every operation declared in that spec with its current mock state — there is no environment axis, so a spec spanning multiple API versions shows all of its operations side by side in one tab.

- **Global toggle**: enables or disables all mocking globally. When off, all requests go to the real network regardless of per-endpoint settings. Shows a count of mocked operations (e.g. "3 of 47 mocked") computed across every spec, not just the visible tab.
- **Search & filter bar**: pinned to the bottom of the screen (a `Scaffold` `bottomBar`, reachable one-handed). The search field is always visible; a chevron button expands/collapses the mock-state, version, and HTTP method filter rows below it. All filters are plain client-side filters over already-loaded data and combine with AND (multi-select chips within one row combine with OR).
- **Search field**: filters the visible operations in the current tab by name, path, or operationId, live as you type.
- **Mock-state filter**: a row of "Mocked"/"Network" chips, not scoped to the current tab — selecting one persists across tab switches, since "what's mocked" is a question about every spec.
- **Version filter**: a per-tab row of chips — "All" plus one per distinct `Operation.version` present among that tab's operations (see [Version Tags](networkmock-core.md#version-tags)). Hidden entirely when a spec has no versioned operations.
- **Method filter**: a per-tab row of chips, one per distinct `Operation.method` present among that tab's operations, ordered `GET`/`POST`/`PUT`/`PATCH`/`DELETE`/`HEAD`/`OPTIONS`. Multi-select — no chip selected shows every method; selecting one or more narrows the list to operations using any of the selected methods.
- **Endpoint rows**: a leading colour rail (the state chip's colour, transparent when not mocked) followed by the endpoint name, a colour-coded HTTP method badge, the path (wraps instead of truncating — never cut off), and the current state chip (Network / bare status code, e.g. "404"). There is no separate version badge — `Operation.version` is parsed from the path shown right next to it, so it would only repeat what's already visible. Tap a row to open its operation sheet.
- **Reset to Network**: toolbar action that resets every endpoint to `Network` state in one tap.

### Operation sheet

A bottom sheet over the operation list — opened by tapping a row, not a navigation destination — showing every discovered mock response for that operation, grouped by status code family (2xx, 4xx, 5xx, etc.). Two pages:

- **Picker page** (opens first): a "No mock" row to route the operation to the actual network, plus one row per response variant. Tapping a row activates it and dismisses the sheet. Each response row also has a preview toggle (an eye icon) that marks it without dismissing the sheet — marking one response reveals a "Preview `statusCode - exampleName`" button at the bottom; marking a second changes it to "Compare 2 responses". Tapping that button opens the preview page.
- **Preview page**: shows the marked response's body, or — when two are marked — a diff between them (a side-by-side or inline diff, LCS-based, collapsing long unchanged runs). A back arrow returns to the picker page without losing the marks.

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
