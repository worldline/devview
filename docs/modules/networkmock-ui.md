# NetworkMock UI

The `devview-networkmock` module provides the Compose UI for the network mocking feature. It implements the `Module` interface and surfaces two navigation screens backed by `devview-networkmock-core`.

## Screens

### Main screen — endpoint list

The main screen shows a global mock toggle at the top, followed by a scrollable tab row with one tab per API group + environment combination (e.g. "My Backend – Staging"). Each tab lists the endpoints for that group/environment with their current mock state.

- **Global toggle**: enables or disables all mocking globally. When off, all requests go to the real network regardless of per-endpoint settings.
- **Endpoint cards**: show the endpoint name, HTTP method, path, and current state chip (Network / HTTP status code). Tap an endpoint to open its detail screen.
- **Reset to Network**: toolbar action that resets every endpoint to `Network` state in one tap.

### Endpoint detail screen

Shows the full endpoint info and all discovered mock response files, grouped by status code family (2xx, 4xx, 5xx, etc.).

- **"No mock" option**: tap to route this endpoint to the real network.
- **Response items**: tap to activate a mock response (shown with its status code chip); long-press to open a preview bottom sheet.
- **Preview bottom sheet**: shows the response file contents. Long-press a second response to enter compare mode, which renders a side-by-side or inline diff (LCS-based, collapses long unchanged runs).

## Registration

```kotlin
val modules = rememberModules {
    module(NetworkMock(
        resourceLoader = { path -> Res.readBytes(path) }
    ))
}
```

`configPath` defaults to `"files/networkmocks/mocks.json"`. Pass a custom value if your config lives elsewhere.

## Related Modules

- [NetworkMock Core](networkmock-core.md): Shared config and state.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor client plugin.
- [NetworkMock Workflows](networkmock-workflows.md): Step-by-step integration guide.
