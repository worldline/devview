package com.worldline.devview.networkmock.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.worldline.devview.networkmock.core.model.EndpointConfig
import com.worldline.devview.networkmock.core.model.EndpointDescriptor
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.viewmodel.EndpointUiModel
import com.worldline.devview.networkmock.viewmodel.HostUiModel
import kotlinx.collections.immutable.toPersistentList

internal fun HostUiModel.Companion.fake(amount: Int = 5): List<HostUiModel> =
    List(size = amount) { index ->
        HostUiModel(
            id = "host-$index",
            name = "Host $index",
            url = "https://api.host$index.com",
            endpoints = EndpointUiModel.fake().toPersistentList()
        )
    }

internal fun EndpointDescriptor.Companion.fake(
    amount: Int = 7,
    availableResponsesAmount: Int = 3,
    hostId: String = "host"
): List<EndpointDescriptor> = List(size = amount) { index ->
    EndpointDescriptor(
        hostId = hostId,
        endpointId = "endpoint-$index",
        config = EndpointConfig(
            id = "endpoint-$index",
            name = "Endpoint $index",
            method = "GET",
            path = "/endpoint$index"
        ),
        availableResponses = MockResponse.fake(
            amount = availableResponsesAmount
        )
    )
}

internal fun EndpointUiModel.Companion.fake(
    amount: Int = 7,
    availableResponsesAmount: Int = 3,
    hostId: String = "host"
): List<EndpointUiModel> = EndpointDescriptor
    .fake(
        amount = amount,
        availableResponsesAmount = availableResponsesAmount,
        hostId = hostId
    ).mapIndexed { index, descriptor ->
        EndpointUiModel(
            descriptor = descriptor,
            currentState = when (index % 6) {
                in 0..5 -> EndpointMockState.Mock(responseFile = "response-${index + 1}00.json")
                else -> EndpointMockState.Network
            }
        )
    }

internal val EndpointMockState.icon: ImageVector
    get() {
        return when (this) {
            is EndpointMockState.Mock -> iconForStatusCode(statusCode = statusCode)

            EndpointMockState.Network -> Icons.Rounded.Wifi
        }
    }

internal fun iconForStatusCode(statusCode: Int?): ImageVector = when (statusCode) {
    in 100..199 -> Icons.Rounded.Info
    in 200..299 -> Icons.Rounded.CheckCircleOutline
    in 300..399 -> Icons.AutoMirrored.Rounded.Redo
    in 400..499 -> Icons.Rounded.ErrorOutline
    in 500..599 -> Icons.Rounded.CloudOff
    else -> Icons.AutoMirrored.Rounded.HelpOutline
}

internal val EndpointMockState.contentColor: Color
    get() {
        return when (this) {
            is EndpointMockState.Mock -> contentColorForStatusCode(statusCode = statusCode)

            EndpointMockState.Network -> Color(color = 0xFF0D1F3A)
        }
    }

internal fun contentColorForStatusCode(statusCode: Int?): Color = when (statusCode) {
    in 100..199 -> Color(color = 0xFF184559)
    in 200..299 -> Color(color = 0xFF103C13)
    in 300..399 -> Color(color = 0xFF603610)
    in 400..499 -> Color(color = 0xFF6F1111)
    in 500..599 -> Color(color = 0xFF611A59)
    else -> Color(color = 0xFF3D3D3D)
}

internal val EndpointMockState.containerColor: Color
    get() {
        return when (this) {
            is EndpointMockState.Mock -> containerColorForStatusCode(statusCode = statusCode)

            EndpointMockState.Network -> Color(color = 0xFFABC4ED)
        }
    }

internal fun containerColorForStatusCode(statusCode: Int?): Color = when (statusCode) {
    in 100..199 -> Color(color = 0xFFB7DCEC)
    in 200..299 -> Color(color = 0xFFB7ECBA)
    in 300..399 -> Color(color = 0xFFF0CAA7)
    in 400..499 -> Color(color = 0xFFECB7B7)
    in 500..599 -> Color(color = 0xFFECB7E6)
    else -> Color(color = 0xFFD1D1D1)
}

internal fun MockResponse.Companion.fake(amount: Int = 3): List<MockResponse> =
    List(size = amount) { index ->
        MockResponse(
            fileName = "response$index.json",
            statusCode = (index + 1) % 6 * 100,
            displayName = "Response $index",
            content = "{\n  \"message\": \"This is a mock response $index\"\n}"
        )
    }
