# Analytics Tracking Example
Monitoring analytics events with the Analytics module.
## Logging Events
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
## Displaying Events
```kotlin
@Composable
fun MyApp() {
    CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
        AnalyticsScreen()
    }
}
```
See [Analytics Module](../modules/analytics.md)
