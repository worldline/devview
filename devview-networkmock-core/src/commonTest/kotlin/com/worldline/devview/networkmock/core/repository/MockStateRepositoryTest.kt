package com.worldline.devview.networkmock.core.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.worldline.devview.networkmock.core.fixtures.MockTestData
import com.worldline.devview.networkmock.core.fixtures.ThrowingPreferencesDataStore
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.test.FakePreferencesDataStore
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class MockStateRepositoryTest {

    private val specId: String = "example"

    private fun key(operationId: String): OperationKey = OperationKey(specId = specId, operationId = operationId)

    private fun createRepository(): MockStateRepository =
        MockStateRepository(dataStore = FakePreferencesDataStore())

    // region Initial state

    @Test
    fun `initial state has global mocking disabled`() = runTest {
        val repository = createRepository()

        val state = repository.getState()

        state.globalMockingEnabled shouldBe MockTestData.defaultNetworkMockState.globalMockingEnabled
    }

    @Test
    fun `initial state has no operation states`() = runTest {
        val repository = createRepository()

        val state = repository.getState()

        state.operationStates shouldBe emptyMap()
    }

    // endregion

    // region Global mocking toggle

    @Test
    fun `setGlobalMockingEnabled true persists to DataStore`() = runTest {
        val repository = createRepository()

        repository.setGlobalMockingEnabled(enabled = true)

        repository.getState().globalMockingEnabled shouldBe true
    }

    @Test
    fun `setGlobalMockingEnabled false persists to DataStore`() = runTest {
        val repository = createRepository()

        repository.setGlobalMockingEnabled(enabled = true)
        repository.setGlobalMockingEnabled(enabled = false)

        repository.getState().globalMockingEnabled shouldBe false
    }

    @Test
    fun `observeState emits updated value when global mocking is toggled`() = runTest {
        val repository = createRepository()

        repository.observeState().test {
            awaitItem().globalMockingEnabled shouldBe false

            repository.setGlobalMockingEnabled(enabled = true)
            awaitItem().globalMockingEnabled shouldBe true

            repository.setGlobalMockingEnabled(enabled = false)
            awaitItem().globalMockingEnabled shouldBe false

            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Operation state persistence

    @Test
    fun `setOperationMockState persists mock state for an operation`() = runTest {
        val repository = createRepository()

        repository.setOperationMockState(
            key = key(operationId = "getUser"),
            state = OperationMockState.Mock(statusCode = 200, exampleName = "default")
        )

        val operationState = repository.getState().getOperationState(key = key(operationId = "getUser"))
        operationState.shouldBeInstanceOf<OperationMockState.Mock>()
        operationState.statusCode shouldBe 200
        operationState.exampleName shouldBe "default"
    }

    @Test
    fun `setOperationMockState persists network state for an operation`() = runTest {
        val repository = createRepository()

        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())
        repository.setOperationMockState(key = key(operationId = "getUser"), state = OperationMockState.Network)

        val operationState = repository.getState().getOperationState(key = key(operationId = "getUser"))
        operationState shouldBe OperationMockState.Network
    }

    @Test
    fun `setOperationMockState is reflected in observeState`() = runTest {
        val repository = createRepository()

        repository.observeState().test {
            awaitItem().operationStates shouldNotContainKey key(operationId = "getUser").compositeKey

            repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())
            awaitItem().operationStates shouldContainKey key(operationId = "getUser").compositeKey

            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Multiple operations tracked independently

    @Test
    fun `multiple operation states are tracked independently`() = runTest {
        val repository = createRepository()

        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())
        repository.setOperationMockState(
            key = key(operationId = "createUser"),
            state = OperationMockState.Mock(statusCode = 201, exampleName = "default")
        )

        val state = repository.getState()
        val getUserState = state.getOperationState(key = key(operationId = "getUser"))
        val createUserState = state.getOperationState(key = key(operationId = "createUser"))

        (getUserState as OperationMockState.Mock).statusCode shouldBe 200
        (createUserState as OperationMockState.Mock).statusCode shouldBe 201
    }

    @Test
    fun `updating one operation does not affect another`() = runTest {
        val repository = createRepository()

        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())
        repository.setOperationMockState(
            key = key(operationId = "createUser"),
            state = OperationMockState.Mock(statusCode = 201, exampleName = "default")
        )

        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState404())

        val createUserState = repository.getState().getOperationState(key = key(operationId = "createUser"))
        (createUserState as OperationMockState.Mock).statusCode shouldBe 201
    }

    // endregion

    // region Reset operations

    @Test
    fun `resetKnownOperationsToNetwork resets all previously written operations`() = runTest {
        val repository = createRepository()

        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())
        repository.setOperationMockState(
            key = key(operationId = "createUser"),
            state = OperationMockState.Mock(statusCode = 201, exampleName = "default")
        )

        repository.resetKnownOperationsToNetwork()

        val state = repository.getState()
        state.getOperationState(key = key(operationId = "getUser")) shouldBe OperationMockState.Network
        state.getOperationState(key = key(operationId = "createUser")) shouldBe OperationMockState.Network
    }

    @Test
    fun `resetKnownOperationsToNetwork does not change global mocking state`() = runTest {
        val repository = createRepository()

        repository.setGlobalMockingEnabled(enabled = true)
        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())

        repository.resetKnownOperationsToNetwork()

        repository.getState().globalMockingEnabled shouldBe true
    }

    @Test
    fun `setAllOperationStates overwrites all operation states`() = runTest {
        val repository = createRepository()

        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())

        repository.setAllOperationStates(
            states = mapOf(
                key(operationId = "getUser") to OperationMockState.Network,
                key(operationId = "createUser") to OperationMockState.Network
            )
        )

        val state = repository.getState()
        state.getOperationState(key = key(operationId = "getUser")) shouldBe OperationMockState.Network
        state.getOperationState(key = key(operationId = "createUser")) shouldBe OperationMockState.Network
    }

    @Test
    fun `setAllOperationStates does not change global mocking state`() = runTest {
        val repository = createRepository()

        repository.setGlobalMockingEnabled(enabled = true)

        repository.setAllOperationStates(
            states = mapOf(key(operationId = "getUser") to OperationMockState.Network)
        )

        repository.getState().globalMockingEnabled shouldBe true
    }

    // endregion

    // region Non-existent operation lookup

    @Test
    fun `getOperationState returns null for operation that has never been set`() = runTest {
        val repository = createRepository()

        val state = repository.getState().getOperationState(key = key(operationId = "nonExistent"))

        state shouldBe null
    }

    // endregion

    // region registerOperations

    @Test
    fun `registerOperations pre-populates keys so resetKnownOperationsToNetwork covers them`() = runTest {
        val repository = createRepository()

        repository.registerOperations(
            operations = listOf(key(operationId = "getUser"), key(operationId = "createUser"))
        )
        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())

        repository.resetKnownOperationsToNetwork()

        repository.getState().getOperationState(key = key(operationId = "getUser")) shouldBe OperationMockState.Network
    }

    // endregion

    // region IO error recovery

    @Test
    fun `observeState emits default state when DataStore throws IOException`() = runTest {
        val throwingDataStore = ThrowingPreferencesDataStore()
        val repository = MockStateRepository(dataStore = throwingDataStore)

        repository.observeState().test {
            val state = awaitItem()
            state.globalMockingEnabled shouldBe false
            state.operationStates shouldBe emptyMap()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Pre-0.2.0 legacy state pruning

    @Test
    fun `pre-0_2_0 endpoint keys are removed on first read`() = runTest {
        val dataStore = FakePreferencesDataStore()
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[stringPreferencesKey(name = "network_mock_endpoint_example-staging-getUser")] =
                    """{"type":"mock","responseFile":"getUser-200.json"}"""
                this[booleanPreferencesKey(name = "network_mock_global_enabled")] = true
            }
        }
        val repository = MockStateRepository(dataStore = dataStore)

        val state = repository.getState()

        state.operationStates shouldBe emptyMap()
        state.globalMockingEnabled shouldBe true
    }

    @Test
    fun `pre-0_2_0 pruning does not remove state written after migration`() = runTest {
        val dataStore = FakePreferencesDataStore()
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[stringPreferencesKey(name = "network_mock_endpoint_example-staging-getUser")] = "legacy"
            }
        }
        val repository = MockStateRepository(dataStore = dataStore)
        repository.getState()
        repository.setOperationMockState(key = key(operationId = "getUser"), state = MockTestData.mockState200())

        val state = repository.getState()

        state.operationStates shouldContainKey key(operationId = "getUser").compositeKey
    }

    // endregion
}
