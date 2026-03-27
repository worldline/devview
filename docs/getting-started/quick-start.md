# Quick Start Guide

Get DevView up and running in your application in just a few minutes!

> _[Placeholder: Insert screenshot of DevView UI in an app. Use a device frame if relevant.]_

## Step 1: Create Your App Structure
Set up your main app structure and state for DevView.

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
Configure which DevView modules you want to use.

```kotlin
val modules = rememberModules {
    module(FeatureFlip)
    module(Analytics)
    module(NetworkMock) // Optional: Add network mocking
}
```

## Step 3: Add DevView
Add DevView to your composable tree and wire up open/close logic.

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
import com.worldline.devview.networkmock.NetworkMock

@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics)
        module(NetworkMock)
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

## Custom Modules

> _[Placeholder: Describe how to add custom DevView modules. This section will be expanded in future updates.]_

## Troubleshooting
For common integration issues, see the [troubleshooting section](troubleshooting-faq.md).

[Next: Configuration →](configuration.md)
