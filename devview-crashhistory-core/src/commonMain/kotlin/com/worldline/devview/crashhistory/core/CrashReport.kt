package com.worldline.devview.crashhistory.core

import kotlin.time.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Formats this record as human-readable text, for the clipboard or the share sheet.
 *
 * The text contains the raw message and stack trace.
 */
public fun CrashRecord.toReportText(): String = buildString {
    appendLine(value = exceptionType ?: "Unknown error")
    message?.let { appendLine(value = it) }
    appendLine()
    val timeLabel = if (source == "metrickit") "Reporting period end" else "Time"
    appendLine(
        value = "$timeLabel: ${Instant.fromEpochMilliseconds(epochMilliseconds = timestampMillis)}"
    )
    appendLine(value = "Kind: $kind${source?.let { " ($it)" }.orEmpty()}")
    if (!isCrash) appendLine(value = "Terminated by the system, not a crash")
    threadName?.let { appendLine(value = "Thread: $it") }
    processId?.let { appendLine(value = "Process: $it") }
    appendLine(value = "App: ${metadata.appVersion} (${metadata.buildNumber})")
    appendLine(
        value = "Device: ${metadata.deviceModel}, ${metadata.platform} ${metadata.osVersion}"
    )
    if (truncated) appendLine(value = "Note: truncated to fit the history limits")
    stackTrace?.let {
        appendLine()
        append(it)
    }
}

/**
 * Serializes these records as a pretty-printed JSON array, for file export.
 *
 * The JSON contains raw messages and stack traces.
 */
public fun List<CrashRecord>.toJson(): String = exportJson.encodeToString(
    serializer = ListSerializer(elementSerializer = CrashRecord.serializer()),
    value = this
)
