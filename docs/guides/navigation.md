# Navigation Guide

Understanding DevView's type-safe navigation system.

> _[Placeholder: Insert diagram of navigation flow in a DevView module. Use a device frame if relevant.]_

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
override val registerSerialisers = {
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

## Best Practices
- Use sealed interfaces for navigation keys
- Keep navigation flows simple and predictable
- Document all navigation paths
- Test navigation on all supported platforms

## Advanced Patterns
> _[Placeholder: Deep linking, multi-module navigation, platform-specific navigation customisation.]_

## Troubleshooting
- **Navigation not working?** Check destination and serialiser definitions.
- **Screen not rendering?** Verify Compose compatibility and state management.
- **Back navigation issues?** Ensure `onNavigateBack` is correctly wired in all screens.

## See Also
[Module Development](module-development.md)
[Custom Modules](../modules/custom-modules.md)

## API Reference
> _[API reference available via Dokka. Add direct link here when available.]_
