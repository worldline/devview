﻿# Module Development Guide

In-depth guide for creating DevView modules.

> _[Placeholder: Insert diagram or screenshot of a module development workflow. Use a device frame if relevant.]_

## Module Structure
A DevView module consists of:
1. **Destinations** - Navigation screens
2. **Serialisers** - For type-safe navigation
3. **Content** - Composable UI
4. **Metadata** - Name, icon, section

## Step-by-Step: Creating a Module

### 1. Define Destinations
```kotlin
sealed interface MyModuleDestination : NavKey {
    @Serializable
    data object Main : MyModuleDestination
    @Serializable
    data class Detail(val id: String) : MyModuleDestination
}
```

### 2. Implement the Module Interface
```kotlin
object MyModule : Module {
    override val moduleName = "My Module"
    override val section = Section.CUSTOM
    override val subtitle = "Custom developer tool"
    override val destinations = persistentListOf(
        MyModuleDestination.Main
    )
    override val registerSerialisers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
        subclass(MyModuleDestination.Main::class, MyModuleDestination.Main.serializer())
        subclass(MyModuleDestination.Detail::class, MyModuleDestination.Detail.serializer())
    }
    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    ) {
        entry<MyModuleDestination.Main> {
            MyModuleMainScreen(
                onNavigateBack = onNavigateBack,
                onDetailClick = { id ->
                    onNavigate(MyModuleDestination.Detail(id))
                }
            )
        }
        entry<MyModuleDestination.Detail> { destination ->
            MyModuleDetailScreen(
                id = destination.id,
                onNavigateBack = onNavigateBack
            )
        }
    }
}
```

### 3. Create UI Screens
```kotlin
@Composable
fun MyModuleMainScreen(
    onNavigateBack: () -> Unit,
    onDetailClick: (String) -> Unit
) {
    Scaffold {
        Column(modifier = Modifier.padding(it)) {
            Text("My Module")
            Button(onClick = { onDetailClick("123") }) {
                Text("View Details")
            }
        }
    }
}
```

### 4. Register Your Module
```kotlin
val modules = rememberModules {
    module(MyModule)
    // ...other modules...
}
```

## Customisation
> _[Placeholder: Guide for customising modules. This section will be expanded in future updates.]_

## Architecture Best Practices
- Keep modules focused and single-purpose
- Use proper state management
- Document all public APIs
- Provide preview samples
- Test on both platforms
- Use descriptive names and icons for your module
- Group related screens under a single module

## Advanced Scenarios
> _[Placeholder: Multi-screen modules, platform-specific customisation, complex navigation, and advanced UI patterns.]_

## Troubleshooting
- **Module not appearing?** Ensure it is registered and implements the required interface.
- **Navigation issues?** Check destination and serialiser definitions.
- **UI not rendering?** Verify Compose compatibility and state management.
- **Custom colours or icons not displaying?** Verify your colour and icon definitions are valid and supported by Compose.

## Next Steps
- See [Custom Modules](../modules/custom-modules.md) for more examples.
- Explore [Navigation](navigation.md) for advanced navigation patterns.

## API Reference
> _[API reference available via Dokka. Add direct link here when available.]_
