# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

`devview-test` is a Kotlin Multiplatform library of shared test utilities consumed by other DevView modules. It has no production runtime use — its source lives in `commonMain`/`androidMain` but it is always declared as a test dependency by consumers.

## Source Structure

All source is under `src/` with two source sets:

- `commonMain` — multiplatform helpers (Android + iOS)
- `androidMain` — Android-only helpers (Compose UI tests, MockK-based ViewModel base)

## Provided Helpers

### commonMain

**`FakePreferencesDataStore`**
In-memory `DataStore<Preferences>` backed by a `MutableStateFlow`. Use this in any test that needs a `DataStore<Preferences>` without real I/O. Only create module-local fakes when you need special behavior (e.g., a DataStore that throws `IOException` to test error recovery).

**`FlowAssertions.kt`**
`Flow<T>.assertEmitsExactly(vararg expected)` — Turbine-based assertion that collects exactly the given values in order, then cancels. Throws `AssertionError` with a diff if any item mismatches.

**`StateFlowCollectors.kt`**
`TestScope.collectState(stateFlow)` and `TestScope.collectStates(vararg stateFlows)` — launch background collectors on a `StateFlow` so that `value` updates are processed during `runTest`. Required whenever a ViewModel or repository exposes `StateFlow` and you need `.value` to reflect upstream emissions in a host test.

**`TestDispatchers.kt`**
- `TestDispatchers` — data class holding `main`, `io`, `default`, and `unconfined` `TestDispatcher` instances all sharing one `TestCoroutineScheduler`.
- `testDispatchers()` — factory function.
- `runTestWithDispatchers { dispatchers -> }` — `runTest` wrapper that passes a pre-built `TestDispatchers` into the test body. The test coroutine runs on `dispatchers.main`.

### androidMain

**`ComposeUiTestWait.kt`**
Extensions on `ComposeUiTest` (requires `ExperimentalTestApi`):
- `waitUntilTagCount(tag, expectedCount, timeoutMillis = 10_000)`
- `waitUntilTagExists(tag)` — waits for exactly 1 node with the tag
- `waitUntilTagGone(tag)` — waits for 0 nodes with the tag

**`ViewModelTest`** (open class, Android only, depends on MockK)
Base class for ViewModel unit tests. `setup()` initialises `dispatchers` via `testDispatchers()` and sets `Dispatchers.Main` to `dispatchers.unconfined`. `tearDown()` calls `clearAllMocks()` and resets `Dispatchers.Main`. Subclasses must call `super.setup()` / `super.tearDown()` from their `@Before`/`@After` methods.

Note: `ViewModelTest` installs `dispatchers.unconfined` (not `dispatchers.main`) as the main dispatcher so that state updates are visible immediately without manually advancing the scheduler.

## Consumers

| Module | Source set |
|---|---|
| `devview-featureflip` | `commonTest`, `androidDeviceTest` |
| `devview-networkmock-core` | `commonTest` |
| `devview-networkmock` | `androidHostTest` |
| `devview-analytics` | `androidDeviceTest` |

Add to a module's `build.gradle.kts`:

```kotlin
commonTest {
    dependencies { implementation(projects.devviewTest) }
}
androidDeviceTest {
    dependencies { implementation(projects.devviewTest) }
}
```

## Opt-ins

All source sets in this module opt into `ExperimentalTestApi` (for `ComposeUiTest`) and `ExperimentalCoroutinesApi`. Consuming modules do **not** need to repeat these opt-ins for code that merely calls these helpers — the opt-ins are propagated through the compiled API.
