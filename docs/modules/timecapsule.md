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

For states containing `TextFieldState`, use the additional overload:

```kotlin
@Composable
public fun <S : Any> TimeCapsuleEffect(
    owner: TimeCapsuleOwner<S>,
    textFields: List<KProperty1<S, TextFieldState>>,
    label: (S) -> String = { it.toString() },
    subtitle: String? = null,
    maxEntries: Int = TimeCapsule.DEFAULT_MAX_ENTRIES
)
```

Declare the properties once at the call site; DevView handles observation, capture, and
restore internally. The existing overload and `TimeCapsuleOwner` contract are unchanged.

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

## State-based text fields

`TextFieldState` is mutable: typing changes its contents without necessarily emitting a
new value through the screen's `StateFlow`. Keeping the screen state alone, or making a
shallow data-class copy, does not freeze these contents.

Pass property references to capture those fields alongside the ordinary screen state:

```kotlin
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.TextField

data class FormState(
    val name: TextFieldState = TextFieldState(),
    val description: TextFieldState = TextFieldState(),
    val isLoading: Boolean = false
)

@Composable
fun FormScreen(viewModel: FormViewModel) { // TimeCapsuleOwner<FormState>
    val state by viewModel.state.collectAsStateWithLifecycle()

    TimeCapsuleEffect(
        owner = viewModel,
        textFields = listOf(FormState::name, FormState::description),
        label = { "isLoading=${it.isLoading}" }
    )

    TextField(state = state.name)
    TextField(state = state.description)
}
```

`label` remains optional. A custom label can focus on ordinary fields and avoid repeating
text fields in the default data-class `toString()`.

Each entry holds detached text strings and selection ranges. Text edits and cursor-only
or selection changes are observed through Compose snapshots, even without a new screen
state emission. The timeline displays captured values under their property names and
includes them in change summaries and expanded highlights; it does not parse text input
out of `toString()` to find these changes.

Restore edits the fields belonging to the saved screen state, then calls
`owner.restoreState(savedState)`. This works both for shared field instances and when
newer screen states replace them: observation follows the current state's properties.
No wrapper owner, mirrored strings, or custom text restoration is needed.

### Scope and limitations

- Properties must have unique names. Use direct `TextFieldState` properties of the screen
  state; automatic discovery, nullable properties, and nested field selectors are not provided.
- Keep the declared property list stable. Changing the owner or property list starts a new
  timeline; ordinary recomposition with equivalent property references does not.
- Text and selection are restored, not IME composition, keyboard visibility, focus, or undo
  history. Programmatic restoration can affect the editor's undo history.
- Snapshot observation may coalesce rapid changes. This is state history, not a keystroke log.
- Labels must be pure and read-only; restore runs on the UI thread through the overlay.
- Other mutable objects inside the screen state are not copied. Restoration does not replay
  business events, and is not atomic across Compose editing state and the owner's state updates
  or side effects.
- Captured text is retained in memory and shown in the developer overlay. Do not register
  password or other sensitive inputs unless exposing their contents is acceptable.

## Sample

See `sample/shared/.../CounterScreen.kt` for a runnable end-to-end example, including a
toggle that demonstrates the history resetting when the screen is disposed.

## API Reference
> _[Dokka API Reference](../api/devview-timecapsule/index.html)_
