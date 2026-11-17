package com.worldline.devview.analytics

import androidx.compose.runtime.mutableStateListOf
import com.worldline.devview.analytics.model.AnalyticsLog
import kotlinx.collections.immutable.toPersistentMap

/**
 * In-memory analytics recorder with observable, read-only history.
 *
 * Each [log] call records a distinct occurrence and snapshots its parameters. History belongs
 * to this logger; consumers cannot insert or remove records through [logs]. Read [logs] inside
 * composition to observe updates, rather than remembering a returned snapshot.
 *
 * @see Analytics
 * @sample com.worldline.devview.analytics.samples.AnalyticsLoggerSamples.logSample
 */
public object AnalyticsLogger {
    private val analyticsLogs = mutableStateListOf<AnalyticsLog>()

    /**
     * Records [log] with a fresh internal occurrence ID and an immutable copy of its parameters.
     * Snapshot-state storage supports calls from different threads and notifies observing UI.
     *
     * @param log Event to record. Never include secrets or personal data in its parameters.
     * @sample com.worldline.devview.analytics.samples.AnalyticsLoggerSamples.logSample
     */
    public fun log(log: AnalyticsLog) {
        analyticsLogs.add(element = log.copy(params = log.params.toPersistentMap()))
    }

    /** Removes all recorded events after the developer confirms the clear-history action. */
    internal fun clear() {
        analyticsLogs.clear()
    }

    /**
     * Immutable history snapshot in recording order. Previously returned snapshots never change.
     * Read the property again to obtain new events; reading in composition observes changes.
     */
    public val logs: List<AnalyticsLog>
        get() = analyticsLogs.toList()

    /** Whether the history currently contains at least one event. */
    public val hasLogs: Boolean
        get() = analyticsLogs.isNotEmpty()
}
