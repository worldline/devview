# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-networkmock-core` is the shared engine for the network mocking feature. It owns OpenAPI 3.x spec parsing (JSON, and YAML on a best-effort basis), request matching, response variant discovery, and DataStore state persistence. It is a dependency of both `devview-networkmock` (UI) and `devview-networkmock-ktor` (Ktor plugin), which do not depend on each other — the module exists specifically to avoid that circular dependency.

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
| `MockConfigRepository` | Loads OpenAPI specs, discovers/loads response variants, matches requests to operations |
| `MockStateRepository` | Reads and writes `NetworkMockState` from/to DataStore |
| `NetworkMockInitializer` | Process-level singleton holding both repos; `@Composable fun initialize(...)` is called once by `devview-networkmock` |
| `NetworkMockDataStoreDelegate` | Top-level `val` holding the shared `DataStoreDelegate`; both UI and Ktor plugin reference this same instance |
| `MockConfiguration` / `ApiSpec` / `Operation` | `@Serializable` model hierarchy, one `ApiSpec` per parsed OpenAPI document |
| `OperationKey` | Value type `(specId, operationId)` with `.compositeKey` property (`"specId-operationId"`) used everywhere as DataStore key and map key |
| `MockMatch` | Returned by `findMatchingMock()`; carries `OperationKey` + resolved `Operation` — kept unrenamed, see naming note below |
| `OperationDescriptor` | Static snapshot of an operation + its discovered responses; used by the UI layer |
| `NetworkMockState` | Persisted state: `globalMockingEnabled`, `operationStates: Map<String, OperationMockState>`, `lastModified` |
| `OperationMockState` | Sealed interface: `Network` (pass-through) or `Mock(statusCode: Int, exampleName: String)` |

**Naming note**: `MockResponse` and `MockMatch` are deliberately *not* renamed to OpenAPI vocabulary — they model DevView's own runtime mocking behavior (a served response, a request-to-operation match), which OpenAPI has no concept of. Everything that models something the spec itself describes uses OpenAPI terms (`ApiSpec`, `Operation`, `OperationKey`).

## Mock Engine Architecture

### OpenAPI Spec Format

One spec file (JSON, or YAML best-effort) is one API group — place anywhere under `composeResources/files/networkmocks/`, e.g. `composeResources/files/networkmocks/specs/my-backend.json`:

```json
{
  "info": { "title": "My Backend" },
  "servers": [
    { "url": "https://staging.api.example.com" },
    { "url": "https://api.example.com" }
  ],
  "x-devview": { "delayMs": 200 },
  "paths": {
    "/v1/users/{userId}": {
      "get": {
        "operationId": "getUser",
        "responses": {
          "200": {
            "content": {
              "application/json": {
                "examples": {
                  "default": { "externalValue": "responses/getUser/getUser-200.json" }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

There is no environment axis and no manifest file. `servers[]` lists every base URL the group can be reached at; a group spanning multiple API versions is just multiple `paths` entries (distinct `operationId`s) in the same document.

### Request Matching (`MockConfigRepository.findMatchingMock`)

1. Compare the incoming request host — **case-insensitive** — against every hostname in each spec's `servers[]`.
2. On a host match, check that spec's operations for a path/method/query match; if none matches, fall through and try the next spec (two specs may legitimately share a host — first spec with an actual match wins).
3. Match path via `RequestMatcher.matchesPath` — splits by `/`, requires identical segment count; `{param}` segments match any value; non-param segments are **case-sensitive**.
4. Match HTTP method — **case-sensitive exact match** (use uppercase: `"GET"`, not `"get"`).
5. Match query parameters via `RequestMatcher.matchesQueryParams` against `Operation.queryParameters`, sourced at parse time from `parameters` entries with `in: query` and a literal `example` value.

**There is no stored active-server selection.** The matching server is determined purely from the request hostname at interception time.

### Response Variant Discovery

`discoverResponseFiles()` / `loadMockResponse()` read exactly the `(statusCode, exampleName)` pairs declared in the spec's `responses.<code>.content.*.examples` — **there is no probing** of status codes or file-name suffixes. `externalValue` resolves relative to the spec file's own location (or root-relative if it starts with `/`), loaded through the same `NetworkMockResourceLoader` used for the spec itself.

`$ref` (in `parameters`, `responses`, or `examples`) resolves one level deep, locally against `#/components/...` in the same document, or externally against another file's `components` via `./other.json#/components/...`.

There is no version-tagging support in 0.2.0 — `/v1/x` and `/v2/x` are simply two distinct operations with distinct `operationId`s; a display-only version tag is a separate, later feature.

### DataStore Schema (`MockStateRepository`)

| Key | Type | Meaning |
|---|---|---|
| `network_mock_global_enabled` | Boolean | Master toggle |
| `network_mock_last_modified` | Long | Epoch ms of last change |
| `network_mock_schema_version` | Int | Gates the one-shot pre-0.2.0 migration |
| `network_mock_operation_{compositeKey}` | String (JSON) | `OperationMockState` per operation |

`OperationMockState` is serialized as `{"type":"network"}` or `{"type":"mock","statusCode":200,"exampleName":"default"}` (discriminator field `type`).

Each operation is stored under its own key, so updating one operation never overwrites another.

**Pre-0.2.0 upgrade**: on first launch, every `network_mock_endpoint_*` entry from the old `EndpointKey`/file-name-based shape is wiped once (gated by `network_mock_schema_version`) — the key shape and the `Mock` payload shape both changed, so a translation wasn't attempted. `network_mock_global_enabled` and `network_mock_last_modified` are untouched.

**`registerOperations(List<OperationKey>)`** pre-populates the in-memory `operationKeys` registry after config load. Call this before the first write; otherwise `resetKnownOperationsToNetwork()` can only reset operations that were already written this session.

## Non-Obvious Patterns

**`NetworkMockInitializer.initialize()` is `@Composable`** even though it is a process-level singleton. It uses `remember` internally so that the repo objects are tied to the Composition. Subsequent calls are early-returned no-ops (`if (stateRepository != null) return`).

**`MockConfigRepository` caches** the parsed `MockConfiguration` in `cachedConfig` after the first successful load. Tests verify this with a recording resource loader that asserts each spec file is read exactly once.

**`MockStateRepository.observeState()`** rebuilds the full `NetworkMockState` on every DataStore emission by scanning all keys with the `network_mock_operation_` prefix. This means new operations written by another process/session are automatically picked up without requiring `registerOperations()` — but `registerOperations()` is still needed for the write-side helpers to know about untouched operations.

**YAML support is best-effort.** kaml's repository is archived (0.104.0 is the final release); every kaml-specific reference is quarantined inside `openapi/YamlSupport.kt` so dropping it later, if a future Kotlin/kotlinx-serialization upgrade breaks it, is a one-line removal plus deleting that file — not a broader refactor.

## The `openapi` Package Is a Pure Seam

`devview-networkmock-core/.../core/openapi/` contains the OpenAPI-specific parser (`OpenApiDocument`, `OpenApiParser`, `YamlSupport`) — all `internal`. No OpenAPI-shaped type may be imported outside this package; `MockConfigRepository` is the only consumer, and it converts everything into the public model (`ApiSpec`, `Operation`, `MockResponse`) before returning. This is what keeps a second, lower-ceremony config format cheap to add later without touching anything downstream.

## Testing

All tests live in `commonTest` and run on the JVM — no emulator needed. The test harness injects dependencies via constructor:

- `MockConfigRepository` takes `specPaths: List<String>` and a `resourceLoader: NetworkMockResourceLoader`; tests pass a `Map<String, String>`-backed loader as a virtual filesystem, with inline OpenAPI JSON strings as spec fixtures.
- `MockStateRepository` takes a `DataStore<Preferences>`; tests supply `FakePreferencesDataStore` (from `devview-test`) or `ThrowingPreferencesDataStore` (local fixture) for IOException recovery tests.
- `MockTestData` (internal fixture object) provides named builders for `ApiSpec`/`Operation`/`OperationMockState`/`NetworkMockState` to keep test bodies focused on behaviour.
