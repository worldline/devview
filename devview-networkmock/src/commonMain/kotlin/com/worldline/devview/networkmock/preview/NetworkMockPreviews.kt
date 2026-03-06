package com.worldline.devview.networkmock.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.EndpointCard
import com.worldline.devview.networkmock.components.GlobalMockToggle
import com.worldline.devview.networkmock.model.AvailableEndpointMock
import com.worldline.devview.networkmock.model.EndpointConfig
import com.worldline.devview.networkmock.model.EndpointMockState
import com.worldline.devview.networkmock.model.MockResponse

private const val PREVIEW_HOST_ID_STAGING = "staging"
private const val PREVIEW_ENDPOINT_ID_GET_USER = "getUser"
private const val PREVIEW_USER_PATH = "/api/v1/user/{userId}"

@Preview(name = "Global Toggle - Enabled")
@Composable
private fun GlobalMockToggleEnabledPreview() {
    MaterialTheme {
        Surface {
            GlobalMockToggle(
                enabled = true,
                onToggle = {},
                modifier = Modifier.padding(all = 16.dp)
            )
        }
    }
}

@Preview(name = "Global Toggle - Disabled")
@Composable
private fun GlobalMockToggleDisabledPreview() {
    MaterialTheme {
        Surface {
            GlobalMockToggle(
                enabled = false,
                onToggle = {},
                modifier = Modifier.padding(all = 16.dp)
            )
        }
    }
}

@Preview(name = "Endpoint Card - Mock Disabled")
@Composable
private fun EndpointCardNetworkPreview() {
    MaterialTheme {
        Surface {
            EndpointCard(
                endpoint = AvailableEndpointMock(
                    hostId = PREVIEW_HOST_ID_STAGING,
                    endpointId = PREVIEW_ENDPOINT_ID_GET_USER,
                    config = EndpointConfig(
                        id = PREVIEW_ENDPOINT_ID_GET_USER,
                        name = "Get User Profile",
                        path = PREVIEW_USER_PATH,
                        method = "GET"
                    ),
                    availableResponses = listOf(
                        MockResponse(
                            statusCode = 200,
                            fileName = "getUser-200.json",
                            displayName = "Success (200)",
                            content = "{}"
                        ),
                        MockResponse(
                            statusCode = 404,
                            fileName = "getUser-404.json",
                            displayName = "Not Found (404)",
                            content = "{}"
                        )
                    ),
                    currentState = EndpointMockState(
                        mockEnabled = false,
                        selectedResponseFile = null
                    )
                ),
                onToggleMock = {},
                onSelectResponse = {},
                modifier = Modifier.padding(all = 16.dp)
            )
        }
    }
}

@Preview(name = "Endpoint Card - Mock Enabled")
@Composable
private fun EndpointCardMockEnabledPreview() {
    MaterialTheme {
        Surface {
            EndpointCard(
                endpoint = AvailableEndpointMock(
                    hostId = PREVIEW_HOST_ID_STAGING,
                    endpointId = PREVIEW_ENDPOINT_ID_GET_USER,
                    config = EndpointConfig(
                        id = PREVIEW_ENDPOINT_ID_GET_USER,
                        name = "Get User Profile",
                        path = PREVIEW_USER_PATH,
                        method = "GET"
                    ),
                    availableResponses = listOf(
                        MockResponse(
                            statusCode = 200,
                            fileName = "getUser-200.json",
                            displayName = "Success (200)",
                            content = "{}"
                        ),
                        MockResponse(
                            statusCode = 404,
                            fileName = "getUser-404.json",
                            displayName = "Not Found (404)",
                            content = "{}"
                        ),
                        MockResponse(
                            statusCode = 500,
                            fileName = "getUser-500.json",
                            displayName = "Server Error (500)",
                            content = "{}"
                        )
                    ),
                    currentState = EndpointMockState(
                        mockEnabled = true,
                        selectedResponseFile = "getUser-200.json"
                    )
                ),
                onToggleMock = {},
                onSelectResponse = {},
                modifier = Modifier.padding(all = 16.dp)
            )
        }
    }
}

@Preview(name = "Endpoint Card - No Responses")
@Composable
private fun EndpointCardNoResponsesPreview() {
    MaterialTheme {
        Surface {
            EndpointCard(
                endpoint = AvailableEndpointMock(
                    hostId = PREVIEW_HOST_ID_STAGING,
                    endpointId = "deleteUser",
                    config = EndpointConfig(
                        id = "deleteUser",
                        name = "Delete User",
                        path = PREVIEW_USER_PATH,
                        method = "DELETE"
                    ),
                    availableResponses = emptyList(),
                    currentState = EndpointMockState(
                        mockEnabled = true,
                        selectedResponseFile = null
                    )
                ),
                onToggleMock = {},
                onSelectResponse = {},
                modifier = Modifier.padding(all = 16.dp)
            )
        }
    }
}

@Preview(name = "Endpoint Card - POST Endpoint")
@Composable
private fun EndpointCardPostPreview() {
    MaterialTheme {
        Surface {
            EndpointCard(
                endpoint = AvailableEndpointMock(
                    hostId = "production",
                    endpointId = "createPost",
                    config = EndpointConfig(
                        id = "createPost",
                        name = "Create Post",
                        path = "/posts",
                        method = "POST"
                    ),
                    availableResponses = listOf(
                        MockResponse(
                            statusCode = 201,
                            fileName = "createPost-201.json",
                            displayName = "Created (201)",
                            content = "{}"
                        ),
                        MockResponse(
                            statusCode = 400,
                            fileName = "createPost-400.json",
                            displayName = "Bad Request (400)",
                            content = "{}"
                        ),
                        MockResponse(
                            statusCode = 401,
                            fileName = "createPost-401.json",
                            displayName = "Unauthorized (401)",
                            content = "{}"
                        )
                    ),
                    currentState = EndpointMockState(
                        mockEnabled = true,
                        selectedResponseFile = "createPost-201.json"
                    )
                ),
                onToggleMock = {},
                onSelectResponse = {},
                modifier = Modifier.padding(all = 16.dp)
            )
        }
    }
}
