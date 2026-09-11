package com.worldline.devview.networkmock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.EndpointStateChip
import com.worldline.devview.networkmock.components.ErrorState
import com.worldline.devview.networkmock.components.LoadingState
import com.worldline.devview.networkmock.components.MockItem
import com.worldline.devview.networkmock.components.NetworkItem
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.preview.OperationUiModelPreviewParameterProvider
import com.worldline.devview.networkmock.utils.badgeContainerColor
import com.worldline.devview.networkmock.utils.badgeContentColor
import com.worldline.devview.networkmock.utils.fake
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

/**
 * A modal bottom sheet showing every mock response available for one operation, letting the
 * user pick one — or preview/compare marked responses on a second page.
 *
 * Replaces the previous full-screen operation detail: picking a response and previewing a
 * response body both now happen in one sheet, without leaving the operation list. Page 1 (the
 * picker) is shown whenever [sheetState] is [OperationSheetState.Content] and no response is
 * marked-and-opened for preview; tapping a row activates it and dismisses the sheet, tapping a
 * row's preview toggle marks it without dismissing, and a "Preview"/"Compare 2 responses" button
 * appears once at least one response is marked. Page 2 ([MockResponsePreviewPage]) shows the
 * marked response(s); its back arrow returns to page 1 without losing the marks.
 *
 * @param sheetState The current [OperationSheetState] from [com.worldline.devview.networkmock.viewmodel.NetworkMockViewModel.sheetState].
 * @param onDismissRequest Called when the sheet should close (row tap, swipe, tap outside, close button).
 * @param onSelectResponse Called with the tapped response (or `null` for "no mock") when a row is selected.
 * @param modifier [Modifier] to be applied to the [ModalBottomSheet].
 */
@Composable
internal fun NetworkMockOperationSheet(
    sheetState: OperationSheetState,
    onDismissRequest: () -> Unit,
    onSelectResponse: (MockResponse?) -> Unit,
    modifier: Modifier = Modifier
) {
    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnDismissRequest by rememberUpdatedState(newValue = onDismissRequest)

    val onClose: () -> Unit = {
        scope
            .launch {
                modalSheetState.hide()
            }.invokeOnCompletion {
                if (!modalSheetState.isVisible) currentOnDismissRequest()
            }
    }

    // Which responses are marked for preview, and whether page 2 is currently showing them —
    // two separate pieces of state, since marking a response and viewing the preview page are
    // distinct actions (see the "Preview"/"Compare" button below).
    var markedForPreview: PreviewSheetState by remember {
        mutableStateOf(
            value = PreviewSheetState.Hidden
        )
    }
    var showingPreviewPage by remember { mutableStateOf(value = false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalSheetState,
        modifier = modifier
    ) {
        when (sheetState) {
            OperationSheetState.Hidden -> Unit
            OperationSheetState.Loading -> LoadingState()
            is OperationSheetState.Error -> ErrorState(message = sheetState.message)
            is OperationSheetState.Content -> {
                AnimatedContent(
                    targetState = showingPreviewPage,
                    transitionSpec = {
                        if (targetState) {
                            slideInHorizontally(
                                animationSpec = tween()
                            ) { it } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween()) { -it } + fadeOut()
                        } else {
                            slideInHorizontally(
                                animationSpec = tween()
                            ) { -it } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween()) { it } + fadeOut()
                        }
                    },
                    label = "operationSheetPage"
                ) { onPreviewPage ->
                    val marked = markedForPreview
                    if (onPreviewPage && marked is PreviewSheetState.HasResponse) {
                        MockResponsePreviewPage(
                            previewSheetState = marked,
                            onBack = { showingPreviewPage = false }
                        )
                    } else {
                        OperationPickerPage(
                            content = sheetState,
                            markedForPreview = markedForPreview,
                            onSelectResponse = { response ->
                                onSelectResponse(response)
                                onClose()
                            },
                            onTogglePreview = { response ->
                                markedForPreview = markedForPreview.transition(response = response)
                            },
                            onOpenPreview = { showingPreviewPage = true },
                            onClose = onClose
                        )
                    }
                }
            }
        }
    }
}

/**
 * Page 1 of [NetworkMockOperationSheet]: the operation header, followed by every response
 * variant grouped by [StatusCodeFamily], and a "Preview"/"Compare 2 responses" button once at
 * least one response is [markedForPreview].
 *
 * `internal` (rather than `private`) so it can be exercised directly in device tests without
 * going through [NetworkMockOperationSheet]'s `ModalBottomSheet` chrome and page-transition
 * animation — this codebase has no established pattern for testing `ModalBottomSheet` itself.
 */
@Composable
internal fun OperationPickerPage(
    content: OperationSheetState.Content,
    markedForPreview: PreviewSheetState,
    onSelectResponse: (MockResponse?) -> Unit,
    onTogglePreview: (MockResponse) -> Unit,
    onOpenPreview: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val endpoint = content.operationUiModel
    val groupedResponses = content.responses.groupBy {
        StatusCodeFamily.fromStatusCode(statusCode = it.statusCode)
    }
    val selectedResponse = when (val currentState = endpoint.currentState) {
        is OperationMockState.Mock -> content.responses.find {
            it.statusCode == currentState.statusCode && it.exampleName == currentState.exampleName
        }

        OperationMockState.Network -> null
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        OperationPickerHeader(endpoint = endpoint, onClose = onClose)
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "network_item") {
                NetworkItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(tag = "operation_sheet_network_item"),
                    selected = selectedResponse == null,
                    onClick = { onSelectResponse(null) }
                )
            }

            groupedResponses.forEach { (statusCodeFamily, mockResponses) ->
                stickyHeader(key = "header_${statusCodeFamily.name.lowercase()}") {
                    Surface {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            text = "${statusCodeFamily.displayName} mocks".uppercase(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                mockResponses.forEachIndexed { index, mockResponse ->
                    val itemKey = "mock_item_${mockResponse.statusCode}_${mockResponse.exampleName}"
                    item(key = itemKey) {
                        MockItem(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .testTag(tag = itemKey),
                            mockResponse = mockResponse,
                            selected = selectedResponse == mockResponse,
                            onClick = { onSelectResponse(mockResponse) },
                            isMarkedForPreview = markedForPreview.isInPreviewMode(
                                response = mockResponse
                            ),
                            onToggleMarkedForPreview = { onTogglePreview(mockResponse) },
                            previewToggleTestTag = "${itemKey}_preview_toggle"
                        )
                        if (index != mockResponses.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
                        }
                    }
                }
            }
        }

        if (markedForPreview is PreviewSheetState.HasResponse) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp)
                    .testTag(tag = "open_preview_button"),
                onClick = onOpenPreview
            ) {
                Text(text = previewButtonLabel(state = markedForPreview))
            }
        }
    }
}

private fun previewButtonLabel(state: PreviewSheetState.HasResponse): String = when (state) {
    is PreviewSheetState.Single -> "Preview ${state.response.displayName}"
    is PreviewSheetState.Compare -> "Compare 2 responses"
}

@Composable
private fun OperationPickerHeader(
    endpoint: OperationUiModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f)
        ) {
            Text(
                text = endpoint.descriptor.config.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = endpoint.descriptor.config.method.badgeContainerColor,
                            shape = RoundedCornerShape(size = 4.dp)
                        ).padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = endpoint.descriptor.config.method.value,
                        style = MaterialTheme.typography.labelSmall,
                        color = endpoint.descriptor.config.method.badgeContentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = endpoint.descriptor.config.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                endpoint.descriptor.config.version?.let { version ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(size = 4.dp)
                            ).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = version,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        EndpointStateChip(endpointMockState = endpoint.currentState)
        IconButton(
            modifier = Modifier.testTag(tag = "operation_sheet_close_button"),
            onClick = onClose
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
        }
    }
}

@Preview(locale = "en")
@Composable
private fun NetworkMockOperationSheetPickerPreview(
    @PreviewParameter(
        OperationUiModelPreviewParameterProvider::class
    ) endpoint: OperationUiModel
) {
    MaterialTheme {
        Surface {
            OperationPickerPage(
                content = OperationSheetState.Content(
                    operationUiModel = endpoint,
                    responses = MockResponse.fake(amount = 6).toPersistentList()
                ),
                markedForPreview = PreviewSheetState.Hidden,
                onSelectResponse = {},
                onTogglePreview = {},
                onOpenPreview = {},
                onClose = {}
            )
        }
    }
}
