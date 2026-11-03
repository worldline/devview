package com.worldline.devview.crashhistory.core

import kotlinx.io.files.Path

/** Test double: captures the crash sink instead of installing real handlers. */
internal class FakeCrashPlatform(override val storageDirectory: Path) : CrashPlatform {
    override val metadata: CrashMetadata = testMetadata
    override val processId: Int = 42

    var installCount = 0
    var crashSink: CrashSink? = null
    var historyToImport: List<CrashRecord> = emptyList()

    override fun installUncaughtHandlers(onCrash: CrashSink) {
        installCount++
        crashSink = onCrash
    }

    override fun importHistory(store: CrashStore, existing: List<CrashRecord>, onImported: (CrashRecord) -> Unit) {
        historyToImport.forEach(onImported)
    }

    /** Simulates the platform handler receiving an uncaught exception. */
    fun crash(message: String? = "boom", stackTrace: String? = "at Foo.bar(Foo.kt:1)") {
        crashSink?.invoke("java.lang.IllegalStateException", message, stackTrace, "main")
    }
}
