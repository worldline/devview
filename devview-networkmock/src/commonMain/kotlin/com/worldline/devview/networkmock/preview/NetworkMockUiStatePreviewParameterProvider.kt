package com.worldline.devview.networkmock.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.networkmock.utils.fake
import com.worldline.devview.networkmock.viewmodel.HostUiModel
import com.worldline.devview.networkmock.viewmodel.NetworkMockUiState
import kotlinx.collections.immutable.toPersistentList

internal class NetworkMockUiStatePreviewParameterProvider :
    PreviewParameterProvider<NetworkMockUiState> {
    override val values: Sequence<NetworkMockUiState>
        get() = sequenceOf(
            NetworkMockUiState.Loading,
            NetworkMockUiState.Error(message = "Failed to load configuration"),
            NetworkMockUiState.Empty,
            NetworkMockUiState.Content(
                globalMockingEnabled = true,
                hosts = HostUiModel.fake().toPersistentList()
            ),
            NetworkMockUiState.Content(
                globalMockingEnabled = false,
                hosts = HostUiModel.fake().toPersistentList()
            )
        )

    override fun getDisplayName(index: Int): String? =
        when (val state = values.elementAtOrNull(index = index)) {
            is NetworkMockUiState.Loading -> "Loading"
            is NetworkMockUiState.Error -> "Error"
            is NetworkMockUiState.Empty -> "Empty"
            is NetworkMockUiState.Content -> "Content (Mocking ${if (state.globalMockingEnabled) {
                "Enabled"
            } else {
                "Disabled"
            }})"
            else -> null
        }
}
