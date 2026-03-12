# Core API Reference

Complete API documentation for the DevView Core module.

## Overview

The Core module provides the foundation for DevView, including:

- Module system and interface
- Navigation infrastructure
- Section organization
- Home screen UI components

## Key Components

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
    LOGGING,    // Analytics, logs, monitoring
    CUSTOM      // App-specific modules
}
```

### Module Registry

```kotlin
class ModuleRegistry {
    fun module(module: Module): ModuleRegistry
    fun modules(vararg modules: Module): ModuleRegistry
    fun build(): ImmutableList<Module>
}

fun buildModules(block: ModuleRegistry.() -> Unit): ImmutableList<Module>

@Composable
fun rememberModules(block: ModuleRegistry.() -> Unit): ImmutableList<Module>
```

### DevView Composable

```kotlin
@Composable
fun DevView(
    devViewIsOpen: Boolean,
    closeDevView: () -> Unit,
    modules: ImmutableList<Module>,
    modifier: Modifier = Modifier
)
```

## Detailed Documentation

All APIs are documented with KDoc comments in the source code. To generate HTML documentation:

```bash
./gradlew dokkaHtml
```

The generated documentation will be available in `build/dokka/html/`.

## See Also

- [Module Development Guide](../guides/module-development.md)
- [Creating Custom Modules](../modules/custom-modules.md)
- [Navigation Guide](../guides/navigation.md)
