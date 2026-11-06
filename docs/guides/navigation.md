# Navigation Guide

Understanding DevView's type-safe navigation system.

## Navigation Architecture
DevView uses Navigation3 with kotlinx.serialization for type-safe navigation. Destinations are defined as sealed interfaces implementing NavKey, and navigation is handled via callbacks.

## Step-by-Step: Setting Up Navigation

### 1. Define Destinations
```kotlin
sealed interface MyDestination : NavKey {
    @Serializable
    data object Main : MyDestination
    @Serializable
    data class Detail(val id: String) : MyDestination
}
```

### 2. Register Destinations and Serialisers
```kotlin
override val registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit = {
    subclass(MyDestination.Main::class, MyDestination.Main.serializer())
    subclass(MyDestination.Detail::class, MyDestination.Detail.serializer())
}
```

### 3. Handle Navigation Flows
```kotlin
entry<MyDestination.Main> {
    MyMainScreen(
        onNavigateBack = onNavigateBack,
        onDetailClick = { id ->
            onNavigate(MyDestination.Detail(id))
        }
    )
}
entry<MyDestination.Detail> { destination ->
    MyDetailScreen(
        id = destination.id,
        onNavigateBack = onNavigateBack
    )
}
```

## Destination toolbars

DevView renders a separate top bar inside each Navigation3 entry. Its title, actions and confirmation state move with that screen, including predictive back. Home has **Close DevView**; module screens have **Back**. A title defaults to `moduleName` unless destination metadata overrides it.

Every toolbar action and menu requires a user-facing `label`. DevView uses it for the accessible name and tooltip. Action callbacks receive the displayed `NavKey`, not the destination currently at the top of an animating backstack:

```kotlin
MyDestination.Detail::class.withTitle("Details") {
    action(label = "Refresh item", icon = Icons.Rounded.Refresh) { key ->
        refresh((key as MyDestination.Detail).id)
    }
    menu(label = "Sort", icon = Icons.AutoMirrored.Rounded.Sort) {
        item(label = "Name") { sortByName() }
    }
}
```

A confirmation popup invokes the same key-aware callback only after confirmation. Menu items keep their zero-argument callbacks.

Registration must be side-effect-free: DevView may call `registerContent` again to pass the destination Scaffold's measured bottom inset. Initialise ViewModels and UI state inside the registered entry's composable content. Top and horizontal insets are applied by DevView; pass `bottomPadding` to your scrollable content.

## Best Practices
- Use sealed interfaces for navigation keys
- Keep navigation flows simple and predictable
- Document all navigation paths
- Test navigation on all supported platforms

## Troubleshooting
- **Navigation not working?** Check destination and serialiser definitions.
- **Screen not rendering?** Verify Compose compatibility and state management.
- **Back navigation issues?** Ensure `onNavigateBack` is correctly wired in all screens.

## See Also
[Module Development](module-development.md)
[Custom Modules](../modules/custom-modules.md)

## API Reference
> _[Dokka API Reference](../api/index.html)_
