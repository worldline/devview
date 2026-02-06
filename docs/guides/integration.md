# Integration Guide
Detailed guide for integrating DevView into different types of projects.
## Basic Integration
```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics)
    }
    Box {
        MainContent()
        DevView(
            devViewIsOpen = isDevViewOpen,
            closeDevView = { isDevViewOpen = false },
            modules = modules
        )
    }
}
```
## Opening DevView
### Triple Tap
```kotlin
var tapCount by remember { mutableStateOf(0) }
Box(
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                tapCount++
                if (tapCount >= 3) {
                    isDevViewOpen = true
                    tapCount = 0
                }
            }
        )
    }
)
```
## Best Practices
1. Debug builds only
2. Conditional modules
3. Proper state management
4. Test in both debug and release
See [Quick Start](../getting-started/quick-start.md) and [Best Practices](best-practices.md)
