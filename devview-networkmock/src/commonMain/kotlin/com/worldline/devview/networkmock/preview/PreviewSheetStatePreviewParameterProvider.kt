package com.worldline.devview.networkmock.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.worldline.devview.networkmock.PreviewSheetState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.utils.fake

internal class PreviewSheetStatePreviewParameterProvider :
    PreviewParameterProvider<PreviewSheetState.HasResponse> {
    override val values: Sequence<PreviewSheetState.HasResponse>
        get() = sequenceOf(
            PreviewSheetState.Single(
                response = MockResponse.fake().first()
            ),
            PreviewSheetState.Compare(
                first = MockResponse(
                    statusCode = 200,
                    fileName = "endpoint-200.json",
                    displayName = "Success (200)",
                    content =
                        """
                        {
                          "id": 1,
                          "name": "Alice",
                          "email": "alice@example.com",
                          "role": "admin"
                        }
                        """.trimIndent()
                ),
                second = MockResponse(
                    statusCode = 200,
                    fileName = "endpoint-200.json",
                    displayName = "Success (200)",
                    content =
                        """
                        {
                          "id": 1,
                          "name": "Bob",
                          "email": "bob@example.com",
                          "role": "admin"
                        }
                        """.trimIndent()
                )
            ),
            PreviewSheetState.Compare(
                first = MockResponse(
                    statusCode = 200,
                    fileName = "endpoint-200.json",
                    displayName = "Success (200)",
                    content =
                        """
                        {
                          "id": 1,
                          "name": "Alice",
                          "email": "alice@example.com",
                          "role": "admin"
                        }
                        """.trimIndent()
                ),
                second = MockResponse(
                    statusCode = 500,
                    fileName = "endpoint-500.json",
                    displayName = "Server Error (500)",
                    content =
                        """
                        {
                          "error": "InternalServerError",
                          "message": "An unexpected error occurred.",
                          "trace": "com.example.SomeService.doThing(SomeService.kt:42)"
                        }
                        """.trimIndent()
                )
            )
        )

    override fun getDisplayName(index: Int): String? = when (
        val state = values.elementAt(
            index = index
        )
    ) {
        is PreviewSheetState.Single -> state.response.displayName
        is PreviewSheetState.Compare -> "${state.first.displayName} vs ${state.second.displayName}"
    }
}
