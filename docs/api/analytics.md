# Analytics API Reference

Complete API documentation for the DevView Analytics module.

## Overview

The Analytics module provides real-time analytics event monitoring and debugging.

## Key Components

### AnalyticsLog Data Class

```kotlin
@Immutable
data class AnalyticsLog(
    val tag: String,
    val screenClass: String,
    val timestamp: Long,
    val type: AnalyticsLogType
) {
    internal val formattedTimestamp: String
}
```

### AnalyticsLogType Enum

```kotlin
enum class AnalyticsLogType {
    SCREEN,   // Screen view events
    EVENT,    // User interaction events
    CUSTOM    // Custom analytics events
}
```

### AnalyticsLogger Object

```kotlin
object AnalyticsLogger {
    fun log(log: AnalyticsLog)
    
    val logs: SnapshotStateList<AnalyticsLog>
    
    val hasLogs: Boolean
    
    internal fun clear()
}
```

### Composables

```kotlin
@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier)

val LocalAnalytics: ProvidableCompositionLocal<List<AnalyticsLog>>
```

## Usage Example

```kotlin
// Log an analytics event
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "user_login",
        screenClass = "LoginScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogType.EVENT
    )
)

// Display analytics screen
@Composable
fun MyApp() {
    CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
        AnalyticsScreen()
    }
}
```

## Detailed Documentation

All APIs are documented with KDoc comments. Generate HTML docs:

```bash
./gradlew dokkaHtml
```

## See Also

- [Analytics Module Guide](../modules/analytics.md)
- [Analytics Tracking Example](../examples/analytics-tracking.md)
