// MXMetricManager/MXCrashDiagnostic are deprecated in iOS 27.2; the replacement is Swift-only async.
@file:Suppress("DEPRECATION")

package com.worldline.devview.crashhistory.core

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.MetricKit.MXCrashDiagnostic
import platform.MetricKit.MXDiagnosticPayload
import platform.MetricKit.MXMetricManager
import platform.MetricKit.MXMetricManagerSubscriberProtocol
import platform.darwin.NSObject

private const val SEEN_LIMIT = 100
private const val MILLIS_PER_SECOND = 1000

// Kept alive here: `MXMetricManager` holds its subscribers weakly.
private var metricKitSubscriber: MetricKitSubscriber? = null

/**
 * Receives crash diagnostics from MetricKit (iOS 14+) and hands each new one to [onImported].
 *
 * MetricKit delivers on its own schedule (typically on the launch after the crash), on real
 * devices only, and without symbolication.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun subscribeToMetricKit(
    store: CrashStore,
    metadata: CrashMetadata,
    onImported: suspend (CrashRecord) -> Boolean,
    enqueueImport: (suspend () -> Unit) -> Unit
) {
    val subscriber = MetricKitSubscriber(store, metadata, onImported, enqueueImport)
    metricKitSubscriber = subscriber
    val manager = MXMetricManager.sharedManager
    manager.addSubscriber(subscriber)
    // pastDiagnosticPayloads only exists from iOS 15; calling it on iOS 14 would crash.
    if (manager.respondsToSelector(NSSelectorFromString("pastDiagnosticPayloads"))) {
        subscriber.handle(manager.pastDiagnosticPayloads)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class MetricKitSubscriber(
    private val store: CrashStore,
    private val metadata: CrashMetadata,
    private val onImported: suspend (CrashRecord) -> Boolean,
    private val enqueueImport: (suspend () -> Unit) -> Unit
) : NSObject(),
    MXMetricManagerSubscriberProtocol {
    override fun didReceiveDiagnosticPayloads(payloads: List<*>) {
        handle(payloads)
    }

    fun handle(payloads: List<*>) {
        enqueueImport {
            payloads.filterIsInstance<MXDiagnosticPayload>().forEach { payload ->
                val timestamp = (payload.timeStampEnd.timeIntervalSince1970 * MILLIS_PER_SECOND)
                    .toLong()
                payload.crashDiagnostics.orEmpty().filterIsInstance<MXCrashDiagnostic>().forEach {
                        crash
                    ->
                    importOnce(
                        json = jsonOf(crash.JSONRepresentation()),
                        timestampMillis = timestamp
                    )
                }
            }
        }
    }

    private suspend fun importOnce(json: String, timestampMillis: Long) {
        val seen = store
            .readState(
                CrashStore.METRICKIT_SEEN
            )?.lines()
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val key = json.hashCode().toString()
        if (key in seen) return
        val record = crashDiagnosticToRecord(json, timestampMillis, metadata) ?: return
        if (!onImported(record)) return
        // Seen state and persistence share the engine lock; failed writes are retried.
        store.writeState(
            CrashStore.METRICKIT_SEEN,
            (seen + key).takeLast(SEEN_LIMIT).joinToString("\n")
        )
    }

    private fun jsonOf(data: NSData): String = NSString
        .create(
            data = data,
            encoding = NSUTF8StringEncoding
        )?.toString() ?: ""
}
