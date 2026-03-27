package com.worldline.devview.networkmock.fixtures

import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointDescriptor
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.HostConfig
import com.worldline.devview.networkmock.core.model.MockConfiguration
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.viewmodel.EndpointUiModel
import com.worldline.devview.networkmock.viewmodel.HostUiModel
import com.worldline.devview.networkmock.viewmodel.NetworkMockUiState
import kotlinx.collections.immutable.toPersistentList

internal object MockScreenTestData {

    const val configPath: String = "files/networkmocks/mocks.json"

    private val stagingGetUserResponses = listOf(
        MockResponse(statusCode = 200, fileName = "getUser-200.json", displayName = "Success (200)", content = "{}"),
        MockResponse(statusCode = 404, fileName = "getUser-404-simple.json", displayName = "Not Found - Simple (404)", content = "{}")
    )

    private val stagingCreateUserResponses = listOf(
        MockResponse(statusCode = 201, fileName = "createUser-201.json", displayName = "Created (201)", content = "{}")
    )

    private val productionGetProductResponses = listOf(
        MockResponse(statusCode = 200, fileName = "getProduct-200.json", displayName = "Success (200)", content = "{}")
    )

    fun configurationWithTwoHosts(): MockConfiguration = MockConfiguration(
        hosts = listOf(
            HostConfig(
                id = "staging",
                url = "https://staging.api.example.com",
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
            ),
            HostConfig(
                id = "production",
                url = "https://api.example.com",
                endpoints = listOf(
                    EndpointConfig(
                        id = "getProduct",
                        name = "Get Product",
                        path = "/api/products/{productId}",
                        method = "GET"
                    )
                )
            )
        )
    )

    fun configurationWithNoHosts(): MockConfiguration = MockConfiguration(hosts = emptyList())

    fun defaultResources(): Map<String, String> = mapOf(
        configPath to defaultConfigJson(),
        "files/networkmocks/responses/getUser/getUser-200.json" to "{}",
        "files/networkmocks/responses/getUser/getUser-404-simple.json" to "{}",
        "files/networkmocks/responses/createUser/createUser-201.json" to "{}",
        "files/networkmocks/responses/getProduct/getProduct-200.json" to "{}"
    )

    fun emptyHostsResources(): Map<String, String> = mapOf(
        configPath to """
            {
              "hosts": []
            }
        """.trimIndent()
    )

    fun defaultConfigJson(): String = """
        {
          "hosts": [
            {
              "id": "staging",
              "url": "https://staging.api.example.com",
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
              ]
            },
            {
              "id": "production",
              "url": "https://api.example.com",
              "endpoints": [
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
    """.trimIndent()

    fun contentState(globalMockingEnabled: Boolean = false): NetworkMockUiState.Content =
        NetworkMockUiState.Content(
            globalMockingEnabled = globalMockingEnabled,
            hosts = listOf(
                HostUiModel(
                    id = "staging",
                    name = "staging",
                    url = "https://staging.api.example.com",
                    endpoints = listOf(
                        EndpointUiModel(
                            descriptor = EndpointDescriptor(
                                hostId = "staging",
                                endpointId = "getUser",
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
                                hostId = "staging",
                                endpointId = "createUser",
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
                    ).toPersistentList()
                ),
                HostUiModel(
                    id = "production",
                    name = "production",
                    url = "https://api.example.com",
                    endpoints = listOf(
                        EndpointUiModel(
                            descriptor = EndpointDescriptor(
                                hostId = "production",
                                endpointId = "getProduct",
                                config = EndpointConfig(
                                    id = "getProduct",
                                    name = "Get Product",
                                    path = "/api/products/{productId}",
                                    method = "GET"
                                ),
                                availableResponses = productionGetProductResponses
                            ),
                            currentState = EndpointMockState.Network
                        )
                    ).toPersistentList()
                )
            ).toPersistentList()
        )
}

