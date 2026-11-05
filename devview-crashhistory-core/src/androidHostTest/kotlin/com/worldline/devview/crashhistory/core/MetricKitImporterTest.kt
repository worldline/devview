package com.worldline.devview.crashhistory.core

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path

class MetricKitImporterTest {
    private val dir = Files.createTempDirectory("metrickit-importer").toFile()
    private val store = CrashStore(Path(dir.absolutePath))
    private val diagnostic = SystemCrashDiagnostic(occurrenceId = "a".repeat(64), periodEndMillis = 123)

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `failed seen write survives clear and retries without reimporting`() = runTest {
        val blocker = File(dir, CrashStore.METRICKIT_SEEN)
        var imports = 0
        val importer = MetricKitImporter(store = store, metadata = testMetadata) { record ->
            imports++
            blocker.mkdir()
            store.write(record)
        }
        importer.importDiagnostic(diagnostic)
        imports shouldBe 1
        store.loadAll().size shouldBe 1

        store.clear()
        blocker.delete()
        importer.importDiagnostic(diagnostic)

        imports shouldBe 1
        store.loadAll().shouldBeEmpty()
        store.readState(CrashStore.METRICKIT_SEEN) shouldBe diagnostic.occurrenceId
    }

    @Test
    fun `failed record is not marked seen and is retried`() = runTest {
        var imports = 0
        val importer = MetricKitImporter(store = store, metadata = testMetadata) {
            imports++
            imports > 1
        }
        importer.importDiagnostic(diagnostic)
        store.readState(CrashStore.METRICKIT_SEEN) shouldBe null
        importer.importDiagnostic(diagnostic)
        imports shouldBe 2
        store.readState(CrashStore.METRICKIT_SEEN) shouldBe diagnostic.occurrenceId
    }
}
