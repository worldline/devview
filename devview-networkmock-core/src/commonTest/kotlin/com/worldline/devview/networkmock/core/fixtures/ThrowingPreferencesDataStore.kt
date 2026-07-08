package com.worldline.devview.networkmock.core.fixtures

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.IOException

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

