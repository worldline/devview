package com.worldline.devview.networkmock

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

    private fun operation(
        operationId: String,
        path: String,
        method: HttpMethod = HttpMethod.Get,
        tags: List<String> = emptyList()
    ): OperationUiModel = OperationUiModel(
        descriptor = OperationDescriptor(
            key = OperationKey(specId = "spec", operationId = operationId),
            config = Operation(
                operationId = operationId,
                name = operationId,
                path = path,
                method = method,
                tags = tags
            )
        ),
        currentState = OperationMockState.Network
    )
}

