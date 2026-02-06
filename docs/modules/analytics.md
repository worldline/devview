# DevView Analytics Module

A Kotlin Multiplatform library for capturing and visualizing analytics events in real-time, providing a developer-friendly interface for monitoring and debugging analytics integration in your application.

## Features

- 📊 **Multi-Type Event Tracking**: Support for Screen views, Custom events, and general Event tracking
- 🔍 **Real-Time Monitoring**: View analytics logs as they happen in your application
- 📝 **Detailed Event Information**: Inspect event tags, screen classes, and timestamps
- 🎨 **Built-in UI**: Ready-to-use Compose Multiplatform UI components with:
  - Tabular log display with headers
  - Timestamp formatting (HH:mm:ss)
  - Event type visualization
  - Auto-sizing columns
- 🔧 **Type-Safe API**: Leverages Kotlin's type system for compile-time safety
- 📱 **Multiplatform**: Supports Android and iOS
- 💾 **In-Memory Storage**: Lightweight logging with snapshot state management

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(projects.devviewAnalytics)
}
```

## Quick Start

### Logging Analytics Events

```kotlin
import com.worldline.devview.analytics.AnalyticsLogger
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogType

// Log a screen view
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "HomeScreen",
        screenClass = "com.example.ui.HomeScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogType.SCREEN
    )
)

// Log a custom event
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "user_login",
        screenClass = "LoginScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogType.EVENT
    )
)

// Log a custom analytics event
AnalyticsLogger.log(
    AnalyticsLog(
        tag = "purchase_completed",
        screenClass = "CheckoutScreen",
        timestamp = System.currentTimeMillis(),
        type = AnalyticsLogType.CUSTOM
    )
)
```

### Using the UI

```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import com.worldline.devview.analytics.AnalyticsScreen
import com.worldline.devview.analytics.LocalAnalytics
import com.worldline.devview.analytics.AnalyticsLogger

@Composable
fun MyApp() {
    // Provide analytics logs to the screen
    CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
        AnalyticsScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

## Analytics Log Types

The module supports three types of analytics logs:

- **`AnalyticsLogType.SCREEN`**: Track screen/page views in your application
- **`AnalyticsLogType.EVENT`**: Track user interactions and general events
- **`AnalyticsLogType.CUSTOM`**: Track custom analytics events specific to your needs

## Data Models

### AnalyticsLog

The core data model for analytics events:

```kotlin
data class AnalyticsLog(
    val tag: String,              // Event name/identifier
    val screenClass: String,      // Screen or component where event occurred
    val timestamp: Long,          // Unix timestamp in milliseconds
    val type: AnalyticsLogType   // Type of analytics event
)
```

The `AnalyticsLog` class automatically formats timestamps for display in HH:mm:ss format based on the current system timezone.

## API Reference

### Core Types

- **`AnalyticsLog`**: Data class representing a single analytics event
- **`AnalyticsLogType`**: Enum defining event types (SCREEN, EVENT, CUSTOM)
- **`AnalyticsLogger`**: Singleton object for managing analytics logs

### AnalyticsLogger

The central logging manager:

```kotlin
// Log an analytics event
AnalyticsLogger.log(analyticsLog: AnalyticsLog)

// Access all logged events
val logs: SnapshotStateList<AnalyticsLog> = AnalyticsLogger.logs

// Check if any logs exist
val hasLogs: Boolean = AnalyticsLogger.hasLogs

// Clear all logs (internal use)
AnalyticsLogger.clear()
```

### Composables

- **`AnalyticsScreen(modifier: Modifier)`**: Main UI component displaying analytics logs in a table format
- **`LocalAnalytics`**: CompositionLocal for providing analytics logs to the screen

## Integration Examples

### Integration with Firebase Analytics

```kotlin
import com.google.firebase.analytics.FirebaseAnalytics
import com.worldline.devview.analytics.AnalyticsLogger
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogType

class AnalyticsWrapper(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    fun logScreenView(screenName: String, screenClass: String) {
        // Send to Firebase
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        
        // Log to DevView for debugging
        AnalyticsLogger.log(
            AnalyticsLog(
                tag = screenName,
                screenClass = screenClass,
                timestamp = System.currentTimeMillis(),
                type = AnalyticsLogType.SCREEN
            )
        )
    }
    
    fun logEvent(eventName: String, screenClass: String) {
        // Send to Firebase
        firebaseAnalytics.logEvent(eventName, null)
        
        // Log to DevView for debugging
        AnalyticsLogger.log(
            AnalyticsLog(
                tag = eventName,
                screenClass = screenClass,
                timestamp = System.currentTimeMillis(),
                type = AnalyticsLogType.EVENT
            )
        )
    }
}
```

### Integration with Custom Analytics Service

```kotlin
class MyAnalyticsService {
    fun trackEvent(
        eventName: String,
        screenClass: String,
        eventType: AnalyticsLogType = AnalyticsLogType.EVENT
    ) {
        // Send to your analytics backend
        sendToBackend(eventName, screenClass)
        
        // Log to DevView for debugging (only in debug builds)
        if (BuildConfig.DEBUG) {
            AnalyticsLogger.log(
                AnalyticsLog(
                    tag = eventName,
                    screenClass = screenClass,
                    timestamp = System.currentTimeMillis(),
                    type = eventType
                )
            )
        }
    }
    
    private fun sendToBackend(eventName: String, screenClass: String) {
        // Your analytics backend implementation
    }
}
```

### Using with Jetpack Compose Navigation

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    // Track screen views automatically
    navController.currentBackStackEntryAsState().value?.destination?.route?.let { route ->
        LaunchedEffect(route) {
            AnalyticsLogger.log(
                AnalyticsLog(
                    tag = route,
                    screenClass = route,
                    timestamp = System.currentTimeMillis(),
                    type = AnalyticsLogType.SCREEN
                )
            )
        }
    }
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen() }
        composable("details") { DetailsScreen() }
    }
}
```

## Architecture

### Logging Flow

1. **Event Occurs**: User action or screen view happens in your app
2. **Log Creation**: Create an `AnalyticsLog` instance with event details
3. **Logger Storage**: `AnalyticsLogger.log()` adds the event to in-memory storage
4. **UI Update**: `AnalyticsScreen` automatically updates via Compose state
5. **Real-time Display**: Events appear immediately in the analytics UI

### State Management

The module uses Jetpack Compose's `mutableStateListOf` for reactive state management:
- **Thread-safe**: Snapshot state list handles concurrent modifications
- **Reactive**: UI automatically recomposes when logs are added
- **Lightweight**: In-memory storage with no disk I/O overhead

### UI Components

```
AnalyticsScreen
├── AnalyticsLogHeader (sticky header)
│   └── Column headers with type-specific width
├── LazyColumn (scrollable list)
│   └── AnalyticsLogItem (for each log)
│       ├── Formatted timestamp
│       ├── Event type indicator
│       ├── Tag/event name
│       └── Screen class
└── HorizontalDivider (between items)
```

## Best Practices

1. **Use Descriptive Tags**: Make event names self-documenting (e.g., `user_clicked_checkout` instead of `event_1`)
2. **Include Screen Context**: Always provide the screen class where the event occurred
3. **Consistent Event Types**: Use SCREEN for navigation, EVENT for interactions, CUSTOM for business-specific events
4. **Debug-Only Logging**: Consider logging to DevView only in debug builds to avoid memory overhead in production
5. **Clear When Needed**: Clear logs periodically if testing for extended periods to avoid memory buildup
6. **Combine with Real Analytics**: Use alongside your production analytics service for verification

## Use Cases

### Development
Monitor analytics events during development to ensure:
- Events fire at the correct time
- Event names follow naming conventions
- Screen tracking works as expected

### QA Testing
Verify analytics implementation by:
- Checking event sequences in user flows
- Validating event timing
- Confirming screen view tracking accuracy

### Debugging
Troubleshoot analytics issues:
- Verify events are being created
- Check event data before it's sent to analytics services
- Identify missing or duplicate events

### Documentation
Use the analytics screen to:
- Generate a list of all tracked events
- Document event names and types
- Create test plans for QA teams

## Requirements

- Kotlin 2.3.0+
- Compose Multiplatform 1.10.0+
- kotlinx-datetime for timestamp formatting
- Android: API 21+
- iOS: iOS 14+

## Integration with DevView

The Analytics module seamlessly integrates with the core DevView library:
- **Section**: Part of the LOGGING section
- **Navigation**: Accessible via `AnalyticsDestination.Main`
- **UI Consistency**: Uses Material Design 3 components
- **Modular Architecture**: Works standalone or as part of DevView suite

## Platform-Specific Notes

### Android

- Uses Compose `mutableStateListOf` for state management
- Timestamp formatting respects device timezone
- No special permissions required

### iOS

- Full Compose Multiplatform support
- Timestamp formatting respects device timezone
- No special entitlements required

## Documentation

All public APIs are documented with KDoc comments. View the documentation:
- In your IDE using Quick Documentation (Ctrl+Q / Cmd+J)
- Generate HTML docs using Dokka: `./gradlew dokkaHtml`

## License

This module is part of the DevView project and follows the same licensing terms.

## Contributing

Contributions are welcome! Please read the main DevView contributing guidelines before submitting pull requests.
