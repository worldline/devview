# NetworkMock Core

The `devview-networkmock-core` module is the shared engine for network mocking. It owns OpenAPI spec parsing, request matching, response variant discovery, and DataStore state persistence. Both `devview-networkmock` (UI) and `devview-networkmock-ktor` (Ktor plugin) depend on it — it exists specifically to let those two modules share state without depending on each other.

## OpenAPI Spec Format

Configuration is one OpenAPI 3.x document (JSON, or YAML on a best-effort basis) per API group — **one spec file = one group**, placed anywhere under `composeResources/files/networkmocks/`, e.g. `composeResources/files/networkmocks/specs/my-backend.json`:

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
        "summary": "Get User",
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
    },
    "/v2/users/{userId}": {
      "get": {
        "operationId": "getUserV2",
        "x-devview": { "delayMs": 500 },
        "responses": { "...": "..." }
      }
    }
  }
}
```

Key concepts:
- **`info.title`** → the spec's display name; slugified into its `id` (e.g. `"My Backend"` → `"my-backend"`).
- **`servers[].url`** → every base URL this group can be reached at. There is no environment axis — an app talks to exactly one base URL at a time, so a request matches this spec if its hostname matches *any* declared server.
- **No manifest, no per-environment overrides.** A group spanning multiple API versions (`/v1/...`, `/v2/...`) is simply multiple `paths` entries — and therefore multiple distinct `operationId`s — in the same document.
- **`responses.<code>.content.<mediaType>.examples.<name>`** → one entry per response variant this operation can mock; `externalValue` points at the response body file on disk. By convention the primary/original response for a status code is named `"default"`.
- **`parameters` with `in: query`** → a literal `example` value on a query parameter becomes a required match for that operation (e.g. `listUsers` only matches requests carrying `?type=user`).
- **`x-devview.delayMs`** → simulated response delay, at the document root (spec-wide default) and/or per operation (overrides the default). See [x-devview extension](#x-devview-extension) below.
- **`{param}` placeholders**: Path segments like `{userId}` match any value during request matching.

## Request Matching

`MockConfigRepository.findMatchingMock(host, path, method, queryParameters)` resolves a mock in three steps:

1. **Hostname match** — compares the request host (case-insensitive) against every hostname declared in the spec's `servers[]`. If two specs both declare a matching hostname, the first spec (in configuration order) that also has a matching operation wins.
2. **Path match** — splits path by `/`, compares segment by segment; `{param}` segments match any value; non-param segments are case-sensitive.
3. **Method match** — case-sensitive exact match. Use uppercase (`"GET"`, `"POST"`).

There is no stored active-server selection. The matching server is determined purely from the request hostname at interception time.

## Response Variant Discovery

Response bodies live wherever `externalValue` points them — the sample app uses `composeResources/files/networkmocks/responses/{specId}/{operationId}/{operationId}-{status}[-{suffix}].json`, but this is only a convention, not a requirement.

Discovery reads exactly the `(statusCode, exampleName)` pairs declared in the spec — **there is no probing** of status codes or file-name suffixes. If a variant isn't declared, it doesn't exist.

```kotlin
MockConfigRepository(
    specPaths = listOf("files/networkmocks/specs/my-backend.json"),
    resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) }
)
```

### `$ref` resolution

Parameters, responses, and examples may be declared via `$ref` instead of inline:
- **Local**: `"$ref": "#/components/parameters/UserId"` resolves against the same document's `components`.
- **External**: `"$ref": "./common.json#/components/responses/Error"` loads another file (relative to the spec's own location) via the same `NetworkMockResourceLoader`.

Refs resolve one level deep — a referenced component's own `$ref` (if any) is not followed further.

### x-devview extension

Vanilla OpenAPI has no field for response delay simulation, so it lives under the standard `x-`-prefixed [Specification Extensions](https://spec.openapis.org/oas/v3.1.0#specification-extensions) mechanism:

```yaml
x-devview:
  delayMs: 200        # document root — spec-wide default

paths:
  /users/{userId}:
    get:
      x-devview:
        delayMs: 500  # per-operation — overrides the document default
```

## DataStore Schema

State is persisted via `MockStateRepository`:

| Key | Type | Meaning |
|-----|------|---------|
| `network_mock_global_enabled` | Boolean | Master toggle |
| `network_mock_last_modified` | Long | Epoch ms of last change |
| `network_mock_schema_version` | Int | Gates the one-shot pre-0.2.0 migration below |
| `network_mock_operation_{compositeKey}` | String (JSON) | Per-operation state |

`OperationMockState` is serialized as `{"type":"network"}` (pass-through) or `{"type":"mock","statusCode":200,"exampleName":"default"}`.

**Upgrading from a pre-0.2.0 release**: the operation-state key shape changed (`{groupId}-{environmentId}-{endpointId}` → `{specId}-{operationId}`), and so did the `Mock` payload (a response file name → `(statusCode, exampleName)`). On first launch after upgrading, every `network_mock_endpoint_*` entry from the old shape is wiped once — this is disabled-by-default developer-tooling state, not user data, so previously-selected mocks are reset rather than translated. The global mocking toggle is unaffected.

## NetworkMockResourceLoader

_Added in v0.1.3._

`NetworkMockResourceLoader` is a `fun interface` that abstracts how mock resource bytes are loaded from a path. It exists to support DI-friendly architectures where the spec files live in a different Gradle module than the one constructing `NetworkMock`.

```kotlin
public fun interface NetworkMockResourceLoader {
    public suspend fun load(path: String): ByteArray
}
```

### Basic Usage

```kotlin
NetworkMock(
    resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) },
    specPaths = listOf("files/networkmocks/specs/my-backend.json")
)
```

### DI Usage (Koin example)

In the module that owns the mock resource files:
```kotlin
single<NetworkMockResourceLoader> {
    NetworkMockResourceLoader { Res.readBytes(it) }
}
```

In the presentation module where `NetworkMock` is constructed:
```kotlin
val loader = koinInject<NetworkMockResourceLoader>()
val modules = rememberModules {
    module(NetworkMock(resourceLoader = loader, specPaths = listOf("files/networkmocks/specs/my-backend.json")))
}
```

### Why This Exists

Before v0.1.3, `NetworkMock` accepted a raw `suspend (String) -> ByteArray` lambda. This worked but was impossible to register as a typed binding in DI frameworks (Koin, Hilt) across Gradle module boundaries. The named `NetworkMockResourceLoader` type solves this.

## Related Modules

- [NetworkMock](networkmock.md): UI layer and module entry point.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor client plugin.
