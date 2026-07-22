# DevView

<div align="center" markdown>

<img class="devview-logo devview-logo--light" src="assets/branding/devview-logo-light.svg" alt="DevView" width="420" />
<img class="devview-logo devview-logo--dark" src="assets/branding/devview-logo-dark.svg" alt="DevView" width="420" />

</div>

<div align="center" markdown>

**A powerful, modular developer tools framework for Kotlin Multiplatform applications**

<!-- renovate: datasource=maven depName=org.jetbrains.kotlin:kotlin-stdlib -->
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
<!-- renovate: datasource=maven depName=org.jetbrains.compose:compose-gradle-plugin -->
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-green.svg?style=flat)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Licence](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](https://opensource.org/licenses/Apache-2.0)
[![GitHub](https://img.shields.io/badge/GitHub-worldline%2Fdevview-blue.svg?style=flat&logo=github)](https://github.com/worldline/devview)

</div>

---

## What's New

### v0.1.4

**Changed**
- Applied logo-inspired color palette to the sample app: custom light/dark `ColorScheme` using violet/indigo/magenta tones from the DevView brand, including the full `surfaceContainer` tonal ramp. (`sample`)
- Module icon containers on the home screen now use per-section colors derived from the logo palette; icon shape changed from circle to squircle (`RoundedCornerShape(8.dp)`) and module names are now `SemiBold`. Chevron indicator removed — touch ripple is the navigation affordance. (`devview`)
- Section headers on the home screen now use `primary` color instead of `outline`, and the DevView chameleon icon appears as a subtle watermark behind the module list. (`devview`)
- Analytics log items now display a `3dp` leading color strip matching the event category, making the log stream scannable by category at a glance. (`devview-analytics`)
- Feature type labels ("Local" / "Remote") are now rendered as small pill badges instead of plain text. (`devview-featureflip`)
- HTTP method labels on endpoint cards and the endpoint detail header are now rendered as proper badges with a `primaryContainer` background, matching the visual language of API explorer tools. (`devview-networkmock`)
- NetworkMock empty, error, and loading state screens now use Material icons instead of an emoji, with consistent typography and `onSurfaceVariant` text colors. (`devview-networkmock`)
- Added empty state to `FeatureFlipScreen` when the feature list is empty or no features match the active filter. (`devview-featureflip`)
- The endpoint detail hint card now shows a `TouchApp` icon for visual clarity. (`devview-networkmock`)
- Top app bar titles across all DevView screens are now `SemiBold` weight. (`devview`)
- Unified surface backgrounds: the `surfaceContainer` explicit color has been removed from the Analytics highlighted-logs header and the NetworkMock global toggle wrapper — backgrounds now inherit from `MaterialTheme` uniformly. (`devview-analytics`, `devview-networkmock`)
- Diff colors shifted from generic blue to lavender (`#DDD8FF`) to align with the brand palette. (`devview-networkmock`)

**Fixed**
- Fixed excessive recompositions and broken Switch animation on `FeatureFlipScreen`: item keys now use the stable `feature.name` instead of `hashCode()`, `FeatureHandler` caches its Flow to prevent `collectAsStateWithLifecycle` from restarting on every recomposition, and redundant explicit `remember` keys have been removed from `derivedStateOf` blocks. (`devview-featureflip`)
- Added `distinctUntilChanged()` to `FeatureHandler.isFeatureEnabledFlow` and `getFeatures` to suppress recompositions when DataStore emits structurally identical values. (`devview-featureflip`)
- Fixed `AnalyticsScreen` `LazyColumn` item key: `log.hashCode()` was replaced by `log.timestamp`, then `log.timestamp` caused a crash because multiple events can share the same millisecond; the key is now the log's original position in the append-only `AnalyticsLogger.logs` list via `withIndex()`. Redundant explicit keys also removed from all `derivedStateOf` blocks. (`devview-analytics`)
- Fixed `HomeScreen` `LazyColumn` using `module.hashCode()` as item key instead of the stable `module.moduleName`. (`devview`)
- Fixed `NetworkMockScreen` using `collectAsState()` instead of `collectAsStateWithLifecycle()`, causing unnecessary state collection when the screen is off-stack or the app is backgrounded. (`devview-networkmock`)
- Added `distinctUntilChanged()` to `MockStateRepository.observeState()` to suppress recompositions triggered by structurally equal `NetworkMockState` emissions from DataStore. (`devview-networkmock-core`)

**Documentation**
- Added Compose List Keys rules to the contributing guide (`code-style.md`): LazyColumn/LazyRow keys must be unique, stable under state changes, and semantically meaningful. Added matching item to the PR checklist.
---

## What is DevView?

DevView is an extensible, in-app developer tools framework designed for Kotlin Multiplatform applications. It provides a unified interface for debugging, testing, and managing development features across Android and iOS platforms.

---

## Getting Started

Ready to integrate DevView into your project?

<div class="grid cards" markdown>

- :material-download: **[Installation Guide](getting-started/installation.md)**

    Get DevView up and running in minutes

- :material-rocket-launch: **[Quick Start](getting-started/quick-start.md)**

    Build your first DevView integration

- :material-puzzle: **[Module Documentation](modules/index.md)**

    Explore available modules and features

- :material-code-braces: **[API Reference](api/index.html)**

    Detailed API documentation

</div>

---

## Key Features

- 🎯 **Modular Architecture** – Pick and choose the modules you need
- 🔧 **Feature Flag Management** – Toggle features on/off during development and testing
- 📊 **Analytics Debugging** – Monitor and inspect analytics events in real time
- 🎨 **Compose Multiplatform UI** – Native Material Design 3 interface
- 🔐 **Type-Safe Navigation** – Built on Navigation3 with kotlinx.serialization
- 💾 **Persistent State** – Feature states survive app restarts with DataStore
- 🚀 **Easy Integration** – Simple setup with minimal boilerplate
- 📱 **Cross-Platform** – Works seamlessly on Android and iOS

---

## Quick Example

```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics())
    }
    Box {
        // Your main app content
        MainAppContent()
        // DevView overlay
        DevView(
            devViewIsOpen = isDevViewOpen,
            closeDevView = { isDevViewOpen = false },
            modules = modules
        )
        // Debug trigger
        FloatingActionButton(
            onClick = { isDevViewOpen = true }
        ) {
            Icon(Icons.Default.DeveloperMode, "Open DevView")
        }
    }
}
```

---

## Available Modules

### 🎚️ FeatureFlip

Manage feature flags with support for both local and remote features.

- Simple on/off toggles for local features
- Remote configuration with local overrides
- Persistent state management
- Search and filter capabilities

[Learn more about FeatureFlip →](modules/featureflip.md)

### 📊 Analytics

Monitor and debug analytics events in real time.

- Real-time event logging
- Multiple event types (Screen, Event, Custom)
- Tabular display with timestamps
- Event type filtering

[Learn more about Analytics →](modules/analytics.md)

### 🌐 NetworkMock

Mock and control network requests and responses for development and testing.

- Mock network requests and responses
- UI for toggling global and per-endpoint mocks
- Ktor plugin for HTTP interception
- Persistent configuration/state
- Multiplatform support (Android/iOS)

[Learn more about NetworkMock →](modules/networkmock.md)

### 🔧 Custom Modules

Extend DevView with your own custom modules.

- Simple module interface
- Type-safe navigation
- Automatic UI integration
- Section-based organisation

[Learn how to create custom modules →](modules/custom-modules.md)

---

## Why DevView?

> **Tip:** DevView is designed to save you time and make debugging, testing, and feature management a breeze. Integrate it early in your project for maximum benefit!

### For Developers

- **Faster Debugging** – Inspect feature flags and analytics without rebuilding
- **Better Testing** – Toggle features to test different configurations
- **Enhanced Visibility** – See exactly what's happening in your app
- **Time Savings** – No need to navigate deep into settings or rebuild

### For QA Teams

- **Feature Validation** – Verify features work in all states
- **Analytics Verification** – Confirm events fire correctly
- **Test Scenarios** – Easily switch between different feature configurations
- **Bug Reporting** – Include feature states in bug reports

### For Product Teams

- **Risk Mitigation** – Test features before full rollout
- **Gradual Rollouts** – Control feature availability
- **Quick Rollbacks** – Disable problematic features instantly
- **Data-Driven Decisions** – Monitor feature usage and analytics

---

## Platform Support

| Platform | Minimum Version | Status |
|----------|----------------|--------|
| Android  | API 26 (Oreo) | ✅ Stable |
| iOS      | iOS 16.0 | ✅ Stable |

---

## Architecture

DevView follows a modular architecture where each module is:

1. **Self-Contained** – Modules manage their own state and UI
2. **Type-Safe** – Uses kotlinx.serialization for navigation
3. **Composable** – Built entirely with Compose Multiplatform
4. **Extensible** – Easy to add new modules

```mermaid
graph TD
    A[DevView Framework] --> B[Core Module]
    A --> C[FeatureFlip Module]
    A --> D[Analytics Module]
    A --> NM[NetworkMock Module]
    A --> E[Custom Modules]
    B --> F[Module Registry]
    B --> G[Navigation System]
    B --> H[UI Components]
    C --> I[Feature Handler]
    C --> J[DataStore Persistence]
    D --> K[Analytics Logger]
    D --> L[Event Display]
    NM --> MC[Mock Config Engine]
    NM --> MS[Mock State DataStore]
    NM --> KP[Ktor Plugin]
```

---

## Community & Support

- 📖 **Documentation** – You're reading it!
- 💬 **Discussions** – [GitHub Discussions](https://github.com/worldline/devview/discussions)
- 🐛 **Bug Reports** – [GitHub Issues](https://github.com/worldline/devview/issues)
- 💡 **Feature Requests** – [GitHub Issues](https://github.com/worldline/devview/issues)

---

## Licence

DevView is released under the [Apache Licence 2.0](license.md).

```
Copyright 2024-2026 Maxime Michel

Licensed under the Apache Licence, Version 2.0 (the "Licence");
you may not use this file except in compliance with the Licence.
You may obtain a copy of the Licence at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

<div align="center" markdown>

**Made with ❤️ by Maxime Michel**

[Get Started](getting-started/installation.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/worldline/devview){ .md-button }

</div>
