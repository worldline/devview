# Creating Custom Modules

DevView is built around a modular architecture. The **core module** provides the foundational interfaces and registry, while all real functionality is implemented as modules—including your own custom modules.

## Architecture Overview

- **Core Module**: Defines the `Module` interface, `Section` enum, and module registry. It does not provide features itself.
- **Feature Modules**: Implement the `Module` interface to provide developer tools (e.g., FeatureFlip, Analytics, NetworkMock, or your custom modules).

## Core Interfaces

### Module Interface
```kotlin
interface Module {
    val moduleName: String  // defaults to class simple name
    val section: Section
    val icon: ImageVector   // defaults to section icon
    val containerColor: Color  // defaults to section container color
    val contentColor: Color    // defaults to section content color
    val subtitle: String?   // optional description text, defaults to null
    val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata>
    val entryDestination: NavKey
    val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
    fun initModule() {}     // optional, called once after DataStore init
    fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit,
        bottomPadding: Dp,
    )
}
```

### Section Enum
```kotlin
enum class Section {
    SETTINGS,   // Configuration and app info
    FEATURES,   // Feature flags and dev tools
    NETWORK,    // Network-related modules
    LOGGING,    // Analytics, logs, monitoring
    CUSTOM      // App-specific modules
}
```

### Module Registry
```kotlin
val modules = rememberModules {
    module(MyModule)
    module(FeatureFlip)
    // ...other modules...
}
```

---

## Creating a Custom Module

A DevView module consists of:
1. **Destinations** - Navigation screens
2. **Serializers** - For type-safe navigation
3. **Content** - Composable UI
4. **Metadata** - Name, icon, section

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

    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        MyToolDestination.Main.withTitle("My Tool"),
        MyToolDestination.Detail::class.asDestination()
    )

    override val entryDestination: NavKey = MyToolDestination.Main

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
                onDetailClick = { id -> onNavigate(MyToolDestination.Detail(id)) }
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

## Troubleshooting / FAQ
- **Why isn't my module appearing in DevView?**
  - Ensure your module is registered in the modules list and implements the required interface.
- **Navigation not working?**
  - Confirm your destinations and serializers are correctly defined and registered.
- **Platform-specific issues?**
  - Check for Compose and navigation compatibility on Android and iOS.
- **Custom colours or icons not displaying?**
  - Verify your colour and icon definitions are valid and supported by Compose.

## API Reference
> _[Dokka API Reference](../api/devview/com.worldline.devview.core/-module/index.html)_
