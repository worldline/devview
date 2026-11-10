# Analytics Tracking Example

Monitoring analytics events with the Analytics module.

## Step 1: Log Analytics Events
```kotlin
// Screen view
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "home_screen_view",
        screenClass = "com.app.HomeScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogCategory.Screen.View
    )
)
// User action
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "button_click",
        screenClass = "HomeScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogCategory.Action.Click,
        params = mapOf("source" to "home")
    )
)
```

## Step 2: Register Analytics Module
```kotlin
val modules = rememberModules {
    module(Analytics()) // constructor params customize summary chip types
    // ...other modules...
}
```

## Step 3: Use AnalyticsScreen in Your Composable
```kotlin
@Composable
fun MyApp() {
    // AnalyticsScreen is rendered automatically when Analytics() is registered in rememberModules.
    // To use it standalone:
    AnalyticsScreen(
        highlightedAnalyticsLogTypes = persistentListOf(
            AnalyticsLogCategory.Action.Click,
            AnalyticsLogCategory.Performance.Error
        )
    )
}
```

## Step 4: Test Analytics Tracking
- Open DevView and navigate to the Analytics module
- Trigger events in your app and observe them in real time
- Use filtering and search to inspect specific events
- Tap an event to inspect parameters and copy its full details

The logger exposes read-only snapshots. Custom standalone sources pass `logs = yourList`; no CompositionLocal provider is needed. Replace lists/maps when updating custom sources. Sample events should be recorded from an effect or event handler, not directly during composition.

## Troubleshooting
- If events do not appear, check that you are using AnalyticsLogger.log() correctly
- For UI issues, verify Compose state and log updates
- For platform-specific issues, check Compose Multiplatform compatibility

## Next Steps
- See [Feature Flags](feature-flags.md) for feature flag usage
- Explore [Advanced Examples](advanced-examples.md) for custom analytics scenarios

---

_If you encounter issues not covered here, consult the [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) or open an issue on the DevView repository._
