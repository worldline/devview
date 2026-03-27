package com.worldline.devview.networkmock.core.fixtures

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import okio.IOException

/**
 * In-memory [DataStore] implementation for use in unit tests.
 *
 * Stores preferences in a [MutableStateFlow] so that [data] emits synchronously
 * on every [updateData] call, making coroutine-based assertions straightforward
 * with [kotlinx.coroutines.flow.first] or Turbine.
 */
internal class FakePreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(value = emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/**
 * A [DataStore] implementation that immediately throws an [IOException] from [data],
 * used to verify that [com.worldline.devview.networkmock.core.repository.MockStateRepository.observeState]
 * recovers gracefully and emits a safe default state instead of propagating the exception.
 */
internal class ThrowingPreferencesDataStore : DataStore<Preferences> {

    override val data: Flow<Preferences> = flow {
        throw IOException("Simulated DataStore read failure")
    }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences = throw IOException("Simulated DataStore write failure")
}

