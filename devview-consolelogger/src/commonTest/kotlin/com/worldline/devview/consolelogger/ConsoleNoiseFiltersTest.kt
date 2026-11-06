package com.worldline.devview.consolelogger

import com.worldline.devview.test.FakePreferencesDataStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ConsoleNoiseFiltersTest {
    @Test
    fun `defaults and additions merge without case insensitive duplicates`() = runTest {
        val store = FakePreferencesDataStore()
        val first = ConsoleNoiseRepository(dataStore = store, defaults = listOf("Host"))
        first.update(change = ConsoleNoiseChange.Add(pattern = " MyTag "))
        first.update(change = ConsoleNoiseChange.Add(pattern = "mytag"))
        val restarted = ConsoleNoiseRepository(dataStore = store, defaults = listOf("Host", "New", "MYTAG"))
        val state = restarted.overrides.first()
        state.patterns(defaults = listOf("Host", "New", "MYTAG")) shouldContainExactly listOf("Host", "New", "MYTAG")
        state.added.size shouldBe 1
    }

    @Test
    fun `removed defaults and disabled additions survive restart and new defaults`() = runTest {
        val store = FakePreferencesDataStore()
        val first = ConsoleNoiseRepository(dataStore = store, defaults = listOf("Host"))
        first.update(change = ConsoleNoiseChange.Remove(pattern = "HOST"))
        first.update(change = ConsoleNoiseChange.Add(pattern = "Mine"))
        first.update(change = ConsoleNoiseChange.Enable(pattern = "Mine", enabled = false))
        val defaults = listOf("host", "New")
        val restarted = ConsoleNoiseRepository(dataStore = store, defaults = defaults)
        val state = restarted.overrides.first()
        state.patterns(defaults = defaults) shouldContainExactly listOf("New", "Mine")
        state.enabledPatterns(defaults = defaults) shouldContainExactly listOf("New")
        restarted.update(change = ConsoleNoiseChange.Add(pattern = "HOST"))
        restarted.overrides.first().enabledPatterns(defaults = defaults) shouldContainExactly listOf("host", "New")
    }

    @Test
    fun `clear disables rules without deleting and reset restores only current host defaults`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = ConsoleNoiseRepository(dataStore = store, defaults = listOf("Host"))
        repository.update(change = ConsoleNoiseChange.Add(pattern = "Mine"))
        repository.update(change = ConsoleNoiseChange.DisableAll)
        val cleared = repository.overrides.first()
        cleared.patterns(defaults = listOf("Host")) shouldContainExactly listOf("Host", "Mine")
        cleared.enabledPatterns(defaults = listOf("Host")) shouldBe emptyList()
        repository.update(change = ConsoleNoiseChange.Reset)
        repository.overrides.first() shouldBe ConsoleNoiseOverrides()
        repository.overrides.first().enabledPatterns(defaults = listOf("Host")) shouldContainExactly listOf("Host")
    }

    @Test
    fun `unicode case equivalent identities merge and retain disabled and removed state`() = runTest {
        listOf("Σ" to "ς", "I" to "ı", "İ" to "i").forEach { (original, equivalent) ->
            normalizeConsolePatterns(patterns = listOf(original, equivalent)) shouldContainExactly listOf(original)
            val store = FakePreferencesDataStore()
            val repository = ConsoleNoiseRepository(dataStore = store, defaults = listOf(original))
            repository.update(change = ConsoleNoiseChange.Add(pattern = equivalent))
            repository.overrides.first().patterns(defaults = listOf(original)) shouldContainExactly listOf(original)
            repository.update(change = ConsoleNoiseChange.Enable(pattern = original, enabled = false))
            val restarted = ConsoleNoiseRepository(dataStore = store, defaults = listOf(equivalent))
            restarted.overrides.first().enabledPatterns(defaults = listOf(equivalent)) shouldBe emptyList()
            restarted.update(change = ConsoleNoiseChange.Remove(pattern = equivalent))
            restarted.overrides.first().patterns(defaults = listOf(original)) shouldBe emptyList()
        }
    }

    @Test
    fun `blank add is rejected and normalized defaults permit append replace or empty`() {
        shouldThrow<IllegalArgumentException> {
            ConsoleNoiseOverrides().apply(change = ConsoleNoiseChange.Add(pattern = "  "), defaults = emptyList())
        }
        Console().defaultExcludedPatterns shouldContainExactly listOf("setRequestedFrameRate", "setFrameRateCategory")
        Console(defaultExcludedPatterns = Console.DEFAULT_EXCLUDED_PATTERNS + listOf("Mine", " mine ", " "))
            .defaultExcludedPatterns shouldContainExactly listOf("setRequestedFrameRate", "setFrameRateCategory", "Mine")
        Console(defaultExcludedPatterns = listOf("Mine")).defaultExcludedPatterns shouldContainExactly listOf("Mine")
        Console(defaultExcludedPatterns = emptyList()).defaultExcludedPatterns shouldBe emptyList()
    }
}
