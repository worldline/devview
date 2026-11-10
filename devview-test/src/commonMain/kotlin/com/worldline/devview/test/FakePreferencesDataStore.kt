package com.worldline.devview.test

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory Preferences DataStore with serialized, atomic updates for tests.
 */
public class FakePreferencesDataStore : DataStore<Preferences> {
    private val state: MutableStateFlow<Preferences> = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        val updated = transform(state.value)
        currentCoroutineContext().ensureActive()
        state.value = updated
        updated
    }
}

