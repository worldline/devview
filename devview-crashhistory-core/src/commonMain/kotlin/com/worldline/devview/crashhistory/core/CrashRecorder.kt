package com.worldline.devview.crashhistory.core

import androidx.compose.runtime.Composable
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

/**
 * Local crash history: captures crashes into a bounded, persistent list on the device.
 *
 * Nothing leaves the device unless the user copies, shares or exports it. Crashlytics, Sentry and
 * other crash SDKs stay the source of truth; this is a convenience for testers and developers.
 *
 * ## What is captured
 * - Uncaught exceptions of the running app ([CrashRecordKind.UNCAUGHT_EXCEPTION]). The handler is
 *   chained: the previously installed handler (Crashlytics, ...) is always called afterwards.
 * - Errors reported with [record] ([CrashRecordKind.APP_REPORTED]).
 * - What the OS reports about earlier runs ([CrashRecordKind.SYSTEM_REPORTED]): Android
 *   `ApplicationExitInfo` (API 30+) and iOS MetricKit (iOS 14+). This is the only way native
 *   crashes show up: DevView installs no signal handlers of its own.
 *
 * ## Storage
 * Records are raw (not redacted), stored as one JSON file each in app-private, non-backed-up
 * storage. At most `maxRecords` are kept, oldest first to go. Messages are cut to
 * [MAX_MESSAGE_LENGTH] and stack traces to [MAX_STACK_TRACE_LENGTH] characters.
 *
 * ## Setup
 * Add `CrashHistory()` to `rememberModules` (module `devview-crashhistory`) which calls
 * [install]. On Android the library also starts itself before `Application.onCreate` from the
 * second launch on. On iOS, `CrashHistoryIos.shared.start()` can be called earlier.
 */
public object CrashRecorder {
    /** Default number of records kept. */
    public const val DEFAULT_MAX_RECORDS: Int = 20

    /** Maximum length of [CrashRecord.message]; longer messages are cut. */
    public const val MAX_MESSAGE_LENGTH: Int = 2_000

    /** Maximum length of [CrashRecord.stackTrace]; longer stack traces are cut. */
    public const val MAX_STACK_TRACE_LENGTH: Int = 32_000

    // shortcut: global IO scope, no DI in a process-wide singleton; tests build their own engine
    @Suppress("InjectDispatcher")
    private val engine = CrashRecorderEngine(
        scope = CoroutineScope(context = SupervisorJob() + Dispatchers.IO)
    )

    /** The recorded history, newest first. Empty until [install] (or the auto-start) has loaded it. */
    public val records: StateFlow<List<CrashRecord>> get() = engine.records

    /**
     * Starts recording. Safe to call repeatedly; only the first call installs the handlers.
     *
     * Called by `CrashHistory.initModule()`, so most integrators never call it directly.
     *
     * The latest call updates retention without reinstalling handlers. Zero is unsupported.
     *
     * @param maxRecords Positive number of records to keep.
     * @throws IllegalArgumentException if [maxRecords] is not positive.
     */
    @Suppress("ComposableNaming")
    @Composable
    public fun install(maxRecords: Int = DEFAULT_MAX_RECORDS) {
        require(value = maxRecords > 0) { "maxRecords must be positive" }
        val platform = rememberCrashPlatform()
        // Idempotent and cheap after the first call; handlers must be live before the first frame.
        engine.start(platform = platform, maxRecords = maxRecords, markEnabled = true)
    }

    /**
     * Records a handled error or one forwarded from a crash SDK as [CrashRecordKind.APP_REPORTED].
     *
     * Reports made before [install] are kept in memory (the last 20) and written once recording
     * starts.
     *
     * @param throwable The error to record.
     */
    public fun record(throwable: Throwable) {
        record(
            exceptionType =
                throwable::class.qualifiedName ?: throwable::class.simpleName ?: "Throwable",
            message = throwable.message,
            stackTrace = throwable.stackTraceToString()
        )
    }

    /**
     * Records an error described by plain values, for adapters that only have a crash SDK's own
     * event type.
     *
     * @param exceptionType Exception class name or similar.
     * @param message Error message.
     * @param stackTrace Stack trace text.
     * @param timestampMillis When the error happened, in milliseconds since the Unix epoch.
     */
    public fun record(
        exceptionType: String,
        message: String?,
        stackTrace: String?,
        timestampMillis: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        engine.record(
            exceptionType = exceptionType,
            message = message,
            stackTrace = stackTrace,
            timestampMillis = timestampMillis
        )
    }

    /** Clears the visible history immediately and schedules deletion from disk. */
    public fun clear() {
        engine.clear()
    }

    /** Starts recording without Compose (Android init provider, iOS entry point). */
    internal fun start(platform: CrashPlatform, maxRecords: Int) {
        engine.start(platform = platform, maxRecords = maxRecords, markEnabled = false)
    }
}
