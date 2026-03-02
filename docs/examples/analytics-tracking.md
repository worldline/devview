# Analytics Tracking Example

Monitoring analytics events with the Analytics module.

> _[Placeholder: Insert screenshot of Analytics UI showing tracked events. Use a device frame if relevant.]_

## Step 1: Log Analytics Events
```kotlin
// Screen view
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "HomeScreen",
        screenClass = "com.app.HomeScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogType.SCREEN
    )
)
// User event
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "button_click",
        screenClass = "HomeScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogType.EVENT
    )
)
```

## Step 2: Register Analytics Module
```kotlin
val modules = rememberModules {
    module(Analytics)
    // ...other modules...
}
```

## Step 3: Use AnalyticsScreen in Your Composable
```kotlin
@Composable
fun MyApp() {
    CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
        AnalyticsScreen()
    }
}
```

## Step 4: Test Analytics Tracking
- Open DevView and navigate to the Analytics module
- Trigger events in your app and observe them in real time
- Use filtering and search to inspect specific events

## Troubleshooting
- If events do not appear, check that you are using AnalyticsLogger.log() correctly
- For UI issues, verify Compose state and log updates
- For platform-specific issues, check Compose Multiplatform compatibility

## Next Steps
- See [Feature Flags](feature-flags.md) for feature flag usage
- Explore [Advanced Examples](advanced-examples.md) for custom analytics scenarios

---

_If you encounter issues not covered here, consult the [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) or open an issue on the DevView repository._
