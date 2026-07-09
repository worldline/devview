# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-networkmock-core` is the shared engine for the network mocking feature. It owns JSON config parsing, request matching, response file discovery, and DataStore state persistence. It is a dependency of both `devview-networkmock` (UI) and `devview-networkmock-ktor` (Ktor plugin), which do not depend on each other — the module exists specifically to avoid that circular dependency.

## Commands

Run tests for this module only (no device required):

```shell
.\gradlew.bat :devview-networkmock-core:testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
```

Single test class:

```shell
.\gradlew.bat :devview-networkmock-core:testAndroidHostTest --tests "com.worldline.devview.networkmock.core.repository.MockConfigRepositoryTest" -Pandroidx.baselineprofile.skipgeneration
```

## Public API Surface

| Class / Object | Role |
|---|---|
| `MockConfigRepository` | Loads `mocks.json`, discovers/loads response files, matches requests to endpoints |
| `MockStateRepository` | Reads and writes `NetworkMockState` from/to DataStore |
| `NetworkMockInitializer` | Process-level singleton holding both repos; `@Composable fun initialize(...)` is called once by `devview-networkmock` |
| `NetworkMockDataStoreDelegate` | Top-level `val` holding the shared `DataStoreDelegate`; both UI and Ktor plugin reference this same instance |
| `MockConfiguration` / `ApiGroupConfig` / `EnvironmentConfig` / `EndpointConfig` / `EndpointOverride` | `@Serializable` model hierarchy parsed from `mocks.json` |
| `EndpointKey` | Value type `(groupId, environmentId, endpointId)` with `.compositeKey` property (`"groupId-environmentId-endpointId"`) used everywhere as DataStore key and map key |
| `MockMatch` | Returned by `findMatchingMock()`; carries `EndpointKey` + resolved `EndpointConfig` |
| `EndpointDescriptor` | Static snapshot of an endpoint + its discovered responses; used by the UI layer |
| `NetworkMockState` | Persisted state: `globalMockingEnabled`, `endpointStates: Map<String, EndpointMockState>`, `lastModified` |
| `EndpointMockState` | Sealed interface: `Network` (pass-through) or `Mock(responseFile: String)` |
| `ApiGroupConfig.effectiveEndpoints(environment)` | Extension function — the single source of truth for endpoint resolution (shared + overrides + additionalEndpoints) |

## Mock Engine Architecture

### JSON Config Format (`mocks.json`)

Place at `composeResources/files/networkmocks/mocks.json`:

```json
{
  "apiGroups": [
    {
      "id": "my-backend",
      "name": "My Backend",
      "endpoints": [
        { "id": "getUser", "name": "Get User", "path": "/v1/users/{userId}", "method": "GET" }
      ],
      "environments": [
        { "id": "staging", "name": "Staging", "url": "https://staging.api.example.com" },
        {
          "id": "production", "name": "Production", "url": "https://api.example.com",
          "endpointOverrides": [{ "id": "getUser", "path": "/v2/users/{userId}" }],
          "additionalEndpoints": [{ "id": "getLegacy", "name": "Legacy", "path": "/users", "method": "GET" }]
        }
      ]
    }
  ]
}
```

### Request Matching (`MockConfigRepository.findMatchingMock`)

1. Extract hostname from each `EnvironmentConfig.url` (strips scheme, port, path)
2. Compare against incoming request host — **case-insensitive**
3. On hostname match, call `ApiGroupConfig.effectiveEndpoints(environment)` to get the resolved endpoint list
4. Match path via `RequestMatcher.matchesPath` — splits by `/`, requires identical segment count; `{param}` segments match any value; non-param segments are **case-sensitive**
5. Match HTTP method — **case-sensitive exact match** (use uppercase: `"GET"`, not `"get"`)

**There is no stored active-environment selection.** The environment is determined purely from the request hostname at interception time, allowing simultaneous different-environment use across groups.

### Response File Discovery

Files live under `composeResources/files/networkmocks/responses/`:

```
responses/{groupId}/{environmentId}/{endpointId}/{endpointId}-{status}[-{suffix}].json  ← highest priority
responses/{groupId}/{endpointId}/{endpointId}-{status}[-{suffix}].json                  ← shared fallback
```

`discoverResponseFiles()` probes `DEFAULT_STATUS_CODES` × `["", "-simple", "-detailed", "-error", "-success"]`. The status code is extracted by `String.parseStatusCode()` (internal extension), which matches the **last** `-{3 digits}` segment via regex — robust to hyphenated endpoint IDs like `get-user`.

`MockConfigRepository` accepts a custom `statusCodesToDiscover` list; pass it if your API uses codes outside the defaults (200, 201, 202, 204, 400, 401, 403, 404, 409, 422, 429, 500, 502, 503, 504).

### DataStore Schema (`MockStateRepository`)

| Key | Type | Meaning |
|---|---|---|
| `network_mock_global_enabled` | Boolean | Master toggle |
| `network_mock_last_modified` | Long | Epoch ms of last change |
| `network_mock_endpoint_{compositeKey}` | String (JSON) | `EndpointMockState` per endpoint |

`EndpointMockState` is serialized as `{"type":"network"}` or `{"type":"mock","responseFile":"getUser-200.json"}` (discriminator field `type`).

Each endpoint is stored under its own key, so updating one endpoint never overwrites another.

**`registerEndpoints(List<EndpointKey>)`** pre-populates the in-memory `endpointKeys` registry after config load. Call this before the first write; otherwise `resetKnownEndpointsToNetwork()` can only reset endpoints that were already written this session.

## Non-Obvious Patterns

**`NetworkMockInitializer.initialize()` is `@Composable`** even though it is a process-level singleton. It uses `remember` internally so that the repo objects are tied to the Composition. Subsequent calls are early-returned no-ops (`if (stateRepository != null) return`).

**`MockConfigRepository` caches** the parsed `MockConfiguration` in `cachedConfig` after the first successful load. Tests verify this with a recording resource loader that asserts the file is read exactly once.

**`MockStateRepository.observeState()`** rebuilds the full `NetworkMockState` on every DataStore emission by scanning all keys with the `network_mock_endpoint_` prefix. This means new endpoints written by another process/session are automatically picked up without requiring `registerEndpoints()` — but `registerEndpoints()` is still needed for the write-side helpers to know about untouched endpoints.

## Testing

All tests live in `commonTest` and run on the JVM — no emulator needed. The test harness injects dependencies via constructor:

- `MockConfigRepository` takes a `resourceLoader: suspend (String) -> ByteArray` lambda; tests pass a `Map<String, String>` as a virtual filesystem.
- `MockStateRepository` takes a `DataStore<Preferences>`; tests supply `FakePreferencesDataStore` (from `devview-test`) or `ThrowingPreferencesDataStore` (local fixture) for IOException recovery tests.
- `MockTestData` (internal fixture object) provides named builders for all model types to keep test bodies focused on behaviour.
