# Analytics Module

A Kotlin Multiplatform library for capturing and visualising analytics events in real time, providing a developer-friendly interface for monitoring and debugging analytics integration in your application.

> _[Placeholder: Insert screenshot of Analytics UI. Use a device frame if relevant.]_

## Overview
Analytics enables real-time monitoring of analytics events (screen views, custom events, etc.) with a built-in Compose UI for debugging and QA.

## Features
- Multi-type event tracking (screen, event, custom)
- Real-time monitoring and tabular display
- Detailed event info: tags, screen classes, timestamps
- Built-in Compose UI
- Type-safe API
- Multiplatform support (Android/iOS)

## Installation
Add to your `build.gradle.kts`:
```kotlin
dependencies {
    implementation(projects.devviewAnalytics)
}
```

## Integration Example
Add Analytics to your DevView modules list:
```kotlin
val modules = rememberModules {
    module(Analytics)
    // ...other modules...
}
```

## Quick Start
### Logging Analytics Events
```kotlin
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "HomeScreen",
        screenClass = "com.example.ui.HomeScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogType.SCREEN
    )
)
```

### Using the UI
```kotlin
CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
    AnalyticsScreen(modifier = Modifier.fillMaxSize())
}
```

## Usage
- Use `AnalyticsLogger.log()` to record events.
- Use `AnalyticsScreen` to view logs in real time.
- Integrate with your analytics backend for dual logging.

## Customisation
> _[Placeholder: Guide for customising Analytics UI and behaviour. This section will be expanded in future updates.]_

## Advanced Usage
> _[Placeholder: Advanced scenarios such as integration with external analytics backends, filtering, and platform-specific customisation.]_

## Best Practices
1. Use descriptive tags and include screen context.
2. Use SCREEN for navigation, EVENT for interactions, CUSTOM for business events.
3. Log to DevView only in debug builds for best performance.
4. Clear logs periodically during long sessions.

## Troubleshooting / FAQ
- **Why aren't my events showing up?**
  - Ensure you use `AnalyticsLogger.log()` and provide the correct event type.
  - Confirm Compose state is correctly updated and the UI is observing the logs.
- **How do I clear logs?**
  - Use `AnalyticsLogger.clear()` (for debug/testing only).
- **Can I use this on iOS?**
  - Yes, Compose Multiplatform is supported. Ensure platform-specific initialisation is correct.
- **Integration issues?**
  - Verify module is included in your DevView modules list and dependencies are present.

## Related Modules
- [FeatureFlip](featureflip.md): Track feature flag usage.
- [NetworkMock](networkmock.md): Monitor analytics during network mocking.
- [Creating Custom Modules](custom-modules.md)

## API Reference
> _[Dokka API Reference](../api/devview-analytics/index.html)_

---

*API reference is available via Dokka or in your IDE.*
