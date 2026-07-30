---
name: add-destination
description: Add a new navigation destination (screen) to an existing DevView module
---

You are adding a new Navigation3 destination to an existing DevView module. Three parts of the Module implementation must change in lockstep: the destinations map, registerSerializers, and registerContent. Missing any one of them compiles fine but crashes at runtime.

## 0. Determine destination type

**`data object`** — singleton screen, no parameters (e.g. `Main`, `Settings`)
**`data class`** — parameterized screen (e.g. `Detail(val id: String)`, `Endpoint(val key: EndpointKey)`)

This choice determines which `withTitle()` extension to use.

## 1. Add the NavKey

In the module's sealed interface (e.g. `MyFeatureDestination.kt`):

```kotlin
@Serializable
sealed interface MyFeatureDestination : NavKey {
    @Serializable
    data object Main : MyFeatureDestination  // existing

    // New destination — data object (singleton):
    @Serializable
    data object Settings : MyFeatureDestination

    // New destination — data class (parameterized):
    @Serializable
    data class Detail(val itemId: String) : MyFeatureDestination
}
```

## 2. Add to `destinations` map

The `destinations` property is `PersistentMap<KClass<out NavKey>, DestinationMetadata>`.

```kotlin
override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
    *MyFeatureDestination.Main.withTitle("My Feature"),        // existing

    // data object → instance extension (spread with *)
    *MyFeatureDestination.Settings.withTitle("Settings"),

    // data class → KClass extension (spread with *)
    *MyFeatureDestination.Detail::class.withTitle("Detail"),
)
```

**Why the distinction:** `withTitle()` has two overloads:
- `NavKey.withTitle(title)` — instance extension, returns `Pair<KClass<T>, DestinationMetadata>`. Works on `data object` because the instance is a singleton.
- `KClass<out NavKey>.withTitle(title)` — KClass extension. Required for `data class` because no single instance represents the destination.

## 3. Register the serializer

In `registerSerializers`, add a `subclass` entry for every new destination:

```kotlin
override fun registerSerializers(builder: PolymorphicModuleBuilder<NavKey>) {
    builder.subclass(MyFeatureDestination.Main::class, MyFeatureDestination.Main.serializer())  // existing

    // New entries:
    builder.subclass(MyFeatureDestination.Settings::class, MyFeatureDestination.Settings.serializer())
    builder.subclass(MyFeatureDestination.Detail::class, MyFeatureDestination.Detail.serializer())
}
```

Missing a serializer entry = crash when Navigation3 saves/restores back-stack state (not caught at compile time).

## 4. Register content

In `registerContent`, add an `entry<T>` block:

```kotlin
@Composable
override fun EntryProviderScope<NavKey>.registerContent(bottomPadding: Dp) {
    entry<MyFeatureDestination.Main> { MyFeatureScreen(bottomPadding = bottomPadding) }  // existing

    // data object — no destination param needed:
    entry<MyFeatureDestination.Settings> {
        MyFeatureSettingsScreen(bottomPadding = bottomPadding)
    }

    // data class — destination carries params:
    entry<MyFeatureDestination.Detail> { destination ->
        MyFeatureDetailScreen(itemId = destination.itemId, bottomPadding = bottomPadding)
    }
}
```

Always pass `bottomPadding` to scrollable content so it clears the navigation bar.

## 5. Optional: TopBar actions

If the new screen needs toolbar buttons (e.g. a refresh or filter button):

```kotlin
// In the module's destinations map:
*MyFeatureDestination.Settings.withTitle("Settings").withActions(
    action(icon = Icons.Default.Refresh, contentDescription = "Refresh") {
        // This lambda fires when the button is tapped
        // To communicate to a ViewModel, use a MutableSharedFlow:
        refreshTrigger.tryEmit(Unit)
    }
),
```

The ViewModel collects the shared flow:
```kotlin
// In the module file (top-level, not inside the Module object):
internal val refreshTrigger = MutableSharedFlow<Unit>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

// In the ViewModel:
init {
    refreshTrigger.onEach { reload() }.launchIn(viewModelScope)
}
```

## 6. Add navigation call sites

To navigate to the new destination from an existing screen, use the Navigation3 back-stack:
```kotlin
// In a composable that has access to NavController:
navController.navigate(MyFeatureDestination.Detail(itemId = id))
```

## 7. Verify

```shell
.\gradlew.bat :devview-myfeature:testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
.\gradlew.bat :sample:androidApp:assembleDebug -Pandroidx.baselineprofile.skipgeneration
```
