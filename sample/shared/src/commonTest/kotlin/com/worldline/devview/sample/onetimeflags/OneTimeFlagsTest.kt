package com.worldline.devview.sample.onetimeflags

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.worldline.devview.test.FakePreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class OneTimeFlagsTest {
    @Test
    fun resetAllRemovesOnlyDeclaredKeysAndIsRepeatable() = runTest {
        val store = FakePreferencesDataStore()
        val sessionKey = booleanPreferencesKey(name = "signed_in")
        store.edit { preferences ->
            OneTimeFlag.entries.forEach { flag -> preferences[flag.key] = true }
            preferences[sessionKey] = true
        }
        val module = OneTimeFlags(dataStore = store)

        module.resetAll()
        module.resetAll()

        val preferences = store.data.first()
        OneTimeFlag.entries.forEach { flag -> assertNull(actual = preferences[flag.key]) }
        assertEquals(expected = true, actual = preferences[sessionKey])
    }

    @Test
    fun resetOneFlagLeavesOtherFlagsAndAppPreferencesUntouched() = runTest {
        val store = FakePreferencesDataStore()
        val sessionKey = booleanPreferencesKey(name = "signed_in")
        store.edit {
            it[OneTimeFlag.RATING_PROMPT_SHOWN.key] = true
            it[OneTimeFlag.ONBOARDING_SEEN.key] = true
            it[sessionKey] = true
        }

        store.edit { it.remove(key = OneTimeFlag.RATING_PROMPT_SHOWN.key) }

        val preferences = store.data.first()
        assertNull(actual = preferences[OneTimeFlag.RATING_PROMPT_SHOWN.key])
        assertEquals(expected = true, actual = preferences[OneTimeFlag.ONBOARDING_SEEN.key])
        assertEquals(expected = true, actual = preferences[sessionKey])
    }
}
