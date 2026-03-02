# Integration Guide

This guide walks you through integrating DevView into a new or existing Kotlin Multiplatform project, with platform-specific notes and troubleshooting tips.

> _[Placeholder: Insert screenshot or diagram of DevView integration in a project. Use a device frame if relevant.]_

## Step 1: Prepare Your Project
- Ensure your project is set up for Kotlin Multiplatform and Compose Multiplatform.
- Confirm minimum supported versions (see Prerequisites).

## Step 2: Add DevView Dependencies
Add the required modules to your `build.gradle.kts`:
```kotlin
dependencies {
    implementation(projects.devview)
    // Add modules as needed
    implementation(projects.devviewFeatureflip)
    implementation(projects.devviewAnalytics)
    implementation(projects.devviewNetworkmock)
}
```

## Step 3: Configure DevView Modules
Register the modules you want to use:
```kotlin
val modules = rememberModules {
    module(FeatureFlip)
    module(Analytics)
    module(NetworkMock)
    // Add custom modules if needed
}
```

## Step 4: Add DevView to Your Composable Tree
Insert DevView into your main app composable:
```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules { /* ... */ }
    Box(modifier = Modifier.fillMaxSize()) {
        MainAppContent()
        DevView(
            devViewIsOpen = isDevViewOpen,
            closeDevView = { isDevViewOpen = false },
            modules = modules
        )
        FloatingActionButton(
            onClick = { isDevViewOpen = true }
        ) {
            Icon(Icons.Default.DeveloperMode, null)
        }
    }
}
```

## Platform-Specific Notes
### Android
- Ensure minimum API level 21.
- Use Android Studio Giraffe or newer for best Compose support.
- Test on both light and dark themes.

### iOS
- Ensure minimum iOS 14.0.
- Use Xcode 15+ and latest Compose Multiplatform plugin.
- Test on both simulator and real device.

## Common Pitfalls
- Forgetting to register modules in `rememberModules`.
- Not including required dependencies in `build.gradle.kts`.
- Compose state not updating DevView visibility.
- Platform-specific initialisation issues (DataStore, navigation).

## Troubleshooting
- If DevView does not appear, check your composable tree and state logic.
- For build errors, verify dependency versions and Gradle configuration.
- For module issues, ensure all modules are registered and initialised before rendering DevView.

## Next Steps
- Explore [Module Development](module-development.md) for creating your own modules.
- See [Examples](../examples/index.md) for platform-specific and advanced usage.

---

_If you encounter issues not covered here, consult the [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) or open an issue on the DevView repository._

