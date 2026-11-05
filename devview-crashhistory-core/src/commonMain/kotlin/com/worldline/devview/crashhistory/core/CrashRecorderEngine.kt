package com.worldline.devview.crashhistory.core

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_PENDING_REPORTS = 20

/**
 * Normal storage work is ordered by one mutex. Fatal capture stays synchronous and never waits
 * for that mutex, because it runs in a dying process.
 */
@OptIn(ExperimentalAtomicApi::class, ExperimentalForInheritanceCoroutinesApi::class)
internal class CrashRecorderEngine(private val scope: CoroutineScope) {
    private data class Session(
        val platform: CrashPlatform,
        val store: CrashStore,
        val maxRecords: MutableStateFlow<Int>
    )

    private data class PendingReport(
        val timestampMillis: Long,
        val exceptionType: String,
        val message: String?,
        val stackTrace: String?
    )

    private data class Queue(
        val generation: Long = 0,
        val reports: List<PendingReport> = emptyList(),
        val records: List<CrashRecord> = emptyList()
    )

    private val session = MutableStateFlow<Session?>(value = null)
    private val pending = MutableStateFlow(value = Queue())
    private val crashHandled = AtomicBoolean(value = false)
    private val storageMutex = Mutex()
    private var generation = 0L

    val records: StateFlow<List<CrashRecord>> = object : StateFlow<List<CrashRecord>> {
        override val value: List<CrashRecord> get() = pending.value.records
        override val replayCache: List<List<CrashRecord>> get() = listOf(element = value)

        override suspend fun collect(collector: FlowCollector<List<CrashRecord>>): Nothing {
            var previous: List<CrashRecord>? = null
            pending.collect { state ->
                if (state.records != previous) {
                    previous = state.records
                    collector.emit(value = state.records)
                }
            }
        }
    }

    /** Installs handlers once and schedules loading and marker writes off the caller's thread. */
    fun start(platform: CrashPlatform, maxRecords: Int, markEnabled: Boolean) {
        require(value = maxRecords > 0) { "maxRecords must be positive" }
        val fresh = Session(
            platform = platform,
            store = CrashStore(directory = platform.storageDirectory),
            maxRecords = MutableStateFlow(value = maxRecords)
        )
        val isFirstStart = session.compareAndSet(expect = null, update = fresh)
        val current = if (isFirstStart) fresh else session.value ?: return
        current.maxRecords.value = maxRecords
        if (isFirstStart) platform.installUncaughtHandlers(onCrash = ::handleCrash)
        val expected = pending.value.generation
        scope.launch {
            try {
                var loaded = emptyList<CrashRecord>()
                withStorage(session = current, expected = expected) {
                    if (markEnabled) {
                        current.store.writeState(
                            name = CrashStore.ENABLED,
                            value = current.maxRecords.value.toString()
                        )
                    }
                    if (isFirstStart) {
                        loaded = current.store.loadAll()
                        if (expected != pending.value.generation) return@withStorage
                        publish(session = current, added = loaded, expected = expected)
                        flushPending(session = current)
                    }
                    current.store.prune(max = current.maxRecords.value)
                    publish(session = current, added = emptyList(), expected = expected)
                }
                if (isFirstStart) {
                    importHistory(session = current, existing = loaded, expected = expected)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: Exception) {
                // Best effort: without a readable history the recorder still captures new crashes.
            }
        }
    }

    private suspend fun importHistory(
        session: Session,
        existing: List<CrashRecord>,
        expected: Long
    ) {
        session.platform.importHistory(
            store = session.store,
            existing = existing,
            onImported = { record ->
                if (generation != pending.value.generation) {
                    false
                } else {
                    persist(session = session, record = record, prune = true)
                }
            },
            enqueueImport = { block ->
                enqueueImport(
                    session = session,
                    block = block
                )
            },
            withStorage = { block ->
                withStorage(
                    session = session,
                    expected = expected,
                    block = block
                )
            }
        )
    }

    private fun enqueueImport(session: Session, block: suspend () -> Unit) {
        val received = pending.value.generation
        scope.launch { withStorage(session = session, expected = received, block = block) }
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
        pending.update {
            it.copy(
                reports = (it.reports + report).takeLast(n = MAX_PENDING_REPORTS)
            )
        }
        val expected = pending.value.generation
        session.value?.let { current ->
            scope.launch {
                withStorage(
                    session = current,
                    expected = expected
                ) { flushPending(session = current) }
            }
        }
    }

    fun clear() {
        pending.update { Queue(generation = it.generation + 1) }
        session.value?.let { current ->
            scope.launch { withStorage(session = current, expected = pending.value.generation) {} }
        }
    }

    private suspend fun withStorage(session: Session, expected: Long, block: suspend () -> Unit) {
        storageMutex.withLock {
            val requested = pending.value.generation
            if (generation != requested) {
                generation = requested
                try {
                    session.store.clear()
                } catch (ignored: Exception) {
                    // Best effort: leftovers are removed by the next clear or prune.
                }
            }
            if (expected == generation) {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (ignored: Exception) {
                    // Best effort: storage failure must not affect the host app.
                }
            }
        }
    }

    private fun flushPending(session: Session) {
        val queued = pending.getAndUpdate {
            if (it.generation == generation) it.copy(reports = emptyList()) else it
        }
        if (queued.generation != generation) return
        queued.reports.forEach {
            if (generation != pending.value.generation) return
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

    private fun persist(session: Session, record: CrashRecord, prune: Boolean): Boolean {
        if (pending.value.records.any { it.id == record.id }) return true
        val expected = if (prune) generation else pending.value.generation
        if (!session.store.write(record = record) ||
            prune && generation != pending.value.generation
        ) {
            return false
        }
        if (prune) {
            try {
                session.store.prune(max = session.maxRecords.value)
            } catch (ignored: Exception) {
                // A saved record is still acknowledged if retention maintenance fails.
            }
        }
        publish(session = session, added = listOf(element = record), expected = expected)
        return true
    }

    private fun publish(session: Session, added: List<CrashRecord>, expected: Long) {
        pending.update {
            if (it.generation == expected) {
                it.copy(
                    records = merge(
                        current = it.records,
                        added = added,
                        max = session.maxRecords.value
                    )
                )
            } else {
                it
            }
        }
    }

    private fun merge(
        current: List<CrashRecord>,
        added: List<CrashRecord>,
        max: Int
    ): List<CrashRecord> = (current + added)
        .distinctBy { it.id }
        .sortedWith(
            comparator = compareByDescending<CrashRecord> {
                it.timestampMillis
            }.thenByDescending { it.id }
        ).take(n = max)
}
