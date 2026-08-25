package com.worldline.devview.networkmock.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.networkmock.model.OperationUiModel
import com.worldline.devview.networkmock.utils.fake

internal class OperationUiModelPreviewParameterProvider :
    PreviewParameterProvider<OperationUiModel> {
    override val values: Sequence<OperationUiModel>
        get() = OperationUiModel
            .fake(
                availableResponsesAmount = 13
            ).asSequence()

    override fun getDisplayName(index: Int): String? = values
        .elementAtOrNull(index = index)
        ?.descriptor
        ?.config
        ?.name
}
