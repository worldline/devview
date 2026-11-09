package com.worldline.devview.networkmock.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.worldline.devview.networkmock.core.fixtures.MockTestData
import com.worldline.devview.networkmock.core.fixtures.ThrowingPreferencesDataStore
import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.test.FakePreferencesDataStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.IOException

class MockStateRepositoryCoordinationTest {
    private val key = OperationKey(specId = "example", operationId = "getUser")
    private val sequence = OperationMockState.Sequence(
        responses = listOf(MockTestData.mockState200(), MockTestData.mockState404())
    )

    @Test
    fun `revision is additive and survives repository recreation`() = runTest {
        val store = FakePreferencesDataStore()
        store.edit { preferences ->
            preferences[intPreferencesKey(name = "network_mock_schema_version")] = 2
            preferences[stringPreferencesKey(name = "network_mock_operation_${key.compositeKey}")] =
                """{"type":"mock","statusCode":200,"mediaType":"application/json","exampleName":"default"}"""
        }
        val repository = MockStateRepository(dataStore = store)
        repository.getStateSnapshot().revision shouldBe 0L
        repository.getStateSnapshot().state.getOperationState(key = key) shouldBe MockTestData.mockState200()

        repository.setGlobalMockingEnabled(enabled = true)
        repository.setOperationMockState(key = key, state = sequence)
        val recreated = MockStateRepository(dataStore = store).getStateSnapshot()
        recreated.revision shouldBe 2L
        recreated.state.getOperationState(key = key) shouldBe sequence
    }

    @Test
    fun `two consumers of one snapshot cannot both advance`() = runTest {
        val store = FakePreferencesDataStore()
        val first = MockStateRepository(dataStore = store)
        val second = MockStateRepository(dataStore = store)
        first.setGlobalMockingEnabled(enabled = true)
        first.setOperationMockState(key = key, state = sequence)
        val snapshot = first.getStateSnapshot()

        val accepted = listOf(first, second).map { repository ->
            async {
                repository.compareAndAdvanceSequence(
                    key = key, expectedRevision = snapshot.revision, expectedSequence = sequence
                )
            }
        }.awaitAll()

        accepted.count { it } shouldBe 1
        first.getStateSnapshot().revision shouldBe snapshot.revision + 1
        first.getStateSnapshot().state.getOperationState(key = key) shouldBe sequence.copy(currentIndex = 1)
    }

    @Test
    fun `every explicit writer invalidates the prepared sequence including equal values`() = runTest {
        val writers: List<suspend (MockStateRepository) -> Unit> = listOf(
            { it.setGlobalMockingEnabled(enabled = true) },
            { it.setGlobalMockingEnabled(enabled = false) },
            { it.setOperationMockState(key = key, state = sequence) },
            { it.setOperationMockState(key = key, state = OperationMockState.Network) },
            { it.setOperationMockState(key = key, state = OperationMockState.Failure(kind = FailureKind.TIMEOUT)) },
            { it.setAllOperationStates(states = mapOf(key to sequence)) },
            { it.resetKnownOperationsToNetwork() },
            { it.resetOperationSequencePosition(key = key) }
        )
        writers.forEach { write ->
            val store = FakePreferencesDataStore()
            val consumer = MockStateRepository(dataStore = store)
            val writer = MockStateRepository(dataStore = store)
            consumer.setGlobalMockingEnabled(enabled = true)
            consumer.setOperationMockState(key = key, state = sequence)
            val snapshot = consumer.getStateSnapshot()
            write(writer)
            val afterWrite = consumer.getStateSnapshot()

            afterWrite.revision shouldBe snapshot.revision + 1
            consumer.compareAndAdvanceSequence(
                key = key, expectedRevision = snapshot.revision, expectedSequence = sequence
            ) shouldBe false
            consumer.getStateSnapshot().revision shouldBe afterWrite.revision
            consumer.getStateSnapshot().state shouldBe afterWrite.state
        }
    }

    @Test
    fun `network then identical sequence replacement cannot revive an old snapshot`() = runTest {
        val repository = MockStateRepository(dataStore = FakePreferencesDataStore())
        repository.setGlobalMockingEnabled(enabled = true)
        repository.setOperationMockState(key = key, state = sequence)
        val snapshot = repository.getStateSnapshot()
        repository.setOperationMockState(key = key, state = OperationMockState.Network)
        repository.setOperationMockState(key = key, state = sequence)

        repository.compareAndAdvanceSequence(
            key = key, expectedRevision = snapshot.revision, expectedSequence = sequence
        ) shouldBe false
        repository.getStateSnapshot().state.getOperationState(key = key) shouldBe sequence
    }

    @Test
    fun `terminal response validates without incrementing revision`() = runTest {
        val repository = MockStateRepository(dataStore = FakePreferencesDataStore())
        val terminal = sequence.copy(currentIndex = 1)
        repository.setGlobalMockingEnabled(enabled = true)
        repository.setOperationMockState(key = key, state = terminal)
        val snapshot = repository.getStateSnapshot()
        repeat(times = 2) {
            repository.compareAndAdvanceSequence(
                key = key, expectedRevision = snapshot.revision, expectedSequence = terminal
            ) shouldBe true
        }
        repository.getStateSnapshot().revision shouldBe snapshot.revision
        repository.resetOperationSequencePosition(key = key)
        repository.compareAndAdvanceSequence(
            key = key, expectedRevision = snapshot.revision, expectedSequence = terminal
        ) shouldBe false
    }

    @Test
    fun `missing current response and mismatched sequence never consume a step`() = runTest {
        val repository = MockStateRepository(dataStore = FakePreferencesDataStore())
        repository.setGlobalMockingEnabled(enabled = true)
        listOf(sequence.copy(responses = emptyList()), sequence.copy(currentIndex = -1), sequence.copy(currentIndex = 2))
            .forEach { invalid ->
                repository.setOperationMockState(key = key, state = invalid)
                val snapshot = repository.getStateSnapshot()
                repository.compareAndAdvanceSequence(
                    key = key, expectedRevision = snapshot.revision, expectedSequence = invalid
                ) shouldBe false
                repository.getStateSnapshot().revision shouldBe snapshot.revision
            }
        repository.setOperationMockState(key = key, state = sequence)
        val snapshot = repository.getStateSnapshot()
        repository.compareAndAdvanceSequence(
            key = key, expectedRevision = snapshot.revision, expectedSequence = sequence.copy(currentIndex = 1)
        ) shouldBe false
        repository.getStateSnapshot().revision shouldBe snapshot.revision
    }

    @Test
    fun `atomic reset preserves newest responses and ignores non sequences`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = MockStateRepository(dataStore = store)
        val otherWriter = MockStateRepository(dataStore = store)
        repository.setOperationMockState(key = key, state = sequence.copy(currentIndex = 1))
        val replacement = sequence.copy(responses = sequence.responses.reversed(), currentIndex = 1)
        otherWriter.setOperationMockState(key = key, state = replacement)
        repository.resetOperationSequencePosition(key = key)
        repository.getStateSnapshot().state.getOperationState(key = key) shouldBe replacement.copy(currentIndex = 0)

        otherWriter.setOperationMockState(key = key, state = OperationMockState.Network)
        val snapshot = repository.getStateSnapshot()
        repository.resetOperationSequencePosition(key = key)
        repository.getStateSnapshot().revision shouldBe snapshot.revision
        repository.getStateSnapshot().state shouldBe snapshot.state
    }

    @Test
    fun `reset finds other writers persisted entries without an observation`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = MockStateRepository(dataStore = store)
        val untouched = OperationKey(specId = "example", operationId = "untouched")
        repository.registerOperations(operations = listOf(untouched))
        MockStateRepository(dataStore = store).setOperationMockState(key = key, state = sequence)
        repository.resetKnownOperationsToNetwork()
        val states = repository.getStateSnapshot().state.operationStates
        states.keys shouldBe setOf(key.compositeKey, untouched.compositeKey)
        states.values.toSet() shouldBe setOf(OperationMockState.Network)
    }

    @Test
    fun `strict snapshot read propagates IO failure instead of inventing defaults`() = runTest {
        val repository = MockStateRepository(dataStore = ThrowingPreferencesDataStore())
        shouldThrow<IOException> { repository.getStateSnapshot() }
    }

    @Test
    fun `failed advance and exhausted revision cannot persist partial changes`() = runTest {
        val store = FakePreferencesDataStore()
        var failWrite = false
        val failingStore = object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                store.updateData { preferences ->
                    val updated = transform(preferences)
                    if (failWrite) throw IOException("Simulated commit failure")
                    updated
                }
        }
        val repository = MockStateRepository(dataStore = failingStore)
        repository.setGlobalMockingEnabled(enabled = true)
        repository.setOperationMockState(key = key, state = sequence)
        val snapshot = repository.getStateSnapshot()
        failWrite = true
        shouldThrow<IOException> {
            repository.compareAndAdvanceSequence(
                key = key, expectedRevision = snapshot.revision, expectedSequence = sequence
            )
        }
        repository.getStateSnapshot().state shouldBe snapshot.state
        repository.getStateSnapshot().revision shouldBe snapshot.revision
        failWrite = false
        store.edit { it[longPreferencesKey(name = "network_mock_revision")] = Long.MAX_VALUE }
        shouldThrow<IllegalStateException> { repository.setGlobalMockingEnabled(enabled = false) }
        repository.getStateSnapshot().state.globalMockingEnabled shouldBe true
        repository.getStateSnapshot().revision shouldBe Long.MAX_VALUE
    }

    @Test
    fun `concurrent first use migration rechecks schema before pruning a newer write`() = runTest {
        val store = FakePreferencesDataStore()
        val secondMigrationEntered = CompletableDeferred<Unit>()
        val newerWriteFinished = CompletableDeferred<Unit>()
        var edits = 0
        val gatedStore = object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                when (++edits) {
                    1 -> secondMigrationEntered.await()
                    2 -> {
                        secondMigrationEntered.complete(value = Unit)
                        newerWriteFinished.await()
                    }
                }
                return store.updateData(transform = transform)
            }
        }
        val first = MockStateRepository(dataStore = gatedStore)
        val second = MockStateRepository(dataStore = gatedStore)
        val firstRead = async { first.getStateSnapshot() }
        val secondRead = async { second.getStateSnapshot() }
        firstRead.await()
        first.setOperationMockState(key = key, state = sequence)
        newerWriteFinished.complete(value = Unit)
        secondRead.await().state.getOperationState(key = key) shouldBe sequence
        store.data.first()[intPreferencesKey(name = "network_mock_schema_version")] shouldBe 2
    }
}
