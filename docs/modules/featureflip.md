# DevView FeatureFlip Module

A Kotlin Multiplatform library for managing feature flags (feature toggles) with a built-in UI for toggling and managing features at runtime.

## Features

- ✨ **Two Feature Types**
  - **Local Features**: Simple on/off flags stored on the device
  - **Remote Features**: Flags controlled by remote configuration with local override capability
  
- 💾 **Persistent Storage**: Automatic state persistence using DataStore (works on Android and iOS)

- 🎨 **Built-in UI**: Ready-to-use Compose Multiplatform UI components
  - Search functionality
  - Filter by feature type (Local/Remote) and state (On/Off)
  - Intuitive switches and tri-state controls
  - Material Design 3 styling

- 🔧 **Type-Safe API**: Leverages Kotlin's type system for compile-time safety

- 📱 **Multiplatform**: Supports Android and iOS

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(projects.devviewFeatureflip)
}
```

## Quick Start

### Defining Features

```kotlin
import com.worldline.devview.featureflip.model.Feature
import com.worldline.devview.featureflip.model.FeatureState

// Local feature - simple on/off toggle
val darkMode = Feature.LocalFeature(
    name = "dark_mode",
    description = "Enable dark theme",
    isEnabled = false
)

// Remote feature - with remote config and local override
val newCheckout = Feature.RemoteFeature(
    name = "new_checkout_flow",
    description = "Enable the redesigned checkout experience",
    defaultRemoteValue = true,  // Value from remote config
    state = FeatureState.REMOTE  // Currently using remote value
)
```

### Using the UI

```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import com.worldline.devview.featureflip.FeatureFlipScreen
import com.worldline.devview.featureflip.LocalFeatures

@Composable
fun MyApp() {
    val features = remember {
        listOf(
            Feature.LocalFeature(
                name = "dark_mode",
                description = "Enable dark theme",
                isEnabled = false
            ),
            Feature.RemoteFeature(
                name = "new_feature",
                description = "Our awesome new feature",
                defaultRemoteValue = true,
                state = FeatureState.REMOTE
            )
        )
    }

    CompositionLocalProvider(LocalFeatures provides features) {
        FeatureFlipScreen(
            onStateChange = { featureName, newState ->
                // Handle state change
                println("Feature '$featureName' changed to $newState")
            }
        )
    }
}
```

## Feature States

Remote features support three states:

- **`FeatureState.REMOTE`**: Use the default value from remote configuration
- **`FeatureState.LOCAL_ON`**: Override to force the feature ON locally
- **`FeatureState.LOCAL_OFF`**: Override to force the feature OFF locally

## Managing Feature State Programmatically

```kotlin
import com.worldline.devview.featureflip.model.FeatureHandler
import com.worldline.devview.featureflip.model.createDataStore

// Create a DataStore instance
val dataStore = createDataStore { "/path/to/datastore" }

// Create a handler
val handler = FeatureHandler(dataStore)

// Add features
handler.addFeatures(features)

// Check if a feature is enabled
handler.isFeatureEnabled("dark_mode")
    .collect { isEnabled ->
        println("Dark mode is ${if (isEnabled) "on" else "off"}")
    }

// Change a feature's state
handler.setFeatureState("new_feature", FeatureState.LOCAL_ON)

// Get all features with current state
handler.getFeatures()
    .collect { features ->
        features.forEach { feature ->
            println("${feature.name}: ${feature.isEnabled}")
        }
    }
```

## Architecture

### Data Models

```
Feature (sealed class)
├── LocalFeature
│   ├── name: String
│   ├── description: String?
│   └── isEnabled: Boolean
│
└── RemoteFeature
    ├── name: String
    ├── description: String?
    ├── defaultRemoteValue: Boolean
    └── state: FeatureState
```

### Feature State Flow

1. **Remote Features**: 
   - Start with a `defaultRemoteValue` from your remote config service
   - Can be overridden locally using `FeatureState.LOCAL_ON` or `LOCAL_OFF`
   - Reset to remote value with `FeatureState.REMOTE`

2. **Local Features**: 
   - Simple boolean enabled/disabled state
   - Stored locally using DataStore

### Persistence

The module uses Jetpack DataStore (preferences) for persisting feature states:
- **Android**: Stored in the app's files directory
- **iOS**: Stored in the app's document directory

## UI Components

### FeatureFlipScreen

The main screen component that displays all features with search and filter capabilities.

**Features:**
- Search by feature name
- Filter by type (Local/Remote)
- Filter by state (On/Off)
- Automatic UI updates when states change

### Feature Controls

- **Local Features**: Display a standard Material Switch
- **Remote Features**: Display a tri-state segmented button:
  - 🌐 Remote (cloud icon) - Use remote configuration
  - ❌ Off (cancel icon) - Force feature off
  - ✅ On (check icon) - Force feature on

## API Reference

For the complete generated API docs, see the [Dokka API Reference](../api/devview-featureflip/index.html).

### Core Types

- **`Feature`**: Sealed class representing a feature flag
- **`Feature.LocalFeature`**: A locally-managed feature
- **`Feature.RemoteFeature`**: A remotely-configured feature with local override
- **`FeatureState`**: Enum for remote feature states (REMOTE, LOCAL_ON, LOCAL_OFF)
- **`FeatureType`**: Enum for feature types (LOCAL, REMOTE)

### Main Functions

- **`createDataStore(producePath: () -> String)`**: Creates a DataStore instance
- **`FeatureHandler.addFeatures(features: List<Feature>)`**: Registers features
- **`FeatureHandler.isFeatureEnabled(featureName: String)`**: Checks feature state
- **`FeatureHandler.setFeatureState(featureName: String, state: FeatureState)`**: Updates feature state
- **`FeatureHandler.getFeatures()`**: Retrieves all features with current state

### Composables

- **`FeatureFlipScreen(onStateChange: (String, FeatureState) -> Unit, modifier: Modifier)`**: Main UI
- **`LocalFeatures`**: CompositionLocal for providing features to the screen

## Example: Integration with Remote Config

```kotlin
// Fetch from your remote config service
val remoteConfig = fetchRemoteConfig()

val features = listOf(
    Feature.RemoteFeature(
        name = "premium_features",
        description = "Enable premium tier features",
        defaultRemoteValue = remoteConfig.getBoolean("premium_features"),
        state = FeatureState.REMOTE
    ),
    Feature.RemoteFeature(
        name = "experimental_ui",
        description = "New experimental UI components",
        defaultRemoteValue = remoteConfig.getBoolean("experimental_ui"),
        state = FeatureState.REMOTE
    )
)

// Users can override remotely configured features in the UI if needed for testing
```

## Best Practices

1. **Use descriptive names**: Make feature names self-documenting (e.g., `new_payment_flow` instead of `feature_1`)
2. **Add descriptions**: Help users understand what each feature does
3. **Start with REMOTE**: Let remote config control features initially, override only when testing
4. **Persist state**: Use `FeatureHandler` to ensure states survive app restarts
5. **Monitor overrides**: Track which features users are overriding to identify issues with remote configuration

## Platform-Specific Notes

### Android

The DataStore file is created at:
```
{appFilesDir}/feature_flip_datastore.preferences_pb
```

### iOS

The DataStore file is created at:
```
{documentDirectory}/feature_flip_datastore.preferences_pb
```

## Documentation

All public APIs are documented with KDoc comments. View the documentation:
- In your IDE using Quick Documentation (Ctrl+Q / Cmd+J)
- Generate HTML docs using Dokka: `./gradlew dokkaHtml`

## License

See the main project LICENSE file.

## Contributing

Contributions are welcome! Please see the main project README for contribution guidelines.
