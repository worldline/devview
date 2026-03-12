package com.worldline.devview.networkmock.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.networkmock.model.MockResponse
import com.worldline.devview.networkmock.utils.fake

internal class MockResponsePreviewParameterProvider : PreviewParameterProvider<MockResponse> {
    override val values: Sequence<MockResponse>
        get() = MockResponse.fake().asSequence()

    override fun getDisplayName(index: Int): String = values.elementAt(index = index).displayName
}
