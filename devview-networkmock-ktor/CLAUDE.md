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
4. If enabled, `mockRepository.findMatchingMock(host, path, method, queryParameters)` is called. Path matching supports `{param}` placeholders; matching considers every server declared across every configured spec — there is no environment axis.
5. If no match → real network.
6. If matched, `currentState.getOperationState(match.key)` is read:
   - `OperationMockState.Network` or `null` → real network.
   - `OperationMockState.Mock(statusCode, exampleName)` → load that declared response variant via `mockRepository.loadMockResponse(key, statusCode, exampleName)`.
7. On a successful load, `createMockHttpClientCall(...)` builds a `MockHttpClientCall` with `HttpResponseData` (HTTP/1.1, empty headers, `ByteReadChannel` body) and returns it — **no network call is made**.
8. On any failure (variant not declared in the spec, exception) → falls back to real network and logs; never throws.

## Non-obvious Patterns and Constraints

**Zero-config dependency on `NetworkMockInitializer`**: When `mockRepository`/`stateRepository` are left `null`, `NetworkMockConfig.resolvedMockRepository()` calls `NetworkMockInitializer.requireConfigRepository()`, which throws if the `NetworkMock` module was not registered via `rememberModules { }`. Always override both repos explicitly in tests.

**Response variant identity is `(statusCode, exampleName)`, not a file name**: what file backs a variant is an implementation detail of the spec's `externalValue` — moving the file doesn't change which variant a user has selected.

**`MockHttpClientCall` is public because of Ktor internals**: `HttpClientCall(client)` is the only available constructor; the class must be public to be instantiable from the plugin object. Its `rawContent` override is annotated `@InternalAPI` — if Ktor's internal API changes, this class is the first breakage point.

**`failOnNoDiscoveredTests = false`**: Set in `build.gradle.kts` so that the Gradle `test` task does not fail for iOS targets that have no test sources discovered in CI.

**Dokka aggregation**: The `dependencies { dokka(...) }` block in `build.gradle.kts` pulls documentation from both `devview-networkmock` (UI) and `devview-networkmock-core` (engine) into this module's published API docs.

## Test Structure

Tests live in `src/androidHostTest/` and run on the JVM (no device required). They use:
- Ktor's `MockEngine` to simulate "real network" responses.
- MockK to stub `MockStateRepository` (both `getState()` and `observeState()`).
- A real `MockConfigRepository` wired to an in-memory resource map from `KtorPluginTestData`, backed by an inline OpenAPI JSON spec fixture.
