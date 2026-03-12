package com.worldline.devview.networkmock.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.networkmock.utils.fake
import com.worldline.devview.networkmock.viewmodel.EndpointUiModel

internal class EndpointUiModelPreviewParameterProvider :
    PreviewParameterProvider<EndpointUiModel> {
    override val values: Sequence<EndpointUiModel>
        get() = EndpointUiModel
            .fake(
                availableResponsesAmount = 13
            ).asSequence()

    override fun getDisplayName(index: Int): String? = values
        .elementAtOrNull(index = index)
        ?.descriptor
        ?.config
        ?.name
}
