package com.worldline.devview.networkmock.fixtures

import com.worldline.devview.networkmock.core.model.ApiGroupConfig
import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointDescriptor
import com.worldline.devview.networkmock.core.model.EndpointKey
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.EnvironmentConfig
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.model.EndpointUiModel
import com.worldline.devview.networkmock.model.GroupEnvironmentUiModel
import com.worldline.devview.networkmock.viewmodel.NetworkMockUiState
import kotlinx.collections.immutable.persistentListOf

internal object MockScreenTestData {

    const val configPath: String = "files/networkmocks/mocks.json"

    private val stagingGetUserResponses = listOf(
        MockResponse(
            statusCode = 200,
            fileName = "getUser-200.json",
            displayName = "Success (200)",
            content = "{}"
        ),
        MockResponse(
            statusCode = 404,
            fileName = "getUser-404-simple.json",
            displayName = "Not Found - Simple (404)",
            content = "{}"
        )
    )

    private val stagingCreateUserResponses = listOf(
        MockResponse(
            statusCode = 201,
            fileName = "createUser-201.json",
            displayName = "Created (201)",
            content = "{}"
        )
    )

    private val productionGetProductResponses = listOf(
        MockResponse(
            statusCode = 200,
            fileName = "getProduct-200.json",
            displayName = "Success (200)",
            content = "{}"
        )
    )

    fun configurationWithTwoEnvironments(): MockConfiguration = MockConfiguration(
        apiGroups = listOf(
            ApiGroupConfig(
                id = "example",
                name = "Example",
                environments = listOf(
                    EnvironmentConfig(
                        id = "staging",
                        name = "Staging",
                        url = "https://staging.api.example.com"
                    ),
                    EnvironmentConfig(
                        id = "production",
                        name = "Production",
                        url = "https://api.example.com"
                    )
                ),
                endpoints = listOf(
                    EndpointConfig(
                        id = "getUser",
                        name = "Get User",
                        path = "/api/users/{userId}",
                        method = "GET"
                    ),
                    EndpointConfig(
                        id = "createUser",
                        name = "Create User",
                        path = "/api/users",
                        method = "POST"
                    )
                )
            )
        )
    )

    fun configurationWithNoHosts(): MockConfiguration = MockConfiguration(apiGroups = emptyList())

    fun defaultResources(): Map<String, String> = mapOf(
        configPath to defaultConfigJson(),
        "files/networkmocks/responses/example/staging/getUser/getUser-200.json" to "{}",
        "files/networkmocks/responses/example/getUser/getUser-404-simple.json" to "{}",
        "files/networkmocks/responses/example/staging/createUser/createUser-201.json" to "{}",
        "files/networkmocks/responses/example/production/getProduct/getProduct-200.json" to "{}"
    )

    fun emptyHostsResources(): Map<String, String> = mapOf(
        configPath to """
            {
              "apiGroups": []
            }
        """.trimIndent()
    )

    fun defaultConfigJson(): String = """
        {
          "apiGroups": [
            {
              "id": "example",
              "name": "Example",
              "endpoints": [
                {
                  "id": "getUser",
                  "name": "Get User",
                  "path": "/api/users/{userId}",
                  "method": "GET"
                },
                {
                  "id": "createUser",
                  "name": "Create User",
                  "path": "/api/users",
                  "method": "POST"
                }
              ],
              "environments": [
                {
                  "id": "staging",
                  "name": "Staging",
                  "url": "https://staging.api.example.com"
                },
                {
                  "id": "production",
                  "name": "Production",
                  "url": "https://api.example.com",
                  "additionalEndpoints": [
                    {
                      "id": "getProduct",
                      "name": "Get Product",
                      "path": "/api/products/{productId}",
                      "method": "GET"
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    fun contentState(globalMockingEnabled: Boolean = false): NetworkMockUiState.Content =
        NetworkMockUiState.Content(
            globalMockingEnabled = globalMockingEnabled,
            groups = persistentListOf(
                GroupEnvironmentUiModel(
                    groupId = "example",
                    environmentId = "staging",
                    name = "Staging",
                    url = "https://staging.api.example.com",
                    endpoints = persistentListOf(
                        EndpointUiModel(
                            descriptor = EndpointDescriptor(
                                key = EndpointKey(
                                    groupId = "example",
                                    environmentId = "staging",
                                    endpointId = "getUser"
                                ),
                                config = EndpointConfig(
                                    id = "getUser",
                                    name = "Get User",
                                    path = "/api/users/{userId}",
                                    method = "GET"
                                ),
                                availableResponses = stagingGetUserResponses
                            ),
                            currentState = EndpointMockState.Network
                        ),
                        EndpointUiModel(
                            descriptor = EndpointDescriptor(
                                key = EndpointKey(
                                    groupId = "example",
                                    environmentId = "staging",
                                    endpointId = "createUser"
                                ),
                                config = EndpointConfig(
                                    id = "createUser",
                                    name = "Create User",
                                    path = "/api/users",
                                    method = "POST"
                                ),
                                availableResponses = stagingCreateUserResponses
                            ),
                            currentState = EndpointMockState.Mock(responseFile = "createUser-201.json")
                        )
                    )
                ),
                GroupEnvironmentUiModel(
                    groupId = "example",
                    environmentId = "production",
                    name = "Production",
                    url = "https://staging.api.example.com",
                    endpoints = persistentListOf(
                        EndpointUiModel(
                            descriptor = EndpointDescriptor(
                                key = EndpointKey(
                                    groupId = "example",
                                    environmentId = "production",
                                    endpointId = "getUser"
                                ),
                                config = EndpointConfig(
                                    id = "getUser",
                                    name = "Get User",
                                    path = "/api/users/{userId}",
                                    method = "GET"
                                ),
                                availableResponses = stagingGetUserResponses
                            ),
                            currentState = EndpointMockState.Network
                        ),
                        EndpointUiModel(
                            descriptor = EndpointDescriptor(
                                key = EndpointKey(
                                    groupId = "example",
                                    environmentId = "production",
                                    endpointId = "createUser"
                                ),
                                config = EndpointConfig(
                                    id = "createUser",
                                    name = "Create User",
                                    path = "/api/users",
                                    method = "POST"
                                ),
                                availableResponses = stagingCreateUserResponses
                            ),
                            currentState = EndpointMockState.Mock(responseFile = "createUser-201.json")
                        )
                    )
                )
            )
        )
}

