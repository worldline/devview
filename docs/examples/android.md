# Android Setup Example

Complete Android integration example.

> _[Placeholder: Insert screenshot of DevView running in an Android app. Use a device frame if relevant.]_

## Prerequisites
- Android Studio Giraffe or newer
- Minimum API level 21
- Kotlin Multiplatform and Compose Multiplatform configured

## Step 1: Add Dependencies
Add DevView and required modules to your `build.gradle.kts`:
```kotlin
dependencies {
    implementation(projects.devview)
    implementation(projects.devviewFeatureflip)
    implementation(projects.devviewAnalytics)
    implementation(projects.devviewNetworkmock)
}
```

## Step 2: MainActivity Setup
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                App()
            }
        }
    }
}
```

## Step 3: Register Modules and Add DevView
```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics)
        module(NetworkMock)
        // Add custom modules if needed
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MainContent()
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

## Step 4: Test and Customise
- Run your app on an emulator or device
- Open DevView using the floating action button
- Customise modules and theming as needed

## Troubleshooting
- If DevView does not appear, check your composable tree and state logic
- For build errors, verify dependency versions and Gradle configuration
- For module issues, ensure all modules are registered and initialised before rendering DevView

## Next Steps
- See [iOS Setup](ios.md) for cross-platform integration
- Explore [Feature Flags](feature-flags.md) and [Analytics Tracking](analytics-tracking.md) for more examples

---

_If you encounter issues not covered here, consult the [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) or open an issue on the DevView repository._
