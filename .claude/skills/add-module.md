---
name: add-module
description: Scaffold a complete new DevView module with all contracts, wiring, and documentation
---

You are scaffolding a new DevView module. Follow this checklist exactly — every step is required. A missed step either breaks Konsist tests or produces a module that silently fails at runtime.

## 0. Gather information

Ask (or infer from context):
- **Module name** — kebab-case, e.g. `devview-myfeature` (for a standalone module) or `devview-myfeature-core` + `devview-myfeature` (for a module that needs a separate data layer or Ktor plugin)
- **Section** — where it appears in the DevView home screen: `Section.DEBUG`, `Section.NETWORK`, `Section.ANALYTICS`, or a new one
- **RequiresDataStore?** — does this module persist state across app launches?
- **Published?** — should it be included in Dokka API docs and have a `docs/modules/<module>.md` page?

## 1. Register in `settings.gradle.kts`

Add the module to the root `settings.gradle.kts` include list:
```kotlin
include(":devview-myfeature")
```

## 2. Create `devview-myfeature/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.unit.test)
    alias(libs.plugins.convention.device.test)
    alias(libs.plugins.convention.kover)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.myfeature"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.devview)
            // add other deps here
        }
    }
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

kover {
    // included by default via convention plugin
}
```

If the module will have no tests initially, add to the bottom:
```kotlin
tasks.named<Test>("testAndroidHostTest") {
    failOnNoDiscoveredTests.set(false)
}
```

## 3. Create the module's source tree

Package naming rule: hyphens → dots, prefixed with `com.worldline.devview.`
- `devview-myfeature` → `com.worldline.devview.myfeature`
- `devview-networkmock-core` → `com.worldline.devview.networkmock.core`

Create: `devview-myfeature/src/commonMain/kotlin/com/worldline/devview/myfeature/`

## 4. Define NavKey destinations

```kotlin
// MyFeatureDestination.kt
package com.worldline.devview.myfeature

import kotlinx.serialization.Serializable
import com.worldline.devview.core.navigation.NavKey

@Serializable
sealed interface MyFeatureDestination : NavKey {
    // Singleton screen (no params) → data object
    @Serializable
    data object Main : MyFeatureDestination

    // Parameterized screen → data class
    @Serializable
    data class Detail(val itemId: String) : MyFeatureDestination
}
```

## 5. Implement the `Module` interface

```kotlin
// MyFeatureModule.kt
package com.worldline.devview.myfeature

import androidx.compose.runtime.Composable
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.navigation.NavKey
import com.worldline.devview.core.navigation.DestinationMetadata
import com.worldline.devview.core.navigation.withTitle
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlin.reflect.KClass

// Use `object` for simple modules, `class` for configurable ones
public object MyFeatureModule : Module {

    override val section: Section = Section.DEBUG

    // destinations is a PersistentMap<KClass<out NavKey>, DestinationMetadata>
    // NOT a list — keyed by KClass
    override val destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata> = persistentMapOf(
        // data object → use instance extension: MyDest.Main.withTitle("Title")
        *MyFeatureDestination.Main.withTitle("My Feature"),
        // data class → use KClass extension: MyDest.Detail::class.withTitle("Title")
        *MyFeatureDestination.Detail::class.withTitle("Detail"),
    )

    override val entryDestination: NavKey = MyFeatureDestination.Main

    // Must register ALL destinations here — forgetting one compiles but crashes
    // at runtime when Navigation3 tries to save/restore state
    override fun registerSerializers(builder: PolymorphicModuleBuilder<NavKey>) {
        builder.subclass(MyFeatureDestination.Main::class, MyFeatureDestination.Main.serializer())
        builder.subclass(MyFeatureDestination.Detail::class, MyFeatureDestination.Detail.serializer())
    }

    @Composable
    override fun EntryProviderScope<NavKey>.registerContent(bottomPadding: Dp) {
        entry<MyFeatureDestination.Main> {
            MyFeatureScreen(bottomPadding = bottomPadding)
        }
        entry<MyFeatureDestination.Detail> { destination ->
            MyFeatureDetailScreen(itemId = destination.itemId, bottomPadding = bottomPadding)
        }
    }
}
```

### If the module needs DataStore persistence

Implement `RequiresDataStore`:
```kotlin
public object MyFeatureModule : Module, RequiresDataStore {

    // Unique filename — never reuse another module's filename
    override val dataStoreFileName: String = "myfeature_preferences"

    // Called automatically by rememberModules BEFORE initModule()
    override fun initDataStore(dataStore: DataStore<Preferences>) {
        MyFeatureRepository.init(dataStore)
    }

    override fun initModule() {
        // called after initDataStore, before first composition
    }
}
```

## 6. Add Kover aggregation

In `devview/build.gradle.kts`, add:
```kotlin
kover {
    merge {
        // ... existing entries ...
        subproject(projects.devviewMyfeature)
    }
}
```

## 7. Register in the sample app

In `sample/shared/src/commonMain/.../DevViewApp.kt` (or wherever `rememberModules` is called):
```kotlin
val modules = rememberModules {
    // ... existing modules ...
    register(MyFeatureModule, section = Section.DEBUG)
}
```

## 8. Documentation (3 coordinated changes)

**8a.** Create `docs/modules/myfeature.md` — follow `docs/modules/featureflip.md` or `docs/modules/analytics.md` as a template. Include: overview, setup (dependency coordinates), usage example, public API surface.

**8b.** Add nav entry in `zensical.toml` under the Modules section:
```toml
{ "MyFeature" = "modules/myfeature.md" },
```
If it's a multi-module family (like NetworkMock), nest it:
```toml
{ "MyFeature" = [
  { "Overview" = "modules/myfeature.md" },
  { "Core" = "modules/myfeature-core.md" },
]},
```

**8c.** Add Dokka dependency in `internal/dokka/build.gradle.kts`:
```kotlin
dependencies {
    // ... existing ...
    dokka(projects.devviewMyfeature)
}
```

## 9. Verification

Run in order — stop at first failure:
```shell
.\gradlew.bat :konsist:test -Pandroidx.baselineprofile.skipgeneration
.\gradlew.bat :devview-myfeature:testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
.\gradlew.bat :sample:androidApp:assembleDebug -Pandroidx.baselineprofile.skipgeneration
```

Konsist will catch: wrong package names, missing `-core` sub-modules, dependency violations.
Compile will catch: serializer registration errors (they manifest as linker errors, not runtime).

## Common mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Used `persistentListOf` instead of `persistentMapOf` | Compile error | Use `PersistentMap<KClass<out NavKey>, DestinationMetadata>` |
| Used instance extension on `data class` | No `withTitle()` method found | Use `MyDest.Detail::class.withTitle()` |
| Forgot `registerSerializers` for a destination | Crash on back-nav state restore | Add `subclass(T::class, T.serializer())` |
| Non-unique DataStore filename | Silent state corruption if two modules share a file | Use `<modulename>_preferences` |
| Wrong package name | Konsist test failure | Follow hyphens→dots rule exactly |
| Missing `dokka()` in internal/dokka | Module absent from API docs | Add `dokka(projects.<module>)` |
