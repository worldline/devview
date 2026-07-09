# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-analytics` is the analytics-inspection module for the DevView developer menu. It lets host apps forward analytics events to `AnalyticsLogger` at runtime; DevView then shows those events in a searchable, filterable Compose UI.

## Public API Surface

### `Analytics` — the `Module` entry point

```kotlin
Analytics(
    highlightedLogType1: AnalyticsLogType? = AnalyticsLogCategory.Action.Click,
    highlightedLogType2: AnalyticsLogType? = AnalyticsLogCategory.Performance.Error,
    highlightedLogType3: AnalyticsLogType? = null
)
```

Registered in the host app's `buildModules { }` block. The three `highlightedLogType*` params control which event-type counters appear in the sticky summary header.

### `AnalyticsLogger` — singleton event store

| Member | Visibility | Purpose |
|---|---|---|
| `log(log: AnalyticsLog)` | `public` | Append an event |
| `logs: SnapshotStateList<AnalyticsLog>` | `public` | Observable list of all events |
| `hasLogs: Boolean` | `public` | Convenience check |
| `clear()` | `internal` | Called by the in-UI "Clear" action only |

### `AnalyticsLog` — event record

```kotlin
data class AnalyticsLog(
    val tag: String,          // event name / identifier
    val screenClass: String,  // origin screen or component
    val timestamp: Long,      // epoch milliseconds
    val type: AnalyticsLogType
)
```

`formattedTimestamp` (HH:mm:ss in system timezone) is `internal` — used by UI components only.

### `AnalyticsLogType` / `AnalyticsLogCategory` — type taxonomy

`AnalyticsLogType` is a sealed interface; every concrete type is a `data object` declared *inside* a nested `AnalyticsLogCategory` sealed interface. The same object simultaneously IS the type and IS the category (via `override val category: AnalyticsLogCategory get() = this`):

```kotlin
// Referencing a type:
AnalyticsLogCategory.Action.Click       // AnalyticsLogType AND AnalyticsLogCategory.Action
AnalyticsLogCategory.Performance.Error  // AnalyticsLogType AND AnalyticsLogCategory.Performance
```

Built-in categories: `Screen`, `Session`, `Action`, `Search`, `Performance`, `Ecommerce`, `Media`, `Feature`, `Social`, `Custom`, `Diagnostic`.

`AnalyticsLogType.allTypes()` returns every known type and **must be updated manually** when a new type is added — KMP has no sealed-class reflection.

### `LocalAnalytics` — CompositionLocal

```kotlin
val LocalAnalytics: ProvidableCompositionLocal<List<AnalyticsLog>>
```

Must be provided above any call site of `AnalyticsScreen`. The DevView host wraps module content with `CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs)` before rendering module destinations.

### `AnalyticsScreen` — main composable

```kotlin
@Composable
fun AnalyticsScreen(
    highlightedAnalyticsLogTypes: PersistentList<AnalyticsLogType>,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
)
```

Reads from `LocalAnalytics.current`. All filtering state (text query, category chips, time range) lives as local `remember` state inside this composable — there is no ViewModel.

## Internal Architecture

```
AnalyticsLogger (singleton, mutableStateListOf)
        │
        │  provides via LocalAnalytics
        ▼
AnalyticsScreen (stateful Compose UI)
    ├── sticky header  →  HighlightedAnalyticsLogsHeader
    │                          └── HighlightedAnalyticsLog.Total / .Type
    ├── log list       →  AnalyticsLogItem
    │                          └── CategoryChip
    └── bottom bar     →  text filter + TimeRange segmented button + category FilterChips
```

`AnalyticsLogger` uses `mutableStateListOf`, so `AnalyticsScreen` recomposes automatically whenever an event is appended — no explicit notification mechanism needed.

`Analytics.destinations` registers a single `AnalyticsDestination.Main` destination with a "Clear" toolbar action. That action calls `AnalyticsLogger.clear()` (the only caller of the `internal` function).

`TimeRange` (All / 5m / 15m / 30m) is a `private enum` inside `AnalyticsScreen.kt`, not exported.

## Platform-Specific Code

There are no `androidMain` or `iosMain` source sets. All production code is in `commonMain`. The `androidDeviceTest` source set holds Compose UI tests that run on a device or emulator using `runComposeUiTest`.

## Conventions Specific to This Module

- **Test tags** follow a fixed scheme used by device tests:
  - Log items: `analytics_log_item_${log.tag}`
  - Category chips: `category_chip_${category.displayName}`
  - Time range buttons: `time_range_${timeRange.label}` (e.g. `time_range_5m`)
  - Text field: `analytics_filter_field`

- **Adding a new `AnalyticsLogType`**: (1) declare a `data object` inside the appropriate `AnalyticsLogCategory` sub-interface implementing both `AnalyticsLogCategory` and `AnalyticsLogType`, (2) add it to `AnalyticsLogType.allTypes()`.

- **Adding a new `AnalyticsLogCategory`**: add a new nested sealed interface inside `AnalyticsLogCategory` defining `displayName`, `containerColor`/`contentColor` (from `MaterialTheme`), and `icon`. Then add its types to `allTypes()`.

- `@Poko` is not applied directly to analytics data classes; it is configured at the project level (annotation class at `com/worldline/devview/core/Poko`) and applied via the convention plugin.
