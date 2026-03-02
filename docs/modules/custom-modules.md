﻿# Creating Custom Modules

DevView is built around a modular architecture. The **core module** provides the foundational interfaces and registry, while all real functionality is implemented as modules—including your own custom modules.

## Architecture Overview

- **Core Module**: Defines the `Module` interface, `Section` enum, and module registry. It does not provide features itself.
- **Feature Modules**: Implement the `Module` interface to provide developer tools (e.g., FeatureFlip, Analytics, NetworkMock, or your custom modules).

## Core Interfaces

### Module Interface
```kotlin
interface Module {
    val moduleName: String
    val section: Section
    val icon: ImageVector
    val containerColor: Color
    val contentColor: Color
    val subtitle: String?
    val destinations: ImmutableList<NavKey>
    val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit
    fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    )
}
```

### Section Enum
```kotlin
enum class Section {
    SETTINGS,   // Configuration and app info
    FEATURES,   // Feature flags and dev tools
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

> _[Placeholder: Insert screenshot or diagram of a custom module UI. Use a device frame if relevant.]_

## Examples
See [Examples section](../examples/index.md) for complete custom module examples.

## Customisation
> _[Placeholder: Guide for customising the appearance and behaviour of custom modules. This section will be expanded in future updates.]_

## Advanced Usage
> _[Placeholder: Advanced scenarios such as multi-screen modules, platform-specific customisation, and complex navigation.]_

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
> _[API reference available via Dokka. Add direct link here when available.]_
