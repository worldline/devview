# Android Setup Example
Complete Android integration example.
## MainActivity
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
## App Composable
```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MainContent()
        DevView(
            devViewIsOpen = isDevViewOpen,
            closeDevView = { isDevViewOpen = false },
            modules = modules
        )
    }
}
```
See [Quick Start](../getting-started/quick-start.md)
