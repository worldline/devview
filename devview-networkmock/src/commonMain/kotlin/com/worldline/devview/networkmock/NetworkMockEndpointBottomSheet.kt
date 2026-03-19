package com.worldline.devview.networkmock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.MockItem
import com.worldline.devview.networkmock.components.NetworkItem
import com.worldline.devview.networkmock.core.model.EndpointDescriptor
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import com.worldline.devview.networkmock.preview.EndpointUiModelPreviewParameterProvider
import com.worldline.devview.networkmock.viewmodel.EndpointUiModel
import kotlinx.coroutines.launch

@Composable
internal fun NetworkMockEndpointBottomSheet(
    descriptor: EndpointDescriptor,
    currentState: EndpointMockState,
    onSelectResponse: (responseFileName: String?) -> Unit,
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    val groupedResponses = descriptor.availableResponses.groupBy {
        StatusCodeFamily.fromStatusCode(statusCode = it.statusCode)
    }

    val selectedResponse = when (currentState) {
        is EndpointMockState.Mock -> descriptor.availableResponses.find {
            it.fileName == currentState.responseFile
        }
        EndpointMockState.Network -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                    text = descriptor.config.name,
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd),
                    onClick = {
                        scope
                            .launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismissRequest()
                                }
                            }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                stickyHeader(
                    key = "network_header"
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                        text = "No mock".uppercase(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                item(
                    key = "network_item"
                ) {
                    NetworkItem(
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.background)
                            .padding(
                                horizontal = 16.dp
                            ),
                        selected = selectedResponse == null,
                        onClick = {
                            onSelectResponse(null)
                        }
                    )
                }

                item(
                    key = "network_spacer"
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = 16.dp)
                    )
                }
                groupedResponses.forEach { (statusCodeFamily, mockResponses) ->
                    stickyHeader(
                        key = "header_${statusCodeFamily.name.lowercase()}"
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),
                            text = "${statusCodeFamily.displayName} mocks".uppercase(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    itemsIndexed(
                        items = mockResponses,
                        key = { _, mockResponse -> "mock_item_${mockResponse.fileName}" }
                    ) { index, mockResponse ->
                        MockItem(
                            mockResponse = mockResponse,
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.background)
                                .padding(
                                    horizontal = 16.dp
                                ),
                            selected = selectedResponse?.fileName == mockResponse.fileName,
                            onClick = {
                                onSelectResponse(mockResponse.fileName)
                            }
                        )

                        if (index != mockResponses.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.background)
                                    .padding(
                                        start = 64.dp
                                    )
                            )
                        }
                    }
                    item(
                        key = "${statusCodeFamily.name.lowercase()}_spacer"
                    ) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun NetworkMockEndpointBottomSheetPreview(
    @PreviewParameter(
        provider = EndpointUiModelPreviewParameterProvider::class
    ) endpointUiModel: EndpointUiModel
) {
    MaterialTheme {
        Surface {
            NetworkMockEndpointBottomSheet(
                descriptor = endpointUiModel.descriptor,
                currentState = endpointUiModel.currentState,
                onSelectResponse = {}
            )
        }
    }
}
