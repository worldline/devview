package com.worldline.devview.crashhistory.core

import crashHistoryBridge.DevViewCrashDiagnostic
import crashHistoryBridge.DevViewMetricKitBridge
import kotlinx.cinterop.ExperimentalForeignApi

// The bridge owns the long-lived Task or legacy subscriber.
@OptIn(ExperimentalForeignApi::class)
private var metricKitBridge: DevViewMetricKitBridge? = null

/** Imports both MetricKit paths through an Objective-C-safe Swift snapshot. */
@OptIn(ExperimentalForeignApi::class)
internal fun subscribeToMetricKit(
    store: CrashStore,
    metadata: CrashMetadata,
    onImported: suspend (CrashRecord) -> Boolean,
    enqueueImport: (suspend () -> Unit) -> Unit
) {
    val importer = MetricKitImporter(store = store, metadata = metadata, onImported = onImported)
    val bridge = DevViewMetricKitBridge()
    metricKitBridge = bridge
    bridge.startWithOnDiagnostic { diagnostic ->
        val snapshot = diagnostic?.snapshot() ?: return@startWithOnDiagnostic
        // Capture the clear generation at delivery, using the engine-owned coroutine and lock.
        enqueueImport { importer.importDiagnostic(diagnostic = snapshot) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun DevViewCrashDiagnostic.snapshot(): SystemCrashDiagnostic = SystemCrashDiagnostic(
    occurrenceId = occurrenceId(),
    periodEndMillis = periodEndMillis(),
    signal = signal()?.intValue,
    exceptionType = exceptionType()?.intValue,
    exceptionCode = exceptionCode()?.stringValue,
    terminationReason = terminationReason(),
    callStackTreeJson = callStackTreeJson(),
    appVersion = appVersion(),
    appBuildVersion = appBuildVersion(),
    osVersion = osVersion(),
    deviceType = deviceType()
)
