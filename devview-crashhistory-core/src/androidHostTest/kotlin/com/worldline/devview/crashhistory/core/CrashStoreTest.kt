package com.worldline.devview.crashhistory.core

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.io.files.Path

class CrashStoreTest {
    private val dir: File = Files.createTempDirectory("crashstore").toFile()
    private val store = CrashStore(Path(dir.absolutePath, "records"))

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `records round-trip and load newest first`() {
        val old = testRecord(id = "1-a", timestampMillis = 1_000)
        val new = testRecord(id = "2-b", timestampMillis = 2_000)
        store.write(old)
        store.write(new)

        store.loadAll() shouldContainExactly listOf(new, old)
    }

    @Test
    fun `prune keeps only the newest records`() {
        (1..5).forEach { store.write(testRecord(id = "$it-x", timestampMillis = it * 1_000L)) }

        store.prune(max = 2)

        store.loadAll().map { it.id } shouldContainExactly listOf("5-x", "4-x")
    }

    @Test
    fun `corrupt files are skipped and deleted`() {
        store.write(testRecord(id = "1-a"))
        File(dir, "records/000000000000001-bad.json").writeText("{ not json")

        store.loadAll().map { it.id } shouldContainExactly listOf("1-a")
        File(dir, "records").listFiles()!!.map { it.name }.none { it.endsWith("bad.json") } shouldBe true
    }

    @Test
    fun `clear removes records but keeps state files`() {
        store.write(testRecord())
        store.writeState(CrashStore.EXIT_INFO_CURSOR, "123")

        store.clear()

        store.loadAll().shouldBeEmpty()
        store.readState(CrashStore.EXIT_INFO_CURSOR) shouldBe "123"
    }

    @Test
    fun `missing state and missing directory read as empty`() {
        store.readState("nope").shouldBeNull()
        store.loadAll().shouldBeEmpty()
    }

    @Test
    fun `concurrent state writes succeed and leave no temporary files`() {
        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val go = CountDownLatch(1)
        try {
            val writes = (1..2).map { value ->
                executor.submit<Boolean> {
                    ready.countDown()
                    go.await(5, TimeUnit.SECONDS)
                    store.writeState("state", value.toString())
                }
            }
            ready.await(5, TimeUnit.SECONDS) shouldBe true
            go.countDown()
            writes.forEach { it.get(5, TimeUnit.SECONDS) shouldBe true }
            (store.readState("state")!!.toInt() in 1..2) shouldBe true
            File(dir, "records").listFiles()!!.none { it.name.endsWith(".tmp") } shouldBe true
        } finally {
            go.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `failed writes return false`() {
        val blocker = File(dir, "blocked").apply { writeText("x") }
        val broken = CrashStore(Path(blocker.absolutePath, "records"))
        broken.write(testRecord()) shouldBe false
        broken.writeState("state", "value") shouldBe false
    }

    @Test
    fun `no temp files are left behind after a write`() {
        store.write(testRecord())

        File(dir, "records").listFiles()!!.none { it.name.endsWith(".tmp") } shouldBe true
    }
}
