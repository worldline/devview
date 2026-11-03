package com.worldline.devview.crashhistory.core

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_PENDING_REPORTS = 20

/**
 * The recorder's logic, kept apart from the [CrashRecorder] singleton so tests can build isolated
 * instances with a fake [CrashPlatform] and their own [CoroutineScope].
 *
 * Lock-free by design: state lives in [MutableStateFlow]s updated with compare-and-set, because
 * the crash handler runs inside a dying process where blocking on a lock is not an option.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class CrashRecorderEngine(private val scope: CoroutineScope) {
    private data class Session(
        val platform: CrashPlatform,
        val store: CrashStore,
        val maxRecords: Int
    )

    private data class PendingReport(
        val timestampMillis: Long,
        val exceptionType: String,
        val message: String?,
        val stackTrace: String?
    )

    private val session = MutableStateFlow<Session?>(value = null)
    private val pending = MutableStateFlow<List<PendingReport>>(value = emptyList())
    private val mutableRecords = MutableStateFlow<List<CrashRecord>>(value = emptyList())
    private val crashHandled = AtomicBoolean(value = false)

    val records: StateFlow<List<CrashRecord>> = mutableRecords.asStateFlow()

    /**
     * Installs the handlers and loads the history. Idempotent: only the first call installs;
     * later calls only refresh the `enabled` marker when [markEnabled] is set.
     */
    fun start(platform: CrashPlatform, maxRecords: Int, markEnabled: Boolean) {
        val fresh = Session(
            platform = platform,
            store = CrashStore(directory = platform.storageDirectory),
            maxRecords = maxRecords
        )
        val isFirstStart = session.compareAndSet(expect = null, update = fresh)
        val current = if (isFirstStart) fresh else session.value ?: return
        if (isFirstStart) platform.installUncaughtHandlers(onCrash = ::handleCrash)
        scope.launch {
            try {
                if (markEnabled) {
                    current.store.writeState(
                        name = CrashStore.ENABLED,
                        value = maxRecords.toString()
                    )
                }
                if (isFirstStart) loadAndImport(session = current)
            } catch (ignored: Exception) {
                // Best effort: without a readable history the recorder still captures new crashes.
            }
        }
    }

    fun record(
        exceptionType: String,
        message: String?,
        stackTrace: String?,
        timestampMillis: Long
    ) {
        val report = PendingReport(
            timestampMillis = timestampMillis,
            exceptionType = exceptionType,
            message = message,
            stackTrace = stackTrace
        )
        pending.update { (it + report).takeLast(n = MAX_PENDING_REPORTS) }
        // Written off the caller's thread: callers are often on the main thread.
        session.value?.let { current -> scope.launch { flushPending(session = current) } }
    }

    fun clear() {
        pending.value = emptyList()
        mutableRecords.value = emptyList()
        try {
            session.value?.store?.clear()
        } catch (ignored: Exception) {
            // Best effort: leftovers are removed by the next clear or prune.
        }
    }

    private fun loadAndImport(session: Session) {
        val loaded = session.store.loadAll()
        mutableRecords.update { merge(current = it, added = loaded, max = session.maxRecords) }
        flushPending(session = session)
        session.platform.importHistory(
            store = session.store,
            existing = loaded,
            onImported = { persist(session = session, record = it, prune = true) }
        )
        session.store.prune(max = session.maxRecords)
    }

    private fun flushPending(session: Session) {
        pending.getAndUpdate { emptyList() }.forEach {
            val record = buildCrashRecord(
                kind = CrashRecordKind.APP_REPORTED,
                metadata = session.platform.metadata,
                timestampMillis = it.timestampMillis,
                exceptionType = it.exceptionType,
                message = it.message,
                stackTrace = it.stackTrace,
                processId = session.platform.processId
            )
            persist(session = session, record = record, prune = true)
        }
    }

    // Called by the platform handlers from the crashing thread. Never throws.
    private fun handleCrash(
        exceptionType: String?,
        message: String?,
        stackTrace: String?,
        threadName: String?
    ) {
        try {
            val current = session.value ?: return
            if (!crashHandled.compareAndSet(expectedValue = false, newValue = true)) return
            val record = buildCrashRecord(
                kind = CrashRecordKind.UNCAUGHT_EXCEPTION,
                metadata = current.platform.metadata,
                timestampMillis = Clock.System.now().toEpochMilliseconds(),
                exceptionType = exceptionType,
                message = message,
                stackTrace = stackTrace,
                threadName = threadName,
                processId = current.platform.processId
            )
            // No pruning here: it lists the directory. The next start prunes, keeping the handler fast.
            persist(session = current, record = record, prune = false)
        } catch (ignored: Throwable) {
            // The original crash must proceed to the previous handler whatever happens here.
        }
    }

    private fun persist(session: Session, record: CrashRecord, prune: Boolean) {
        try {
            session.store.write(record = record)
            if (prune) session.store.prune(max = session.maxRecords)
            mutableRecords.update {
                merge(
                    current = it,
                    added = listOf(element = record),
                    max = session.maxRecords
                )
            }
        } catch (ignored: Exception) {
            // A full disk must never turn a handled report into a crash of its own.
        }
    }

    private fun merge(
        current: List<CrashRecord>,
        added: List<CrashRecord>,
        max: Int
    ): List<CrashRecord> =
        (current + added).distinctBy { it.id }.sortedByDescending { it.timestampMillis }.take(
            n = max
        )
}
