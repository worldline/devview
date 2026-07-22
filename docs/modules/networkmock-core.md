# NetworkMock Core

The `devview-networkmock-core` module is the shared engine for network mocking. It owns JSON config parsing, request matching, response file discovery, and DataStore state persistence. Both `devview-networkmock` (UI) and `devview-networkmock-ktor` (Ktor plugin) depend on it — it exists specifically to let those two modules share state without depending on each other.

## mocks.json Format

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
        {
          "id": "staging",
          "name": "Staging",
          "url": "https://staging.api.example.com"
        },
        {
          "id": "production",
          "name": "Production",
          "url": "https://api.example.com",
          "endpointOverrides": [
            { "id": "getUser", "path": "/v2/users/{userId}" }
          ],
          "additionalEndpoints": [
            { "id": "getLegacy", "name": "Legacy", "path": "/users", "method": "GET" }
          ]
        }
      ]
    }
  ]
}
```

Key concepts:
- **`apiGroups`**: Logical backends, each with shared endpoints and multiple environments.
- **`endpointOverrides`**: Per-environment path overrides for specific endpoint IDs.
- **`additionalEndpoints`**: Endpoints that only exist in a specific environment.
- **`{param}` placeholders**: Path segments like `{userId}` match any value during request matching.

## Request Matching

`MockConfigRepository.findMatchingMock(host, path, method)` resolves a mock in three steps:

1. **Hostname match** — extracts the hostname from each `EnvironmentConfig.url` (strips scheme, port, path), compares case-insensitively against the incoming request host.
2. **Path match** — splits path by `/`, compares segment by segment; `{param}` segments match any value; non-param segments are case-sensitive.
3. **Method match** — case-sensitive exact match. Use uppercase (`"GET"`, `"POST"`).

There is no stored active-environment selection. The environment is determined purely from the request hostname at interception time, allowing simultaneous requests to multiple environments across groups.

## Response File Discovery

Response files live under `composeResources/files/networkmocks/responses/`:

```
responses/{groupId}/{environmentId}/{endpointId}/{endpointId}-{status}[-{suffix}].json  ← highest priority
responses/{groupId}/{endpointId}/{endpointId}-{status}[-{suffix}].json                  ← shared fallback
```

The core module probes the following status codes by default: `200, 201, 202, 204, 400, 401, 403, 404, 409, 422, 429, 500, 502, 503, 504`.

Default suffixes: `""`, `"-simple"`, `"-detailed"`, `"-error"`, `"-success"`.

If your API uses status codes outside the defaults, pass a custom list:

```kotlin
MockConfigRepository(
    configPath = "files/networkmocks/mocks.json",
    resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) },
    statusCodesToDiscover = listOf(200, 400, 503, 418)
)
```

## DataStore Schema

State is persisted via `MockStateRepository`:

| Key | Type | Meaning |
|-----|------|---------|
| `network_mock_global_enabled` | Boolean | Master toggle |
| `network_mock_last_modified` | Long | Epoch ms of last change |
| `network_mock_endpoint_{compositeKey}` | String (JSON) | Per-endpoint state |

`EndpointMockState` is serialized as `{"type":"network"}` (pass-through) or `{"type":"mock","responseFile":"getUser-200.json"}`.

## NetworkMockResourceLoader

_Added in v0.1.3._

`NetworkMockResourceLoader` is a `fun interface` that abstracts how mock resource bytes are loaded from a path. It exists to support DI-friendly architectures where the mock JSON files live in a different Gradle module than the one constructing `NetworkMock`.

```kotlin
public fun interface NetworkMockResourceLoader {
    public suspend fun load(path: String): ByteArray
}
```

### Basic Usage

```kotlin
NetworkMock(
    resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) }
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
    module(NetworkMock(resourceLoader = loader))
}
```

### Why This Exists

Before v0.1.3, `NetworkMock` accepted a raw `suspend (String) -> ByteArray` lambda. This worked but was impossible to register as a typed binding in DI frameworks (Koin, Hilt) across Gradle module boundaries. The named `NetworkMockResourceLoader` type solves this.

## Related Modules

- [NetworkMock](networkmock.md): UI layer and module entry point.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor client plugin.
