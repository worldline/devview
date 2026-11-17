package com.worldline.devview.networkmock

import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

/**
 * [OperationSort.SPEC_ORDER] isn't asserted here beyond "returns the same list unmodified" —
 * there's nothing else to check for a documented no-op.
 */
class NetworkMockScreenSortTest {

    @Test
    fun `SPEC_ORDER returns operations in their original order`() {
        val operations = listOf(
            operation(operationId = "b", path = "/b"),
            operation(operationId = "a", path = "/a")
        )

        val sorted = operations.sortedByOption(sort = OperationSort.SPEC_ORDER)

        sorted.map { it.descriptor.operationId } shouldContainExactly listOf("b", "a")
    }

    @Test
    fun `PATH sorts operations alphabetically by path`() {
        val operations = listOf(
            operation(operationId = "c", path = "/c"),
            operation(operationId = "a", path = "/a"),
            operation(operationId = "b", path = "/b")
        )

        val sorted = operations.sortedByOption(sort = OperationSort.PATH)

        sorted.map { it.descriptor.config.path } shouldContainExactly listOf("/a", "/b", "/c")
    }

    @Test
    fun `METHOD sorts operations using HttpMethod DefaultMethods canonical order`() {
        val operations = listOf(
            operation(operationId = "delete", path = "/x", method = HttpMethod.Delete),
            operation(operationId = "get", path = "/x", method = HttpMethod.Get),
            operation(operationId = "post", path = "/x", method = HttpMethod.Post)
        )

        val sorted = operations.sortedByOption(sort = OperationSort.METHOD)

        sorted.map { it.descriptor.operationId } shouldContainExactly listOf("get", "post", "delete")
    }

    @Test
    fun `TAG sorts operations by their first declared tag with untagged operations first`() {
        val operations = listOf(
            operation(operationId = "users", path = "/x", tags = listOf("Users")),
            operation(operationId = "untagged", path = "/y", tags = emptyList()),
            operation(operationId = "admin", path = "/z", tags = listOf("Admin"))
        )

        val sorted = operations.sortedByOption(sort = OperationSort.TAG)

        sorted.map { it.descriptor.operationId } shouldContainExactly
            listOf("untagged", "admin", "users")
    }

    @Test
    fun `MOCKED_FIRST puts Mock Failure and Sequence before Network keeping order within both groups`() {
        val mock = OperationMockState.Mock(
            statusCode = 200,
            mediaType = "application/json",
            exampleName = "default"
        )
        val operations = listOf(
            operation(operationId = "network-z", path = "/z"),
            operation(operationId = "mock-z", path = "/z", currentState = mock),
            operation(operationId = "network-a", path = "/a"),
            operation(
                operationId = "failure",
                path = "/f",
                currentState = OperationMockState.Failure(kind = FailureKind.TIMEOUT)
            ),
            operation(
                operationId = "sequence",
                path = "/s",
                currentState = OperationMockState.Sequence(responses = listOf(mock, mock))
            ),
            operation(operationId = "mock-a", path = "/a", currentState = mock)
        )

        val sorted = operations.sortedByOption(sort = OperationSort.MOCKED_FIRST)

        sorted.map { it.descriptor.operationId } shouldContainExactly
            listOf("mock-z", "failure", "sequence", "mock-a", "network-z", "network-a")
    }

    @Test
    fun `MOCKED_FIRST preserves order when all operations use Network`() {
        val operations = listOf(
            operation(operationId = "b", path = "/b"),
            operation(operationId = "a", path = "/a")
        )

        val sorted = operations.sortedByOption(sort = OperationSort.MOCKED_FIRST)

        sorted.map { it.descriptor.operationId } shouldContainExactly listOf("b", "a")
    }

    @Test
    fun `filters intersect dimensions and union methods and tags without changing operations`() {
        val operations = listOf(
            operation(operationId = "first", path = "/v1/users", version = "v1", tags = listOf("Users")),
            operation(operationId = "second", path = "/v1/admin", version = "v1", method = HttpMethod.Post, tags = listOf("Admin")),
            operation(operationId = "other-version", path = "/v2/users", version = "v2", tags = listOf("Users"))
        )
        val filters = OperationFilters(
            query = "/V1/",
            version = "v1",
            methods = setOf("GET", "POST"),
            tags = setOf("Users", "Admin"),
            states = setOf(MockStateFilter.NETWORK),
            sort = OperationSort.PATH
        )
        filterOperations(operations = operations, filters = filters).map { it.descriptor.operationId } shouldContainExactly
            listOf("second", "first")
        operations.map { it.descriptor.operationId } shouldContainExactly listOf("first", "second", "other-version")
        filterOperations(operations = operations, filters = filters.copy(tags = setOf("Removed tag"))) shouldContainExactly emptyList()
    }

    @Test
    fun `mocked state filter includes failures and sequences`() {
        val mock = OperationMockState.Mock(statusCode = 200, mediaType = "application/json", exampleName = "default")
        val operations = listOf(
            operation(operationId = "network", path = "/n"),
            operation(operationId = "mock", path = "/m", currentState = mock),
            operation(operationId = "failure", path = "/f", currentState = OperationMockState.Failure(kind = FailureKind.TIMEOUT)),
            operation(operationId = "sequence", path = "/s", currentState = OperationMockState.Sequence(responses = listOf(mock, mock)))
        )
        filterOperations(operations = operations, filters = OperationFilters(states = setOf(MockStateFilter.MOCKED)))
            .map { it.descriptor.operationId } shouldContainExactly listOf("mock", "failure", "sequence")
    }

    private fun operation(
        operationId: String,
        path: String,
        method: HttpMethod = HttpMethod.Get,
        tags: List<String> = emptyList(),
        version: String? = null,
        currentState: OperationMockState = OperationMockState.Network
    ): OperationUiModel = OperationUiModel(
        descriptor = OperationDescriptor(
            key = OperationKey(specId = "spec", operationId = operationId),
            config = Operation(
                operationId = operationId,
                name = operationId,
                path = path,
                method = method,
                tags = tags,
                version = version
            )
        ),
        currentState = currentState
    )
}

