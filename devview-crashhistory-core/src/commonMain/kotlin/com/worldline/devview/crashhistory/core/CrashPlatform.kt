package com.worldline.devview.crashhistory.core

import androidx.compose.runtime.Composable
import kotlinx.io.files.Path

/**
 * Receives a crash captured by a platform handler. Must never throw and must stay fast: it runs
 * inside the dying process.
 */
internal typealias CrashSink = (
    exceptionType: String?,
    message: String?,
    stackTrace: String?,
    threadName: String?
) -> Unit

/** Everything [CrashRecorder] needs from the platform. */
internal interface CrashPlatform {
    /** App-private, non-backed-up directory holding the records and state files. */
    val storageDirectory: Path

    /** App and device information stamped on every record. */
    val metadata: CrashMetadata

    /** Id of the current process, stamped on in-process records. */
    val processId: Int

    /**
     * Installs the uncaught-exception handlers. Each one must call the previously installed
     * handler after reporting to [onCrash], so Crashlytics and friends keep working.
     */
    fun installUncaughtHandlers(onCrash: CrashSink)

    /**
     * Hands records the OS kept about previous runs (Android exit reasons, iOS MetricKit) to
     * [onImported], skipping those already in [existing]. May be called from any thread and may
     * call [onImported] later (MetricKit delivers asynchronously). Implementations must advance
     * their "already seen" state only after [onImported] returned, so a failed write is retried.
     */
    fun importHistory(
        store: CrashStore,
        existing: List<CrashRecord>,
        onImported: (CrashRecord) -> Unit
    )
}

/** Remembers the [CrashPlatform] of the current app. */
@Composable
internal expect fun rememberCrashPlatform(): CrashPlatform
