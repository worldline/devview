# FeatureFlip Module

A Kotlin Multiplatform library for managing feature flags (feature toggles) with a built-in UI for toggling and managing features at runtime.

> _[Placeholder: Insert screenshot of FeatureFlip UI. Use a device frame if relevant.]_

## Overview
FeatureFlip enables runtime control of feature flags for development, QA, and experimentation. It supports both local and remote features, persistent state, and a ready-to-use Compose UI.

## Features
- Local and remote feature flags
- Persistent state with DataStore (Android/iOS)
- Built-in Compose UI: search, filter, toggle, tri-state controls
- Type-safe API
- Multiplatform support

## Installation
Add to your `build.gradle.kts`:
```kotlin
dependencies {
    implementation(projects.devviewFeatureflip)
}
```

## Integration Example
Add FeatureFlip to your DevView modules list:
```kotlin
val modules = rememberModules {
    module(FeatureFlip)
    // ...other modules...
}
```

## Quick Start
### Defining Features
```kotlin
val darkMode = Feature.LocalFeature(
    name = "dark_mode",
    description = "Enable dark theme",
    isEnabled = false
)
val newCheckout = Feature.RemoteFeature(
    name = "new_checkout_flow",
    description = "Enable the redesigned checkout experience",
    defaultRemoteValue = true,
    state = FeatureState.REMOTE
)
```

### Using the UI
```kotlin
CompositionLocalProvider(LocalFeatures provides features) {
    FeatureFlipScreen(onStateChange = { featureName, newState ->
        // Handle state change
    })
}
```

## Usage
- Use `FeatureHandler` to manage features programmatically.
- Use `FeatureFlipScreen` for a ready-made UI.
- Features are persisted using DataStore.

## Customisation
> _[Placeholder: Guide for customising FeatureFlip UI and behaviour. This section will be expanded in future updates.]_

## Advanced Usage
> _[Placeholder: Advanced scenarios such as remote config integration, tri-state features, and platform-specific customisation.]_

## Best Practices
1. Use descriptive names and add descriptions.
2. Start with REMOTE for remote features, override only for testing.
3. Persist state for reliability.
4. Monitor overrides for QA.

## Troubleshooting / FAQ
- **Why isn't my feature state persisting?**
  - Ensure you use `FeatureHandler` and DataStore correctly.
  - Check DataStore permissions and initialisation on both Android and iOS.
- **How do I reset a remote feature to remote config?**
  - Set its state to `FeatureState.REMOTE`.
- **Can I use this on iOS?**
  - Yes, DataStore is supported on both Android and iOS. Ensure platform-specific initialisation is correct.
- **Feature UI not updating?**
  - Confirm you are using Compose state correctly and updating the feature list.
- **Integration issues?**
  - Verify module is included in your DevView modules list and dependencies are present.

## Related Modules
- [Analytics](analytics.md): Monitor feature usage and events.
- [NetworkMock](networkmock.md): Combine with feature flags for advanced testing.
- [Creating Custom Modules](custom-modules.md)

## API Reference
> _[Dokka API Reference](../api/devview-featureflip/index.html)_

---

*API reference is available via Dokka or in your IDE.*
