# Core Module
The DevView Core module provides the foundation for the entire framework.
## Overview
The core module includes:
- **Module System** - Interface and registry for all modules
- **Navigation** - Type-safe navigation with Navigation3
- **UI Infrastructure** - Home screen and module components
- **Section Organization** - Grouping modules by category
## Architecture
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
### Sections
```kotlin
enum class Section {
    SETTINGS,   // Configuration and app info
    FEATURES,   // Feature flags and dev tools
    LOGGING,    // Analytics, logs, monitoring
    CUSTOM      // App-specific modules
}
```
## Creating a Module
```kotlin
object MyModule : Module {
    override val section = Section.CUSTOM
    override val destinations = persistentListOf(
        MyDestination.Main
    )
    override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
        subclass(MyDestination.Main::class, MyDestination.Main.serializer())
    }
    override fun EntryProviderScope<NavKey>.registerContent(
        onNavigateBack: () -> Unit,
        onNavigate: (NavKey) -> Unit
    ) {
        entry<MyDestination.Main> {
            MyScreen(onNavigateBack = onNavigateBack)
        }
    }
}
```
## Module Registry
```kotlin
val modules = rememberModules {
    module(MyModule)
    module(FeatureFlip)
    module(Analytics)
}
```
[Creating Custom Modules →](custom-modules.md)
