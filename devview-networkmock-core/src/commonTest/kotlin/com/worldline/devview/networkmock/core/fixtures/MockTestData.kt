package com.worldline.devview.networkmock.core.fixtures

import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.HostConfig
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.NetworkMockState

/**
 * Shared test data fixtures for the networkmock-core module.
 *
 * All builders produce immutable values and are safe to reuse across tests.
 * Prefer these helpers over inline object construction so that test assertions
 * stay focused on the behaviour being tested rather than on data setup.
 */
internal object MockTestData {

    // -------------------------------------------------------------------------
    // EndpointConfig builders
    // -------------------------------------------------------------------------

    /** A minimal GET endpoint with no path parameters. */
    fun endpointConfig(
        id: String = "getUser",
        name: String = "Get User",
        path: String = "/api/users",
        method: String = "GET",
    ): EndpointConfig = EndpointConfig(id = id, name = name, path = path, method = method)

    /** An endpoint whose path contains a single path parameter. */
    fun endpointConfigWithParam(
        id: String = "getUserById",
        name: String = "Get User By ID",
        path: String = "/api/users/{userId}",
        method: String = "GET",
    ): EndpointConfig = EndpointConfig(id = id, name = name, path = path, method = method)

    /** A POST endpoint (no path parameters). */
    fun postEndpointConfig(
        id: String = "createUser",
        name: String = "Create User",
        path: String = "/api/users",
        method: String = "POST",
    ): EndpointConfig = EndpointConfig(id = id, name = name, path = path, method = method)

    // -------------------------------------------------------------------------
    // HostConfig builders
    // -------------------------------------------------------------------------

    /** A staging host with a single GET endpoint (no path parameters). */
    fun stagingHostConfig(
        id: String = "staging",
        url: String = "https://staging.api.example.com",
        endpoints: List<EndpointConfig> = listOf(endpointConfig()),
    ): HostConfig = HostConfig(id = id, url = url, endpoints = endpoints)

    /** A production host with a single endpoint. */
    fun productionHostConfig(
        id: String = "production",
        url: String = "https://api.example.com",
        endpoints: List<EndpointConfig> = listOf(endpointConfig()),
    ): HostConfig = HostConfig(id = id, url = url, endpoints = endpoints)

    /** A host with multiple endpoints (GET + POST). */
    fun multiEndpointHostConfig(
        id: String = "staging",
        url: String = "https://staging.api.example.com",
    ): HostConfig = HostConfig(
        id = id,
        url = url,
        endpoints = listOf(
            endpointConfig(id = "getUser", path = "/api/users/{userId}", method = "GET"),
            postEndpointConfig(id = "createUser", path = "/api/users", method = "POST"),
            endpointConfig(id = "deleteUser", path = "/api/users/{userId}", method = "DELETE"),
        )
    )

    // -------------------------------------------------------------------------
    // MockConfiguration builders
    // -------------------------------------------------------------------------

    /** A configuration with a single staging host. */
    fun singleHostConfig(
        host: HostConfig = stagingHostConfig(),
    ): MockConfiguration = MockConfiguration(hosts = listOf(host))

    /** A configuration with both a staging and production host. */
    fun multiHostConfig(): MockConfiguration = MockConfiguration(
        hosts = listOf(stagingHostConfig(), productionHostConfig())
    )

    /** An empty configuration with no hosts. */
    fun emptyConfig(): MockConfiguration = MockConfiguration(hosts = emptyList())

    // -------------------------------------------------------------------------
    // EndpointMockState variations
    // -------------------------------------------------------------------------

    /** Network (pass-through) state — the default for every endpoint. */
    val networkState: EndpointMockState = EndpointMockState.Network

    /** A mock state backed by a 200 OK response file. */
    fun mockState200(endpointId: String = "getUser"): EndpointMockState.Mock =
        EndpointMockState.Mock(responseFile = "$endpointId-200.json")

    /** A mock state backed by a 404 Not Found response file. */
    fun mockState404(endpointId: String = "getUser"): EndpointMockState.Mock =
        EndpointMockState.Mock(responseFile = "$endpointId-404.json")

    /** A mock state backed by a 500 Internal Server Error response file. */
    fun mockState500(endpointId: String = "getUser"): EndpointMockState.Mock =
        EndpointMockState.Mock(responseFile = "$endpointId-500.json")

    /** A mock state with an explicit suffix in the file name (e.g. `getUser-404-simple.json`). */
    fun mockStateWithSuffix(
        endpointId: String = "getUser",
        statusCode: Int = 404,
        suffix: String = "simple",
    ): EndpointMockState.Mock =
        EndpointMockState.Mock(responseFile = "$endpointId-$statusCode-$suffix.json")

    // -------------------------------------------------------------------------
    // NetworkMockState builders
    // -------------------------------------------------------------------------

    /** Default state: global mocking disabled, no endpoint states. */
    val defaultNetworkMockState: NetworkMockState = NetworkMockState()

    /** State with global mocking enabled but no individual endpoint overrides. */
    val globalMockingEnabled: NetworkMockState = NetworkMockState(globalMockingEnabled = true)

    /**
     * State with global mocking enabled and a single endpoint set to a 200 mock.
     *
     * Key used: `"staging-getUser"`.
     */
    val singleEndpointMocked: NetworkMockState = NetworkMockState(
        globalMockingEnabled = true,
        endpointStates = mapOf("staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-200.json"))
    )

    /**
     * State with global mocking enabled and two endpoints configured
     * with different mock responses.
     *
     * Keys used: `"staging-getUser"`, `"staging-createUser"`.
     */
    val multipleEndpointsMocked: NetworkMockState = NetworkMockState(
        globalMockingEnabled = true,
        endpointStates = mapOf(
            "staging-getUser" to EndpointMockState.Mock(responseFile = "getUser-200.json"),
            "staging-createUser" to EndpointMockState.Mock(responseFile = "createUser-201.json"),
        )
    )
}

