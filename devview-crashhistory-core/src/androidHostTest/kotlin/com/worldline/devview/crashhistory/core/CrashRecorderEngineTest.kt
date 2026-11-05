package com.worldline.devview.crashhistory.core

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.io.files.Path

class CrashRecorderEngineTest {
    private val dir: File = Files.createTempDirectory("crashengine").toFile()
    private val platform = FakeCrashPlatform(Path(dir.absolutePath, "records"))

    // Unconfined: launched work runs inline, so tests need no waiting.
    private val engine = CrashRecorderEngine(CoroutineScope(Dispatchers.Unconfined))

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `start installs handlers only once`() {
        engine.start(platform, maxRecords = 5, markEnabled = false)
        engine.start(platform, maxRecords = 5, markEnabled = false)

        platform.installCount shouldBe 1
    }

    @Test
    fun `latest retention prunes memory and disk without reinstalling handlers`() {
        engine.start(platform, maxRecords = 20, markEnabled = true)
        (1..20).forEach { engine.record("Error", "$it", null, it.toLong()) }

        engine.start(platform, maxRecords = 5, markEnabled = true)

        platform.installCount shouldBe 1
        engine.records.value shouldHaveSize 5
        val store = CrashStore(platform.storageDirectory)
        store.loadAll() shouldBe engine.records.value
        store.readState(CrashStore.ENABLED) shouldBe "5"
    }

    @Test
    fun `nonpositive retention is rejected before installing handlers`() {
        listOf(0, -1).forEach { max ->
            assertFailsWith<IllegalArgumentException> {
                engine.start(platform, maxRecords = max, markEnabled = false)
            }
        }
        platform.installCount shouldBe 0
    }

    @Test
    fun `start with markEnabled writes the marker`() {
        engine.start(platform, maxRecords = 7, markEnabled = true)

        CrashStore(platform.storageDirectory).readState(CrashStore.ENABLED) shouldBe "7"
    }

    @Test
    fun `an uncaught crash is persisted and published`() {
        engine.start(platform, maxRecords = 5, markEnabled = false)

        platform.crash()

        val record = engine.records.value.single()
        record.kind shouldBe CrashRecordKind.UNCAUGHT_EXCEPTION
        record.message shouldBe "boom"
        record.threadName shouldBe "main"
        record.processId shouldBe 42
        CrashStore(platform.storageDirectory).loadAll() shouldHaveSize 1
    }

    @Test
    fun `only the first crash of a process is written`() {
        engine.start(platform, maxRecords = 5, markEnabled = false)

        platform.crash(message = "first")
        platform.crash(message = "second")

        engine.records.value.single().message shouldBe "first"
    }

    @Test
    fun `long messages and stack traces are truncated and flagged`() {
        engine.start(platform, maxRecords = 5, markEnabled = false)

        platform.crash(
            message = "m".repeat(CrashRecorder.MAX_MESSAGE_LENGTH + 10),
            stackTrace = "s".repeat(CrashRecorder.MAX_STACK_TRACE_LENGTH + 10)
        )

        val record = engine.records.value.single()
        record.message!!.length shouldBe CrashRecorder.MAX_MESSAGE_LENGTH
        record.stackTrace!!.length shouldBe CrashRecorder.MAX_STACK_TRACE_LENGTH
        record.truncated shouldBe true
    }

    @Test
    fun `a failing write never throws inside the crash handler`() {
        // A regular file where the directory should be makes every write fail.
        val blocker = File(dir, "blocker").apply { writeText("x") }
        val broken = FakeCrashPlatform(Path(blocker.absolutePath, "records"))
        engine.start(broken, maxRecords = 5, markEnabled = false)

        broken.crash() // must not throw

        engine.records.value.shouldBeEmpty()
    }

    @Test
    fun `reports made before start are buffered then written`() {
        engine.record("java.lang.Error", "early", "trace", timestampMillis = 10)
        engine.records.value.shouldBeEmpty()

        engine.start(platform, maxRecords = 5, markEnabled = false)

        val record = engine.records.value.single()
        record.kind shouldBe CrashRecordKind.APP_REPORTED
        record.message shouldBe "early"
        record.timestampMillis shouldBe 10
    }

    @Test
    fun `reports after start are written immediately`() {
        engine.start(platform, maxRecords = 5, markEnabled = false)

        engine.record("java.lang.Error", "late", null, timestampMillis = 10)

        engine.records.value.single().message shouldBe "late"
    }

    @Test
    fun `existing history is loaded and pruned to maxRecords`() {
        val store = CrashStore(platform.storageDirectory)
        (1..5).forEach { store.write(testRecord(id = "$it-x", timestampMillis = it * 1_000L)) }

        engine.start(platform, maxRecords = 3, markEnabled = false)

        engine.records.value.map { it.id } shouldBe listOf("5-x", "4-x", "3-x")
        store.loadAll() shouldHaveSize 3
    }

    @Test
    fun `platform history is imported`() {
        platform.historyToImport = listOf(testRecord(id = "9-sys", kind = CrashRecordKind.SYSTEM_REPORTED))

        engine.start(platform, maxRecords = 5, markEnabled = false)

        engine.records.value.single().kind shouldBe CrashRecordKind.SYSTEM_REPORTED
        CrashStore(platform.storageDirectory).loadAll().single().id shouldBe "9-sys"
    }

    @Test
    fun `clear during import does not bring records back`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val resume = CompletableDeferred<Unit>()
        platform.historyToImport = listOf(testRecord(id = "old"))
        platform.beforeImport = { entered.complete(Unit); resume.await() }
        val recorder = CrashRecorderEngine(this)
        recorder.start(platform, maxRecords = 5, markEnabled = false)
        entered.await()

        recorder.clear()
        resume.complete(Unit)
        runCurrent()

        recorder.records.value.shouldBeEmpty()
        CrashStore(platform.storageDirectory).loadAll().shouldBeEmpty()
    }

    @Test
    fun `queued load and reports are discarded by clear but later reports survive`() = runTest {
        val store = CrashStore(platform.storageDirectory)
        store.write(testRecord(id = "old"))
        val recorder = CrashRecorderEngine(CoroutineScope(StandardTestDispatcher(testScheduler)))
        recorder.start(platform, maxRecords = 5, markEnabled = false)
        recorder.record("Error", "old", null, 1)
        recorder.clear()
        recorder.record("Error", "new", null, 2)
        runCurrent()

        recorder.records.value.single().message shouldBe "new"
        store.loadAll().single().message shouldBe "new"
    }

    @Test
    fun `delivery queued before clear is discarded but later deliveries work`() = runTest {
        val recorder = CrashRecorderEngine(this)
        recorder.start(platform, maxRecords = 5, markEnabled = false)
        runCurrent()
        platform.deliver(testRecord(id = "old"))
        recorder.clear()
        runCurrent()
        recorder.records.value.shouldBeEmpty()

        platform.deliver(testRecord(id = "new"))
        runCurrent()
        recorder.records.value.single().id shouldBe "new"
        CrashStore(platform.storageDirectory).loadAll().single().id shouldBe "new"
    }

    @Test
    fun `cancelled engine does not process later deliveries`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val recorder = CrashRecorderEngine(scope)
        recorder.start(platform, maxRecords = 5, markEnabled = false)
        runCurrent()
        scope.cancel()
        platform.deliver(testRecord(id = "cancelled"))
        runCurrent()
        recorder.records.value.shouldBeEmpty()
        CrashStore(platform.storageDirectory).loadAll().shouldBeEmpty()
    }

    @Test
    fun `equal timestamps retain the same record in memory and on disk`() {
        platform.historyToImport = listOf(testRecord(id = "a"), testRecord(id = "b"))
        engine.start(platform, maxRecords = 1, markEnabled = false)
        engine.records.value.single().id shouldBe "b"
        CrashStore(platform.storageDirectory).loadAll().single().id shouldBe "b"
    }

    @Test
    fun `failed imports keep the cursor and retry on the next start`() {
        val blocker = File(dir, "blocked").apply { writeText("x") }
        val broken = FakeCrashPlatform(Path(blocker.absolutePath, "records"))
        broken.historyToImport = listOf(testRecord(id = "system", timestampMillis = 10))
        engine.start(broken, maxRecords = 5, markEnabled = false)
        engine.records.value.shouldBeEmpty()
        blocker.delete()

        val next = CrashRecorderEngine(CoroutineScope(Dispatchers.Unconfined))
        next.start(broken, maxRecords = 5, markEnabled = false)

        next.records.value.single().id shouldBe "system"
        CrashStore(broken.storageDirectory).readState(CrashStore.EXIT_INFO_CURSOR) shouldBe "10"
    }

    @Test
    fun `clear empties the list and the files`() {
        engine.start(platform, maxRecords = 5, markEnabled = false)
        platform.crash()
        engine.records.value.shouldNotBeNull() shouldHaveSize 1

        engine.clear()

        engine.records.value.shouldBeEmpty()
        CrashStore(platform.storageDirectory).loadAll().shouldBeEmpty()
    }
}
