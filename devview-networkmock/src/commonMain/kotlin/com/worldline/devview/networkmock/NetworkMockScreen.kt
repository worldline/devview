package com.worldline.devview.networkmock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.preview.NetworkMockUiStatePreviewParameterProvider
import com.worldline.devview.networkmock.viewmodel.NetworkMockUiState
import com.worldline.devview.networkmock.viewmodel.NetworkMockViewModel
import kotlinx.coroutines.flow.SharedFlow

/**
 * Main screen for the Network Mock module.
 *
 * Displays all configured API endpoints with controls to:
 * - Toggle global mocking on/off
 * - Enable/disable individual endpoint mocks
 * - Select which mock response to return for each endpoint
 * - Reset all mocks to use actual network
 *
 * @param resetToNetworkSharedFlow Shared flow emitted by [NetworkMock] when the user triggers
 *   the "Reset to Network" toolbar action. Collected here to call [NetworkMockViewModel.resetAllToNetwork].
 * @param navigateToEndpointScreen Callback invoked when the user taps an [EndpointCard],
 *   passing the corresponding [OperationKey] so the caller can push [NetworkMockDestination.Endpoint]
 *   onto the backstack.
 * @param viewModel The [NetworkMockViewModel] instance. Constructed and provided by
 *   [NetworkMock.registerContent] via the `viewModel { }` factory so that it is scoped to the
 *   navigation entry.
 * @param modifier Optional modifier for the screen.
 * @param bottomPadding Bottom inset padding provided by the DevView [androidx.compose.material3.Scaffold].
 *   Applied to the operation list so the last item is not obscured by system navigation bars.
 */
@Composable
public fun NetworkMockScreen(
    resetToNetworkSharedFlow: SharedFlow<Unit>,
    navigateToEndpointScreen: (OperationKey) -> Unit,
    viewModel: NetworkMockViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) {
        resetToNetworkSharedFlow.collect {
            viewModel.resetAllToNetwork()
        }
    }

    NetworkMockScreenContent(
        uiState = uiState,
        onGlobalToggle = viewModel::setGlobalMockingEnabled,
        navigateToEndpointScreen = navigateToEndpointScreen,
        modifier = modifier,
        bottomPadding = bottomPadding
    )
}

@Composable
internal fun NetworkMockScreenContent(
    uiState: NetworkMockUiState,
    onGlobalToggle: (Boolean) -> Unit,
    navigateToEndpointScreen: (OperationKey) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    when (uiState) {
        is NetworkMockUiState.Loading -> LoadingState(modifier = modifier)
        is NetworkMockUiState.Error -> ErrorState(message = uiState.message, modifier = modifier)
        is NetworkMockUiState.Empty -> EmptyState(modifier = modifier)
        is NetworkMockUiState.Content -> {
            ContentState(
                uiState = uiState,
                onGlobalToggle = onGlobalToggle,
                openEndpointDetails = navigateToEndpointScreen,
                modifier = modifier,
                bottomPadding = bottomPadding
            )
        }
    }
}

@Composable
private fun ContentState(
    uiState: NetworkMockUiState.Content,
    onGlobalToggle: (Boolean) -> Unit,
    openEndpointDetails: (OperationKey) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    var selectedTabIndex by remember { mutableIntStateOf(value = 0) }
    var searchQuery by remember { mutableStateOf(value = "") }
    var filtersExpanded by remember { mutableStateOf(value = false) }

    // Keyed by ApiSpec.id so each tab keeps its own selection independently of the others.
    val selectedVersions = remember { mutableStateMapOf<String, String>() }
    val selectedMethods = remember { mutableStateMapOf<String, Set<HttpMethod>>() }

    // Not keyed by spec — mocked-ness is a question about everything, not the current tab,
    // so unlike version/method the selection persists across tab switches.
    var selectedMockStates by remember { mutableStateOf(value = emptySet<MockStateFilter>()) }

    val iconRotation by animateFloatAsState(
        targetValue = if (filtersExpanded) 180f else 0f
    )

    val totalOperations by remember(key1 = uiState.specs) {
        derivedStateOf { uiState.specs.sumOf { it.operations.size } }
    }
    val mockedOperations by remember(key1 = uiState.specs) {
        derivedStateOf {
            uiState.specs.sumOf { spec ->
                spec.operations.count { it.currentState is OperationMockState.Mock }
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { uiState.specs.size })

    LaunchedEffect(key1 = selectedTabIndex) {
        pagerState.animateScrollToPage(page = selectedTabIndex)
    }

    LaunchedEffect(key1 = pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }

    val currentSpecId = uiState.specs.getOrNull(index = selectedTabIndex)?.specId
    val currentSpecOperations = uiState.specs.getOrNull(index = selectedTabIndex)?.operations
    val availableVersions = remember(key1 = currentSpecOperations) {
        currentSpecOperations.orEmpty().mapNotNull { it.descriptor.config.version }.distinct()
    }
    val availableMethods = remember(key1 = currentSpecOperations) {
        val distinctMethods = currentSpecOperations
            .orEmpty()
            .map { it.descriptor.config.method }
            .distinct()
        distinctMethods.sortedBy { method ->
            HttpMethod.DefaultMethods.indexOf(element = method).takeIf { it >= 0 } ?: Int.MAX_VALUE
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        bottomBar = {
            Surface(
                modifier = Modifier.padding(bottom = bottomPadding)
            ) {
                Column {
                    AnimatedVisibility(visible = filtersExpanded) {
                        Column {
                            HorizontalDivider()
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(tag = "state_filter_row"),
                                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                items(items = MockStateFilter.entries) { state ->
                                    val selected = state in selectedMockStates
                                    FilterChip(
                                        modifier = Modifier.testTag(
                                            tag = "state_filter_chip_${state.name}"
                                        ),
                                        selected = selected,
                                        onClick = {
                                            selectedMockStates = if (selected) {
                                                selectedMockStates - state
                                            } else {
                                                selectedMockStates + state
                                            }
                                        },
                                        label = { Text(text = state.label) },
                                        leadingIcon = {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Done,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            if (currentSpecId != null && availableVersions.isNotEmpty()) {
                                HorizontalDivider()
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(tag = "version_filter_row_$currentSpecId"),
                                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    item {
                                        FilterChip(
                                            modifier = Modifier.testTag(
                                                tag = "version_filter_all_$currentSpecId"
                                            ),
                                            selected = selectedVersions[currentSpecId] == null,
                                            onClick = {
                                                selectedVersions.remove(
                                                    key = currentSpecId
                                                )
                                            },
                                            label = { Text(text = "All") }
                                        )
                                    }
                                    items(items = availableVersions) { version ->
                                        FilterChip(
                                            modifier = Modifier.testTag(
                                                tag = "version_filter_${currentSpecId}_$version"
                                            ),
                                            selected = selectedVersions[currentSpecId] == version,
                                            onClick = { selectedVersions[currentSpecId] = version },
                                            label = { Text(text = version) }
                                        )
                                    }
                                }
                            }
                            if (currentSpecId != null && availableMethods.isNotEmpty()) {
                                val activeMethods = selectedMethods[currentSpecId].orEmpty()
                                HorizontalDivider()
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(tag = "method_filter_row_$currentSpecId"),
                                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    items(items = availableMethods) { method ->
                                        val selected = method in activeMethods
                                        FilterChip(
                                            modifier = Modifier.testTag(
                                                tag = "method_filter_${currentSpecId}_${method.value}"
                                            ),
                                            selected = selected,
                                            onClick = {
                                                selectedMethods[currentSpecId] = if (selected) {
                                                    activeMethods - method
                                                } else {
                                                    activeMethods + method
                                                }
                                            },
                                            label = { Text(text = method.value) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .height(intrinsicSize = IntrinsicSize.Min)
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(weight = 1f)
                                .padding(vertical = 8.dp)
                                .testTag(tag = "networkmock_search_field"),
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(text = "Search operations...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                    IconButton(
                                        modifier = Modifier.testTag(
                                            tag = "networkmock_clear_search_button"
                                        ),
                                        onClick = { searchQuery = "" }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        IconButton(
                            modifier = Modifier.testTag(tag = "expand_filter_button"),
                            onClick = { filtersExpanded = !filtersExpanded }
                        ) {
                            Icon(
                                modifier = Modifier.graphicsLayer(rotationX = iconRotation),
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Expand filters"
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface {
                GlobalMockToggle(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = uiState.globalMockingEnabled,
                    mockedCount = mockedOperations,
                    totalCount = totalOperations,
                    onToggle = onGlobalToggle
                )
            }
            HorizontalDivider()

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp
            ) {
                uiState.specs.forEachIndexed { index, spec ->
                    Tab(
                        modifier = Modifier.testTag(
                            tag = "spec_tab_${spec.specId}"
                        ),
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = spec.name) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                val spec = uiState.specs.getOrNull(index = pageIndex) ?: return@HorizontalPager

                val selectedVersion = selectedVersions[spec.specId]
                val methodFilter = selectedMethods[spec.specId].orEmpty()
                val filteredOperations = remember(
                    spec.operations,
                    searchQuery,
                    selectedVersion,
                    methodFilter,
                    selectedMockStates
                ) {
                    spec.operations.filter {
                        it.matches(
                            query = searchQuery,
                            version = selectedVersion,
                            methods = methodFilter,
                            mockStates = selectedMockStates
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(space = 0.dp)
                ) {
                    if (filteredOperations.isEmpty()) {
                        item(key = "empty_filter_state") {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(all = 16.dp)
                                    .testTag(tag = "networkmock_empty_filter_message"),
                                text = "No operations match your filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    itemsIndexed(
                        items = filteredOperations,
                        key = { _, operation -> operation.descriptor.key.compositeKey }
                    ) { index, operation ->
                        EndpointCard(
                            modifier = Modifier.testTag(
                                tag = "endpoint_card_${operation.descriptor.specId}" +
                                    "_${operation.descriptor.operationId}"
                            ),
                            endpoint = operation,
                            openEndpointDetails = {
                                openEndpointDetails(operation.descriptor.key)
                            }
                        )
                        if (index != filteredOperations.lastIndex) {
                            HorizontalDivider()
                        }
                    }

                    item {
                        Spacer(
                            modifier = Modifier.height(
                                height = paddingValues.calculateBottomPadding()
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Whether this operation's name, path, or operationId contains [query], and matches
 * [version], [methods], and [mockStates].
 */
@Suppress("DocumentationOverPrivateFunction")
private fun OperationUiModel.matches(
    query: String,
    version: String?,
    methods: Set<HttpMethod>,
    mockStates: Set<MockStateFilter>
): Boolean {
    val config = descriptor.config
    val matchesQuery = query.isBlank() ||
        config.name.contains(other = query, ignoreCase = true) ||
        config.path.contains(other = query, ignoreCase = true) ||
        config.operationId.contains(other = query, ignoreCase = true)
    val matchesVersion = version == null || config.version == version
    val matchesMethod = methods.isEmpty() || config.method in methods
    val matchesMockState = mockStates.isEmpty() || when (currentState) {
        is OperationMockState.Mock -> MockStateFilter.MOCKED in mockStates
        OperationMockState.Network -> MockStateFilter.NETWORK in mockStates
    }
    return matchesQuery && matchesVersion && matchesMethod && matchesMockState
}

/** Filter dimension over whether an operation is currently mocked or passing through to the network. */
private enum class MockStateFilter(val label: String) {
    MOCKED(label = "Mocked"),
    NETWORK(label = "Network")
}

@Preview(locale = "en")
@Composable
private fun NetworkMockScreenPreview(
    @PreviewParameter(NetworkMockUiStatePreviewParameterProvider::class) uiState: NetworkMockUiState
) {
    MaterialTheme {
        Scaffold {
            NetworkMockScreenContent(
                uiState = uiState,
                onGlobalToggle = {},
                navigateToEndpointScreen = {}
            )
        }
    }
}
