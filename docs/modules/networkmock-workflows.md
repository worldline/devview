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
4. After testing, select **Use actual network** in the operation sheet, or confirm the **Reset all to network** toolbar action to restore every operation to pass-through.

## Previewing and comparing responses

1. Open DevView → Network Mock and tap an operation.
2. Tap a response's **Details** button. The quick-choice sheet closes before the full-screen response opens; no mock is activated. The Responses tab's search can narrow by status code, name, example or media type.
3. Use **Compare with** to select another variant. The control is hidden when no other variant exists.
4. Inspect selectable, pretty-printed JSON or unchanged non-JSON text. Logical lines stay unwrapped; each pane scrolls horizontally and creates only visible vertical rows. Large comparisons use stacked previews instead of an unbounded diff table.
5. **Copy body** copies the original response body. **Use this response** saves and returns to the list; **In use** is disabled for an already-selected response. Back returns to the list without reopening the sheet.

## Finding mocked operations first

Expand the search field's filter panel and select **Mocked first** under Sort by. Mock, Failure and Sequence precede Network, retaining spec order within each group. Other choices are Default, Path (A-Z), Method and Tag. Sort is saved per API group with its search and filters; it does not change persisted mock settings or request matching.

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

Both operations appear in the same tab with their version visible in the wrapping path (no separate version badge). The tab's version filter can narrow on `v1`/`v2`, extracted from the `/v{n}/` path segment. The engine mocks whichever path the app actually calls — it does not rewrite or force a version (that is a deliberately separate, deferred feature).

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
2. Open the **Failures** tab and tap Timeout or Connection refused. Browsing the tab alone changes nothing.
3. After the save succeeds, the sheet closes and the state chip reflects the failure. Select **Use actual network** or a response to stop simulating it.

**Probabilistically** — a percentage of requests fail on their own, the rest behave normally:

```yaml
paths:
  /v1/users/{userId}:
    get:
      x-devview:
        failureRate: 0.1  # 10% of requests to this operation fail, independently, each time
```

This only rolls for requests that would otherwise be mocked — an operation left on `Network`
passthrough is never affected. The operation sheet's read-only failure-rate note describes
this field; edit the spec to change it. Explicit Timeout and Connection refused choices do
not roll this probability. See
[Simulating failures](networkmock-core.md#simulating-failures) for the exact exception each
failure kind throws.

## Simulating a staged/polling flow (sequential mocks)

For repeated calls whose response changes over time, such as order status or upload progress:

1. Open an operation and choose **Build sequence**. The sheet closes and the full-screen builder opens.
2. Tap response variants in serving order. Repeated responses create separate, stable steps.
3. Use labelled Move up, Move down and Remove controls to edit the order.
4. **Save sequence (n)** becomes available at two steps. Save persists the complete draft, resets its position to step 1, and returns to the list.
5. **Cancel** or Back returns to the list without applying edits.

A configured sequence shows **Sequence · next n/total** and the next response in the operation sheet. That response is not selected as a standalone radio choice. **Edit** seeds a draft from the saved steps; **Restart** resets the latest persisted sequence to step 1 without replacing its response list. A successful restart returns to the list. Missing draft variants freeze editing and saving until Reload resolves them; the draft stays intact.

The final step repeats instead of looping. Selecting Network, a single response or a failure replaces the sequence. Concurrent requests consume steps in successful DataStore commit order. A request retries against newer state if another request or UI action wins first, without rerolling its configured probability or applying its mock delay twice.

## Saving failures and interruptions

Selections and drafts stay visible while saving. Conflicting changes, back navigation and sheet dismissal remain disabled. A failed save offers **Retry** for the same frozen command or **Cancel** to abandon that failed command. Cancel does not undo a running transaction. Hiding the overlay or a host configuration change retains the session; removing the registered module ends it. Filtering a displayed module out returns to Home and discards its entry drafts without cancelling the registered consumer. Pending writes are not restored after process death.

If a selected mock resource is missing, unreadable or no longer declared, the Ktor request fails without sending the real request or advancing its sequence. Missing configuration and unmatched operations still use the network; this is not blanket network isolation.

## Resetting all mocks

Open the **Reset all to network** toolbar action and confirm **Reset**. One transaction resets persisted and pre-registered operations, including untouched operations and entries written by another repository. It clears selections and sequences, but leaves the global toggle unchanged. The same Retry/Cancel recovery applies if persistence fails.

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
