package com.worldline.devview.crashhistory.core

private const val SEEN_LIMIT = 100

/** Called under the recorder's storage lock, including retries of a failed seen-state write. */
internal class MetricKitImporter(
    private val store: CrashStore,
    private val metadata: CrashMetadata,
    private val onImported: suspend (CrashRecord) -> Boolean
) {
    private var seen: List<String>? = null
    private var needsSave = false

    suspend fun importDiagnostic(diagnostic: SystemCrashDiagnostic) {
        val current = seen ?: store
            .readState(name = CrashStore.METRICKIT_SEEN)
            ?.lines()
            ?.filter { it.isNotBlank() }
            .orEmpty()
            .takeLast(n = SEEN_LIMIT)
        seen = current
        if (diagnostic.occurrenceId !in current) {
            val record = crashDiagnosticToRecord(
                diagnostic = diagnostic,
                metadata = metadata
            ) ?: return
            if (!onImported(record)) return
            // Keep IDs through Clear even if saving fails; restart still requires a writable store.
            seen = (current + diagnostic.occurrenceId).takeLast(n = SEEN_LIMIT)
            needsSave = true
        }
        if (needsSave) {
            needsSave = !store.writeState(
                name = CrashStore.METRICKIT_SEEN,
                value = seen.orEmpty().joinToString(separator = "\n")
            )
        }
    }
}
