# Analytics module

`devview-analytics` records and inspects in-memory analytics events on Android and iOS. Production code is in `commonMain`.

## Public API

- `Analytics(highlightedLogType1, highlightedLogType2, highlightedLogType3)` registers Main and Detail destinations. The highlighted types choose compact summary counts.
- `AnalyticsLogger.log(log: AnalyticsLog)` appends a fresh copy with an immutable parameter map. `logs: List<AnalyticsLog>` returns the private SnapshotStateList’s immutable snapshot; `hasLogs` observes whether it is empty. `clear()` is internal and used by the toolbar.
- `AnalyticsLog(tag: String, screenClass: String, timestamp: Long, type: AnalyticsLogType, params: Map<String, String> = emptyMap())` keeps value equality. Its internal random UUID is outside the constructor/equality and changes on copy. Do not mark the model Immutable: the public model can contain mutable maps before recording.
- `AnalyticsDestination.Detail(id: String)` identifies the recorded event. Missing identities show an unavailable-event state.

`LocalAnalytics` was removed. Registered list and detail destinations both use the logger.
Integrators register `Analytics()` and render `DevView`; screens are internal. Read logger
snapshots in composition, not through a remembered stale snapshot.

## Internal UI

`AnalyticsScreen` uses DevViewFilterBar, DevViewEmptyState and compact HighlightedAnalyticsLogsHeader. Search/category/time/panel state uses rememberSaveable. NavDisplay and its saved-entry decorator preserve the registered list and detail destinations. The list displays newest first
without automatic following. It saves the measured first-visible row key and offset, constructs
a fresh LazyListState from that anchor on re-entry, and flushes the anchor before opening details.
When the summary header is first, it saves the clicked event's measured position instead, so
new arrivals do not replace the inspected content on return. Missing anchors fall back to the
top; Latest events is an explicit scroll action.

`filterLogs(logs, query, categories, range, now)` is pure. Query matches tag or screen; categories and time combine with it. The clock refreshes every five seconds only for limited ranges. Summary counts use filtered results. `eventRowKeys` adds occurrence ranks to UUIDs so explicitly supplied duplicate objects cannot collide in LazyColumn. Assign ranks oldest-first, then reverse both rows and keys; input lists remain oldest-first.

`AnalyticsDetailScreen` shows selectable event data and sorted string parameters. Copy uses the core multiplatform clipboard callback. No payload is persisted.

## Taxonomy and tests

Types are data objects nested in AnalyticsLogCategory, not enum constants. Examples: `AnalyticsLogCategory.Screen.View`, `AnalyticsLogCategory.Action.Click`, `AnalyticsLogCategory.Performance.Error`. Each object implements AnalyticsLogType and its category. Adding a type also requires updating `AnalyticsLogType.allTypes()`; KMP has no sealed-class reflection. Category colours/icons come from the host MaterialTheme.

Host tests cover filtering, boundary expiry, duplicate-safe keys, logger snapshot/parameter isolation, identities and copied text. Device tests cover live updates, filters/restoration, returning from details, missing restored identities, periodic expiry and clipboard contents, plus newest-first order and exact row/offset preservation
during arrivals, detail return and saved-state restoration.

Test tags: `analytics_log_item_${log.tag}`, `category_chip_${category.displayName}`, `time_range_${range.label}`, `analytics_filter_bar`. Duplicate tags are allowed for explicitly repeated objects; list keys are distinct.
