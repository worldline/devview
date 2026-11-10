package com.worldline.devview.analytics.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/**
 * Represents a single analytics event log entry.
 *
 * This data class captures all essential information about an analytics event,
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
 * @property params Optional event parameters. Do not record secrets or personal data.
 *        The logger takes a defensive copy when recording the event; later changes to the
 *        supplied map do not modify recorded history.
 *
 * @see AnalyticsLogType
 * @see AnalyticsLogCategory
 * @see com.worldline.devview.analytics.AnalyticsLogger
 *
 * @sample com.worldline.devview.analytics.samples.AnalyticsLogSamples.screenLogSample
 */
public data class AnalyticsLog(
    val tag: String,
    val screenClass: String,
    val timestamp: Long,
    val type: AnalyticsLogType,
    val params: Map<String, String> = emptyMap()
) {
    // Occurrence identity deliberately stays outside data-class equality and copy arguments.
    @OptIn(ExperimentalUuidApi::class)
    internal val id: String = Uuid.random().toString()
    private val instantTimestamp = Instant.fromEpochMilliseconds(epochMilliseconds = timestamp)

    internal val detailedTimestamp: String
        get() {
            val zone = TimeZone.currentSystemDefault()
            val dateTime = instantTimestamp.toLocalDateTime(timeZone = zone)
            val time = dateTime.time.format(
                format = LocalTime.Format {
                    hour()
                    char(value = ':')
                    minute()
                    char(value = ':')
                    second()
                }
            )
            val millis = (dateTime.nanosecond / 1_000_000).toString().padStart(
                length = 3,
                padChar = '0'
            )
            return "${dateTime.date} $time.$millis (${zone.id})"
        }

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
