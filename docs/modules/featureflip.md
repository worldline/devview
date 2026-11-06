# FeatureFlip Module

A Kotlin Multiplatform library for managing feature flags (feature toggles) with a built-in UI for toggling and managing features at runtime.

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
val handler = rememberFeatureHandler(features)
CompositionLocalProvider(LocalFeatureHandler provides handler) {
    DevView(
        devViewIsOpen = isDevViewOpen,
        closeDevView = { isDevViewOpen = false },
        modules = modules
    )
}
```

## Controls and filters

Local flags use a named on/off switch. Remote flags use Material3 segmented buttons
labelled **Remote**, **Off** and **On**, with supporting text showing the remote value.
Selecting Remote follows that value; Off and On force a local override. Selection
uses the host theme's paired container/content colours rather than an error colour
to represent a disabled flag. Local/Remote badges are no longer shown on each row.

Search stays pinned below the list. Expand filters to choose Type and State; Type
is omitted when all flags have the same kind unless a saved type filter is still active. Filters combine OR within a group
and AND across groups. The toggle reports the active filter count even when
collapsed. **Clear filters** resets the chips; the no-results action clears both
search and filters. Search, chip choices and panel visibility survive saved-state
restoration, including type filters when the registered flags change. Flag values continue to persist in DataStore.

Changes are saved in click order. If a save fails, the persisted selection stays
unchanged and the screen offers **Retry** instead of crashing the host app.

## Usage
- Use `FeatureHandler` to manage features programmatically.
- Open DevView → Feature Flip to manage flags; registration owns the screen.
- Features are persisted using DataStore.

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

