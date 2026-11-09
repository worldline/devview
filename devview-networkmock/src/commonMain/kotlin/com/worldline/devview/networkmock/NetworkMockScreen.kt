package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldline.devview.networkmock.components.EmptyState
import com.worldline.devview.networkmock.components.EndpointCard
import com.worldline.devview.networkmock.components.ErrorState
import com.worldline.devview.networkmock.components.GlobalMockToggle
import com.worldline.devview.networkmock.components.LoadingState
import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.ApiSpecUiModel
import com.worldline.devview.networkmock.preview.NetworkMockUiStatePreviewParameterProvider
import com.worldline.devview.networkmock.viewmodel.NetworkMockSessionViewModel
import com.worldline.devview.networkmock.viewmodel.NetworkMockUiState
import com.worldline.devview.networkmock.viewmodel.NetworkMockViewModel
import com.worldline.devview.networkmock.viewmodel.OperationSheetState
import com.worldline.devview.ui.DevViewEmptyState
import com.worldline.devview.ui.DevViewFilterBar
import com.worldline.devview.ui.DevViewSectionHeader
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Main screen for the Network Mock module.
 *
 * Displays all configured API endpoints with controls to:
 * - Toggle global mocking on/off
 * - Enable/disable individual endpoint mocks
 * - Select which mock response to return for each endpoint, via [NetworkMockOperationSheet]
 * - Reset all mocks to use actual network
 * - Reload the configured OpenAPI specs from disk, picking up edits without an app restart
 *
 * @param session Retained module session owning accepted writes and toolbar reload requests.
 * @param viewModel Entry-owned list, filters and quick-choice sheet state.
 * @param onOpenResponse Opens registered response details after the picker has closed.
 * @param onBuildSequence Opens the registered sequence builder after the picker has closed.
 * @param modifier Optional modifier for the screen.
 * @param bottomPadding Bottom inset padding provided by the DevView [androidx.compose.material3.Scaffold].
 *   Applied to the operation list so the last item is not obscured by system navigation bars.
 */
@Composable
internal fun NetworkMockScreen(
    session: NetworkMockSessionViewModel,
    viewModel: NetworkMockViewModel,
    onOpenResponse: (OperationKey, MockResponse) -> Unit,
    onBuildSequence: (OperationKey) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val mutation by session.mutations.state.collectAsState()
    val selection by viewModel.selection.collectAsState()

    LaunchedEffect(key1 = session, key2 = viewModel) {
        combine(
            flow = session.reloadGeneration,
            flow2 = session.mutations.state
        ) { generation, mutation ->
            generation to mutation.busy
        }.collect { (generation, busy) ->
            if (!busy) viewModel.consumeReload(generation = generation)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (sheetState == OperationSheetState.Hidden) {
            MutationStatus(
                state = mutation,
                onRetry = session.mutations::retryFailed,
                onCancel = session.mutations::cancelFailed
            )
        }
        NetworkMockScreenContent(
            uiState = uiState,
            onGlobalToggle = {
                if (session.mutations.interactionsEnabled) {
                    viewModel
                        .setGlobalMockingEnabled(
                            enabled = it
                        )
                }
            },
            onSelectOperation = {
                if (session.mutations.interactionsEnabled) {
                    viewModel
                        .openOperation(
                            key = it
                        )
                }
            },
            listState = listState,
            onSpecSelect = viewModel::selectSpec,
            onFiltersExpandedChange = viewModel::setFiltersExpanded,
            onFiltersChange = viewModel::updateFilters,
            interactionsEnabled = !mutation.busy,
            modifier = Modifier.weight(weight = 1f),
            bottomPadding = bottomPadding
        )
    }

    if (sheetState != OperationSheetState.Hidden) {
        val openKey = (sheetState as? OperationSheetState.Content)
            ?.operationUiModel
            ?.descriptor
            ?.key
        NetworkMockOperationSheet(
            sheetState = sheetState,
            mutations = session.mutations,
            selection = selection,
            onSelectionComplete = { viewModel.acknowledgeSelection(ticketId = it) },
            onTransitionChange = { session.sheetTransitionInProgress = it },
            canCompleteSelection = viewModel::canAcknowledgeSelection,
            onDismissRequest = viewModel::closeSheet,
            onSelectResponse = { response ->
                if (openKey != null) {
                    viewModel.setOperationMockState(key = openKey, response = response)
                }
            },
            onSelectFailure = { kind ->
                if (openKey != null) {
                    viewModel.setOperationFailureState(key = openKey, kind = kind)
                }
            },
            onOpenResponse = { response -> if (openKey != null) onOpenResponse(openKey, response) },
            onBuildSequence = { if (openKey != null) onBuildSequence(openKey) },
            onReload = viewModel::reloadOpenOperation,
            onResetSequencePosition = {
                if (openKey != null) {
                    viewModel.resetOperationSequencePosition(key = openKey)
                }
            }
        )
    }
}

@Composable
internal fun NetworkMockScreenContent(
    uiState: NetworkMockUiState,
    onGlobalToggle: (Boolean) -> Unit,
    onSelectOperation: (OperationKey) -> Unit,
    listState: NetworkMockListState,
    onSpecSelect: (String) -> Unit,
    onFiltersExpandedChange: (Boolean) -> Unit,
    onFiltersChange: (String, OperationFilters) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    interactionsEnabled: Boolean = true
) {
    when (uiState) {
        is NetworkMockUiState.Loading -> LoadingState(modifier = modifier)
        is NetworkMockUiState.Error -> ErrorState(message = uiState.message, modifier = modifier)
        is NetworkMockUiState.Empty -> EmptyState(modifier = modifier)
        is NetworkMockUiState.Content -> ContentState(
            uiState = uiState,
            listState = listState,
            onSpecSelect = onSpecSelect,
            onFiltersExpandedChange = onFiltersExpandedChange,
            onFiltersChange = onFiltersChange,
            onGlobalToggle = onGlobalToggle,
            onSelectOperation = onSelectOperation,
            interactionsEnabled = interactionsEnabled,
            modifier = modifier,
            bottomPadding = bottomPadding
        )
    }
}

@Composable
private fun ContentState(
    uiState: NetworkMockUiState.Content,
    listState: NetworkMockListState,
    onSpecSelect: (String) -> Unit,
    onFiltersExpandedChange: (Boolean) -> Unit,
    onFiltersChange: (String, OperationFilters) -> Unit,
    onGlobalToggle: (Boolean) -> Unit,
    onSelectOperation: (OperationKey) -> Unit,
    interactionsEnabled: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (uiState.specs.isEmpty()) {
        EmptyState(modifier = modifier)
        return
    }
    val initialPage = uiState.specs
        .indexOfFirst { it.specId == listState.selectedSpecId }
        .coerceAtLeast(
            minimumValue = 0
        )
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { uiState.specs.size }
    )
    val scope = rememberCoroutineScope()
    val latestListState by rememberUpdatedState(newValue = listState)
    val latestOnSpecSelect by rememberUpdatedState(newValue = onSpecSelect)
    LaunchedEffect(key1 = uiState.specs.map { it.specId }) {
        val restoredPage = uiState.specs
            .indexOfFirst { it.specId == latestListState.selectedSpecId }
            .coerceAtLeast(minimumValue = 0)
        pagerState.scrollToPage(page = restoredPage)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val specId = uiState.specs.getOrNull(index = page)?.specId ?: return@collect
            latestOnSpecSelect(specId)
        }
    }
    val currentPage = pagerState.currentPage.coerceIn(
        minimumValue = 0,
        maximumValue = uiState.specs.lastIndex
    )
    val currentSpec = uiState.specs[currentPage]
    val currentFilters = listState.filters[currentSpec.specId] ?: OperationFilters()
    val totalOperations = uiState.specs.sumOf { it.operations.size }
    val mockedOperations = uiState.specs.sumOf { spec ->
        spec.operations.count { it.currentState !is OperationMockState.Network }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        bottomBar = {
            Surface {
                DevViewFilterBar(
                    query = currentFilters.query,
                    onQueryChange = {
                        onFiltersChange(
                            currentSpec.specId,
                            currentFilters.copy(query = it)
                        )
                    },
                    placeholder = "Search operations",
                    activeFilterCount = currentFilters.activeCount,
                    expanded = listState.expanded,
                    onExpandedChange = onFiltersExpandedChange,
                    onClearFilters = {
                        onFiltersChange(
                            currentSpec.specId,
                            OperationFilters(query = currentFilters.query)
                        )
                    },
                    bottomPadding = bottomPadding
                ) {
                    OperationFilterPanel(
                        spec = currentSpec,
                        filters = currentFilters,
                        onFiltersChange = { onFiltersChange(currentSpec.specId, it) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues = paddingValues)) {
            GlobalMockToggle(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = uiState.globalMockingEnabled,
                mockedCount = mockedOperations,
                totalCount = totalOperations,
                onToggle = onGlobalToggle,
                interactionsEnabled = interactionsEnabled
            )
            HorizontalDivider()
            PrimaryScrollableTabRow(selectedTabIndex = currentPage, edgePadding = 0.dp) {
                uiState.specs.forEachIndexed { index, spec ->
                    Tab(
                        modifier = Modifier.testTag(tag = "spec_tab_" + spec.specId),
                        selected = currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(page = index) } },
                        text = { Text(text = spec.name) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(weight = 1f),
                verticalAlignment = Alignment.Top,
                key = { uiState.specs[it].specId }
            ) { pageIndex ->
                val spec = uiState.specs.getOrNull(index = pageIndex) ?: return@HorizontalPager
                val filters = listState.filters[spec.specId] ?: OperationFilters()
                val operations = remember(key1 = spec.operations, key2 = filters) {
                    filterOperations(operations = spec.operations, filters = filters)
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (operations.isEmpty()) {
                        item(key = "empty_filter_state") {
                            DevViewEmptyState(
                                icon = Icons.Rounded.Search,
                                title = if (spec.operations.isEmpty()) {
                                    "No operations in this API group"
                                } else {
                                    "No matching operations"
                                },
                                message = if (spec.operations.isEmpty()) {
                                    "Check the specification and reload specs."
                                } else {
                                    "Try another search or clear search and filters."
                                },
                                modifier = Modifier.fillMaxWidth().testTag(
                                    tag = "networkmock_empty_filter_message"
                                ),
                                action = if (spec.operations.isEmpty()) {
                                    null
                                } else {
                                    {
                                        TextButton(
                                            onClick = {
                                                onFiltersChange(
                                                    spec.specId,
                                                    OperationFilters()
                                                )
                                            }
                                        ) {
                                            Text(text = "Clear search and filters")
                                        }
                                    }
                                }
                            )
                        }
                    }
                    itemsIndexed(
                        items = operations,
                        key = { _, operation -> operation.descriptor.key.compositeKey }
                    ) { index, operation ->
                        Column(modifier = Modifier.animateItem()) {
                            EndpointCard(
                                modifier = Modifier.testTag(
                                    tag =
                                        "endpoint_card_" + operation.descriptor.specId + "_" +
                                            operation.descriptor.operationId
                                ),
                                endpoint = operation,
                                enabled = interactionsEnabled,
                                openEndpointDetails = {
                                    onSelectOperation(
                                        operation.descriptor.key
                                    )
                                }
                            )
                            if (index != operations.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationFilterPanel(
    spec: ApiSpecUiModel,
    filters: OperationFilters,
    onFiltersChange: (OperationFilters) -> Unit
) {
    Column {
        DevViewSectionHeader(text = "Sort by")
        FilterOptions(
            options = OperationSort.entries,
            selected = { it == filters.sort },
            label = { it.label },
            testTag = { "sort_filter_" + it.name },
            onClick = { onFiltersChange(filters.copy(sort = it)) }
        )
        DevViewSectionHeader(text = "State")
        FilterOptions(
            options = MockStateFilter.entries,
            selected = { it in filters.states },
            label = { it.label },
            testTag = { "state_filter_chip_" + it.name },
            onClick = {
                onFiltersChange(
                    filters.copy(states = filters.states.toggled(value = it))
                )
            },
            modifier = Modifier.testTag(tag = "state_filter_row")
        )
        val versions = (
            spec.operations.mapNotNull { it.descriptor.config.version } + listOfNotNull(
                element = filters.version
            )
            ).distinct()
        if (versions.isNotEmpty()) {
            DevViewSectionHeader(text = "Version")
            FilterOptions(
                options = listOf<String?>(element = null) + versions,
                selected = { it == filters.version },
                label = { it ?: "All" },
                testTag = {
                    if (it ==
                        null
                    ) {
                        "version_filter_all_" + spec.specId
                    } else {
                        "version_filter_" + spec.specId + "_" +
                            it
                    }
                },
                onClick = { onFiltersChange(filters.copy(version = it)) },
                modifier = Modifier.testTag(tag = "version_filter_row_" + spec.specId)
            )
        }
        val methods = (spec.operations.map { it.descriptor.config.method.value } + filters.methods)
            .distinct()
            .sortedBy { method ->
                HttpMethod.DefaultMethods.indexOfFirst { it.value == method }.takeIf {
                    it >=
                        0
                }
                    ?: Int.MAX_VALUE
            }
        DevViewSectionHeader(text = "Method")
        FilterOptions(
            options = methods,
            selected = { it in filters.methods },
            label = { it },
            testTag = { "method_filter_" + spec.specId + "_" + it },
            onClick = {
                onFiltersChange(
                    filters.copy(methods = filters.methods.toggled(value = it))
                )
            },
            modifier = Modifier.testTag(tag = "method_filter_row_" + spec.specId)
        )
        val tags = (spec.operations.flatMap { it.descriptor.config.tags } + filters.tags)
            .distinct()
            .sorted()
        if (tags.isNotEmpty()) {
            DevViewSectionHeader(text = "Tag")
            FilterOptions(
                options = tags,
                selected = { it in filters.tags },
                label = { it },
                testTag = { "tag_filter_" + spec.specId + "_" + it },
                onClick = {
                    onFiltersChange(
                        filters.copy(tags = filters.tags.toggled(value = it))
                    )
                },
                modifier = Modifier.testTag(tag = "tag_filter_row_" + spec.specId)
            )
        }
    }
}

@Composable
private fun <T> FilterOptions(
    options: List<T>,
    selected: (T) -> Boolean,
    label: (T) -> String,
    testTag: (T) -> String,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(items = options) { option ->
            val isSelected = selected(option)
            FilterChip(
                modifier = Modifier.testTag(tag = testTag(option)),
                selected = isSelected,
                onClick = { onClick(option) },
                label = { Text(text = label(option)) },
                leadingIcon = if (isSelected) {
                    { Icon(imageVector = Icons.Rounded.Done, contentDescription = null) }
                } else {
                    null
                }
            )
        }
    }
}

private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value

@Preview(locale = "en")
@Composable
private fun NetworkMockScreenPreview(
    @PreviewParameter(NetworkMockUiStatePreviewParameterProvider::class) uiState: NetworkMockUiState
) {
    var listState by remember { mutableStateOf(value = NetworkMockListState()) }
    MaterialTheme {
        NetworkMockScreenContent(
            uiState = uiState,
            onGlobalToggle = {},
            onSelectOperation = {},
            listState = listState,
            onSpecSelect = { listState = listState.copy(selectedSpecId = it) },
            onFiltersExpandedChange = { listState = listState.copy(expanded = it) },
            onFiltersChange = { specId, filters ->
                listState = listState.copy(
                    filters = listState.filters + (specId to filters)
                )
            }
        )
    }
}
