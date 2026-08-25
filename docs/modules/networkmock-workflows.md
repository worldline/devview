# NetworkMock Workflows

Step-by-step guides for common network mocking tasks.

## Adding a new mock operation

### 1. Add the operation to a spec file

```json
{
  "info": { "title": "My Backend" },
  "servers": [{ "url": "https://staging.api.example.com" }],
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
                  "default": { "externalValue": "responses/my-backend/getUser/getUser-200.json" }
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

### 2. Create response files

Place response files wherever `externalValue` points them — a `{specId}/{operationId}/` layout keeps things organized:

```
responses/my-backend/getUser/getUser-200.json
responses/my-backend/getUser/getUser-200-simple.json
responses/my-backend/getUser/getUser-404.json
responses/my-backend/getUser/getUser-500.json
```

Each file must be referenced by an `examples.<name>.externalValue` entry under the matching status code — there is no filename convention the parser relies on, since discovery reads exactly what the spec declares.

### 3. Launch the app

Open DevView → Network Mock. Your new operation appears in the list under its spec's tab.

## Testing an error scenario

1. Open DevView → Network Mock → tap your operation.
2. Tap a 4xx or 5xx response variant to activate it.
3. The state chip turns red/orange. The Ktor plugin now returns that response for matching requests.
4. After testing, tap "No mock" or use the "Reset to Network" toolbar action to restore pass-through.

## Naming response variants

Multiple examples can be declared for the same status code — useful for a "simple" vs. "detailed" error body, or an empty vs. populated list response:

```json
"404": {
  "content": {
    "application/json": {
      "examples": {
        "default":  { "externalValue": "responses/getUser/getUser-404.json" },
        "detailed": { "externalValue": "responses/getUser/getUser-404-detailed.json" }
      }
    }
  }
}
```

By convention, the primary/original response for a status code is named `"default"` — any other name shows up as a suffix in the UI (e.g. `"detailed"` → "Not Found - Detailed (404)").

## Serving different API versions from one spec

There is no environment axis, so "staging returns v1, production returns v2" doesn't apply — instead, declare both versions as distinct operations in the same spec, and let the app's actual request determine which one gets matched:

```json
{
  "info": { "title": "My Backend" },
  "servers": [
    { "url": "https://staging.api.example.com" },
    { "url": "https://api.example.com" }
  ],
  "paths": {
    "/v1/users/{userId}": {
      "get": { "operationId": "getUser", "responses": { "...": "..." } }
    },
    "/v2/users/{userId}": {
      "get": { "operationId": "getUserV2", "responses": { "...": "..." } }
    }
  }
}
```

Both operations appear in the same tab. The engine mocks whichever path the app actually calls — it does not rewrite or force a version (that is a deliberately separate, deferred feature).

## Simulating response delay

Set `x-devview.delayMs` at the document root for a spec-wide default, and/or per operation to override it:

```yaml
x-devview:
  delayMs: 200

paths:
  /v1/users/{userId}:
    get:
      x-devview:
        delayMs: 500  # overrides the 200ms default for this operation only
```

## Resetting all mocks

- **UI**: Open DevView → Network Mock → tap the restore icon in the top toolbar.
- **All mocks are reset to `Network` state**, including operations the user has never explicitly touched.

## Related Modules

- [NetworkMock](networkmock.md): Overview and installation.
- [NetworkMock Core](networkmock-core.md): Spec format details, request matching.
- [NetworkMock UI](networkmock-ui.md): Screen descriptions.
- [NetworkMock Ktor](networkmock-ktor.md): Plugin installation.
