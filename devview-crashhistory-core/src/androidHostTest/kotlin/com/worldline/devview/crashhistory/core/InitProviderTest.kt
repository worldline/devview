package com.worldline.devview.crashhistory.core

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.io.files.Path

class InitProviderTest {
    private val dir: File = Files.createTempDirectory("crashprovider").toFile()
    private val platform = FakeCrashPlatform(Path(dir.absolutePath, "records"))
    private val started = mutableListOf<Int>()

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `no marker means no start`() {
        startIfEnabled(platform) { _, max -> started += max }

        started.shouldBeEmpty()
    }

    @Test
    fun `marker starts the recorder with the stored maxRecords`() {
        CrashStore(platform.storageDirectory).writeState(CrashStore.ENABLED, "12")

        startIfEnabled(platform) { _, max -> started += max }

        started shouldContainExactly listOf(12)
    }

    @Test
    fun `nonpositive markers mean no start`() {
        listOf("0", "-1").forEach { value ->
            CrashStore(platform.storageDirectory).writeState(CrashStore.ENABLED, value)
            startIfEnabled(platform) { _, max -> started += max }
        }
        started.shouldBeEmpty()
    }

    @Test
    fun `a corrupt marker means no start`() {
        CrashStore(platform.storageDirectory).writeState(CrashStore.ENABLED, "garbage")

        startIfEnabled(platform) { _, max -> started += max }

        started.shouldBeEmpty()
    }
}
