# Analytics Module

A Kotlin Multiplatform library for capturing and visualising analytics events in real time, providing a developer-friendly interface for monitoring and debugging analytics integration in your application.

## Overview
Analytics enables real-time monitoring of analytics events (screen views, custom events, etc.) with a built-in Compose UI for debugging and QA.

## Features
- Multi-type event tracking (screen, event, custom)
- Live event list with compact counts for the filtered results
- Event details with screen, full timestamp, string parameters and Copy event
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
    module(Analytics()) // optional: pass highlightedLogType1/2/3 to customize summary chips
    // ...other modules...
}
```

## Quick Start
### Logging Analytics Events
```kotlin
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "home_screen_view",
        screenClass = "com.example.ui.HomeScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogCategory.Screen.View,
        params = mapOf("source" to "home")
    )
)
```

### Using the UI
```kotlin
DevView(
    devViewIsOpen = isDevViewOpen,
    closeDevView = { isDevViewOpen = false },
    modules = modules
)
```

## Usage
- Use `AnalyticsLogger.log()` to record events.
- Open DevView → Analytics to inspect recorded events.
- Integrate with your analytics backend for dual logging.

## Inspection and history

Events appear newest first. New arrivals do not automatically scroll away from the event you are inspecting; **Latest events** returns to the top. Search matches tags and screens, ignoring case. Expand the pinned bottom panel for category and time filters; the toggle keeps their active count visible when collapsed. Limited time windows refresh every five seconds. Counts reflect the filtered results. Clear filters resets category and time selection; the no-results action clears search too.

Tap a row to see its full timestamp with milliseconds and timezone, event type and sorted parameters. Copy event writes that data to the system clipboard. Returning to the list preserves search, filters and the inspected event’s scroll offset, even when events arrive while details are open. If that row is no longer available, the list starts at the top. If the event has been cleared or is unavailable after restoration, the detail page shows an empty state.

`AnalyticsLogger.logs: List<AnalyticsLog>` is a read-only snapshot, observable when read in composition. Do not remember a single snapshot if you need live updates. Record through `AnalyticsLogger.log()`; direct list writes are no longer supported. Each recording gets a fresh internal UUID without changing value equality, and the logger copies the parameter map. Events are in memory only.

The module owns its list and detail destinations. Integrators choose how DevView is presented, not how its internal screens are composed.

### Migration

Remove `CompositionLocalProvider(LocalAnalytics provides ...)`: `LocalAnalytics` has been removed. Register `Analytics()` through `rememberModules` and render `DevView`; direct screen calls are no longer supported. Replace mutations of `AnalyticsLogger.logs` with `AnalyticsLogger.log()`.

## Best Practices
1. Use descriptive tags and include screen context.
2. Choose a type such as `AnalyticsLogCategory.Screen.View`, `.Action.Click` or `.Custom.Event`.
   Avoid credentials and personal data in parameters, especially when copying events.
3. Log to DevView only in debug builds for best performance.
4. Clear logs periodically during long sessions.

## Troubleshooting / FAQ
- **Why aren't my events showing up?**
  - Ensure you use `AnalyticsLogger.log()` and provide the correct event type.
  - Confirm Compose state is correctly updated and the UI is observing the logs.
- **How do I clear logs?**
  - Use the Clear logs toolbar action. `AnalyticsLogger.clear()` is internal.
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

