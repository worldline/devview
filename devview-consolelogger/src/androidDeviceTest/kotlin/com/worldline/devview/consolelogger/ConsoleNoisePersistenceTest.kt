package com.worldline.devview.consolelogger

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.collections.shouldContainExactly
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Test

class ConsoleNoisePersistenceTest {
    @Test
    fun rules_survive_closing_and_reopening_the_preferences_file() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("console-noise-", ".preferences_pb", context.cacheDir)
        file.delete()
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val firstStore = PreferenceDataStoreFactory.createWithPath(
                scope = CoroutineScope(context = Dispatchers.IO + firstJob),
                produceFile = { file.absolutePath.toPath() }
            )
            val first = ConsoleNoiseRepository(dataStore = firstStore, defaults = listOf("Host"))
            first.update(change = ConsoleNoiseChange.Add(pattern = "Mine"))
            first.update(change = ConsoleNoiseChange.Enable(pattern = "Mine", enabled = false))
            first.update(change = ConsoleNoiseChange.Remove(pattern = "Host"))
            firstJob.cancelAndJoin()

            val defaults = listOf("HOST", "New", "mine")
            val secondStore = PreferenceDataStoreFactory.createWithPath(
                scope = CoroutineScope(context = Dispatchers.IO + secondJob),
                produceFile = { file.absolutePath.toPath() }
            )
            val restarted = ConsoleNoiseRepository(dataStore = secondStore, defaults = defaults)
            val restored = restarted.overrides.first()
            restored.patterns(defaults = defaults) shouldContainExactly listOf("New", "mine")
            restored.enabledPatterns(defaults = defaults) shouldContainExactly listOf("New")
            restarted.update(change = ConsoleNoiseChange.Reset)
            restarted.overrides.first().enabledPatterns(defaults = defaults) shouldContainExactly defaults
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            file.delete()
        }
    }
}
