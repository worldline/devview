package com.worldline.devview.consolelogger

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class ConsoleNoiseOverrides(
    val added: Set<String> = emptySet(),
    val removed: Set<String> = emptySet(),
    val disabled: Set<String> = emptySet()
) {
    fun patterns(defaults: List<String>): List<String> = normalizeConsolePatterns(
        patterns = defaults + added.sorted()
    ).filter { it.consoleNoiseKey() !in removed }

    fun enabledPatterns(defaults: List<String>): List<String> = patterns(defaults = defaults)
        .filter { it.consoleNoiseKey() !in disabled }

    fun apply(change: ConsoleNoiseChange, defaults: List<String>): ConsoleNoiseOverrides =
        when (change) {
            is ConsoleNoiseChange.Add -> {
                val pattern = change.pattern.trim()
                require(value = pattern.isNotEmpty()) { "An exclusion pattern cannot be blank" }
                val key = pattern.consoleNoiseKey()
                copy(
                    added = if (defaults.any { it.equals(other = pattern, ignoreCase = true) }) {
                        added
                    } else {
                        normalizeConsolePatterns(patterns = added.toList() + pattern).toSet()
                    },
                    removed = removed - key,
                    disabled = disabled - key
                )
            }
            is ConsoleNoiseChange.Remove -> copy(
                added = added
                    .filterNot {
                        it.equals(
                            other = change.pattern,
                            ignoreCase = true
                        )
                    }.toSet(),
                removed = removed + change.pattern.consoleNoiseKey(),
                disabled = disabled - change.pattern.consoleNoiseKey()
            )
            is ConsoleNoiseChange.Enable -> copy(
                disabled = if (change.enabled) {
                    disabled - change.pattern.consoleNoiseKey()
                } else {
                    disabled + change.pattern.consoleNoiseKey()
                }
            )
            ConsoleNoiseChange.DisableAll -> copy(
                disabled = disabled + patterns(defaults = defaults).map { it.consoleNoiseKey() }
            )
            ConsoleNoiseChange.Reset -> ConsoleNoiseOverrides()
        }
}

internal sealed interface ConsoleNoiseChange {
    data class Add(val pattern: String) : ConsoleNoiseChange

    data class Remove(val pattern: String) : ConsoleNoiseChange

    data class Enable(val pattern: String, val enabled: Boolean) : ConsoleNoiseChange

    data object DisableAll : ConsoleNoiseChange

    data object Reset : ConsoleNoiseChange
}

internal fun normalizeConsolePatterns(patterns: List<String>): List<String> = patterns
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .distinctBy { it.consoleNoiseKey() }

// Match Kotlin’s ignoreCase character equivalence, including final sigma and dotted/dotless I.
internal fun String.consoleNoiseKey(): String = map { it.uppercaseChar().lowercaseChar() }
    .joinToString(separator = "")

internal class ConsoleNoiseRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaults: List<String>
) {
    val overrides: Flow<ConsoleNoiseOverrides> = dataStore.data.map { preferences ->
        preferences.toNoiseOverrides()
    }

    suspend fun update(change: ConsoleNoiseChange) {
        dataStore.edit { preferences ->
            val updated = preferences.toNoiseOverrides().apply(change = change, defaults = defaults)
            preferences[ADDED] = updated.added
            preferences[REMOVED] = updated.removed
            preferences[DISABLED] = updated.disabled
        }
    }

    private fun Preferences.toNoiseOverrides(): ConsoleNoiseOverrides = ConsoleNoiseOverrides(
        added = normalizeConsolePatterns(patterns = this[ADDED].orEmpty().sorted()).toSet(),
        removed = this[REMOVED].orEmpty().map { it.consoleNoiseKey() }.toSet(),
        disabled = this[DISABLED].orEmpty().map { it.consoleNoiseKey() }.toSet()
    )

    private companion object {
        val ADDED = stringSetPreferencesKey(name = "console_noise_added")
        val REMOVED = stringSetPreferencesKey(name = "console_noise_removed")
        val DISABLED = stringSetPreferencesKey(name = "console_noise_disabled")
    }
}
