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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.worldline.devview.networkmock.components.ErrorState
import com.worldline.devview.networkmock.components.FailureItem
import com.worldline.devview.networkmock.components.LoadingState
import com.worldline.devview.networkmock.components.MockItem
import com.worldline.devview.networkmock.components.NetworkItem
import com.worldline.devview.networkmock.core.model.FailureKind
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

private enum class SheetPage { Picker, Preview, Sequence }

/**
 * A fixed-height operation sheet with picker, response preview/compare, and sequence builder pages.
 * Selecting a response or failure, or saving a sequence, activates it and closes the sheet.
 * The eye opens a response directly; Back from preview or sequence returns to the picker.
 *
 * @param sheetState The current [OperationSheetState] from the ViewModel.
 * @param onDismissRequest Called when the sheet should close.
 * @param onSelectResponse Called with the tapped response, or null for no mock.
 * @param onSelectFailure Called with the tapped failure kind.
 * @param onCommitSequence Called with the ordered responses when Save Sequence is tapped.
 * @param onResetSequencePosition Called when Reset is tapped for an active sequence.
 * @param modifier [Modifier] applied to the [ModalBottomSheet].
 */
@Composable
internal fun NetworkMockOperationSheet(
    sheetState: OperationSheetState,
    onDismissRequest: () -> Unit,
    onSelectResponse: (MockResponse?) -> Unit,
    onSelectFailure: (FailureKind) -> Unit,
    onCommitSequence: (List<MockResponse>) -> Unit,
    onResetSequencePosition: () -> Unit,
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
    val operationKey = (sheetState as? OperationSheetState.Content)
        ?.operationUiModel
        ?.descriptor
        ?.key
    var page by remember(key1 = operationKey) { mutableStateOf(value = SheetPage.Picker) }
    var previewState: PreviewSheetState by remember(key1 = operationKey) {
        mutableStateOf(value = PreviewSheetState.Hidden)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalSheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        NavigationEventHandler(
            state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
            isForwardEnabled = false,
            isBackEnabled = page != SheetPage.Picker,
            onBackCompleted = { page = SheetPage.Picker }
        )
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.9f)) {
            when (sheetState) {
                OperationSheetState.Hidden -> Unit
                OperationSheetState.Loading -> LoadingState()
                is OperationSheetState.Error -> ErrorState(message = sheetState.message)
                is OperationSheetState.Content -> AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = page,
                    transitionSpec = {
                        if (targetState != SheetPage.Picker) {
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
                ) { currentPage ->
                    when (currentPage) {
                        SheetPage.Picker -> OperationPickerPage(
                            content = sheetState,
                            onSelectResponse = { response ->
                                onSelectResponse(response)
                                onClose()
                            },
                            onSelectFailure = { kind ->
                                onSelectFailure(kind)
                                onClose()
                            },
                            onResetSequencePosition = onResetSequencePosition,
                            onPreview = { response ->
                                previewState = PreviewSheetState.Single(response = response)
                                page = SheetPage.Preview
                            },
                            onOpenSequence = { page = SheetPage.Sequence },
                            onClose = onClose
                        )
                        SheetPage.Preview -> {
                            val preview = previewState as? PreviewSheetState.HasResponse
                            if (preview != null) {
                                val first = when (preview) {
                                    is PreviewSheetState.Single -> preview.response
                                    is PreviewSheetState.Compare -> preview.first
                                }
                                MockResponsePreviewPage(
                                    previewSheetState = preview,
                                    otherResponses = sheetState.responses.filter { it != first },
                                    onCompareWith = { response ->
                                        previewState = PreviewSheetState.Compare(
                                            first = first,
                                            second = response
                                        )
                                    },
                                    onBack = { page = SheetPage.Picker }
                                )
                            }
                        }
                        SheetPage.Sequence -> SequenceBuilderPage(
                            content = sheetState,
                            onBack = { page = SheetPage.Picker },
                            onCommitSequence = { responses ->
                                onCommitSequence(responses)
                                onClose()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * The operation picker: grouped responses and failures, direct eye previews, and a sequence action.
 * Kept internal so device tests can exercise callbacks without modal chrome or page animations.
 */
@Composable
internal fun OperationPickerPage(
    content: OperationSheetState.Content,
    onSelectResponse: (MockResponse?) -> Unit,
    onSelectFailure: (FailureKind) -> Unit,
    onResetSequencePosition: () -> Unit,
    onPreview: (MockResponse) -> Unit,
    onOpenSequence: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val endpoint = content.operationUiModel
    val groupedResponses = content.responses.groupBy {
        StatusCodeFamily.fromStatusCode(statusCode = it.statusCode)
    }
    val hasMultipleMediaTypes = content.responses
        .map { it.mediaType }
        .distinct()
        .size > 1
    val selectedState = endpoint.currentState as? OperationMockState.Mock
    val selectedFailureKind = (endpoint.currentState as? OperationMockState.Failure)?.kind
    val activeSequence = endpoint.currentState as? OperationMockState.Sequence
    val failureRate = endpoint.descriptor.config.failureRate

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    weight = 1f
                ).testTag(tag = "operation_picker_list")
        ) {
            item(key = "operation_header") {
                Column(modifier = Modifier.animateItem()) {
                    OperationPickerHeader(endpoint = endpoint, onClose = onClose)
                    HorizontalDivider()
                }
            }
            item(key = "sequence_status") {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .padding(all = 16.dp)
                        .groupedCard(index = 0, count = 1)
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeSequence != null) {
                        Text(
                            modifier = Modifier
                                .weight(
                                    weight = 1f
                                ).testTag(tag = "sequence_position"),
                            text = "Step ${activeSequence.currentIndex + 1} of ${activeSequence.responses.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            modifier = Modifier.testTag(tag = "sequence_reset_button"),
                            onClick = onResetSequencePosition
                        ) { Text(text = "Reset") }
                    }
                    TextButton(
                        modifier = Modifier.testTag(tag = "sequence_build_button"),
                        onClick = onOpenSequence
                    ) { Text(text = if (activeSequence == null) "Build a Sequence" else "Edit") }
                }
            }
            stickyHeader(key = "header_network") { PickerGroupHeader(text = "NO MOCK") }
            item(key = "network_item") {
                NetworkItem(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp)
                        .groupedCard(index = 0, count = 1)
                        .padding(horizontal = 16.dp)
                        .testTag(tag = "operation_sheet_network_item"),
                    selected = endpoint.currentState == OperationMockState.Network,
                    onClick = { onSelectResponse(null) }
                )
            }
            groupedResponses.forEach { (family, responses) ->
                stickyHeader(key = "header_${family.name.lowercase()}") {
                    PickerGroupHeader(text = "${family.displayName} mocks".uppercase())
                }
                responses.forEachIndexed { index, response ->
                    val itemKey = "mock_item_${response.statusCode}_${response.mediaType}_${response.exampleName}"
                    item(key = itemKey) {
                        Column(
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp)
                                .groupedCard(index = index, count = responses.size)
                        ) {
                            MockItem(
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    ).testTag(tag = itemKey),
                                mockResponse = response,
                                secondaryLabel = response.mediaType.takeIf {
                                    hasMultipleMediaTypes &&
                                        it.isNotBlank()
                                },
                                selected = selectedState?.let {
                                    it.statusCode == response.statusCode &&
                                        it.mediaType == response.mediaType &&
                                        it.exampleName == response.exampleName
                                } == true,
                                onClick = { onSelectResponse(response) },
                                onPreview = { onPreview(response) },
                                previewToggleTestTag = "${itemKey}_preview_toggle"
                            )
                            if (index != responses.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 64.dp, end = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
            item(key = "response_hint") {
                Text(
                    modifier = Modifier.animateItem().padding(horizontal = 32.dp, vertical = 8.dp),
                    text = "Tap a response to apply it, or the eye to preview and compare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            stickyHeader(
                key = "header_simulate_failure"
            ) { PickerGroupHeader(text = "SIMULATE FAILURE") }
            FailureKind.entries.forEachIndexed { index, kind ->
                item(key = "failure_item_${kind.name}") {
                    Column(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp)
                            .groupedCard(index = index, count = FailureKind.entries.size)
                    ) {
                        FailureItem(
                            modifier = Modifier
                                .padding(
                                    horizontal = 16.dp
                                ).testTag(tag = "failure_item_${kind.name}"),
                            kind = kind,
                            selected = selectedFailureKind == kind,
                            onClick = { onSelectFailure(kind) }
                        )
                        if (index != FailureKind.entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp, end = 16.dp)
                            )
                        }
                    }
                }
            }
            if (failureRate != null) {
                item(key = "failure_rate") {
                    Text(
                        modifier = Modifier
                            .animateItem()
                            .padding(
                                horizontal = 32.dp,
                                vertical = 8.dp
                            ).testTag(tag = "operation_sheet_failure_rate"),
                        text = "Also configured to fail ${(failureRate * 100).toInt()}% " +
                            "of mocked requests on its own (x-devview.failureRate)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
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
                verticalAlignment = Alignment.Top
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
                    text = endpoint.descriptor.config.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        IconButton(
            modifier = Modifier.testTag(tag = "operation_sheet_close_button"),
            onClick = onClose
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun PickerGroupHeader(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ).padding(horizontal = 32.dp, vertical = 8.dp),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Modifier.groupedCard(index: Int, count: Int): Modifier {
    val shape = RoundedCornerShape(
        topStart = if (index == 0) 12.dp else 0.dp,
        topEnd = if (index == 0) 12.dp else 0.dp,
        bottomStart = if (index == count - 1) 12.dp else 0.dp,
        bottomEnd = if (index == count - 1) 12.dp else 0.dp
    )
    return clip(shape = shape).background(color = MaterialTheme.colorScheme.surfaceContainerHighest)
}

@Preview(locale = "en")
@Composable
private fun NetworkMockOperationSheetPickerPreview(
    @PreviewParameter(OperationUiModelPreviewParameterProvider::class) endpoint: OperationUiModel
) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            OperationPickerPage(
                content = OperationSheetState.Content(
                    operationUiModel = endpoint,
                    responses = MockResponse.fake(amount = 6).toPersistentList()
                ),
                onSelectResponse = {},
                onSelectFailure = {},
                onResetSequencePosition = {},
                onPreview = {},
                onOpenSequence = {},
                onClose = {}
            )
        }
    }
}
