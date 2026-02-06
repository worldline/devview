# Configuration
Advanced configuration options for DevView.
## Module Configuration
### Conditional Modules
```kotlin
val modules = rememberModules {
    module(AppInfo)
    if (BuildConfig.DEBUG) {
        module(DebugTools)
    }
    if (isFeatureEnabled("dev_tools")) {
        module(FeatureFlip)
    }
}
```
## Opening DevView
### Gesture Detection
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
```kotlin
if (Build.DEBUG) {
    IconButton(onClick = { isDevViewOpen = true }) {
        Icon(Icons.Default.DeveloperMode, null)
    }
}
```
## Customization
### Module Appearance
```kotlin
object MyModule : Module {
    override val moduleName = "My Custom Tool"
    override val section = Section.CUSTOM
    override val icon = Icons.Default.Build
    override val containerColor = Color(0xFFFF5722)
    override val subtitle = "Custom developer tool"
}
```
}
```

## Troubleshooting

### Common Issues

#### DevView not appearing

- Verify that you've wrapped your content with `DevView` composable
- Check that the `isOpen` state is being set to `true`
- Ensure all required modules are properly initialized
- For gesture-based opening, verify gesture detection is properly configured

#### Modules not showing up

- Confirm modules are added to the `rememberModules` block
- Check that conditional modules meet their requirements (e.g., DEBUG mode)
- Verify module dependencies are included in your build.gradle
- Ensure module initialization happens before DevView is rendered

#### Build errors

- Make sure you've added the correct dependency for your platform
- Verify that you're using compatible versions of Kotlin and the library
- Check that Compose Multiplatform is properly configured
- Clean and rebuild your project: `./gradlew clean build`

#### Styling issues

- Ensure you're using Material3 components and theme
- Check that custom colors are properly defined
- Verify containerColor values are valid Color objects
- Test on both light and dark themes

#### Feature flags not working

- Verify FeatureFlip module is included in your dependencies
- Check that feature flag keys match exactly (case-sensitive)
- Ensure remote config has been fetched if using remote sources
- For testing, consider using local overrides

[Explore Modules →](../modules/index.md)
