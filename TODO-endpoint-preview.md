# NetworkMock — Endpoint Detail Screen & Mock Preview

Migration of `NetworkMockEndpointBottomSheet` to a proper full-screen navigation
destination, enabling a `ModalBottomSheet` for previewing individual mock response
content without any sheet-nesting concerns.

---

## Current State (as of 2026-03-13)

| File | Status |
|---|---|
| `NetworkMock.kt` | ✅ `Endpoint` destination declared, serializer registered, `entryDestination` set, `entry<Endpoint>` fully wired with inline VM factory |
| `NetworkMockScreen.kt` | Old bottom-sheet block **commented out**; `selectedDescriptor` / `selectedEndpointState` observations and related dead params still present — pending Step 6 |
| `NetworkMockEndpointScreen.kt` | ✅ Rewritten as a proper full screen with endpoint header, VM-driven state, and `previewingResponse` stub |
| `NetworkMockViewModel.kt` | Selection state (`selectedEndpointKey`, `selectedEndpointDescriptor`, `selectedEndpointState`, `selectEndpoint()`, `clearSelectedEndpoint()`) still present — pending Step 7 |
| `NetworkMockEndpointViewModel.kt` | ✅ Created |
| `MockItem.kt` | ✅ `onLongClick`, `isInPreviewMode`, `combinedClickable`, vertical-axis flip animation implemented |
| `NetworkMockEndpointPreviewBottomSheet.kt` | ✅ Complete — Single and Compare modes implemented, smart diff wired | 3c |
| `components/MockResponseDiffContent.kt` | ✅ Created — `DiffLine`, LCS algorithm, `InlineDiffContent`, `SplitDiffContent` | 3c |

---

## Implementation Steps

### Step 1 — `NetworkMockEndpointViewModel` ✅ Done

Created `viewmodel/NetworkMockEndpointViewModel.kt` with:
- `uiState: StateFlow<NetworkMockEndpointUiState>` (`Loading` / `Error` / `Content`)
- `setMockState(responseFileName: String?)` delegating to `stateRepository`
- `EndpointLoadingState` private sealed interface (renamed from `LoadingState` to avoid
  package-level redeclaration conflict with `NetworkMockViewModel.kt`)

**Note:** `EndpointDescriptor` was subsequently made `@Serializable` (along with `MockResponse`
and `EndpointConfig`). `NetworkMockDestination.Endpoint` still carries `EndpointKey` rather than
`EndpointDescriptor` — embedding the full descriptor (including all `MockResponse.content` bodies)
in a `NavKey` would bloat serialised navigation state proportionally to mock file count and size.
`EndpointKey` is the correct minimal identifier; the VM reconstructs the descriptor from the
cached repositories on the other side.

---

### Step 2 — `NetworkMockEndpointScreen` ✅ Done

Rewrote `NetworkMockEndpointScreen.kt` as a proper full screen:
- Removed `ModalBottomSheet`, `rememberModalBottomSheetState`, `rememberCoroutineScope`,
  close `IconButton`, `onDismissRequest`, and the wrapping `Box` header
- Final signature: `viewModel: NetworkMockEndpointViewModel`, `modifier`, `bottomPadding` —
  repositories are not passed through the screen; the VM is constructed by the caller
  (`NetworkMock.registerContent`) and injected directly
- Collects `uiState` from `NetworkMockEndpointViewModel`; delegates to existing `LoadingState`
  and `ErrorState` components for those variants
- Extracted the list body into a private `NetworkMockEndpointScreenContent` composable;
  wraps the `LazyColumn` in a `Column` with a sticky header `Surface` showing:
  - `config.name` at `titleLarge` (was the old bottom sheet title)
  - `method` + `path` in monospaced style, consistent with `EndpointCard`
  - `EndpointStateChip` for the current active state
  - `HorizontalDivider` separating the header from the list
- `bottomPadding` applied via `contentPadding` on the `LazyColumn`
- `previewingResponse: MockResponse?` local state added as a stub — wired to `onPreviewClick`
  on each `MockItem` but not yet consumed (intentional — Step 3 will add the sheet)
- `@Preview` calls `NetworkMockEndpointScreenContent` directly with fake data

---

### Step 3 — `MockResponsePreviewSheet` / `NetworkMockEndpointPreviewBottomSheet` ✅ Done

> **Interaction model:** long-press on a `MockItem` opens/toggles the preview sheet.
> Tap continues to mean "select this mock for the endpoint" — the two actions are fully
> orthogonal. A hint card in the endpoint header tells the user that long-press
> is available, making the gesture discoverable without cluttering the rows.

#### 3a — `PreviewSheetState` ✅ Done

Implemented as a `sealed interface` in `NetworkMockEndpointScreen.kt`:

```kotlin
sealed interface PreviewSheetState {
    sealed interface HasResponse : PreviewSheetState
    data object Hidden : PreviewSheetState
    data class Single(val response: MockResponse) : HasResponse
    data class Compare(val left: MockResponse, val right: MockResponse) : HasResponse

    fun transition(response: MockResponse): PreviewSheetState
    fun isInPreviewMode(response: MockResponse): Boolean
}
```

Transition behaviour (deviates slightly from original plan — better UX):

| Current state | Long-pressed item | Next state |
|---|---|---|
| `Hidden` | any `r` | `Single(r)` |
| `Single(r)` | same `r` | `Hidden` (toggle off / close) |
| `Single(a)` | different `b` | `Compare(a, b)` |
| `Compare(a, b)` | `a` | `Single(b)` (de-selects `a`, keeps `b`) |
| `Compare(a, b)` | `b` | `Single(a)` (de-selects `b`, keeps `a`) |
| `Compare(a, b)` | new `c` | `Compare(a, b)` unchanged (cap enforced — no-op) |

#### 3b — `NetworkMockEndpointScreenContent` wiring ✅ Done

- `previewSheetState: PreviewSheetState` owned in `NetworkMockEndpointScreenContent`
- `showPreviewBottomSheet: Boolean` separate boolean guards the actual sheet composition
- Centered FAB appears with `slideInVertically + fadeIn + scaleIn` when `previewSheetState != Hidden`; tapping it sets `showPreviewBottomSheet = true`
- Each `MockItem` receives `onLongClick = { previewSheetState = previewSheetState.transition(response) }` and `isInPreviewMode = previewSheetState.isInPreviewMode(response)`
- Hint card (`ElevatedCard` + `bodySmall` text) shown above the `HorizontalDivider`
- Sheet composed conditionally: `if (showPreviewBottomSheet && previewSheetState is HasResponse)`

#### 3c — `NetworkMockEndpointPreviewBottomSheet` body ✅ Done

`NetworkMockEndpointPreviewBottomSheet.kt` exists with the shell in place:
- `ModalBottomSheet(skipPartiallyExpanded = true)` ✅
- Animated close via `sheetState.hide()` + `invokeOnCompletion` ✅
- `HasResponse` typed parameter ✅
- **Body content not yet implemented** — needs the two modes below.

##### Single mode (`PreviewSheetState.Single`)

- Header row: status-code coloured icon chip + `displayName` as title + existing close button
- Body: `Text` with `FontFamily.Monospace` inside `verticalScroll` + `horizontalScroll`
  so wide JSON lines do not wrap

##### Compare mode (`PreviewSheetState.Compare`)

- Header: two response chips side-by-side + close button at trailing end
- Body: **smart diff** — choose display mode based on content similarity:

**Diff algorithm (pure Kotlin, no library):**

Create `components/MockResponseDiffContent.kt` containing:

```kotlin
internal sealed interface DiffLine {
    data class Unchanged(val text: String) : DiffLine
    data class Different(val textLeft: String?, val textRight: String?) : DiffLine
}
```

1. Split both `content` strings by `\n` into `linesA` and `linesB`
2. Compute LCS (Longest Common Subsequence) of the two line lists — standard O(n²) DP
3. Derive similarity ratio: `lcsLength / max(linesA.size, linesB.size)`
4. If `ratio >= 0.4` → **inline diff** (Mode A); otherwise → **split view** (Mode B)
   - Threshold is hardcoded at `0.4` for now

**Mode A — Inline diff** (similarity ≥ 0.4):
- Single scrollable column, full width
- Each `DiffLine.Unchanged` → plain `Text` in `FontFamily.Monospace`
- Each `DiffLine.Different` → two sub-rows:
  - Left line: `primary` colour background tint + `onPrimary` text
  - Right line: `secondary` colour background tint + `onSecondary` text
- Labels above the header chips ("left" / "right") clarify which colour belongs to which response
- Avoids red/green semantics — `primary`/`secondary` are neutral "A vs B" colours

**Mode B — Split view** (similarity < 0.4):
- Two `Column`s stacked **vertically** via `weight(1f)` each in a parent `Column`
- A `HorizontalDivider` separates the two halves
- Each half has its own independent `verticalScroll` + `horizontalScroll`
- Each half has a small chip header identifying which response it shows
- Content rendered in `FontFamily.Monospace`
- No side-by-side splitting — safe for portrait phone screens

Both `computeLineDiff()` and `shouldUseInlineDiff()` computed via `remember(left.content, right.content)` in the composable so they only rerun when content changes.

`@Preview` variants needed:
- `Single` with a fake response
- `Compare` — inline diff case (similar content)
- `Compare` — split view case (dissimilar content)

---

### Step 4 — `MockItem` — long-press + animated leading slot swap 🚧 In Progress

#### 4a — `onLongClick` + `isInPreviewMode` ✅ Done

`MockItem` updated with:
- `onLongClick: () -> Unit` parameter
- `isInPreviewMode: Boolean` parameter
- `combinedClickable(onClick = onClick, onLongClick = onLongClick)` on the row

#### 4b — Animated leading slot swap ✅ Done

Gmail-style vertical-axis flip on the leading icon chip:
- `updateTransition(isInPreviewMode)` animates `flipProgress: Float` from `-1f` → `1f`
- `abs(flipProgress)` applied as `scaleX` via `graphicsLayer` — collapses to 0 at midpoint
- Icon swaps at midpoint via `derivedStateOf { if (flipProgress >= 0f) CheckBox else statusIcon }`
- `NetworkItem` is unaffected (no long-press, no flip)

#### 4c — Hint card ✅ Done

`ElevatedCard` with `bodySmall` text above the list divider in `NetworkMockEndpointScreenContent`:
> *"Long press a mock response to be able to preview its content"*

---

### Step 5 — Wire `NetworkMock.registerContent` ✅ Done

`entry<NetworkMockDestination.Endpoint>` is fully wired in `NetworkMock.kt`:
- VM constructed inline via `viewModel { NetworkMockEndpointViewModel(...) }` using
  `it.endpointKey` from the typed destination
- `modifier = Modifier.fillMaxSize()` and `bottomPadding` forwarded correctly

---

### Step 6 — Clean up `NetworkMockScreen` ✅ TODO

The bottom-sheet call block is already commented out. Finish the cleanup:

- Delete the commented-out `selectedDescriptor?.let { ... }` block
- Remove `val selectedDescriptor` and `val selectedEndpointState` observations from
  `NetworkMockScreen`
- Remove `setEndpointMockState`, `clearSelectedEndpoint`, `selectedDescriptor`,
  `selectedEndpointState` parameters from `NetworkMockScreen`, `NetworkMockScreenContent`,
  `ContentState`, and the `@Preview`
- Remove now-unused imports: `EndpointDescriptor`, `EndpointMockState`

---

### Step 7 — Clean up `NetworkMockViewModel` ✅ TODO

Remove the endpoint selection concern entirely:

- Delete `selectedEndpointKey: MutableStateFlow`
- Delete `selectedEndpointDescriptor: StateFlow` and its KDoc
- Delete `selectedEndpointState: StateFlow` and its KDoc
- Delete `selectEndpoint(key: EndpointKey)` and its KDoc
- Delete `clearSelectedEndpoint()` and its KDoc
- Remove `EndpointDescriptor` import if it becomes unused after the deletions

---

## Files Changed Summary

| File | Action | Step |
|---|---|---|
| `viewmodel/NetworkMockEndpointViewModel.kt` | ✅ Created | 1 |
| `NetworkMockEndpointScreen.kt` | ✅ Rewritten — real screen, VM-driven, header added, preview stub in place | 2 |
| `components/MockResponsePreviewSheet.kt` *(new)* | Create preview sheet — single & compare modes; `PreviewSheetState` sealed interface | 3 |
| `components/MockResponseDiffContent.kt` *(new)* | `DiffLine` sealed interface, LCS algorithm, `computeLineDiff()`, `shouldUseInlineDiff()`, `InlineDiffContent`, `SplitDiffContent` composables | 3c |
| `components/MockItem.kt` | Add `MockItemPreviewState` enum + `onLongPress` + `inPreviewMode` boolean + Gmail-style animated leading slot swap (status chip ↔ Checkbox) | 4a |
| `NetworkMockEndpointScreen.kt` | Add hint card to endpoint header; compute `previewState` per item from `previewSheetState` | 4b |
| `NetworkMock.kt` | ✅ `entry<Endpoint>` wired with inline VM factory | 5 |
| `NetworkMockScreen.kt` | Remove dead selected-endpoint wiring | 6 |
| `NetworkMockViewModel.kt` | Remove selection state, flows, and methods | 7 |
