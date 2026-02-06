# Navigation Guide
Understanding DevView's type-safe navigation system.
## Navigation Architecture
DevView uses Navigation3 with kotlinx.serialization for type-safe navigation.
## Key Concepts
### NavKey
All destinations implement NavKey:
```kotlin
sealed interface MyDestination : NavKey {
    @Serializable
    data object Main : MyDestination
}
```
### Registration
Register destinations in your module:
```kotlin
override val registerSerializers = {
    subclass(MyDestination.Main::class, MyDestination.Main.serializer())
}
```
### Navigation Callbacks
- **onNavigateBack**: Pop the backstack
- **onNavigate**: Navigate to a destination
## See Also
[Module Development](module-development.md)
