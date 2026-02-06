# Quick Start Guide
Get DevView up and running in your application in just a few minutes!
## Step 1: Create Your App Structure
```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    Box {
        // Your main app content
        MainAppContent()
        // Add DevView here
    }
}
```
## Step 2: Configure Modules
```kotlin
val modules = rememberModules {
    module(FeatureFlip)
    module(Analytics)
}
```
## Step 3: Add DevView
```kotlin
DevView(
    devViewIsOpen = isDevViewOpen,
    closeDevView = { isDevViewOpen = false },
    modules = modules
)
```
## Complete Example
```kotlin
import com.worldline.devview.DevView
import com.worldline.devview.core.rememberModules
import com.worldline.devview.featureflip.FeatureFlip
import com.worldline.devview.analytics.Analytics
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics)
    }
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
[Next: Configuration →](configuration.md)
