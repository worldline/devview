# iOS Setup Example
Complete iOS integration example.
## ContentView (SwiftUI)
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
## Kotlin Code
```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics)
    }
    DevView(
        devViewIsOpen = isDevViewOpen,
        closeDevView = { isDevViewOpen = false },
        modules = modules
    )
}
```
See [Quick Start](../getting-started/quick-start.md)
