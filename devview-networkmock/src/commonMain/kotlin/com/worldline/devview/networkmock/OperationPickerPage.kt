package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.components.EndpointStateChip
import com.worldline.devview.networkmock.components.FailureItem
import com.worldline.devview.networkmock.components.MockItem
import com.worldline.devview.networkmock.components.NetworkItem
import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.model.StatusCodeFamily
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import com.worldline.devview.ui.DevViewEmptyState
import com.worldline.devview.ui.DevViewGroupedDivider
import com.worldline.devview.ui.DevViewSectionHeader
import com.worldline.devview.ui.devViewGroupedItem

/** Browsing never changes configuration; selecting a row saves it and closes the sheet. */
@Composable
internal fun OperationPickerPage(
    content: OperationSheetState.Content,
    onSelectResponse: (MockResponse?) -> Unit,
    onPreview: (MockResponse) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectFailure: (FailureKind) -> Unit = {},
    onBuildSequence: () -> Unit = {},
    onResetSequencePosition: () -> Unit = {}
) {
    val current = content.operationUiModel.currentState
    val operationKey = content.operationUiModel.descriptor.key
    var failuresTab by rememberSaveable(operationKey) { mutableStateOf(value = false) }
    var query by rememberSaveable(operationKey) { mutableStateOf(value = "") }
    val responses = remember(key1 = content.responses, key2 = query) {
        filterPickerResponses(responses = content.responses, query = query)
    }
    val keyboard = LocalSoftwareKeyboardController.current
    var controlsHeight by remember(key1 = operationKey) { mutableIntStateOf(value = 0) }
    val density = LocalDensity.current
    val searchViewport = remember(key1 = operationKey) { BringIntoViewRequester() }
    var searchFocused by remember(key1 = operationKey) { mutableStateOf(value = false) }
    var searchSize by remember(key1 = operationKey) { mutableStateOf(value = IntSize.Zero) }
    var searchTop by remember(key1 = operationKey) { mutableFloatStateOf(value = 0f) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Pin controls only when their measured height leaves room for two choices.
        val compact = controlsHeight == 0 || with(
            receiver = density
        ) { controlsHeight.toDp() } + 160.dp > maxHeight
        LaunchedEffect(
            searchFocused,
            compact,
            maxHeight,
            query,
            failuresTab,
            searchSize,
            searchTop,
            controlsHeight
        ) {
            if (searchSize == IntSize.Zero) return@LaunchedEffect
            if (compact && searchFocused && !failuresTab) {
                withFrameNanos { }
                val bottom = controlsHeight - searchTop + with(receiver = density) { 240.dp.toPx() }
                // Include intervening notes; prioritize results if search and choices cannot both fit.
                searchViewport.bringIntoView(
                    rect = Rect(
                        left = 0f,
                        top = (
                            bottom - with(
                                receiver = density
                            ) { maxHeight.toPx() }
                            ).coerceAtLeast(minimumValue = 0f),
                        right = searchSize.width.toFloat(),
                        bottom = bottom
                    )
                )
            }
        }
        Column(
            modifier = if (compact) {
                Modifier.verticalScroll(
                    state = rememberScrollState()
                )
            } else {
                Modifier
            }
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .onSizeChanged { controlsHeight = it.height }
            ) {
                ConfiguredBehaviour(
                    current = current,
                    showMediaType = content.responses
                        .map { it.mediaType }
                        .distinct()
                        .size > 1,
                    enabled = enabled,
                    onBuildSequence = onBuildSequence,
                    onResetSequencePosition = onResetSequencePosition
                )
                HorizontalDivider()
                NetworkItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .devViewGroupedItem(index = 0, count = 1)
                        .testTag(tag = "operation_sheet_network_item"),
                    selected = current == OperationMockState.Network,
                    enabled = enabled,
                    onClick = { onSelectResponse(null) }
                )
                Text(
                    text = "Selecting a choice saves it and closes this sheet. Details only inspects a response.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PrimaryTabRow(selectedTabIndex = if (failuresTab) 1 else 0) {
                    Tab(
                        selected = !failuresTab,
                        enabled = enabled,
                        onClick = { failuresTab = false },
                        modifier = Modifier.testTag(tag = "operation_responses_tab"),
                        text = { Text(text = "Responses (${content.responses.size})") }
                    )
                    Tab(
                        selected = failuresTab,
                        enabled = enabled,
                        onClick = {
                            failuresTab = true
                            keyboard?.hide()
                        },
                        modifier = Modifier.testTag(tag = "operation_failures_tab"),
                        text = { Text(text = "Failures") }
                    )
                }
                if (!failuresTab) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .bringIntoViewRequester(bringIntoViewRequester = searchViewport)
                            .onGloballyPositioned {
                                searchSize = it.size
                                searchTop = it.positionInParent().y
                            }.onFocusChanged { searchFocused = it.isFocused }
                            .testTag(tag = "operation_response_search"),
                        label = { Text(text = "Search responses") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(enabled = enabled, onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear response search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() })
                    )
                }
                val failureRate = content.operationUiModel.descriptor.config.failureRate
                if (failureRate != null) {
                    Text(
                        text =
                            "The spec applies a ${(failureRate * 100).toInt()}% random failure rate " +
                                "to mock responses, including sequences. " +
                                "It does not affect actual network or explicit failures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            OperationPickerChoices(
                modifier = if (compact) {
                    Modifier.heightIn(
                        max = 240.dp
                    )
                } else {
                    Modifier.weight(weight = 1f, fill = false)
                },
                content = content,
                responses = responses,
                failuresTab = failuresTab,
                enabled = enabled,
                onSelectResponse = onSelectResponse,
                onSelectFailure = onSelectFailure,
                onPreview = onPreview,
                onClearSearch = { query = "" }
            )
        }
    }
}

internal fun filterPickerResponses(
    responses: List<MockResponse>,
    query: String
): List<MockResponse> {
    val search = query.trim()
    return if (search.isEmpty()) {
        responses
    } else {
        responses.filter { response ->
            response.statusCode.toString().contains(other = search) ||
                response.displayName.contains(other = search, ignoreCase = true) ||
                response.exampleName.contains(other = search, ignoreCase = true) ||
                response.mediaType.contains(other = search, ignoreCase = true)
        }
    }
}

@Composable
private fun ConfiguredBehaviour(
    current: OperationMockState,
    showMediaType: Boolean,
    enabled: Boolean,
    onBuildSequence: () -> Unit,
    onResetSequencePosition: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(tag = "operation_sheet_current_state"),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (current) {
                    OperationMockState.Network -> "Actual network"
                    is OperationMockState.Mock -> "Single response"
                    is OperationMockState.Sequence -> if (current.currentResponse == null) {
                        "Sequence · unavailable"
                    } else {
                        "Sequence · next ${current.currentIndex + 1}/${current.responses.size}"
                    }
                    is OperationMockState.Failure -> "Simulated failure"
                },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() }
            )
            Row {
                TextButton(
                    enabled = enabled,
                    onClick = onBuildSequence,
                    modifier = Modifier
                        .heightIn(
                            min = 48.dp
                        ).testTag(tag = "build_sequence")
                        .semantics {
                            contentDescription = if (current is OperationMockState.Sequence) {
                                "Edit sequence"
                            } else {
                                "Build sequence"
                            }
                        }
                ) {
                    Text(
                        text = if (current is OperationMockState.Sequence) "Edit" else "Build sequence"
                    )
                }
                if (current is OperationMockState.Sequence) {
                    TextButton(
                        enabled = enabled,
                        onClick = onResetSequencePosition,
                        modifier = Modifier
                            .heightIn(
                                min = 48.dp
                            ).testTag(tag = "reset_sequence_position")
                            .semantics {
                                contentDescription = "Restart sequence"
                            }
                    ) { Text(text = "Restart") }
                }
            }
        }
        val response = when (current) {
            is OperationMockState.Mock, is OperationMockState.Failure -> current
            is OperationMockState.Sequence -> current.currentResponse
            OperationMockState.Network -> null
        }
        if (response != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EndpointStateChip(
                    endpointMockState = response,
                    label = when (response) {
                        is OperationMockState.Mock -> response.statusCode.toString()
                        is OperationMockState.Failure -> "Failure"
                        else -> response.displayName
                    }
                )
                Column(modifier = Modifier.weight(weight = 1f)) {
                    Text(text = response.displayName, style = MaterialTheme.typography.bodyMedium)
                    if (showMediaType && response is OperationMockState.Mock) {
                        Text(text = response.mediaType, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (current is OperationMockState.Sequence) {
            Text(
                text = if (current.currentResponse == null) {
                    "No available next response. Edit this sequence."
                } else {
                    "Last step repeats"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OperationPickerChoices(
    content: OperationSheetState.Content,
    responses: List<MockResponse>,
    failuresTab: Boolean,
    enabled: Boolean,
    onSelectResponse: (MockResponse?) -> Unit,
    onSelectFailure: (FailureKind) -> Unit,
    onPreview: (MockResponse) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = content.operationUiModel.currentState
    val grouped = responses.groupBy { StatusCodeFamily.fromStatusCode(statusCode = it.statusCode) }
    val showMediaTypes = content.responses
        .map { it.mediaType }
        .distinct()
        .size > 1
    LazyColumn(
        modifier = modifier.fillMaxWidth().selectableGroup().testTag(tag = "operation_picker_list")
    ) {
        if (failuresTab) {
            item(key = "header_failure") { DevViewSectionHeader(text = "Simulate failure") }
            FailureKind.entries.forEachIndexed { index, kind ->
                item(key = "failure_item_${kind.name}") {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .devViewGroupedItem(index = index, count = FailureKind.entries.size)
                    ) {
                        FailureItem(
                            modifier = Modifier.testTag(tag = "failure_item_${kind.name}"),
                            kind = kind,
                            selected = (current as? OperationMockState.Failure)?.kind == kind,
                            enabled = enabled,
                            onClick = { onSelectFailure(kind) }
                        )
                        if (index != FailureKind.entries.lastIndex) {
                            DevViewGroupedDivider(modifier = Modifier.padding(start = 64.dp))
                        }
                    }
                }
            }
        } else if (responses.isEmpty()) {
            item(key = "empty_responses") {
                DevViewEmptyState(
                    icon = Icons.Rounded.Search,
                    title = if (content.responses.isEmpty()) {
                        "No responses in this specification"
                    } else {
                        "No matching responses"
                    },
                    message = if (content.responses.isEmpty()) {
                        "Use actual network or choose a simulated failure."
                    } else {
                        "Try a status code, response name, example or media type."
                    },
                    action = if (content.responses.isEmpty()) {
                        null
                    } else {
                        {
                            TextButton(
                                enabled = enabled,
                                onClick = onClearSearch
                            ) { Text(text = "Clear search") }
                        }
                    }
                )
            }
        } else {
            grouped.forEach { (family, familyResponses) ->
                item(
                    key = "header_${family.name}"
                ) { DevViewSectionHeader(text = "${family.displayName} mocks") }
                familyResponses.forEachIndexed { index, response ->
                    val itemKey = "mock_item_${response.statusCode}_${response.mediaType}_${response.exampleName}"
                    item(key = itemKey) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .devViewGroupedItem(index = index, count = familyResponses.size)
                        ) {
                            MockItem(
                                modifier = Modifier.testTag(tag = itemKey),
                                mockResponse = response,
                                secondaryLabel = response.mediaType.takeIf { showMediaTypes },
                                selected = (current as? OperationMockState.Mock)?.let {
                                    it.statusCode == response.statusCode &&
                                        it.mediaType == response.mediaType &&
                                        it.exampleName == response.exampleName
                                } == true,
                                enabled = enabled,
                                onClick = { onSelectResponse(response) },
                                onPreview = { onPreview(response) },
                                previewToggleTestTag = "${itemKey}_preview_toggle"
                            )
                            if (index != familyResponses.lastIndex) {
                                DevViewGroupedDivider(modifier = Modifier.padding(start = 64.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
