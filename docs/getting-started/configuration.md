# Configuration

Advanced configuration options for DevView.

> _[Placeholder: Insert screenshot of DevView configuration UI or module selection. Use a device frame if relevant.]_

## Module Configuration

### Conditional Modules
Dynamically include modules based on build type or feature flags.

```kotlin
val modules = rememberModules {
    module(AppInfo)
    if (BuildConfig.DEBUG) {
        module(DebugTools)
    }
    if (isFeatureEnabled("dev_tools")) {
        module(FeatureFlip)
    }
    if (isNetworkMockEnabled) {
        module(NetworkMock) // Optional: Add network mocking for development/testing
    }
}
```

## Opening DevView

### Gesture Detection
Open DevView using custom gestures for developer convenience.

```kotlin
Box(
    modifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    tapCount++
                    if (tapCount >= 3) {
                        isDevViewOpen = true
                    }
                }
            )
        }
)
```

### Debug Menu
Provide a quick-access button for DevView in debug builds.

```kotlin
if (Build.DEBUG) {
    IconButton(onClick = { isDevViewOpen = true }) {
        Icon(Icons.Default.DeveloperMode, null)
    }
}
```

## Customisation

### Module Appearance
Customise the look and feel of your modules to match your app's theme.

```kotlin
object MyModule : Module {
    override val moduleName = "My Custom Tool"
    override val section = Section.CUSTOM
    override val icon = Icons.Default.Build
    override val containerColour = Colour(0xFFFF5722)
    override val subtitle = "Custom developer tool"
}
```

## Custom Modules

> _[Placeholder: Guide for developing custom DevView modules. This section will be expanded in future updates.]_

## Troubleshooting

### Common Issues

#### DevView not appearing
- Verify that you've wrapped your content with `DevView` composable
- Check that the `isOpen` state is being set to `true`
- Ensure all required modules are properly initialised
- For gesture-based opening, verify gesture detection is properly configured

#### Modules not showing up
- Confirm modules are added to the `rememberModules` block
- Check that conditional modules meet their requirements (e.g., DEBUG mode)
- Verify module dependencies are included in your build.gradle
- Ensure module initialisation happens before DevView is rendered
- For Network Mock, ensure `isNetworkMockEnabled` is set and the module is included
- For custom modules, verify your implementation matches the required interface

#### Build errors
- Make sure you've added the correct dependency for your platform
- Verify that you're using compatible versions of Kotlin and the library
- Check that Compose Multiplatform is properly configured
- Clean and rebuild your project: `./gradlew clean build`

#### Styling issues
- Ensure you're using Material3 components and theme
- Check that custom colours are properly defined
- Verify containerColour values are valid Colour objects
- Test on both light and dark themes

#### Feature flags not working
- Verify FeatureFlip module is included in your dependencies
- Check that feature flag keys match exactly (case-sensitive)
- Ensure remote config has been fetched if using remote sources
- For testing, consider using local overrides

[Explore Modules →](../modules/index.md)
