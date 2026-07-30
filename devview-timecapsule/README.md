# DevView TimeCapsule Module

A Kotlin Multiplatform library that records the state history of whichever screen is
currently visible under the DevView overlay, and lets a developer restore any earlier
state back into that screen while the app keeps running.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(projects.devviewTimecapsule)
}
```

## Quick Start

Implement `TimeCapsuleOwner` on the screen's state holder, then record it with one call:

```kotlin
import com.worldline.devview.timecapsule.TimeCapsuleEffect
import com.worldline.devview.timecapsule.TimeCapsuleOwner

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
    TimeCapsuleEffect(owner = viewModel, label = { "Count = ${it.count}" })
    // ...screen content
}
```

Register the module like any other:

```kotlin
val modules = rememberModules {
    module(TimeCapsule)
}
```

## How Scoping Works

Only one screen records at a time — whichever most recently called `TimeCapsuleEffect`.
The recorded history lives only as long as that composable stays in composition: navigate
away from the screen in your host app, and the history is discarded. There is no
cross-screen history and nothing is persisted to disk.

## API

- **`TimeCapsuleOwner<S>`**: contract implemented by a state holder — `state: StateFlow<S>`
  and `fun restoreState(state: S)`.
- **`TimeCapsuleEffect(owner, label, maxEntries)`**: composable that records `owner.state`
  and registers it with the module for as long as it stays composed.
- **`TimeCapsule`**: the `Module` entry point, registered with no arguments.

## Risk

Restoring a state is the integrator's responsibility to use safely. DevView does not
guarantee the host app keeps working correctly after a state is pushed back into a
running screen out of band.

## Documentation

All public APIs are documented with KDoc comments. View the documentation:
- In your IDE using Quick Documentation (Ctrl+Q / Cmd+J)
- Generate HTML docs using Dokka: `./gradlew dokkaHtml`

## License

This module is part of the DevView project and follows the same licensing terms.
