package com.worldline.devview.crashhistory.core

import android.app.ApplicationExitInfo
import kotlin.math.abs

/**
 * Maps one Android `ApplicationExitInfo` (API 30+) to a [CrashRecord], or `null` when the exit
 * is not worth recording (user swipe, app update, normal exit, ...).
 *
 * - CRASH, CRASH_NATIVE, ANR and INITIALIZATION_FAILURE are crashes.
 * - LOW_MEMORY and EXCESSIVE_RESOURCE_USAGE are kills by the system, flagged `isCrash = false`.
 *
 * Pure function on plain values so it can be unit-tested without a device.
 */
@Suppress("LongParameterList")
internal fun exitInfoToRecord(
    reason: Int,
    description: String?,
    timestampMillis: Long,
    processId: Int,
    trace: String?,
    metadata: CrashMetadata
): CrashRecord? {
    val (type, isCrash) = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "Crash" to true
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash" to true
        ApplicationExitInfo.REASON_ANR -> "ANR (app not responding)" to true
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "Initialization failure" to true
        ApplicationExitInfo.REASON_LOW_MEMORY -> "Low memory" to false
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Excessive resource usage" to false
        else -> return null
    }
    return buildCrashRecord(
        kind = CrashRecordKind.SYSTEM_REPORTED,
        metadata = metadata,
        timestampMillis = timestampMillis,
        exceptionType = type,
        message = description,
        stackTrace = trace,
        processId = processId,
        source = "exit-info:${exitReasonName(reason = reason)}",
        isCrash = isCrash
    )
}

private fun exitReasonName(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_CRASH -> "CRASH"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
    ApplicationExitInfo.REASON_ANR -> "ANR"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
    else -> "EXCESSIVE_RESOURCE_USAGE"
}

private const val DUPLICATE_WINDOW_MILLIS = 60_000L

/**
 * `true` when [record] describes a crash that the in-process handler already recorded: same
 * process id, close in time. Android reports every Java crash again as `REASON_CRASH`.
 */
internal fun isAlreadyRecorded(record: CrashRecord, existing: List<CrashRecord>): Boolean =
    record.exceptionType == "Crash" &&
        existing.any {
            it.kind == CrashRecordKind.UNCAUGHT_EXCEPTION &&
                it.processId == record.processId &&
                abs(n = it.timestampMillis - record.timestampMillis) < DUPLICATE_WINDOW_MILLIS
        }
