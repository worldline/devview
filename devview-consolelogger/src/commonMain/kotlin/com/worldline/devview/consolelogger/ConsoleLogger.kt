package com.worldline.devview.consolelogger

import androidx.compose.runtime.mutableStateListOf
import com.worldline.devview.consolelogger.model.ConsoleLog
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central sink for native console capture, manual entries and [DevViewLogWriter].
 *
 * [log] enqueues into a bounded, drop-oldest channel from any thread. A single collector
 * owned by [Console] drains that queue into bounded, observable history. Entries submitted
 * before initialization or between collector lifetimes remain buffered within the limit.
 *
 * @see ConsoleLog
 */
public object ConsoleLogger {
    /** Queue capacity before a [Console] module supplies its retained-history limit. */
    internal const val DEFAULT_MAX_ENTRIES: Int = 1_000

    private val consoleLogs = mutableStateListOf<ConsoleLog>()
    private val lock = ConsoleLock()
    private val collectorMutex = Mutex()
    private var maxEntries: Int = DEFAULT_MAX_ENTRIES
    private var generation: Long = 0
    private var channel = newChannel(capacity = DEFAULT_MAX_ENTRIES)

    /**
     * Records an entry with a fresh internal occurrence ID, even when submitted repeatedly.
     *
     * Safe from any thread. If the pending queue is full, its oldest entry is dropped.
     *
     * @param log The console entry to record.
     */
    public fun log(log: ConsoleLog) {
        val recorded = log.copy()
        lock.withLock {
            channel.trySend(
                element = QueuedLog(log = recorded, generation = generation)
            )
        }
    }

    /** Clears retained, pending and previously received entries; subsequent logs still arrive. */
    internal fun clear() {
        lock.withLock {
            generation++
            while (channel.tryReceive().isSuccess) { /* Discard pending entries. */ }
            consoleLogs.clear()
        }
    }

    /** Reconfigures between collector lifetimes, preserving pending entries and trimming history. */
    internal fun configure(maxEntries: Int) {
        require(value = maxEntries in 1 until Channel.UNLIMITED) {
            "maxEntries must be between 1 and ${Channel.UNLIMITED - 1}, was $maxEntries"
        }
        lock.withLock {
            if (this.maxEntries != maxEntries) {
                val replacement = newChannel(capacity = maxEntries)
                while (true) {
                    val pending = channel.tryReceive().getOrNull() ?: break
                    replacement.trySend(element = pending)
                }
                channel = replacement
                this.maxEntries = maxEntries
            }
            while (consoleLogs.size > maxEntries) consoleLogs.removeAt(index = 0)
        }
    }

    /** Owns configuration, native capture and draining until both child jobs finish cancelling. */
    internal suspend fun collectInto(maxEntries: Int? = null, capture: suspend () -> Unit = {}) {
        collectorMutex.withLock {
            if (maxEntries != null) configure(maxEntries = maxEntries)
            val source = lock.withLock { channel }
            coroutineScope {
                launch { capture() }
                for (first in source) {
                    currentCoroutineContext().ensureActive()
                    appendBounded(entry = first)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val next = source.tryReceive().getOrNull() ?: break
                        appendBounded(entry = next)
                    }
                }
            }
        }
    }

    private fun appendBounded(entry: QueuedLog) {
        lock.withLock {
            // Clear can run after receive but before this append.
            if (entry.generation == generation) {
                if (consoleLogs.size >= maxEntries) consoleLogs.removeAt(index = 0)
                consoleLogs.add(element = entry.log)
            }
        }
    }

    /**
     * Read-only snapshot of retained entries, oldest first.
     *
     * Reading this property in composition observes history changes. Do not remember the
     * returned snapshot: read it again to obtain new entries. Record through [log] instead
     * of mutating history.
     */
    public val logs: List<ConsoleLog>
        get() = consoleLogs.toList()

    /** Whether retained history contains any entries. */
    public val hasLogs: Boolean
        get() = consoleLogs.isNotEmpty()

    private fun newChannel(capacity: Int): Channel<QueuedLog> =
        Channel(capacity = capacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private data class QueuedLog(val log: ConsoleLog, val generation: Long)
}
