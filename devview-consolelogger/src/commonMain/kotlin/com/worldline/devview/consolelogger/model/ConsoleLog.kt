package com.worldline.devview.consolelogger.model

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/**
 * Represents a single captured console log line.
 *
 * Instances are produced either by the platform-native capture layer (logcat on Android,
 * a stdout/stderr redirect on iOS) or manually via
 * [com.worldline.devview.consolelogger.ConsoleLogger.log].
 *
 * @property level The severity of this log line.
 * @property tag The originating tag, e.g. an Android log tag. Empty when the source has no
 *           concept of a tag (such as a raw stdout line on iOS).
 * @property message The log message text.
 * @property timestamp Unix timestamp in milliseconds representing when the line was captured.
 *
 * @see LogLevel
 * @see com.worldline.devview.consolelogger.ConsoleLogger
 */
@Immutable
public data class ConsoleLog(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestamp: Long
) {
    @OptIn(ExperimentalUuidApi::class)
    internal val id: String = Uuid.random().toString()

    private val instantTimestamp = Instant.fromEpochMilliseconds(epochMilliseconds = timestamp)

    /**
     * Returns the timestamp formatted as HH:mm:ss in the current system timezone.
     *
     * This property is used internally by the console UI to display line times in a
     * human-readable format.
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
