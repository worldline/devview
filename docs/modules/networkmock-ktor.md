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
   - `Failure(kind)` → throws immediately, simulating that failure kind (see below). No response is loaded.
   - `Mock(statusCode, exampleName)`:
     - If the operation declares `x-devview.failureRate` and the configured `Random` rolls below it → throws a simulated connection failure instead, same as `Failure(CONNECTION_REFUSED)`.
     - Otherwise, loads that declared response variant and returns a synthetic response.
6. On any error loading a declared mock (undeclared variant, missing file, exception) → falls back to the real network and logs the reason.

Simulated failures (deterministic `Failure` states and the probabilistic `failureRate` roll) are the one case where **the plugin does throw** — this is deliberate: it's mirroring a real network failure, not an internal error to recover from. See [Simulating failures](networkmock-core.md#simulating-failures) for which exception each failure kind throws.

Mock responses are returned with HTTP/1.1 status, `Content-Type` set from the response's declared media type (defaulting to `application/json`), any additional headers declared on `responses.<code>.headers`, and the response body as the content. See [Response headers and content type](networkmock-core.md#response-headers-and-content-type).

Each intercepted request logs exactly one line through [Kermit](https://github.com/touchlab/Kermit) (tag `DevViewNetworkMock`, `debug` level, `warn` for a failed mock load) — e.g. `GET /v1/users/42 -> MOCK 200/default` or `-> NETWORK (no operation match)`. No response body content is ever logged. See [Logging](networkmock-core.md#logging) for how to adjust verbosity or route these into `devview-consolelogger`.

## Platform Actuals

- **Android**: Use `HttpClient(OkHttp)` as the engine.
- **iOS**: Use `HttpClient(Darwin)` as the engine.

## Related Modules

- [NetworkMock](networkmock.md): UI layer and module entry point.
- [NetworkMock Core](networkmock-core.md): Config parsing, request matching, and state.
