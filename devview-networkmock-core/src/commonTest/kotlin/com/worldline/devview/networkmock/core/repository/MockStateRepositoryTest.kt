package com.worldline.devview.networkmock.core.repository

import app.cash.turbine.test
import com.worldline.devview.networkmock.core.fixtures.FakePreferencesDataStore
import com.worldline.devview.networkmock.core.fixtures.ThrowingPreferencesDataStore
import com.worldline.devview.networkmock.core.model.EndpointMockState
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class MockStateRepositoryTest {

    private fun createRepository(): MockStateRepository =
        MockStateRepository(dataStore = FakePreferencesDataStore())

    // region Initial state

    @Test
    fun `initial state has global mocking disabled`() = runTest {
        val repository = createRepository()

        val state = repository.getState()

        state.globalMockingEnabled shouldBe false
    }

    @Test
    fun `initial state has no endpoint states`() = runTest {
        val repository = createRepository()

        val state = repository.getState()

        state.endpointStates shouldBe emptyMap()
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

    // region Endpoint state persistence

    @Test
    fun `setEndpointMockState persists mock state for an endpoint`() = runTest {
        val repository = createRepository()

        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )

        val endpointState = repository.getState().getEndpointState(
            hostId = "staging",
            endpointId = "getUser"
        )
        endpointState.shouldBeInstanceOf<EndpointMockState.Mock>()
        endpointState.responseFile shouldBe "getUser-200.json"
    }

    @Test
    fun `setEndpointMockState persists network state for an endpoint`() = runTest {
        val repository = createRepository()

        // First set to mock, then back to network
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Network
        )

        val endpointState = repository.getState().getEndpointState(
            hostId = "staging",
            endpointId = "getUser"
        )
        endpointState shouldBe EndpointMockState.Network
    }

    @Test
    fun `setEndpointMockState is reflected in observeState`() = runTest {
        val repository = createRepository()

        repository.observeState().test {
            awaitItem().endpointStates shouldNotContainKey "staging-getUser"

            repository.setEndpointMockState(
                hostId = "staging",
                endpointId = "getUser",
                state = EndpointMockState.Mock(responseFile = "getUser-200.json")
            )
            awaitItem().endpointStates shouldContainKey "staging-getUser"

            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Multiple endpoints tracked independently

    @Test
    fun `multiple endpoint states are tracked independently`() = runTest {
        val repository = createRepository()

        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "createUser",
            state = EndpointMockState.Mock(responseFile = "createUser-201.json")
        )

        val state = repository.getState()
        val getUserState = state.getEndpointState(hostId = "staging", endpointId = "getUser")
        val createUserState = state.getEndpointState(hostId = "staging", endpointId = "createUser")

        (getUserState as EndpointMockState.Mock).responseFile shouldBe "getUser-200.json"
        (createUserState as EndpointMockState.Mock).responseFile shouldBe "createUser-201.json"
    }

    @Test
    fun `updating one endpoint does not affect another`() = runTest {
        val repository = createRepository()

        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "createUser",
            state = EndpointMockState.Mock(responseFile = "createUser-201.json")
        )

        // Update only getUser
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-404.json")
        )

        val createUserState = repository.getState().getEndpointState(
            hostId = "staging",
            endpointId = "createUser"
        )
        (createUserState as EndpointMockState.Mock).responseFile shouldBe "createUser-201.json"
    }

    @Test
    fun `endpoints on different hosts are tracked independently`() = runTest {
        val repository = createRepository()

        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )
        repository.setEndpointMockState(
            hostId = "production",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-500.json")
        )

        val stagingState = repository.getState().getEndpointState(
            hostId = "staging",
            endpointId = "getUser"
        )
        val productionState = repository.getState().getEndpointState(
            hostId = "production",
            endpointId = "getUser"
        )

        (stagingState as EndpointMockState.Mock).responseFile shouldBe "getUser-200.json"
        (productionState as EndpointMockState.Mock).responseFile shouldBe "getUser-500.json"
    }

    // endregion

    // region Reset operations

    @Test
    fun `resetKnownEndpointsToNetwork resets all previously written endpoints`() = runTest {
        val repository = createRepository()

        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "createUser",
            state = EndpointMockState.Mock(responseFile = "createUser-201.json")
        )

        repository.resetKnownEndpointsToNetwork()

        val state = repository.getState()
        state.getEndpointState(hostId = "staging", endpointId = "getUser") shouldBe EndpointMockState.Network
        state.getEndpointState(hostId = "staging", endpointId = "createUser") shouldBe EndpointMockState.Network
    }

    @Test
    fun `resetKnownEndpointsToNetwork does not change global mocking state`() = runTest {
        val repository = createRepository()

        repository.setGlobalMockingEnabled(enabled = true)
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )

        repository.resetKnownEndpointsToNetwork()

        repository.getState().globalMockingEnabled shouldBe true
    }

    @Test
    fun `setAllEndpointStates overwrites all endpoint states`() = runTest {
        val repository = createRepository()

        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )

        repository.setAllEndpointStates(
            states = mapOf(
                "staging-getUser" to EndpointMockState.Network,
                "staging-createUser" to EndpointMockState.Network
            )
        )

        val state = repository.getState()
        state.getEndpointState(hostId = "staging", endpointId = "getUser") shouldBe EndpointMockState.Network
        state.getEndpointState(hostId = "staging", endpointId = "createUser") shouldBe EndpointMockState.Network
    }

    @Test
    fun `setAllEndpointStates does not change global mocking state`() = runTest {
        val repository = createRepository()

        repository.setGlobalMockingEnabled(enabled = true)

        repository.setAllEndpointStates(
            states = mapOf("staging-getUser" to EndpointMockState.Network)
        )

        repository.getState().globalMockingEnabled shouldBe true
    }

    // endregion

    // region Non-existent endpoint lookup

    @Test
    fun `getEndpointState returns null for endpoint that has never been set`() = runTest {
        val repository = createRepository()

        val state = repository.getState().getEndpointState(
            hostId = "staging",
            endpointId = "nonExistent"
        )

        state shouldBe null
    }

    // endregion

    // region registerEndpoints

    @Test
    fun `registerEndpoints pre-populates keys so resetKnownEndpointsToNetwork covers them`() = runTest {
        val repository = createRepository()

        // Register endpoints without any prior writes
        repository.registerEndpoints(
            endpoints = listOf("staging" to "getUser", "staging" to "createUser")
        )

        // Write a mock state so there is something to reset for one of them
        repository.setEndpointMockState(
            hostId = "staging",
            endpointId = "getUser",
            state = EndpointMockState.Mock(responseFile = "getUser-200.json")
        )

        repository.resetKnownEndpointsToNetwork()

        repository.getState().getEndpointState(
            hostId = "staging",
            endpointId = "getUser"
        ) shouldBe EndpointMockState.Network
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
            state.endpointStates shouldBe emptyMap()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion
}

