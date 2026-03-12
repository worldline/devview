package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldline.devview.networkmock.components.EmptyState
import com.worldline.devview.networkmock.components.EndpointCard
import com.worldline.devview.networkmock.components.ErrorState
import com.worldline.devview.networkmock.components.GlobalMockToggle
import com.worldline.devview.networkmock.components.LoadingState
import com.worldline.devview.networkmock.model.EndpointDescriptor
import com.worldline.devview.networkmock.model.EndpointKey
import com.worldline.devview.networkmock.model.EndpointMockState
import com.worldline.devview.networkmock.preview.NetworkMockUiStatePreviewParameterProvider
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
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
 * @param modifier Optional modifier for the screen
 * @param configRepository Repository for loading mock configuration. Should be the shared
 *   instance constructed by [NetworkMock] so that the plugin and UI use the same cache.
 * @param stateRepository Repository for managing mock state (shared instance from integrator)
 */
@Composable
public fun NetworkMockScreen(
    configRepository: MockConfigRepository,
    stateRepository: MockStateRepository,
    resetToNetworkSharedFlow: SharedFlow<Unit>,
    modifier: Modifier = Modifier,
    viewModel: NetworkMockViewModel = viewModel {
        NetworkMockViewModel(
            configRepository = configRepository,
            stateRepository = stateRepository
        )
    },
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDescriptor by viewModel.selectedEndpointDescriptor.collectAsStateWithLifecycle()
    val selectedEndpointState by viewModel.selectedEndpointState.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) {
        resetToNetworkSharedFlow.collect {
            viewModel.resetAllToNetwork()
        }
    }

    NetworkMockScreenContent(
        uiState = uiState,
        onGlobalToggle = viewModel::setGlobalMockingEnabled,
        setEndpointMockState = viewModel::setEndpointMockState,
        selectEndpoint = viewModel::selectEndpoint,
        clearSelectedEndpoint = viewModel::clearSelectedEndpoint,
        selectedDescriptor = selectedDescriptor,
        selectedEndpointState = selectedEndpointState,
        modifier = modifier,
        bottomPadding = bottomPadding
    )
}

@Composable
private fun NetworkMockScreenContent(
    uiState: NetworkMockUiState,
    onGlobalToggle: (Boolean) -> Unit,
    setEndpointMockState: (EndpointKey, String?) -> Unit,
    selectEndpoint: (EndpointKey) -> Unit,
    clearSelectedEndpoint: () -> Unit,
    selectedDescriptor: EndpointDescriptor?,
    selectedEndpointState: EndpointMockState,
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
                setEndpointMockState = setEndpointMockState,
                selectEndpoint = selectEndpoint,
                clearSelectedEndpoint = clearSelectedEndpoint,
                selectedDescriptor = selectedDescriptor,
                selectedEndpointState = selectedEndpointState,
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
    setEndpointMockState: (EndpointKey, String?) -> Unit,
    selectEndpoint: (EndpointKey) -> Unit,
    clearSelectedEndpoint: () -> Unit,
    selectedDescriptor: EndpointDescriptor?,
    selectedEndpointState: EndpointMockState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    var selectedTabIndex by remember { mutableIntStateOf(value = 0) }

    val pagerState = rememberPagerState(pageCount = { uiState.groups.size })

    LaunchedEffect(key1 = selectedTabIndex) {
        pagerState.animateScrollToPage(page = selectedTabIndex)
    }

    LaunchedEffect(key1 = pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            GlobalMockToggle(
                modifier = Modifier
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                enabled = uiState.globalMockingEnabled,
                onToggle = onGlobalToggle
            )
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp
        ) {
            uiState.groups.forEachIndexed { index, group ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = group.name) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            val group = uiState.groups.getOrNull(index = pageIndex) ?: return@HorizontalPager
            LazyColumn(
                modifier = Modifier
                    .weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 0.dp)
            ) {
                itemsIndexed(
                    items = group.endpoints,
                    key = { _, endpoint -> endpoint.descriptor.key.compositeKey }
                ) { index, endpoint ->
                    EndpointCard(
                        endpoint = endpoint,
                        openEndpointBottomSheet = {
                            selectEndpoint(endpoint.descriptor.key)
                        },
                        showFileName = true
                    )
                    if (index != group.endpoints.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    selectedDescriptor?.let { descriptor ->
        NetworkMockEndpointBottomSheet(
            descriptor = descriptor,
            currentState = selectedEndpointState,
            onDismissRequest = { clearSelectedEndpoint() },
            onSelectResponse = { fileName ->
                setEndpointMockState(descriptor.key, fileName)
            }
        )
    }
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
                setEndpointMockState = { _, _ -> },
                selectEndpoint = {},
                clearSelectedEndpoint = {},
                selectedDescriptor = null,
                selectedEndpointState = EndpointMockState.Network
            )
        }
    }
}
