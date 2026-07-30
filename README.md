# DevView

<p align="center">
  <img src="docs/assets/branding/devview-logo-light.svg#gh-light-mode-only" alt="DevView" width="420">
  <img src="docs/assets/branding/devview-logo-dark.svg#gh-dark-mode-only" alt="DevView" width="420">
</p>

A Kotlin Multiplatform library that adds an in-app developer overlay to Android and iOS apps. The overlay hosts pluggable modules for feature flags, analytics inspection, and network mocking.

**Full documentation:** [worldline.github.io/devview](https://worldline.github.io/devview/)

---

## Requirements

- Android minSdk 26
- iOS 16+
- Kotlin 2.x
- Compose Multiplatform

---

## Installation

DevView is published to Maven Central. Add the modules you need to your shared KMP module:

```kotlin
// build.gradle.kts (shared / commonMain)
dependencies {
    implementation("com.worldline.devview:devview:<version>")           // core — always required

    implementation("com.worldline.devview:devview-featureflip:<version>")   // feature flags
    implementation("com.worldline.devview:devview-analytics:<version>")     // analytics inspector
    implementation("com.worldline.devview:devview-timecapsule:<version>")   // per-screen state history
    implementation("com.worldline.devview:devview-networkmock:<version>")   // network mock UI

    // Ktor plugin only (no UI — lightweight alternative for network-layer integration)
    implementation("com.worldline.devview:devview-networkmock-core:<version>")
    implementation("com.worldline.devview:devview-networkmock-ktor:<version>")
}
```

---

## Quick Setup

### 1. Register modules and render the overlay

```kotlin
@Composable
fun MyApp() {
    val modules = rememberModules {
        module(module = FeatureFlip)
        module(module = Analytics())
        module(
            module = NetworkMock(
                resourceLoader = { path -> Res.readBytes(path = path) }
            )
        )
    }

    var devViewOpen by remember { mutableStateOf(false) }

    // Your existing app content
    AppContent(onOpenDevView = { devViewOpen = true })

    // DevView overlay
    DevView(
        devViewIsOpen = devViewOpen,
        closeDevView = { devViewOpen = false },
        modules = modules
    )
}
```

`rememberModules` handles DataStore initialisation and module setup automatically. `DevView` renders as a transparent overlay on top of your content.

### 2. Feature Flags

Define flags and provide the handler to the composition:

```kotlin
val featureHandler = rememberFeatureHandler(
    features = listOf(
        Feature.LocalFeature(name = "dark_mode", description = "Enable dark theme", isEnabled = false),
        Feature.RemoteFeature(
            name = "new_checkout",
            description = "New checkout experience",
            defaultRemoteValue = remoteConfig.getBoolean("new_checkout"),
            state = FeatureState.REMOTE
        )
    )
)

CompositionLocalProvider(LocalFeatureHandler provides featureHandler) {
    // Read flags anywhere in the tree
    val isDarkMode by LocalFeatureHandler.current.isFeatureEnabled("dark_mode")
}
```

### 3. Analytics

Forward events to `AnalyticsLogger` from your existing analytics layer:

```kotlin
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "purchase_completed",
        screenClass = "CheckoutScreen",
        timestamp = Clock.System.now().toEpochMilliseconds(),
        type = AnalyticsLogCategory.Ecommerce.Purchase
    )
)
```

Provide the log list above `DevView`:

```kotlin
CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) { … }
```

### 4. Network Mocking (Ktor)

Place your mock config at `composeResources/files/networkmocks/mocks.json` and install the plugin:

```kotlin
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin)
}
```

`rememberModules { }` must be called (and composed) before the first HTTP request reaches this client.

### 5. TimeCapsule

Implement `TimeCapsuleOwner` on a screen's state holder and record it with one call:

```kotlin
class CounterViewModel : ViewModel(), TimeCapsuleOwner<CounterState> {
    override val state: StateFlow<CounterState> = _state.asStateFlow()
    override fun restoreState(state: CounterState) { _state.value = state }
}

@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    TimeCapsuleEffect(owner = viewModel)
}
```

The recorded history resets automatically when the screen leaves composition.

---

## Available Modules

| Module | Artifact | Description |
|---|---|---|
| Core | `devview` | `DevView` composable + `rememberModules` DSL. Required by all modules. |
| FeatureFlip | `devview-featureflip` | Runtime feature flag management with Compose UI. Supports local and remote-config flags with local overrides. |
| Analytics | `devview-analytics` | Real-time analytics event inspector with filtering by type, category, and time range. |
| TimeCapsule | `devview-timecapsule` | Records the state history of the currently visible screen and lets you restore any earlier state back into it. |
| NetworkMock (UI) | `devview-networkmock` | Full mock management UI: enable/disable endpoints, switch responses, preview and diff mock payloads. |
| NetworkMock Core | `devview-networkmock-core` | Mock engine: JSON config parsing, request matching, DataStore state. No UI dependency. |
| NetworkMock Ktor | `devview-networkmock-ktor` | Ktor `HttpClientPlugin` that intercepts requests and returns mock responses. |

---

## Acknowledgments

DevView's build infrastructure, CI setup, and library conventions draw inspiration from [chrisbanes/haze](https://github.com/chrisbanes/haze). Many thanks to Chris Banes for open-sourcing that work.

---

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
