package com.worldline.devview.networkmock

import androidx.compose.runtime.Immutable
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.model.DiffLine
import com.worldline.devview.networkmock.utils.INLINE_DIFF_THRESHOLD
import com.worldline.devview.networkmock.utils.computeLineDiff
import com.worldline.devview.networkmock.utils.shouldUseInlineDiff
import kotlinx.collections.immutable.PersistentList

@Immutable
internal sealed interface PreviewSheetState {
    @Immutable
    sealed interface HasResponse : PreviewSheetState

    /** Sheet is closed. */
    @Immutable
    data object Hidden : PreviewSheetState

    /** Sheet shows one response. */
    @Immutable
    data class Single(val response: MockResponse) : HasResponse

    /** Sheet shows two responses side by side. */
    @Immutable
    data class Compare(
        val first: MockResponse,
        val second: MockResponse,
        val threshold: Float = INLINE_DIFF_THRESHOLD
    ) : HasResponse {
        val useInlineDiff: Boolean
            get() = shouldUseInlineDiff(
                contentLeft = first.content,
                contentRight = second.content,
                threshold = threshold
            )

        val lineDiff: PersistentList<DiffLine>
            get() = computeLineDiff(
                contentLeft = first.content,
                contentRight = second.content
            )
    }

    fun transition(response: MockResponse): PreviewSheetState = when (this) {
        is Hidden -> Single(response = response)
        is Single -> if (response == this.response) {
            Hidden
        } else {
            Compare(first = this.response, second = response)
        }

        is Compare -> when (response) {
            first -> {
                Single(response = second)
            }

            second -> {
                Single(response = first)
            }

            else -> {
                this
            }
        }
    }

    fun isInPreviewMode(response: MockResponse): Boolean = when (this) {
        is Hidden -> false
        is Single -> response == this.response
        is Compare -> response == first || response == second
    }
}
