# NetworkMock UI

The `devview-networkmock` module provides the registered Compose UI for network mocking, backed by `devview-networkmock-core`. The operation list owns a quick-choice sheet. Response inspection and sequence editing use separate full-screen destinations owned by the module, not public screen composables.

## Screens

### Main screen — operation list

The main screen shows a global mock toggle at the top, followed by a scrollable tab row with one tab per OpenAPI spec (e.g. "My Backend"). Each tab lists every operation declared in that spec with its current mock state — there is no environment axis, so a spec spanning multiple API versions shows all of its operations side by side in one tab.

- **Global toggle**: enables or disables all mocking globally. When off, all requests go to the real network regardless of per-endpoint settings. Shows a count of mocked operations (e.g. "3 of 47 mocked") computed across every spec, not just the visible tab.
- **Search & filter bar**: the shared pinned search field expands a bounded, scrollable panel with Sort by, State, Version, Method, and Tag groups. The badge counts active non-search selections, including a non-default sort. Filters combine with AND; multi-select chips within one group combine with OR. Search and every selection are independent per API group and survive navigation and screen recreation through navigation-owned saved state, without changing persisted mock settings. **Clear filters** resets that group’s selections and sort but keeps its search; an empty result offers **Clear search and filters** to reset both.
- **Search field**: filters the visible operations in the current tab by name, path, or operationId, live as you type.
- **Mock-state filter**: per-group "Mocked"/"Network" chips. Mocked includes response mocks, failures, and sequences; no selection shows both.
- **Version filter**: "All" or one version per API group (see [Version Tags](networkmock-core.md#version-tags)). Hidden when there are no versioned operations or retained version selection. Selections absent after a spec reload remain visible so they can be cleared.
- **Method filter**: a per-tab row of chips, one per distinct `Operation.method` present among that tab's operations, ordered `GET`/`POST`/`PUT`/`PATCH`/`DELETE`/`HEAD`/`OPTIONS`. Multi-select — no chip selected shows every method; selecting one or more narrows the list to operations using any of the selected methods.
- **Tag filter**: a per-tab row of chips, one per distinct `Operation.tag` present among that tab's operations (see [Tags](networkmock-core.md#tags)), alphabetically ordered. Multi-select, same semantics as the method filter. Hidden when a spec has no tagged operations and no retained tag selection; selections absent after a reload remain available to clear.
- **Endpoint rows**: a leading colour rail (the state chip's colour, transparent when not mocked) followed by the endpoint name, a colour-coded HTTP method badge, the path (wraps instead of truncating — never cut off), and the current state (quiet Network text / a semantic status chip with a bare code, e.g. "404"). There is no separate version badge — `Operation.version` is parsed from the path shown right next to it, so it would only repeat what's already visible. Tap a row to open its operation sheet.
- **Reset all to network**: confirmed toolbar action that atomically resets persisted and pre-registered operations, including untouched endpoints. The global toggle is unchanged.
- **Reload specs**: toolbar action that re-reads and re-parses every configured OpenAPI spec from disk, picking up edits to a spec file without restarting the app. Operations added, removed, or renamed in the spec appear immediately; per-operation mock selections are untouched. See [Caching & Reload](networkmock-core.md#caching-reload).
- **Sort**: selected chips inside the filter panel, not a separate toolbar menu. Choose Default (spec document order), Path (A-Z), Method (`HttpMethod.DefaultMethods` order), Tag (first declared tag, untagged first), or Mocked first (Mock, Failure, and Sequence before Network, stable within each group). Sort is saved per API group alongside its filters.

### Operation sheet

Tapping an endpoint opens a wrap-content sheet. Short operations do not reserve a fixed screen fraction; longer choices scroll within the available height. The header shows the operation name, method, wrapping path and current persisted state.

- **Single-choice picker**: Network, response variants grouped by status family, and Simulate failure share one radio-selection group. Network uses the actual backend. The leading radio indicates the current choice; media types distinguish otherwise identical variants. Generated bodies have a quiet "Generated" label.
- **Response details**: a labelled trailing chevron opens that variant without activating it. The sheet finishes closing before navigation begins.
- **Failures**: Timeout and Connection refused sit below the response groups. The configured failure-rate footnote is read-only; edit the spec to change it.
- **Sequences**: Build sequence or Edit sequence opens the builder. For an active sequence, Reset position atomically resets the latest persisted sequence to step 1, preserving its response list. A successful reset returns to the list.

Selecting Network, a response or a failure saves the choice before closing the sheet. There are no Responses/Sequence/Failure tabs or nested preview pages.

### Response detail

The full-screen `NetworkMockDestination.ResponseDetail(operation, statusCode, mediaType, exampleName)` identifies one exact response variant. **Compare with** appears only when another variant exists. Valid JSON is pretty-printed for inspection; malformed JSON and non-JSON bodies remain unchanged. Text is selectable and **Copy body** copies the original body, not its display formatting.

Comparisons use an inline or side-by-side LCS diff, collapsing long unchanged runs. Logical lines stay unwrapped, with horizontal scrolling per pane and lazy vertical rows. Comparisons exceeding 1,000,000 LCS cells use stacked previews to bound memory. Light and dark diff colours follow the host surface luminance.

**Use this response** saves the variant and returns to the list; it becomes disabled **In use** when already selected. Back also returns to the list, without reopening the sheet. Inspection and copying do not change the served body or selection. A removed or unreadable variant shows an error with Reload instead of silently displaying another response.

### Sequence builder

The full-screen `NetworkMockDestination.SequenceBuilder(operation)` owns an ordered draft. Pick at least two responses; repeated variants are allowed and the last step repeats. Existing sequences seed the draft once. Each occurrence has a stable identity, so Move up, Move down and Remove act on the intended step even when variants repeat.

**Save sequence (n)** is disabled below two steps, with an Add N more steps helper. Saving resets the position to step 1 and returns to the list. Cancel or Back discards unapplied edits and returns to the list. If a draft references a variant no longer available, editing and saving freeze until Reload resolves it; the draft is not silently truncated.

### Saving and session lifetime

The registered module has one ordered mutation consumer shared by its list, sheet and detail entries. A pending save disables conflicting writes, navigation, dismissal and reload. A failed save retains its exact selection or draft and exposes **Retry** and **Cancel**. Retry repeats that command before later queued writes; Cancel abandons a failed command, not an already-running transaction. The screen closes only after its originating command succeeds, once.

Temporarily hiding the overlay and recreating the host for a configuration change retain pending writes, errors and entry drafts. Filtering a module out of `DevView.modules` returns to Home and discards its navigation entries and drafts, but does not unregister its mutation consumer. Removing its `rememberModules` registration ends that session. Process-death recovery of pending writes is not guaranteed.

List search, filters and sort remain client-side saved UI state, separate from persisted mock settings. Request coordination and failure behaviour are described in [NetworkMock Core](networkmock-core.md#state-coordination) and [NetworkMock Ktor](networkmock-ktor.md#how-interception-works).

## Theming

Status-family colors (2xx green, 4xx/5xx red, etc.) are a fixed, hand-tuned palette per
theme, not derived from `MaterialTheme.colorScheme` — see the
[Theming guide](../guides/theming.md#network-mock-status-colors) for how to provide and
override them.

## Registration

```kotlin
val modules = rememberModules {
    module(NetworkMock(
        resourceLoader = NetworkMockResourceLoader { path -> Res.readBytes(path) },
        specPaths = listOf("files/networkmocks/specs/my-backend.json")
    ))
}
```

`specPaths` lists every OpenAPI spec file to load — one per API group. There is no default; pass every spec your app should mock.

## Related Modules

- [NetworkMock Core](networkmock-core.md): Shared config and state.
- [NetworkMock Ktor](networkmock-ktor.md): Ktor client plugin.
- [NetworkMock Workflows](networkmock-workflows.md): Step-by-step integration guide.
