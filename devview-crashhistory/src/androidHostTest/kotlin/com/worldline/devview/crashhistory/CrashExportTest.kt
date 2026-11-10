package com.worldline.devview.crashhistory

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test

class CrashExportTest {
    private val dir = Files.createTempDirectory("crash-export").toFile()

    @AfterTest
    fun tearDown() { dir.deleteRecursively() }

    @Test
    fun `blocked directory returns failure`() {
        val blocker = File(dir, "blocked").apply { writeText("x") }
        writeCrashExport(blocker, "crashes.json", "[]").shouldBeNull()
    }

    @Test
    fun `exports have unique names and earlier content stays available`() {
        val first = writeCrashExport(dir, "crashes.json", "first").shouldNotBeNull()
        val second = writeCrashExport(dir, "crashes.json", "second").shouldNotBeNull()
        (first.name == second.name) shouldBe false
        first.readText() shouldBe "first"
        second.readText() shouldBe "second"
    }

    @Test
    fun `only old exports are deleted`() {
        val old = File(dir, "old.json").apply { writeText("old"); setLastModified(1L) }
        val recent = File(dir, "recent.json").apply { writeText("recent") }
        writeCrashExport(dir, "crashes.json", "[]").shouldNotBeNull()
        old.exists() shouldBe false
        recent.exists() shouldBe true
    }
}
