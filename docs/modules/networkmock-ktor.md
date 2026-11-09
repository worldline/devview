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

1. Reads a strict `getStateSnapshot()`. Disabled global mocking sends the real request.
2. Matches the host, path, method, query and applicable request body against the loaded configuration. No operation match or unavailable configuration sends the real request.
3. Reads fresh state after matching. Network or an unset operation sends the real request; a forced Failure throws the corresponding simulated transport exception.
4. For Mock or Sequence, rolls the operation's configured probability at most once per request. A hit throws a simulated connection refusal. Otherwise it loads the exact selected variant and applies the configured delay at most once.
5. Revalidates the state before returning a prepared response. Sequence validation and non-terminal advancement share one atomic DataStore transaction. If a newer write wins, the prepared body is discarded and the request retries against fresh state. New Network, forced failure or replacement mock settings take precedence; retries do not add probability rolls or mock delays.

Concurrent sequence requests consume steps in successful commit order. The final step repeats but still requires validation. A failed commit returns neither the mock nor a real-network fallback, and a reset cannot be overwritten by a stale prepared response.

**Selected mocks fail without network fallback.** If loading a superseded selection fails or returns no response, the request retries the latest operation state before reporting that stale error. An undeclared variant, missing resource or resource-loading exception for the still-current selection fails the request and is logged; it does not send a real GET, POST or other request, and does not consume the sequence. State-read errors also propagate. Coroutine cancellation propagates without being treated as a mock-load fallback. Missing configuration and unmatched operations retain pass-through behaviour, so this is not a general network-isolation guarantee.

Simulated failures deliberately throw the same categories of exception as a real engine; see [Simulating failures](networkmock-core.md#simulating-failures).

Mock responses use HTTP/1.1, the resolved content type, declared response headers and body bytes. Bodyless variants have no media-type-derived header. See [Response headers and content type](networkmock-core.md#response-headers-and-content-type).

The plugin logs routing, retries and errors through Kermit (tag `DevViewNetworkMock`) without logging response bodies. Retries can produce more than one log line. See [Logging](networkmock-core.md#logging) to adjust verbosity or capture logs in Console.

## Platform Actuals

- **Android**: Use `HttpClient(OkHttp)` as the engine.
- **iOS**: Use `HttpClient(Darwin)` as the engine.

## Related Modules

- [NetworkMock](networkmock.md): UI layer and module entry point.
- [NetworkMock Core](networkmock-core.md): Config parsing, request matching, and state.
