# Feature Flags Example

Managing feature toggles with FeatureFlip.

> _[Placeholder: Insert screenshot of FeatureFlip UI showing feature toggles. Use a device frame if relevant.]_

## Step 1: Define Features
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

## Step 2: Register Features and Add FeatureFlip Module
```kotlin
val features = listOf(darkMode, newCheckout)
val modules = rememberModules {
    module(FeatureFlip)
    // ...other modules...
}
```

## Step 3: Use FeatureFlipScreen in Your Composable
```kotlin
CompositionLocalProvider(LocalFeatures provides features) {
    FeatureFlipScreen(onStateChange = { featureName, newState ->
        // Handle state change
    })
}
```

## Step 4: Test Feature Flags
- Open DevView and navigate to the FeatureFlip module
- Toggle features on and off
- Verify changes in your app's behaviour

## Using Features in Your App
```kotlin
@Composable
fun MyScreen() {
    val featureHandler = LocalFeatureHandler.current
    val isDarkMode by featureHandler.isFeatureEnabled("dark_mode")
    if (isDarkMode) {
        DarkTheme { Content() }
    } else {
        LightTheme { Content() }
    }
}
```

## Troubleshooting
- If feature state does not persist, check DataStore initialisation
- For remote features, ensure remote config is fetched and state is set to REMOTE
- For UI issues, verify Compose state and feature list updates

## Next Steps
- See [FeatureFlip Module](../modules/featureflip.md) for more details
- See [Analytics Tracking](analytics-tracking.md) for monitoring feature usage
- Explore [Advanced Examples](advanced-examples.md) for custom feature flag scenarios

---

_If you encounter issues not covered here, consult the [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) or open an issue on the DevView repository._
