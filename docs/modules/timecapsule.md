# TimeCapsule Module

Records the state history of whichever screen is currently visible under the DevView
overlay, and lets a developer restore any earlier state back into that screen while the
app keeps running.

## Overview

TimeCapsule tracks exactly one screen at a time — whichever most recently started
recording — and its history resets whenever that screen leaves composition (the user
navigates away in the host app). There is no cross-screen history and nothing is
persisted to disk; everything lives in memory for the lifetime of the screen.

Restoring a state is the integrator's risk. DevView does not guarantee the host app keeps
working correctly after a state is pushed back into a running screen out of band — that
is the same risk any time-travel debugging tool carries, and it is on the integrator to
judge whether a given screen is safe to rewind.

## Installation

```kotlin
dependencies {
    implementation("com.worldline.devview:devview-timecapsule:<version>")
}
```

## Core API

### `TimeCapsuleOwner`

Implement this on your screen's state holder (typically a `ViewModel`) so TimeCapsule can
observe and restore its state:

```kotlin
public interface TimeCapsuleOwner<S : Any> {
    public val state: StateFlow<S>
    public fun restoreState(state: S)
}
```

### `TimeCapsuleEffect`

Call once per screen, inside the screen's top-level composable:

```kotlin
@Composable
public fun <S : Any> TimeCapsuleEffect(
    owner: TimeCapsuleOwner<S>,
    label: (S) -> String = { it.toString() },
    subtitle: String? = null,
    maxEntries: Int = TimeCapsule.DEFAULT_MAX_ENTRIES
)
```

`label` produces the one-line description shown for each recorded entry — defaults to
`state.toString()`. When a label is shaped like `key=value, key=value` (what `toString()`
produces on a data class), the timeline shows a change summary against the previous entry
instead — e.g. `count 3 → 4` — and expanding a row reveals the full label with changed
values highlighted. A label with no parseable `key=value` pairs is shown verbatim, exactly
as before.

`subtitle` names the recorded screen in the Time Capsule header ("Recording {subtitle}"),
so a tester can confirm which screen they're looking at. Defaults to `owner`'s class name;
pass this explicitly if `owner` is an anonymous class, where the class name is unavailable.
`maxEntries` bounds retention; the oldest entry is dropped once reached.

### `TimeCapsule`

The DevView module. Registered with no arguments, like `FeatureFlip`:

```kotlin
val modules = rememberModules {
    module(TimeCapsule)
}
```

## Usage

```kotlin
data class CounterState(val count: Int)

class CounterViewModel : ViewModel(), TimeCapsuleOwner<CounterState> {
    private val _state = MutableStateFlow(CounterState(count = 0))
    override val state: StateFlow<CounterState> = _state.asStateFlow()

    override fun restoreState(state: CounterState) {
        _state.value = state
    }

    fun increment() {
        _state.update { it.copy(count = it.count + 1) }
    }
}

@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TimeCapsuleEffect(owner = viewModel)

    // ...screen content
}
```

With this wired up, incrementing the counter records a new entry on every change. Opening
DevView → Time Capsule shows a "Recording CounterViewModel" header, then the history
newest-first: `CounterState`'s default `toString()`-shaped label lets each row show a
change summary (`count 3 → 4`) instead of the full state, expandable to see the rest.
Tapping **Restore** on any entry calls `restoreState` with that entry's value, and the
running screen reflects it immediately. Navigating away from `CounterScreen` and back
starts a fresh, empty history.

## Sample

See `sample/shared/.../CounterScreen.kt` for a runnable end-to-end example, including a
toggle that demonstrates the history resetting when the screen is disposed.

## API Reference
> _[Dokka API Reference](../api/devview-timecapsule/index.html)_
