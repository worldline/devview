package com.worldline.devview.crashhistory.core

import kotlinx.serialization.Serializable

/**
 * One entry of the local crash history.
 *
 * Records are persisted as JSON in app-private, non-backed-up storage. They are **raw**: no
 * redaction is applied (same approach as Crashlytics), and they only leave the device through an
 * explicit user action (copy, share or export).
 *
 * @property id Unique identifier, also used as part of the file name on disk.
 * @property timestampMillis Milliseconds since the Unix epoch: the crash time, or the reporting
 *   period end for MetricKit (which does not supply an exact crash time).
 * @property kind How the record was obtained, see [CrashRecordKind].
 * @property exceptionType Exception class name, signal name or OS exit reason, when known.
 * @property message Exception message or OS description, truncated to
 *   [CrashRecorder.MAX_MESSAGE_LENGTH] characters.
 * @property stackTrace Stack trace, truncated to [CrashRecorder.MAX_STACK_TRACE_LENGTH]
 *   characters. `null` when the platform provides none (for example a native Android crash).
 * @property threadName Name of the crashing thread, when known.
 * @property processId Id of the process that crashed, when known.
 * @property source Finer-grained origin of [CrashRecordKind.SYSTEM_REPORTED] records, for example
 *   `"exit-info:ANR"` or `"metrickit"`. `null` for in-process records.
 * @property isCrash `false` for system terminations that are not crashes (low memory, excessive
 *   resource usage). The UI shows them as "terminated by system".
 * @property metadata App and device information captured with the record.
 * @property truncated `true` when [message] or [stackTrace] was cut to fit the limits.
 */
@Serializable
public data class CrashRecord(
    val id: String,
    val timestampMillis: Long,
    val kind: CrashRecordKind,
    val exceptionType: String? = null,
    val message: String? = null,
    val stackTrace: String? = null,
    val threadName: String? = null,
    val processId: Int? = null,
    val source: String? = null,
    val isCrash: Boolean = true,
    val metadata: CrashMetadata,
    val truncated: Boolean = false
)

/**
 * How a [CrashRecord] was obtained. Every kind is confirmed: DevView does not guess. An unclean
 * exit for which neither the app nor the OS provides data is simply not recorded.
 */
@Serializable
public enum class CrashRecordKind {
    /** An uncaught exception caught by the handler DevView installed in the crashing process. */
    UNCAUGHT_EXCEPTION,

    /** A handled error or SDK-reported crash passed to [CrashRecorder.record]. */
    APP_REPORTED,

    /**
     * Reported by the OS on a later launch: Android `ApplicationExitInfo` (API 30+) or iOS
     * MetricKit. Covers native crashes, ANRs and system terminations.
     */
    SYSTEM_REPORTED
}

/**
 * App and device information captured with a [CrashRecord].
 *
 * @property platform `"android"` or `"ios"`.
 * @property osVersion Operating system version.
 * @property deviceModel Device manufacturer and model.
 * @property appVersion Application version name.
 * @property buildNumber Application build number or version code.
 */
@Serializable
public data class CrashMetadata(
    val platform: String,
    val osVersion: String,
    val deviceModel: String,
    val appVersion: String,
    val buildNumber: String
)
