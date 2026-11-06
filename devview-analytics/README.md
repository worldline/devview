# DevView Analytics

Inspect analytics events in DevView on Android and iOS. The module shows a searchable event list, compact counts and an event detail page with parameters and copy support.

## Register the module

```kotlin
val modules = rememberModules {
    module(Analytics())
}
```

The optional `highlightedLogType1`, `highlightedLogType2` and `highlightedLogType3` constructor parameters choose the counts displayed beside the total.

## Record an event

```kotlin
import com.worldline.devview.analytics.AnalyticsLogger
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import kotlin.time.Clock

AnalyticsLogger.log(
    AnalyticsLog(
        tag = "checkout_tap",
        screenClass = "CheckoutScreen",
        timestamp = Clock.System.now().toEpochMilliseconds(),
        type = AnalyticsLogCategory.Action.Click,
        params = mapOf("source" to "cart")
    )
)
```

Each call records a new event identity, even when the same object is logged again. The logger copies the parameter map so later caller mutations do not change recorded events. Avoid credentials and personal data in parameters; Copy event writes the displayed data to the system clipboard.

## Inspect events

Search matches the event tag or screen, ignoring case. Expand the bottom filter panel to choose categories and a time range. The filter button shows the number of active non-search filters, even when collapsed. Time windows refresh every five seconds. Summary counts describe the filtered results.

Tap an event to inspect its screen, full timestamp with milliseconds and timezone, type and sorted parameters. Copy event copies this information as plain text. Cleared or unavailable events show an empty state rather than resolving to a different event.

## Show DevView

```kotlin
DevView(
    devViewIsOpen = isDevViewOpen,
    closeDevView = { isDevViewOpen = false },
    modules = modules
)
```

Module registration owns the list and detail destinations. Search, filters and the inspected event’s position survive navigation and saved-state restoration.

## Read-only history

`AnalyticsLogger.logs` returns an observable, immutable list snapshot. Read it in composition to observe updates; remembering one snapshot freezes that view. Record through `AnalyticsLogger.log()` and clear using the module toolbar. Events remain in memory only.

Migration from the previous API:

- Remove `CompositionLocalProvider(LocalAnalytics provides ...)`; `LocalAnalytics` has been removed.
- Replace direct screen calls with `rememberModules { module(Analytics()) }` and `DevView`.
- Replace writes to `AnalyticsLogger.logs` with `AnalyticsLogger.log()`.

See [the Analytics guide](../docs/modules/analytics.md) for installation and workflow details.
