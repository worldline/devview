# NetworkMock Ktor Plugin

The `devview-networkmock-ktor` module is a Ktor `HttpClientPlugin` that intercepts outgoing HTTP requests and returns in-memory mock responses. It delegates all config and state lookups to `devview-networkmock-core` and has no UI.

## Installation

### Zero-config (recommended)

When `NetworkMock` is registered via `rememberModules`, the plugin resolves its repositories automatically:

```kotlin
// Register the NetworkMock module in your app
val modules = rememberModules {
    module(NetworkMock(resourceLoader = { path -> Res.readBytes(path) }))
}

// Install the plugin in your Ktor client — no configuration needed
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin)
}
```

`rememberModules { }` must be called (and composed) before any request reaches the network layer.

### Explicit repository injection (for tests or advanced DI)

```kotlin
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin) {
        mockRepository = myMockConfigRepository
        stateRepository = myMockStateRepository
    }
}
```

## How Interception Works

For every outgoing request, the plugin:

1. Calls `stateRepository.getState()` to read the current mock state.
2. If `globalMockingEnabled` is `false` → sends the real request.
3. Calls `mockRepository.findMatchingMock(host, path, method)`.
4. If no match → sends the real request.
5. If matched, reads the endpoint's `EndpointMockState`:
   - `Network` or `null` → sends the real request.
   - `Mock(responseFile)` → loads the response file and returns a synthetic response.
6. On any error (missing file, malformed name, exception) → falls back to the real network and logs the reason. **The plugin never throws.**

Mock responses are returned with HTTP/1.1 status, an empty header set, and the file contents as the body.

## Platform Actuals

- **Android**: Use `HttpClient(OkHttp)` as the engine.
- **iOS**: Use `HttpClient(Darwin)` as the engine.

## Related Modules

- [NetworkMock](networkmock.md): UI layer and module entry point.
- [NetworkMock Core](networkmock-core.md): Config parsing, request matching, and state.
