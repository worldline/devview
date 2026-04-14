package com.worldline.devview.networkmock.core.fixtures

import com.worldline.devview.networkmock.core.model.ApiGroupConfig
import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointKey
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.EnvironmentConfig
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
    // Environment / ApiGroup builders
    // -------------------------------------------------------------------------

    /** A staging environment. */
    fun stagingEnvironment(
        id: String = "staging",
        name: String = "Staging",
        url: String = "https://staging.api.example.com",
    ): EnvironmentConfig = EnvironmentConfig(id = id, name = name, url = url)

    /** A production environment. */
    fun productionEnvironment(
        id: String = "production",
        name: String = "Production",
        url: String = "https://api.example.com",
    ): EnvironmentConfig = EnvironmentConfig(id = id, name = name, url = url)

    /** A single API group with the provided environments and endpoints. */
    fun apiGroupConfig(
        id: String = "example",
        name: String = "Example",
        environments: List<EnvironmentConfig> = listOf(stagingEnvironment()),
        endpoints: List<EndpointConfig> = listOf(endpointConfig()),
    ): ApiGroupConfig = ApiGroupConfig(
        id = id,
        name = name,
        endpoints = endpoints,
        environments = environments,
    )

    /** An API group with multiple endpoints (GET + POST + DELETE). */
    fun multiEndpointApiGroup(
        id: String = "example",
        name: String = "Example",
        environments: List<EnvironmentConfig> = listOf(stagingEnvironment()),
    ): ApiGroupConfig = ApiGroupConfig(
        id = id,
        name = name,
        environments = environments,
        endpoints = listOf(
            endpointConfig(id = "getUser", path = "/api/users/{userId}", method = "GET"),
            postEndpointConfig(id = "createUser", path = "/api/users", method = "POST"),
            endpointConfig(id = "deleteUser", path = "/api/users/{userId}", method = "DELETE"),
        ),
    )

    // -------------------------------------------------------------------------
    // MockConfiguration builders
    // -------------------------------------------------------------------------

    /** A configuration with one API group and one environment. */
    fun singleGroupConfig(
        group: ApiGroupConfig = apiGroupConfig(),
    ): MockConfiguration = MockConfiguration(apiGroups = listOf(group))

    /** A configuration with one API group and both staging + production environments. */
    fun multiEnvironmentGroupConfig(): MockConfiguration = MockConfiguration(
        apiGroups = listOf(
            apiGroupConfig(
                environments = listOf(stagingEnvironment(), productionEnvironment())
            )
        )
    )

    /** A configuration with two API groups. */
    fun multiGroupConfig(): MockConfiguration = MockConfiguration(
        apiGroups = listOf(
            apiGroupConfig(id = "example", name = "Example"),
            apiGroupConfig(id = "catalog", name = "Catalog")
        )
    )

    /** An empty configuration with no API groups. */
    fun emptyConfig(): MockConfiguration = MockConfiguration(apiGroups = emptyList())

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
     * Key used: `"example-staging-getUser"`.
     */
    val singleEndpointMocked: NetworkMockState = NetworkMockState(
        globalMockingEnabled = true,
        endpointStates = mapOf(
            EndpointKey(
                groupId = "example",
                environmentId = "staging",
                endpointId = "getUser"
            ).compositeKey to EndpointMockState.Mock(responseFile = "getUser-200.json")
        )
    )

    /**
     * State with global mocking enabled and two endpoints configured
     * with different mock responses.
     *
     * Keys used: `"example-staging-getUser"`, `"example-staging-createUser"`.
     */
    val multipleEndpointsMocked: NetworkMockState = NetworkMockState(
        globalMockingEnabled = true,
        endpointStates = mapOf(
            EndpointKey(
                groupId = "example",
                environmentId = "staging",
                endpointId = "getUser"
            ).compositeKey to EndpointMockState.Mock(responseFile = "getUser-200.json"),
            EndpointKey(
                groupId = "example",
                environmentId = "staging",
                endpointId = "createUser"
            ).compositeKey to EndpointMockState.Mock(responseFile = "createUser-201.json"),
        )
    )
}

