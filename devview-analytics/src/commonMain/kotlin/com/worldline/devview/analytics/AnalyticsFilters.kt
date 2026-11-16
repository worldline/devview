package com.worldline.devview.analytics

import com.worldline.devview.analytics.model.AnalyticsLog

// Injected lists may repeat one instance; ranks distinguish rows without changing event identity.
internal fun eventRowKeys(logs: List<AnalyticsLog>): List<String> {
    val occurrences = mutableMapOf<String, Int>()
    return logs.map { log ->
        val rank = occurrences[log.id] ?: 0
        occurrences[log.id] = rank + 1
        "${log.id}:$rank"
    }
}

internal enum class TimeRange(val label: String, val durationMillis: Long?) {
    All(label = "All", durationMillis = null),
    Last5Min(label = "5m", durationMillis = 5 * 60 * 1000L),
    Last15Min(label = "15m", durationMillis = 15 * 60 * 1000L),
    Last30Min(label = "30m", durationMillis = 30 * 60 * 1000L)
}

internal fun filterLogs(
    logs: List<AnalyticsLog>,
    query: String,
    categories: Set<String>,
    range: TimeRange,
    now: Long
): List<AnalyticsLog> = logs.filter { log ->
    val matchesQuery = query.isBlank() ||
        log.tag.contains(other = query, ignoreCase = true) ||
        log.screenClass.contains(other = query, ignoreCase = true)
    val matchesCategory = categories.isEmpty() || log.type.category.displayName in categories
    val matchesTime = range.durationMillis?.let { log.timestamp >= now - it } ?: true
    matchesQuery && matchesCategory && matchesTime
}
