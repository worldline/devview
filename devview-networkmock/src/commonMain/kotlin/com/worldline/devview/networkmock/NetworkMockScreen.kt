package com.worldline.devview.networkmock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldline.devview.networkmock.components.EndpointCard
import com.worldline.devview.networkmock.components.GlobalMockToggle
import com.worldline.devview.networkmock.repository.MockConfigRepository
import com.worldline.devview.networkmock.repository.MockStateRepository
import com.worldline.devview.networkmock.viewmodel.NetworkMockViewModel

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
    modifier: Modifier = Modifier,
    viewModel: NetworkMockViewModel = viewModel {
        NetworkMockViewModel(
            configRepository = configRepository,
            stateRepository = stateRepository
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()

    NetworkMockScreenContent(
        uiState = uiState,
        onGlobalToggle = viewModel::setGlobalMockingEnabled,
        onEndpointToggle = { hostId, endpointId, enabled ->
            viewModel.setEndpointMockEnabled(
                hostId = hostId,
                endpointId = endpointId,
                enabled = enabled
            )
        },
        onSelectResponse = { hostId, endpointId, fileName ->
            viewModel.selectResponse(
                hostId = hostId,
                endpointId = endpointId,
                responseFileName = fileName
            )
        },
        onResetAll = viewModel::resetAllToNetwork,
        modifier = modifier
    )
}

@Composable
private fun NetworkMockScreenContent(
    uiState: com.worldline.devview.networkmock.viewmodel.NetworkMockUiState,
    onGlobalToggle: (Boolean) -> Unit,
    onEndpointToggle: (String, String, Boolean) -> Unit,
    onSelectResponse: (String, String, String) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is com.worldline.devview.networkmock.viewmodel.NetworkMockUiState.Loading -> {
            LoadingState(modifier = modifier)
        }
        is com.worldline.devview.networkmock.viewmodel.NetworkMockUiState.Error -> {
            ErrorState(
                message = uiState.message,
                modifier = modifier
            )
        }
        is com.worldline.devview.networkmock.viewmodel.NetworkMockUiState.Empty -> {
            EmptyState(modifier = modifier)
        }
        is com.worldline.devview.networkmock.viewmodel.NetworkMockUiState.Content -> {
            ContentState(
                uiState = uiState,
                onGlobalToggle = onGlobalToggle,
                onEndpointToggle = onEndpointToggle,
                onSelectResponse = onSelectResponse,
                onResetAll = onResetAll,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading mock configuration...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            modifier = Modifier.padding(all = 32.dp)
        ) {
            Text(
                text = "Error Loading Configuration",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            modifier = Modifier.padding(all = 32.dp)
        ) {
            Text(
                text = "📄 No Mocks Configured",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Add a mocks.json file to:\ncomposeResources/files/networkmocks/",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ContentState(
    uiState: com.worldline.devview.networkmock.viewmodel.NetworkMockUiState.Content,
    onGlobalToggle: (Boolean) -> Unit,
    onEndpointToggle: (String, String, Boolean) -> Unit,
    onSelectResponse: (String, String, String) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        // Global toggle
        item {
            GlobalMockToggle(
                enabled = uiState.globalMockingEnabled,
                onToggle = onGlobalToggle
            )
        }

        // Reset all button
        if (uiState.globalMockingEnabled) {
            item {
                TextButton(
                    onClick = onResetAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Reset All to Network")
                }
            }
        }

        // Hosts and endpoints
        uiState.hosts.forEach { host ->
            // Host header (if multiple hosts)
            if (uiState.hosts.size > 1) {
                item(key = "host-${host.id}") {
                    Column {
                        Spacer(modifier = Modifier.height(height = 8.dp))
                        Text(
                            text = host.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = host.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Endpoint cards
            items(
                items = host.endpoints,
                key = { "${host.id}-${it.endpointId}" }
            ) { endpoint ->
                EndpointCard(
                    endpoint = endpoint,
                    onToggleMock = { enabled ->
                        onEndpointToggle(host.id, endpoint.endpointId, enabled)
                    },
                    onSelectResponse = { fileName ->
                        onSelectResponse(host.id, endpoint.endpointId, fileName)
                    }
                )
            }
        }
    }
}
