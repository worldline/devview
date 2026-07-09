# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-networkmock-ktor` is a Ktor `HttpClientPlugin` that intercepts outgoing HTTP requests and returns in-memory mock responses, delegating all config and state lookups to `devview-networkmock-core`. It has no UI; it is the client-side network layer only.

## Public API Surface

```kotlin
// Install with zero config (repositories resolved from NetworkMockInitializer)
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin)
}

// Install with explicit repositories (for tests or advanced DI scenarios)
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin) {
        mockRepository = myMockConfigRepository   // MockConfigRepository
        stateRepository = myMockStateRepository   // MockStateRepository
    }
}
```

- `NetworkMockPlugin` — the `HttpClientPlugin<NetworkMockConfig, NetworkMockPluginConfig>` singleton (`NetworkMockPlugin.kt`)
- `NetworkMockConfig` — DSL receiver; exposes `mockRepository` and `stateRepository` as nullable vars (`NetworkMockConfig.kt`)
- `MockHttpClientCall` — public subclass of `HttpClientCall` that wraps synthetic request/response data without touching the network (`NetworkMockPlugin.kt`)

## Interception Flow

The plugin hooks into Ktor's `HttpSend` phase during `install`:

1. Every request is intercepted before it reaches the engine.
2. `stateRepository.getState()` is called (suspend; reads DataStore via `devview-networkmock-core`).
3. If `globalMockingEnabled` is `false` → `execute(requestBuilder)` (real network, no mock lookup).
4. If enabled, `mockRepository.findMatchingMock(host, path, method)` is called. Path matching supports `{param}` placeholders.
5. If no match → real network.
6. If matched, `currentState.getEndpointState(match.key)` is read:
   - `EndpointMockState.Network` or `null` → real network.
   - `EndpointMockState.Mock(responseFile)` → load the response file via `mockRepository.loadMockResponse(key, fileName)`.
7. On a successful load, `createMockHttpClientCall(...)` builds a `MockHttpClientCall` with `HttpResponseData` (HTTP/1.1, empty headers, `ByteReadChannel` body) and returns it — **no network call is made**.
8. On any failure (null response, exception, malformed file name) → falls back to real network and logs; never throws.

## Non-obvious Patterns and Constraints

**Zero-config dependency on `NetworkMockInitializer`**: When `mockRepository`/`stateRepository` are left `null`, `NetworkMockConfig.resolvedMockRepository()` calls `NetworkMockInitializer.requireConfigRepository()`, which throws if the `NetworkMock` module was not registered via `rememberModules { }`. Always override both repos explicitly in tests.

**Response file naming encodes the HTTP status code**: The file name must follow the pattern `{endpointId}-{statusCode}.json` (e.g. `getUser-200.json`). Files whose name cannot be parsed to extract a status code are treated as missing and fall back to the real network — this is enforced inside `devview-networkmock-core`'s `MockResponse.fromFile`.

**`MockHttpClientCall` is public because of Ktor internals**: `HttpClientCall(client)` is the only available constructor; the class must be public to be instantiable from the plugin object. Its `rawContent` override is annotated `@InternalAPI` — if Ktor's internal API changes, this class is the first breakage point.

**`failOnNoDiscoveredTests = false`**: Set in `build.gradle.kts` so that the Gradle `test` task does not fail for iOS targets that have no test sources discovered in CI.

**Dokka aggregation**: The `dependencies { dokka(...) }` block in `build.gradle.kts` pulls documentation from both `devview-networkmock` (UI) and `devview-networkmock-core` (engine) into this module's published API docs.

## Test Structure

Tests live in `src/androidHostTest/` and run on the JVM (no device required). They use:
- Ktor's `MockEngine` to simulate "real network" responses.
- MockK to stub `MockStateRepository` (both `getState()` and `observeState()`).
- A real `MockConfigRepository` wired to an in-memory resource map from `KtorPluginTestData`.
