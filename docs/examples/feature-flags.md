# Feature Flags Example
Managing feature toggles with FeatureFlip.
## Defining Features
```kotlin
val features = listOf(
    Feature.LocalFeature(
        name = "dark_mode",
        description = "Enable dark theme",
        isEnabled = false
    ),
    Feature.RemoteFeature(
        name = "new_checkout",
        description = "New checkout flow",
        defaultRemoteValue = true,
        state = FeatureState.REMOTE
    )
)
```
## Using Features
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
See [FeatureFlip Module](../modules/featureflip.md)
