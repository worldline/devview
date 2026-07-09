# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

The `devview` module is the core framework: it defines the `Module` interface that all feature modules implement, provides the `DevView` Compose composable that hosts the overlay UI, and exposes the `buildModules`/`rememberModules` DSL for assembling modules. All other `devview-*` feature modules depend on this one.

## Public API Surface

| Type | Location | Role |
|---|---|---|
| `DevView` composable | `DevView.kt` | Main entry point. Wraps content in `AnimatedVisibility`, owns the Navigation3 backstack, renders top app bar with dynamic title + actions |
| `Module` interface | `core/Module.kt` | Contract every feature module must implement |
| `ModuleRegistry` | `core/ModuleRegistry.kt` | Builder (not typically instantiated directly) |
| `buildModules { }` | `core/ModuleRegistry.kt` | Non-composable DSL to assemble an `ImmutableList<Module>` |
| `rememberModules { }` | `core/ModuleRegistry.kt` | Composable version; also calls `initDataStore()` + `initModule()` on first composition |
| `Section` enum | `core/Section.kt` | `SETTINGS`, `FEATURES`, `NETWORK`, `LOGGING`, `CUSTOM` — controls home screen grouping and default icons |
| `DestinationMetadata` | `core/DestinationMetadata.kt` | Per-destination top bar title + action list |
| `DestinationMetadataBuilder` | `core/DestinationMetadata.kt` | DSL receiver inside `withTitle { }` / `withActions { }` blocks |
| `ModuleDestinationAction` | `core/ModuleDestinationAction.kt` | Icon button descriptor (icon, callback, optional popup) |
| `ModuleDestinationActionPopup` | `core/ModuleDestinationActionPopup.kt` | Confirmation `AlertDialog` data (title, subtitle, button labels) |
| `NavKey` extension fns | `core/DestinationMetadataExtensions.kt` | `asDestination()`, `withTitle()`, `withActions()` — available on both `NavKey` instances and `KClass<out NavKey>` |
| `Home` | `HomeScreen.kt` | Serializable `data object` / NavKey for the home screen |
| `@Poko` | `core/Poko.kt` | Annotation for the Poko compiler plugin (generates `equals`/`hashCode`/`toString`/`copy` on non-data-classes) |

## Internal Architecture

```
DevView (composable)
├── rememberNavBackStack — seeded with Home; all module destination serializers
│   registered polymorphically via each Module.registerSerializers
├── AnimatedVisibility — controlled by devViewIsOpen param
├── Scaffold + MediumTopAppBar
│   ├── Title: resolved in order: HasTitle (framework screens) →
│   │         DestinationMetadata.title → Module.moduleName
│   └── Actions: DestinationMetadata.actions rendered as IconButtons;
│               if action.popup != null, shows AlertDialog before invoking action
└── NavDisplay (Navigation3)
    ├── entry<Home> → HomeScreen (groups modules by Section, sticky headers)
    │       └── ModuleItem (card per module, shape adapts by ModulePosition)
    └── per-module entries registered via Module.registerContent(...)
```

**`HomeScreen`** groups the module list by `Section` using `derivedStateOf`, renders sticky section headers (section name, underscores replaced with spaces), and assigns each `ModuleItem` a `ModulePosition` (SINGLE / FIRST / MIDDLE / LAST) that controls card corner rounding and whether a top divider is shown.

**`HasTitle`** is an internal-only interface for framework-level NavKeys (currently only `Home`). Module destinations must NOT implement it; they declare titles via `DestinationMetadata`.

## Module Implementation Contract

Every `Module` must provide:

- `section: Section` — where to appear in the home screen
- `destinations: PersistentMap<KClass<out NavKey>, DestinationMetadata>` — all screens keyed by their KClass
- `entryDestination: NavKey` — concrete instance pushed when the user opens the module from home; its `::class` must be a key in `destinations`
- `registerSerializers: PolymorphicModuleBuilder<NavKey>.() -> Unit` — registers each destination as a `NavKey` subclass for state persistence
- `fun EntryProviderScope<NavKey>.registerContent(onNavigateBack, onNavigate, bottomPadding)` — wires composable content to each destination

Optional overrides: `moduleName`, `icon`, `containerColor`, `contentColor`, `subtitle`, `initModule()`.

## Non-Obvious Patterns

**`data object` vs `data class` destinations in `destinations` map:**
Use the instance extension (`MyDest.Main.withTitle(...)`) for `data object` destinations. Use the `KClass` extension (`MyDest.Detail::class.withTitle(...)`) for `data class` destinations since no instance exists at module-construction time.

**Top app bar actions and ViewModels:**
`ModuleDestinationAction.action` is a plain lambda captured at construction time. To trigger a ViewModel from an action, expose a `MutableSharedFlow` on the module and observe it where the ViewModel is in scope (e.g. with `LaunchedEffect` in the host composable).

**`rememberModules` initialization order:**
For any module implementing `RequiresDataStore` (from `devview-utils`), `initDataStore()` is called before `initModule()`. Each module is initialized at most once per composition (tracked via an internal `mutableSetOf`).

**`@Poko` annotation:**
The Poko plugin is configured (in `build.gradle.kts`) to recognize `com/worldline/devview/core/Poko` as its trigger annotation. Apply `@Poko` to regular classes (not `data class`) in this module when you need multiplatform-compatible `equals`/`hashCode`/`toString`/`copy`.

**`bottomPadding` parameter in `registerContent`:**
Carries the `Scaffold`'s bottom inset (navigation bar height). Pass it down to any lazy list or scrollable content in the module screen so the last item is not hidden behind the system navigation bar.

**Kover aggregation:**
This module's `build.gradle.kts` pulls `kover()` coverage from all sibling feature modules (`devview-analytics`, `devview-featureflip`, `devview-networkmock`, etc.), so `:devview:koverXmlReport` produces a combined coverage report for the whole library.

## Platform-Specific Code

There is no `androidMain` or `iosMain` source set in this module — all source lives in `commonMain`. There are no `expect`/`actual` declarations here. Instrumented UI tests live in `androidDeviceTest` and use Compose test rules against the real composables.
