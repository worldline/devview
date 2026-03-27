# iOS Setup Example

Complete iOS integration example.

> _[Placeholder: Insert screenshot of DevView running in an iOS app. Use a device frame if relevant.]_

## Prerequisites
- Xcode 15 or newer
- Minimum iOS 14.0
- Kotlin Multiplatform and Compose Multiplatform configured

## Step 1: Add Dependencies
Add DevView and required modules to your shared module's build.gradle.kts:
```kotlin
dependencies {
    implementation(projects.devview)
    implementation(projects.devviewFeatureflip)
    implementation(projects.devviewAnalytics)
    implementation(projects.devviewNetworkmock)
}
```

## Step 2: SwiftUI ContentView Setup
```swift
import SwiftUI
import shared
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

## Step 3: Register Modules and Add DevView (Kotlin)
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
    DevView(
        devViewIsOpen = isDevViewOpen,
        closeDevView = { isDevViewOpen = false },
        modules = modules
    )
}
```

## Step 4: Test and Customise
- Run your app on a simulator or real device
- Open DevView using your chosen trigger (e.g., button, gesture)
- Customise modules and theming as needed

## Troubleshooting
- If DevView does not appear, check your composable tree and state logic
- For build errors, verify dependency versions and Gradle configuration
- For module issues, ensure all modules are registered and initialised before rendering DevView
- For iOS-specific issues, check Compose Multiplatform and DataStore compatibility

## Next Steps
- See [Android Setup](android.md) for cross-platform integration
- Explore [Feature Flags](feature-flags.md) and [Analytics Tracking](analytics-tracking.md) for more examples

---

_If you encounter issues not covered here, consult the [Troubleshooting & FAQ](../getting-started/troubleshooting-faq.md) or open an issue on the DevView repository._
