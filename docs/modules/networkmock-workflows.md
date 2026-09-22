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

Both operations appear in the same tab, each labeled with a version chip (`v1`/`v2`, extracted from the `/v{n}/` path segment) that the tab's version filter can narrow on. The engine mocks whichever path the app actually calls — it does not rewrite or force a version (that is a deliberately separate, deferred feature).

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

## Simulating a network failure

**Deterministically** — every request to the operation fails the same way until you change it:

1. Open DevView → Network Mock → tap your operation.
2. Scroll past the response variants to "Simulate Failure" and tap Timeout or Connection Refused.
3. The operation's state chip reflects the selected failure. Tap "No mock" (or select a response) to stop simulating it.

**Probabilistically** — a percentage of requests fail on their own, the rest behave normally:

```yaml
paths:
  /v1/users/{userId}:
    get:
      x-devview:
        failureRate: 0.1  # 10% of requests to this operation fail, independently, each time
```

This only rolls for requests that would otherwise be mocked — an operation left on `Network`
passthrough is never affected. If the operation's picker page shows a configured failure rate,
that's this field — it's read-only in the UI; edit the spec to change it. See
[Simulating failures](networkmock-core.md#simulating-failures) for the exact exception each
failure kind throws.

## Simulating a staged/polling flow (sequential mocks)

For endpoints where the interesting behavior is the transition across repeated calls (order
status, upload progress, async job completion):

1. Open DevView → Network Mock → tap your operation.
2. In the "SEQUENCE" section, tap "Build a Sequence".
3. Tap responses above, in the order you want them served (e.g. `202 Pending`, then `200 Success`).
4. Tap "Save Sequence" (needs at least 2 steps). The operation now advances one step per request.

Once the sequence reaches its last step, every further request keeps serving that last step —
it does not loop back to the start. "Reset Position" (in the same section) restarts at step 1
without leaving the sequence; selecting "No mock" or a single response exits it entirely.

## Resetting all mocks

- **UI**: Open DevView → Network Mock → tap the restore icon in the top toolbar.
- **All mocks are reset to `Network` state**, including operations the user has never explicitly touched.

## Reloading a spec after editing it

Editing a spec file on disk (adding an operation, changing a response example) isn't picked up
automatically — `MockConfigRepository` caches the parsed spec after the first load.

- **UI**: Open DevView → Network Mock → tap the refresh icon in the top toolbar.
- The spec is re-read and re-parsed from scratch; operations added, removed, or renamed appear
  immediately. Per-operation mock selections already stored in DataStore are untouched.
- No app restart required.

## Related Modules

- [NetworkMock](networkmock.md): Overview and installation.
- [NetworkMock Core](networkmock-core.md): Spec format details, request matching.
- [NetworkMock UI](networkmock-ui.md): Screen descriptions.
- [NetworkMock Ktor](networkmock-ktor.md): Plugin installation.
