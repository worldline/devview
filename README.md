<p align="center">
  <img src="docs/assets/branding/devview-logo-light.svg#gh-light-mode-only" alt="DevView" width="420">
  <img src="docs/assets/branding/devview-logo-dark.svg#gh-dark-mode-only" alt="DevView" width="420">
</p>

# DevView

[![Maven Central](https://img.shields.io/maven-central/v/com.worldline.devview/devview)](https://central.sonatype.com/artifact/com.worldline.devview/devview) ![Build status](https://github.com/worldline/devview/actions/workflows/build.yml/badge.svg)

In-app developer tools for Compose Multiplatform.

DevView brings feature flags, analytics events, console logs, screen-state history, and network mocks into your Android or iOS app. Register the modules you need and choose how to present the developer menu in your app.

## Platforms

| Platform | Minimum version |
|---|---|
| Android | API 26 |
| iOS | 16.0 |

Requires Kotlin 2.x and Compose Multiplatform.

## Modules

Add the core module and only the tools you use. Keep all DevView artifacts on the same version.

| Artifact | Purpose |
|---|---|
| `devview` | Developer menu, module registry, and custom-module UI kit. |
| `devview-featureflip` | Local and remote feature flags with persistent overrides. |
| `devview-analytics` | Analytics event inspection, search, and filtering. |
| `devview-consolelogger` | Native console logs with level filters and text search. |
| `devview-timecapsule` | Per-screen state history and restoration. |
| `devview-networkmock` | Network mock management UI. |
| `devview-networkmock-core` | OpenAPI parsing, request matching, and persistent mock state without Compose UI. |
| `devview-networkmock-ktor` | Ktor client plugin backed by the mock engine. |

## Download

DevView is available from [Maven Central](https://central.sonatype.com/artifact/com.worldline.devview/devview). Add dependencies to your shared module's `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.worldline.devview:devview:<version>")
            implementation("com.worldline.devview:devview-analytics:<version>")
            implementation("com.worldline.devview:devview-consolelogger:<version>")
        }
    }
}
```

For network mocking, also add `devview-networkmock` and `devview-networkmock-ktor`. The mock engine is included transitively. If you only need client-side interception without the DevView UI, use `devview-networkmock-ktor` on its own.

See the [installation guide](https://worldline.github.io/devview/getting-started/installation/) for repository configuration and version-catalog examples.

## Developer menu

```kotlin
@Composable
fun MyApp() {
    val modules = rememberModules {
        module(module = Analytics())
        module(module = Console())
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

`rememberModules` handles DataStore initialisation and module setup automatically. You control when the menu opens and how it is presented. Keep developer tools and captured logs out of production builds.

## Feature flags

Add `devview-featureflip`, register `FeatureFlip` in `rememberModules`, and provide the handler around your app and DevView:

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

## Analytics

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

Register `Analytics()` in `rememberModules` and render `DevView`; the module observes
`AnalyticsLogger.logs` directly. No analytics composition-local provider or standalone screen is needed.

## Network mocking (Ktor)

Place an OpenAPI 3.x spec (JSON or YAML) at `composeResources/files/networkmocks/specs/my-backend.json` — one spec file per API group — and install the plugin:

```kotlin
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin)
}
```

Register the spec with the UI module:

```kotlin
val modules = rememberModules {
    module(
        NetworkMock(
            resourceLoader = { path -> Res.readBytes(path = path) },
            specPaths = listOf("files/networkmocks/specs/my-backend.json")
        )
    )
}
```

This registry must be composed before the first HTTP request reaches the client. See the [network mocking guide](https://worldline.github.io/devview/modules/networkmock/) for headless configuration and request-matching behaviour.

## TimeCapsule

Add `devview-timecapsule` and register `TimeCapsule()` in `rememberModules`. Implement `TimeCapsuleOwner` on a screen's state holder and record it with one call:

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

## Console Logger

Displays native console output (logcat on Android, a stdout/stderr redirect on iOS) inside
DevView — works on an untethered device, no debugger required:

```kotlin
val modules = rememberModules {
    module(module = Console())
}

// Optional: route Kermit-based logging into the same view.
Logger.addLogWriter(DevViewLogWriter)
```

See the [Console Logger guide](https://worldline.github.io/devview/modules/consolelogger/)
for the exact per-platform capture matrix and its one limitation (`NSLog`/`os_log` on iOS
with no debugger attached).

## Learn more

- [Documentation](https://worldline.github.io/devview/)
- [Integration guide](https://worldline.github.io/devview/guides/integration-guide/)
- [Feature flags](https://worldline.github.io/devview/modules/featureflip/)
- [Analytics](https://worldline.github.io/devview/modules/analytics/)
- [TimeCapsule](https://worldline.github.io/devview/modules/timecapsule/)
- [Custom modules](https://worldline.github.io/devview/modules/custom-modules/)
- [Sample app](sample/)
- [Changelog](CHANGELOG.md)

## Acknowledgments

DevView's build infrastructure, CI setup, and library conventions draw inspiration from [chrisbanes/haze](https://github.com/chrisbanes/haze). Many thanks to Chris Banes for open-sourcing that work.

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
