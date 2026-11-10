package com.worldline.devview.crashhistory

import com.worldline.devview.crashhistory.core.CrashRecord
import com.worldline.devview.crashhistory.core.CrashRecordKind
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

private val timeFormat = LocalDateTime.Format {
    date(format = LocalDate.Formats.ISO)
    char(value = ' ')
    hour()
    char(value = ':')
    minute()
    char(value = ':')
    second()
}

/** `yyyy-MM-dd HH:mm:ss` in the device's time zone. */
internal fun formatCrashTime(timestampMillis: Long): String = Instant
    .fromEpochMilliseconds(epochMilliseconds = timestampMillis)
    .toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
    .format(format = timeFormat)

/** Short label telling how sure we are that this was a crash. */
internal val CrashRecord.kindLabel: String
    get() = when (kind) {
        CrashRecordKind.UNCAUGHT_EXCEPTION -> "Crash"
        CrashRecordKind.APP_REPORTED -> "Reported"
        CrashRecordKind.SYSTEM_REPORTED -> if (isCrash) "System report" else "Killed by system"
    }
