package com.worldline.devview.networkmock.core.fixtures

import com.worldline.devview.networkmock.core.model.ApiSpec
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.NetworkMockState
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState

/**
 * Shared test data fixtures for the networkmock-core module.
 *
 * All builders produce immutable values and are safe to reuse across tests.
 * Prefer these helpers over inline object construction so that test assertions
 * stay focused on the behaviour being tested rather than on data setup.
 */
internal object MockTestData {

    // -------------------------------------------------------------------------
    // Operation builders
    // -------------------------------------------------------------------------

    /** A minimal GET operation with no path parameters. */
    fun operation(
        operationId: String = "getUser",
        name: String = "Get User",
        path: String = "/api/users",
        method: String = "GET",
    ): Operation = Operation(operationId = operationId, name = name, path = path, method = method)

    /** An operation whose path contains a single path parameter. */
    fun operationWithParam(
        operationId: String = "getUserById",
        name: String = "Get User By ID",
        path: String = "/api/users/{userId}",
        method: String = "GET",
    ): Operation = Operation(operationId = operationId, name = name, path = path, method = method)

    /** A POST operation (no path parameters). */
    fun postOperation(
        operationId: String = "createUser",
        name: String = "Create User",
        path: String = "/api/users",
        method: String = "POST",
    ): Operation = Operation(operationId = operationId, name = name, path = path, method = method)

    // -------------------------------------------------------------------------
    // ApiSpec / MockConfiguration builders
    // -------------------------------------------------------------------------

    /** A single spec with the provided servers and operations. */
    fun apiSpec(
        id: String = "example",
        name: String = "Example",
        servers: List<String> = listOf("https://staging.api.example.com"),
        operations: List<Operation> = listOf(operation()),
    ): ApiSpec = ApiSpec(id = id, name = name, servers = servers, operations = operations)

    /** A spec with multiple operations (GET + POST + DELETE). */
    fun multiOperationApiSpec(
        id: String = "example",
        name: String = "Example",
        servers: List<String> = listOf("https://staging.api.example.com"),
    ): ApiSpec = ApiSpec(
        id = id,
        name = name,
        servers = servers,
        operations = listOf(
            operation(operationId = "getUser", path = "/api/users/{userId}", method = "GET"),
            postOperation(operationId = "createUser", path = "/api/users", method = "POST"),
            operation(operationId = "deleteUser", path = "/api/users/{userId}", method = "DELETE"),
        ),
    )

    /** A configuration with a single spec. */
    fun singleSpecConfig(spec: ApiSpec = apiSpec()): MockConfiguration = MockConfiguration(specs = listOf(spec))

    /** A configuration with two specs. */
    fun multiSpecConfig(): MockConfiguration = MockConfiguration(
        specs = listOf(
            apiSpec(id = "example", name = "Example"),
            apiSpec(id = "catalog", name = "Catalog")
        )
    )

    /** An empty configuration with no specs. */
    fun emptyConfig(): MockConfiguration = MockConfiguration(specs = emptyList())

    // -------------------------------------------------------------------------
    // OperationMockState variations
    // -------------------------------------------------------------------------

    /** Network (pass-through) state — the default for every operation. */
    val networkState: OperationMockState = OperationMockState.Network

    /** A mock state selecting the `default` 200 OK example. */
    fun mockState200(): OperationMockState.Mock = OperationMockState.Mock(statusCode = 200, exampleName = "default")

    /** A mock state selecting the `default` 404 Not Found example. */
    fun mockState404(): OperationMockState.Mock = OperationMockState.Mock(statusCode = 404, exampleName = "default")

    /** A mock state selecting the `default` 500 Internal Server Error example. */
    fun mockState500(): OperationMockState.Mock = OperationMockState.Mock(statusCode = 500, exampleName = "default")

    /** A mock state selecting a named example other than `default`. */
    fun mockStateWithExample(
        statusCode: Int = 404,
        exampleName: String = "simple",
    ): OperationMockState.Mock = OperationMockState.Mock(statusCode = statusCode, exampleName = exampleName)

    // -------------------------------------------------------------------------
    // NetworkMockState builders
    // -------------------------------------------------------------------------

    /** Default state: global mocking disabled, no operation states. */
    val defaultNetworkMockState: NetworkMockState = NetworkMockState()

    /** State with global mocking enabled but no individual operation overrides. */
    val globalMockingEnabled: NetworkMockState = NetworkMockState(globalMockingEnabled = true)

    /**
     * State with global mocking enabled and a single operation set to a 200 mock.
     *
     * Key used: `"example-getUser"`.
     */
    val singleOperationMocked: NetworkMockState = NetworkMockState(
        globalMockingEnabled = true,
        operationStates = mapOf(
            OperationKey(specId = "example", operationId = "getUser").compositeKey to
                OperationMockState.Mock(statusCode = 200, exampleName = "default")
        )
    )

    /**
     * State with global mocking enabled and two operations configured with different mock
     * responses.
     *
     * Keys used: `"example-getUser"`, `"example-createUser"`.
     */
    val multipleOperationsMocked: NetworkMockState = NetworkMockState(
        globalMockingEnabled = true,
        operationStates = mapOf(
            OperationKey(specId = "example", operationId = "getUser").compositeKey to
                OperationMockState.Mock(statusCode = 200, exampleName = "default"),
            OperationKey(specId = "example", operationId = "createUser").compositeKey to
                OperationMockState.Mock(statusCode = 201, exampleName = "default"),
        )
    )
}
