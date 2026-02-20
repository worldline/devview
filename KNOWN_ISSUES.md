# Known Issues

This file tracks known issues and limitations identified in the DevView network mock module
that have been deferred for future work.

---

## [NM-001] ~~Separate `MockConfigRepository` instances — No shared cache between plugin and UI~~ ✅ Fixed

**Fix:** `NetworkMock` now constructs a single `MockConfigRepository` internally and exposes
it via `networkMock.configRepository`. Integrators pass this shared instance to both the
Ktor plugin (`mockRepository = networkMock.configRepository`) and the UI (via `NetworkMock`
which forwards it automatically to `NetworkMockScreen`). `BaseHttpClientConfig` now accepts
a `MockConfigRepository` directly instead of a `resourceLoader`.

---

## [NM-002] ~~`MockResponse.fromFile()` — Fragile filename parsing breaks on hyphenated endpoint IDs~~ ✅ Fixed

**Fix:** `MockResponse.fromFile()` now uses `Regex("""-(\d{3})(-.+)?$""")` to locate the
status code by searching for the last `-NNN` boundary in the filename. Since all HTTP status
codes are exactly 3 digits, this is unambiguous regardless of how many hyphens appear in
the endpoint ID prefix (e.g. `get-user-200.json`, `create-new-post-201.json`).

---

## [NM-003] ~~`resetAllToNetwork()` only resets endpoints already stored in DataStore~~ ✅ Fixed

**Fix:**
- `MockStateRepository.resetKnownEndpointsToNetwork()` — renamed from the old
  `resetAllToNetwork()`, honestly describes what it does (only touches stored entries).
- `MockStateRepository.setAllEndpointStates(states)` — new method that fully overwrites
  the stored endpoint map with a caller-supplied set.
- `NetworkMockViewModel.resetAllToNetwork()` — now builds a complete disabled-state map
  from the loaded configuration (covering every endpoint, including never-touched ones)
  and writes it via `setAllEndpointStates`. Falls back to `resetKnownEndpointsToNetwork()`
  if config is not yet loaded.

---


**File(s):** `sample/shared/.../BaseHttpClientConfig.kt`, `NetworkMockScreen.kt`

**Description:**
The Ktor plugin (`NetworkMockPlugin`) and the DevView UI screen (`NetworkMockScreen`) each
create their own independent `MockConfigRepository` instance. Because `cachedConfig` is an
instance-level field, the two caches are completely separate — meaning configuration is
loaded twice (once at plugin install time, once when the UI screen loads).

More critically, the two instances have no awareness of each other. If the integrator
passes a different `configPath` or `resourceLoader` to each, behaviour will be inconsistent.
The shared `MockStateRepository` (DataStore) is the only thing keeping them in sync at
runtime.

**Suggested fix:**
The integrator should construct a single `MockConfigRepository` instance and pass it to
**both** the `NetworkMockPlugin` installation and the `NetworkMockScreen` (via
`NetworkMock`). The `NetworkMock` class should accept a `MockConfigRepository` parameter
rather than creating its own internally.

---

## [NM-002] `MockResponse.fromFile()` — Fragile filename parsing breaks on hyphenated endpoint IDs

**File(s):** `devview-networkmock/.../model/MockResponse.kt`,
`devview-networkmock/.../repository/MockConfigRepository.kt`

**Description:**
Response file names are parsed by splitting on `-` and assuming index `[0]` is the
endpoint ID and index `[1]` is the status code:

```kotlin
val parts = nameWithoutExtension.split("-")
val statusCode = parts[1].toIntOrNull() ?: return null
```

If an endpoint ID contains a hyphen (e.g., `get-user`), `parts[1]` will be `"user"` rather
than the status code, causing `toIntOrNull()` to return `null` and the response to be
silently dropped. This means **no mock responses will be discoverable** for any endpoint
whose ID contains a `-`.

**Suggested fix:**
Rather than splitting on the first `-`, use a regex to find the *last* `-{3-digit-number}`
boundary, since all HTTP status codes are exactly 3 digits. This is unambiguous regardless
of how many hyphens appear in the endpoint ID prefix:

```kotlin
val match = Regex("""-(\d{3})(-.+)?$""").find(nameWithoutExtension) ?: return null
val statusCode = match.groupValues[1].toIntOrNull() ?: return null
```

This correctly handles IDs like `get-user`, `create-new-post`, `my-api-v2`, etc.

---

## [NM-003] `resetAllToNetwork()` only resets endpoints already stored in DataStore

**File(s):** `devview-networkmock/.../repository/MockStateRepository.kt`,
`devview-networkmock/.../viewmodel/NetworkMockViewModel.kt`

**Description:**
`MockStateRepository.resetAllToNetwork()` only iterates over endpoints that already have
a persisted state in DataStore. If an endpoint has never been toggled by the user, it has
no entry in DataStore and is therefore silently skipped. This means "Reset All to Network"
is misleading — it is really "Reset previously-touched endpoints to Network".

A simple rename to `resetKnownEndpointsToNetwork()` would be more honest but doesn't
fix the underlying gap. The real fix requires resetting *all configured endpoints*,
including those never touched.

**Suggested fix:**
Move the reset logic to `NetworkMockViewModel`, which already has access to both
`configRepository` (the full list of endpoints from `mocks.json`) and `stateRepository`
(for writing state). The ViewModel can build the complete set of `EndpointMockState`
entries from config (all set to `mockEnabled = false`, `selectedResponseFile = null`) and
write them all at once. `MockStateRepository` can expose a new
`setAllEndpointStates(states: Map<String, EndpointMockState>)` method to support this.

---



