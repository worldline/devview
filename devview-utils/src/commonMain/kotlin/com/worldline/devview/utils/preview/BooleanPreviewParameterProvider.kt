package com.worldline.devview.utils.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

public class BooleanPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean>
        get() = sequenceOf(
            true,
            false
        )

    override fun getDisplayName(index: Int): String? =
        when (values.elementAtOrNull(index = index)) {
            true -> "True"
            false -> "False"
            else -> null
        }
}
