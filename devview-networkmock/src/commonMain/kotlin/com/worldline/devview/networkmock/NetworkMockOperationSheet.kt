package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
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
import com.worldline.devview.networkmock.viewmodel.MutationOutcome
import com.worldline.devview.networkmock.viewmodel.MutationTicket
import com.worldline.devview.networkmock.viewmodel.NetworkMockMutationCoordinator
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import com.worldline.devview.ui.DevViewGroupedDivider
import com.worldline.devview.ui.DevViewSectionHeader
import com.worldline.devview.ui.devViewGroupedItem
import kotlinx.coroutines.launch

/** Quick choices stay in the sheet; inspection and sequence editing are separate destinations. */
@Composable
internal fun NetworkMockOperationSheet(
    sheetState: OperationSheetState,
    mutations: NetworkMockMutationCoordinator,
    selection: MutationTicket?,
    onSelectionComplete: (Long) -> Unit,
    onTransitionChange: (Boolean) -> Unit,
    canCompleteSelection: (Long) -> Boolean,
    onDismissRequest: () -> Unit,
    onSelectResponse: (MockResponse?) -> Unit,
    onSelectFailure: (FailureKind) -> Unit,
    onOpenResponse: (MockResponse) -> Unit,
    onBuildSequence: () -> Unit,
    onResetSequencePosition: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mutationState by mutations.state.collectAsState()
    val outcome = selection?.outcome?.collectAsState()?.value
    var transitioning by remember { mutableStateOf(value = false) }
    val sheet = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || mutations.interactionsEnabled }
    )
    val scope = rememberCoroutineScope()
    val currentDismiss by rememberUpdatedState(newValue = onDismissRequest)
    val currentComplete by rememberUpdatedState(newValue = onSelectionComplete)
    val currentCanComplete by rememberUpdatedState(newValue = canCompleteSelection)
    val currentTransition by rememberUpdatedState(newValue = onTransitionChange)
    DisposableEffect(key1 = Unit) { onDispose { currentTransition(false) } }

    fun setTransition(inProgress: Boolean) {
        transitioning = inProgress
        currentTransition(inProgress)
    }
    val enabled = mutations.interactionsEnabled && !transitioning

    fun closeThen(action: () -> Unit) {
        if (!mutations.interactionsEnabled || transitioning) return
        setTransition(inProgress = true)
        scope.launch {
            try {
                sheet.hide()
                if (!sheet.isVisible && mutations.interactionsEnabled) {
                    currentDismiss()
                    setTransition(inProgress = false)
                    action()
                }
            } finally {
                setTransition(inProgress = false)
            }
        }
    }
    LaunchedEffect(key1 = selection?.id, key2 = outcome, key3 = mutationState.busy) {
        val completed = selection ?: return@LaunchedEffect
        if (outcome != MutationOutcome.Succeeded || mutationState.busy) return@LaunchedEffect
        if (!currentCanComplete(completed.id)) return@LaunchedEffect
        setTransition(inProgress = true)
        try {
            sheet.hide()
            if (!sheet.isVisible && currentCanComplete(completed.id)) {
                currentComplete(completed.id)
            }
        } finally {
            setTransition(inProgress = false)
        }
    }
    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheet,
        sheetGesturesEnabled = enabled,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = enabled,
            shouldDismissOnClickOutside = enabled
        ),
        onDismissRequest = {
            if (mutations.interactionsEnabled &&
                !transitioning
            ) {
                currentDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth().testTag(tag = "operation_sheet_content")) {
            MutationStatus(
                state = mutationState,
                onRetry = mutations::retryFailed,
                onCancel = mutations::cancelFailed
            )
            when (sheetState) {
                OperationSheetState.Hidden -> Unit
                OperationSheetState.Loading -> LoadingState(
                    modifier = Modifier.padding(all = 24.dp)
                )
                is OperationSheetState.Error -> {
                    ErrorState(
                        message = sheetState.message,
                        modifier = Modifier.padding(all = 24.dp)
                    )
                    Row {
                        TextButton(
                            enabled = enabled,
                            onClick = onReload
                        ) { Text(text = "Retry loading") }
                        TextButton(
                            enabled = enabled,
                            onClick = { closeThen {} }
                        ) { Text(text = "Close") }
                    }
                }
                is OperationSheetState.Content -> {
                    OperationPickerHeader(
                        endpoint = sheetState.operationUiModel,
                        enabled = enabled,
                        onClose = { closeThen {} }
                    )
                    OperationPickerPage(
                        modifier = Modifier.weight(weight = 1f, fill = false),
                        content = sheetState,
                        enabled = enabled,
                        onSelectResponse = {
                            if (mutations.interactionsEnabled && !transitioning) {
                                onSelectResponse(
                                    it
                                )
                            }
                        },
                        onSelectFailure = {
                            if (mutations.interactionsEnabled && !transitioning) onSelectFailure(it)
                        },
                        onPreview = { response -> closeThen { onOpenResponse(response) } },
                        onBuildSequence = { closeThen(action = onBuildSequence) },
                        onResetSequencePosition = {
                            if (mutations.interactionsEnabled &&
                                !transitioning
                            ) {
                                onResetSequencePosition()
                            }
                        }
                    )
                }
            }
        }
    }
}

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
    val grouped = content.responses.groupBy {
        StatusCodeFamily.fromStatusCode(
            statusCode = it.statusCode
        )
    }
    val showMediaTypes = content.responses
        .map { it.mediaType }
        .distinct()
        .size > 1
    LazyColumn(
        modifier = modifier.fillMaxWidth().selectableGroup().testTag(tag = "operation_picker_list")
    ) {
        item(key = "header_network") { DevViewSectionHeader(text = "No mock") }
        item(key = "network_item") {
            NetworkItem(
                modifier = Modifier
                    .padding(
                        horizontal = 16.dp
                    ).devViewGroupedItem(index = 0, count = 1)
                    .testTag(tag = "operation_sheet_network_item"),
                selected = current == OperationMockState.Network,
                enabled = enabled,
                onClick = { onSelectResponse(null) }
            )
        }
        grouped.forEach { (family, responses) ->
            item(
                key = "header_${family.name}"
            ) { DevViewSectionHeader(text = "${family.displayName} mocks") }
            responses.forEachIndexed { index, response ->
                val itemKey = "mock_item_${response.statusCode}_${response.mediaType}_${response.exampleName}"
                item(key = itemKey) {
                    Column(
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp
                            ).devViewGroupedItem(index = index, count = responses.size)
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
                        if (index != responses.lastIndex) {
                            DevViewGroupedDivider(
                                modifier = Modifier.padding(start = 64.dp)
                            )
                        }
                    }
                }
            }
        }
        item(key = "header_failure") { DevViewSectionHeader(text = "Simulate failure") }
        FailureKind.entries.forEachIndexed { index, kind ->
            item(key = "failure_item_${kind.name}") {
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        ).devViewGroupedItem(index = index, count = FailureKind.entries.size)
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
        val failureRate = content.operationUiModel.descriptor.config.failureRate
        if (failureRate != null) {
            item(key = "failure_rate") {
                Text(
                    text =
                        "The spec also applies a ${(failureRate * 100).toInt()}% random failure rate " +
                            "to mock responses.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        item(key = "sequence_footer") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TextButton(
                    enabled = enabled,
                    onClick = onBuildSequence,
                    modifier = Modifier.testTag(tag = "build_sequence")
                ) {
                    Text(
                        text = if (current is OperationMockState.Sequence) "Edit sequence" else "Build sequence…"
                    )
                }
                if (current is OperationMockState.Sequence) {
                    TextButton(
                        enabled = enabled,
                        onClick = onResetSequencePosition,
                        modifier = Modifier.testTag(tag = "reset_sequence_position")
                    ) { Text(text = "Reset position") }
                }
            }
        }
    }
}

@Composable
private fun OperationPickerHeader(
    endpoint: OperationUiModel,
    enabled: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = endpoint.descriptor.config.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${endpoint.descriptor.config.method.value} ${endpoint.descriptor.config.path}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Current: ${endpoint.currentState.displayName}",
                modifier = Modifier.testTag(tag = "operation_sheet_current_state"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            enabled = enabled,
            onClick = onClose,
            modifier = Modifier.testTag(tag = "operation_sheet_close_button")
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close operation")
        }
    }
}
