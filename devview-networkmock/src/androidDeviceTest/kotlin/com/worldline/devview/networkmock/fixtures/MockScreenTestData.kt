package com.worldline.devview.networkmock.fixtures

import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.ApiSpecUiModel
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.viewmodel.NetworkMockUiState
import kotlinx.collections.immutable.persistentListOf

internal object MockScreenTestData {

    /**
     * @param versioned Whether this spec's `getUser`/`createUser` operations carry a `/v{n}/`
     * path segment. `example` is versioned, `catalog` is not — covering both the
     * chip/filter-row-present and filter-row-absent cases.
     * @param tagged Whether this spec's operations carry tags — `getUser`="Users",
     * `createUser`="Admin", `health`="Public" (one distinct tag each, so a single tag chip
     * narrows to exactly one operation). `example` is tagged, `catalog` is not — same
     * present/absent coverage as [versioned].
     */
    private fun spec(
        specId: String,
        name: String,
        versioned: Boolean,
        tagged: Boolean
    ): ApiSpecUiModel = ApiSpecUiModel(
        specId = specId,
        name = name,
        operations = persistentListOf(
            OperationUiModel(
                descriptor = OperationDescriptor(
                    key = OperationKey(specId = specId, operationId = "getUser"),
                    config = Operation(
                        operationId = "getUser",
                        name = "Get User",
                        path = if (versioned) "/api/v1/users/{userId}" else "/api/users/{userId}",
                        method = HttpMethod.Get,
                        version = if (versioned) "v1" else null,
                        tags = if (tagged) listOf("Users") else emptyList()
                    )
                ),
                currentState = OperationMockState.Network
            ),
            OperationUiModel(
                descriptor = OperationDescriptor(
                    key = OperationKey(specId = specId, operationId = "createUser"),
                    config = Operation(
                        operationId = "createUser",
                        name = "Create User",
                        path = if (versioned) "/api/v2/users" else "/api/users",
                        method = HttpMethod.Post,
                        version = if (versioned) "v2" else null,
                        tags = if (tagged) listOf("Admin") else emptyList()
                    )
                ),
                currentState = OperationMockState.Mock(statusCode = 201, exampleName = "default")
            ),
            OperationUiModel(
                descriptor = OperationDescriptor(
                    key = OperationKey(specId = specId, operationId = "health"),
                    config = Operation(
                        operationId = "health",
                        name = "Health",
                        path = "/health",
                        method = HttpMethod.Get,
                        tags = if (tagged) listOf("Public") else emptyList()
                    )
                ),
                currentState = OperationMockState.Network
            )
        )
    )

    fun contentState(globalMockingEnabled: Boolean = false): NetworkMockUiState.Content =
        NetworkMockUiState.Content(
            globalMockingEnabled = globalMockingEnabled,
            specs = persistentListOf(
                spec(specId = "example", name = "Example", versioned = true, tagged = true),
                spec(specId = "catalog", name = "Catalog", versioned = false, tagged = false)
            )
        )
}
