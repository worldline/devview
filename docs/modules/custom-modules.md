# Creating Custom Modules
Extend DevView with your own custom modules for app-specific developer tools.
## Module Structure
A DevView module consists of:
1. **Destinations** - Navigation screens
2. **Serializers** - For type-safe navigation
3. **Content** - Composable UI
4. **Metadata** - Name, icon, section
## Step-by-Step Guide
### 1. Define Destinations
```kotlin
sealed interface MyToolDestination : NavKey {
    @Serializable
    data object Main : MyToolDestination
    @Serializable
    data class Detail(val id: String) : MyToolDestination
}
```
### 2. Create Module Object
```kotlin
object MyTool : Module {
    override val moduleName = "My Tool"
    override val section = Section.CUSTOM
    override val subtitle = "Custom developer tool"
    override val destinations = persistentListOf(
        MyToolDestination.Main
    )
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
        subclass(MyToolDestination.Main::class, MyToolDestination.Main.serializer())
        subclass(MyToolDestination.Detail::class, MyToolDestination.Detail.serializer())
    }
    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp,
    ) {
        entry<MyToolDestination.Main> {
            MyToolMainScreen(
                onNavigateBack = onNavigateBack,
                onDetailClick = { id ->
                    onNavigate(MyToolDestination.Detail(id))
                }
            )
        }
        entry<MyToolDestination.Detail> { destination ->
            MyToolDetailScreen(
                id = destination.id,
                onNavigateBack = onNavigateBack
            )
        }
    }
}
```
### 3. Create UI
```kotlin
@Composable
fun MyToolMainScreen(
    onNavigateBack: () -> Unit,
    onDetailClick: (String) -> Unit
) {
    Scaffold {
        Column(modifier = Modifier.padding(it)) {
            Text("My Custom Tool")
            Button(onClick = { onDetailClick("123") }) {
                Text("View Details")
            }
        }
    }
}
```
### 4. Register Module
```kotlin
val modules = rememberModules {
    module(MyTool)
    module(FeatureFlip)
}
```
## Examples
See [Examples section](../examples/index.md) for complete custom module examples.
