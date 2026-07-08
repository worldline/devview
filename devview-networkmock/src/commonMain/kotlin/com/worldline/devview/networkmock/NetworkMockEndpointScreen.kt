package com.worldline.devview.networkmock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldline.devview.networkmock.components.EndpointHeaderCard
import com.worldline.devview.networkmock.components.ErrorState
import com.worldline.devview.networkmock.components.LoadingState
import com.worldline.devview.networkmock.components.MockItem
import com.worldline.devview.networkmock.components.NetworkItem
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import com.worldline.devview.networkmock.model.DiffLine
import com.worldline.devview.networkmock.model.EndpointUiModel
import com.worldline.devview.networkmock.preview.EndpointUiModelPreviewParameterProvider
import com.worldline.devview.networkmock.utils.INLINE_DIFF_THRESHOLD
import com.worldline.devview.networkmock.utils.computeLineDiff
import com.worldline.devview.networkmock.utils.shouldUseInlineDiff
import com.worldline.devview.networkmock.viewmodel.NetworkMockEndpointUiState
import com.worldline.devview.networkmock.viewmodel.NetworkMockEndpointViewModel
import kotlinx.collections.immutable.PersistentList

/**
 * Detail screen for a single API endpoint, showing all available mock responses and
 * allowing the user to activate one or revert to the actual network.
 *
 * Driven entirely by [viewModel], which is constructed and provided by
 * [NetworkMock.registerContent] inside the `entry<NetworkMockDestination.Endpoint>` lambda
 * so that it is scoped to the navigation entry and receives the correct
 * [com.worldline.devview.networkmock.model.EndpointKey].
 *
 * Renders three possible states from [NetworkMockEndpointViewModel.uiState]:
 * - [NetworkMockEndpointUiState.Loading] — shown while mock response files are being discovered
 * - [NetworkMockEndpointUiState.Error] — shown if discovery or config lookup fails
 * - [NetworkMockEndpointUiState.Content] — the grouped mock list (see [NetworkMockEndpointScreenContent])
 *
 * @param viewModel The [NetworkMockEndpointViewModel] scoped to this navigation entry.
 * @param modifier Optional modifier for the screen.
 * @param bottomPadding Bottom inset padding provided by the DevView [androidx.compose.material3.Scaffold].
 *   Applied as [LazyColumn] content padding so the last item is not obscured by system navigation bars.
 */
@Composable
internal fun NetworkMockEndpointScreen(
    viewModel: NetworkMockEndpointViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is NetworkMockEndpointUiState.Loading -> LoadingState(modifier = modifier.fillMaxSize())
        is NetworkMockEndpointUiState.Error -> ErrorState(
            message = state.message,
            modifier = modifier.fillMaxSize()
        )

        is NetworkMockEndpointUiState.Content -> {
            // Stub for Step 3 — will be replaced with the preview sheet
            var previewingResponse by remember { mutableStateOf<MockResponse?>(value = null) }

            NetworkMockEndpointScreenContent(
                content = state,
                onSelectResponse = viewModel::setMockState,
                onPreviewClick = { previewingResponse = it },
                modifier = modifier,
                bottomPadding = bottomPadding
            )
        }
    }
}

@Composable
private fun NetworkMockEndpointScreenContent(
    content: NetworkMockEndpointUiState.Content,
    onSelectResponse: (responseFileName: String?) -> Unit,
    onPreviewClick: (MockResponse) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val endpointUiModel = content.endpointUiModel
    val descriptor = endpointUiModel.descriptor
    val groupedResponses = descriptor.availableResponses.groupBy {
        StatusCodeFamily.fromStatusCode(statusCode = it.statusCode)
    }
    val selectedResponse = when (val currentState = endpointUiModel.currentState) {
        is EndpointMockState.Mock ->
            descriptor.availableResponses.find { it.fileName == currentState.responseFile }

        EndpointMockState.Network -> null
    }

    var previewSheetState: PreviewSheetState by remember {
        mutableStateOf(
            value = PreviewSheetState.Hidden
        )
    }

    var showPreviewBottomSheet by remember { mutableStateOf(value = false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        floatingActionButton = {
            AnimatedVisibility(
                visible = previewSheetState != PreviewSheetState.Hidden,
                enter = slideInVertically { it * 2 } + fadeIn() + scaleIn(),
                exit = slideOutVertically { it * 2 } + fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        showPreviewBottomSheet = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                EndpointHeaderCard(
                    endpoint = endpointUiModel
                )
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        text = "Long press a mock response to be able to preview its content",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                stickyHeader(key = "network_header") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = "No mock".uppercase(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                item(key = "network_item") {
                    NetworkItem(
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp),
                        selected = selectedResponse == null,
                        onClick = { onSelectResponse(null) }
                    )
                }
                item(key = "network_spacer") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = 16.dp)
                    )
                }

                groupedResponses.forEach { (statusCodeFamily, mockResponses) ->
                    stickyHeader(key = "header_${statusCodeFamily.name.lowercase()}") {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                .padding(horizontal = 16.dp),
                            selected = selectedResponse?.fileName == mockResponse.fileName,
                            onClick = { onSelectResponse(mockResponse.fileName) },
                            onLongClick = {
                                previewSheetState =
                                    previewSheetState.transition(response = mockResponse)
                            },
                            isInPreviewMode = previewSheetState.isInPreviewMode(
                                response = mockResponse
                            )
                        )
                        if (index != mockResponses.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.background)
                                    .padding(start = 64.dp)
                            )
                        }
                    }
                    item(key = "${statusCodeFamily.name.lowercase()}_spacer") {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = 16.dp)
                        )
                    }
                }

                item {
                    Spacer(
                        modifier = Modifier
                            .padding(bottom = bottomPadding)
                    )
                }
            }
        }
    }

    if (showPreviewBottomSheet && previewSheetState is PreviewSheetState.HasResponse) {
        NetworkMockEndpointPreviewBottomSheet(
            previewSheetState = previewSheetState as PreviewSheetState.HasResponse,
            onDismissRequest = { showPreviewBottomSheet = false }
        )
    }
}

@Immutable
internal sealed interface PreviewSheetState {
    @Immutable
    sealed interface HasResponse : PreviewSheetState

    /** Sheet is closed. */
    @Immutable
    data object Hidden : PreviewSheetState

    /** Sheet shows one response. */
    @Immutable
    data class Single(val response: MockResponse) : HasResponse

    /** Sheet shows two responses. */
    @Immutable
    data class Compare(
        val first: MockResponse,
        val second: MockResponse,
        val threshold: Float = INLINE_DIFF_THRESHOLD
    ) : HasResponse {
        val useInlineDiff: Boolean
            get() = shouldUseInlineDiff(
                contentLeft = first.content,
                contentRight = second.content,
                threshold = threshold
            )

        val lineDiff: PersistentList<DiffLine>
            get() = computeLineDiff(
                contentLeft = first.content,
                contentRight = second.content
            )
    }

    fun transition(response: MockResponse): PreviewSheetState = when (this) {
        is Hidden -> Single(response = response)
        is Single -> if (response == this.response) {
            Hidden
        } else {
            Compare(first = this.response, second = response)
        }

        is Compare -> when (response) {
            first -> {
                Single(response = second)
            }

            second -> {
                Single(response = first)
            }

            else -> {
                this
            }
        }
    }

    fun isInPreviewMode(response: MockResponse): Boolean = when (this) {
        is Hidden -> false
        is Single -> response == this.response
        is Compare -> response == first || response == second
    }
}

@Preview(locale = "en")
@Composable
private fun NetworkMockEndpointScreenPreview(
    @PreviewParameter(
        provider = EndpointUiModelPreviewParameterProvider::class
    ) endpointUiModel: EndpointUiModel
) {
    MaterialTheme {
        Surface {
            NetworkMockEndpointScreenContent(
                content = NetworkMockEndpointUiState.Content(
                    endpointUiModel = endpointUiModel
                ),
                onSelectResponse = {},
                onPreviewClick = {}
            )
        }
    }
}
