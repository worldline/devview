package com.worldline.devview.analytics.model

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/**
 * Represents a single analytics event log entry.
 *
 * This immutable data class captures all essential information about an analytics event,
 * including what happened ([tag]), where it happened ([screenClass]), when it happened
 * ([timestamp]), and what type of event it was ([type]).
 *
 * The class automatically handles timestamp formatting for display purposes through
 * the [formattedTimestamp] property.
 *
 * @property tag The name or identifier of the analytics event (e.g., "user_login", "HomeScreen").
 * @property screenClass The fully qualified class name or identifier of the screen/component
 *           where the event occurred (e.g., "com.example.ui.HomeScreen").
 * @property timestamp Unix timestamp in milliseconds representing when the event occurred.
 * @property type The category of this analytics event.
 *
 * @see AnalyticsLogType
 * @see AnalyticsLogCategory
 * @see com.worldline.devview.analytics.AnalyticsLogger
 *
 * @sample com.worldline.devview.analytics.samples.AnalyticsLogSamples.screenLogSample
 */
@Immutable
public data class AnalyticsLog(
    val tag: String,
    val screenClass: String,
    val timestamp: Long,
    val type: AnalyticsLogType
) {
    private val instantTimestamp = Instant.fromEpochMilliseconds(epochMilliseconds = timestamp)

    /**
     * Returns the timestamp formatted as HH:mm:ss in the current system timezone.
     *
     * This property is used internally by the analytics UI to display event times
     * in a human-readable format.
     *
     * @return A string representing the time in 24-hour format (e.g., "14:35:22").
     */
    internal val formattedTimestamp: String
        get() = instantTimestamp
            .toLocalDateTime(
                timeZone = TimeZone.currentSystemDefault()
            ).time
            .format(
                format = LocalTime.Format {
                    hour()
                    char(value = ':')
                    minute()
                    char(value = ':')
                    second()
                }
            )
}
