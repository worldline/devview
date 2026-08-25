# NetworkMock Ktor Plugin

The `devview-networkmock-ktor` module is a Ktor `HttpClientPlugin` that intercepts outgoing HTTP requests and returns in-memory mock responses. It delegates all config and state lookups to `devview-networkmock-core` and has no UI.

## Installation

### Zero-config (recommended)

When `NetworkMock` is registered via `rememberModules`, the plugin resolves its repositories automatically:

```kotlin
// Register the NetworkMock module in your app
val modules = rememberModules {
    module(NetworkMock(
        resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) },
        specPaths = listOf("files/networkmocks/specs/my-backend.json")
    ))
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
3. Calls `mockRepository.findMatchingMock(host, path, method, queryParameters)`.
4. If no match → sends the real request.
5. If matched, reads the operation's `OperationMockState`:
   - `Network` or `null` → sends the real request.
   - `Mock(statusCode, exampleName)` → loads that declared response variant and returns a synthetic response.
6. On any error (undeclared variant, missing file, exception) → falls back to the real network and logs the reason. **The plugin never throws.**

Mock responses are returned with HTTP/1.1 status, an empty header set, and the response body as the content.

## Platform Actuals

- **Android**: Use `HttpClient(OkHttp)` as the engine.
- **iOS**: Use `HttpClient(Darwin)` as the engine.

## Related Modules

- [NetworkMock](networkmock.md): UI layer and module entry point.
- [NetworkMock Core](networkmock-core.md): Config parsing, request matching, and state.
