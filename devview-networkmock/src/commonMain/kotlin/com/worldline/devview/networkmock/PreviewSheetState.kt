package com.worldline.devview.networkmock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.model.DiffLine
import com.worldline.devview.networkmock.utils.INLINE_DIFF_THRESHOLD
import com.worldline.devview.networkmock.utils.computeLineDiff
import com.worldline.devview.networkmock.utils.displayContent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun rememberResponsePreview(
    primary: MockResponse,
    second: MockResponse?
): PreviewSheetState.HasResponse = remember(key1 = primary, key2 = second) {
    if (second == null) {
        PreviewSheetState.Single(response = primary)
    } else {
        PreviewSheetState.Compare(first = primary, second = second)
    }
}

private const val MAX_DIFF_CELLS = 1_000_000L

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
        // shortcut: large comparisons use split view; upgrade to a bounded diff algorithm for huge bodies.
        val lineDiff: PersistentList<DiffLine> by lazy {
            val left = first.displayContent()
            val right = second.displayContent()
            val cells = (left.lines().size.toLong() + 1) * (right.lines().size.toLong() + 1)
            if (cells <= MAX_DIFF_CELLS) {
                computeLineDiff(contentLeft = left, contentRight = right)
            } else {
                persistentListOf()
            }
        }

        val useInlineDiff: Boolean by lazy {
            val shared = lineDiff.count { it is DiffLine.Unchanged }
            val leftLines = lineDiff.count {
                it is DiffLine.Unchanged ||
                    it is DiffLine.Different && it.textLeft != null
            }
            val rightLines = lineDiff.count {
                it is DiffLine.Unchanged ||
                    it is DiffLine.Different && it.textRight != null
            }
            lineDiff.isNotEmpty() && shared.toFloat() / maxOf(
                a = leftLines,
                b = rightLines
            ) >= threshold
        }
    }
}
