@file:Suppress("StringLiteralDuplication")

package com.worldline.devview.networkmock.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.Operation
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import com.worldline.devview.networkmock.model.ApiSpecUiModel
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.theme.rememberMockColorScheme
import kotlinx.collections.immutable.toPersistentList

internal fun ApiSpecUiModel.Companion.fake(amount: Int = 4): List<ApiSpecUiModel> =
    List(size = amount) { index ->
        val specId = "spec${if (index <= 2) "" else index / 3 % 3}"
        ApiSpecUiModel(
            specId = specId,
            name = specId.capitalize(locale = Locale.current),
            operations = OperationUiModel.fake(specId = specId).toPersistentList()
        )
    }

internal fun OperationDescriptor.Companion.fake(
    amount: Int = 7,
    specId: String = "spec"
): List<OperationDescriptor> = List(size = amount) { index ->
    OperationDescriptor(
        key = OperationKey(specId = specId, operationId = "operation-${index + 1}"),
        config = Operation(
            operationId = "operation-${index + 1}",
            name = "Operation ${index + 1}",
            method = "GET",
            path = "/operation${index + 1}"
        )
    )
}

internal fun OperationUiModel.Companion.fake(
    amount: Int = 7,
    specId: String = "spec"
): List<OperationUiModel> = OperationDescriptor
    .fake(
        amount = amount,
        specId = specId
    ).mapIndexed { index, descriptor ->
        OperationUiModel(
            descriptor = descriptor,
            currentState = when (index) {
                in 0..5 -> OperationMockState.Mock(
                    statusCode = exampleStatusCode(index = index),
                    exampleName = "default"
                )
                else -> OperationMockState.Network
            }
        )
    }

internal val OperationMockState.icon: ImageVector
    get() = when (this) {
        is OperationMockState.Mock -> iconForStatusCode(statusCode = statusCode)
        OperationMockState.Network -> Icons.Rounded.Wifi
    }

internal fun iconForStatusCode(statusCode: Int?): ImageVector = when (statusCode) {
    in 100..199 -> Icons.Rounded.Info
    in 200..299 -> Icons.Rounded.CheckCircleOutline
    in 300..399 -> Icons.AutoMirrored.Rounded.Redo
    in 400..499 -> Icons.Rounded.ErrorOutline
    in 500..599 -> Icons.Rounded.CloudOff
    else -> Icons.AutoMirrored.Rounded.HelpOutline
}

internal val OperationMockState.contentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        is OperationMockState.Mock -> contentColorForStatusCode(statusCode = statusCode)
        OperationMockState.Network -> rememberMockColorScheme().network.content
    }

@Composable
@ReadOnlyComposable
internal fun contentColorForStatusCode(statusCode: Int?): Color =
    rememberMockColorScheme()[statusCode.toStatusCodeFamily()].content

internal val OperationMockState.containerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        is OperationMockState.Mock -> containerColorForStatusCode(statusCode = statusCode)
        OperationMockState.Network -> rememberMockColorScheme().network.container
    }

@Composable
@ReadOnlyComposable
internal fun containerColorForStatusCode(statusCode: Int?): Color =
    rememberMockColorScheme()[statusCode.toStatusCodeFamily()].container

private fun Int?.toStatusCodeFamily(): StatusCodeFamily =
    this?.let { StatusCodeFamily.fromStatusCode(statusCode = it) } ?: StatusCodeFamily.UNKNOWN

internal fun MockResponse.Companion.fake(amount: Int = 3): List<MockResponse> =
    List(size = amount) { index ->
        MockResponse(
            statusCode = exampleStatusCode(index = index),
            exampleName = "default",
            displayName = "Response $index",
            content = "{\n  \"message\": \"This is a mock response $index\"\n}"
        )
    }

private fun exampleStatusCode(index: Int) = (index + 1) % 6 * 100
