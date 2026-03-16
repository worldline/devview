package com.worldline.devview.networkmock.utils

import com.worldline.devview.networkmock.model.DiffLine
import com.worldline.devview.networkmock.model.DisplayLine
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * Threshold for deciding whether to use an inline diff (single column) or a split view
 * (two columns) when comparing two content strings. Expressed as the minimum ratio of
 * lines shared between both sides to the total number of lines. For example, 0.4 means
 * at least 40 % of lines must be identical for an inline diff to be used.
 */
internal const val INLINE_DIFF_THRESHOLD = 0.4f

/**
 * Number of unchanged lines shown immediately above and below each changed region.
 * Lines beyond this window are collapsed to keep the diff focused on what actually changed.
 */
internal const val CONTEXT_LINES = 3

/**
 * Minimum run of consecutive unchanged lines that triggers collapsing. Runs shorter than
 * this are shown in full — collapsing them would cost more space than it saves.
 */
internal const val COLLAPSE_THRESHOLD = CONTEXT_LINES * 2 + 1

/**
 * Returns `true` when the two content strings are similar enough to be displayed as an inline
 * diff rather than a split view. Similarity is measured as the ratio of shared lines to the
 * total number of lines; the result is `true` when that ratio meets or exceeds [threshold].
 *
 * @param contentLeft The left-hand content string to compare.
 * @param contentRight The right-hand content string to compare.
 * @param threshold Minimum similarity ratio required to prefer an inline diff. Defaults to
 * [INLINE_DIFF_THRESHOLD].
 */
internal fun shouldUseInlineDiff(
    contentLeft: String,
    contentRight: String,
    threshold: Float = INLINE_DIFF_THRESHOLD
): Boolean {
    val linesA = contentLeft.lines()
    val linesB = contentRight.lines()
    val lcsLength = lcsLength(a = linesA, b = linesB)
    val maxLines = maxOf(a = linesA.size, b = linesB.size)
    return if (maxLines == 0) true else lcsLength.toFloat() / maxLines >= threshold
}

/**
 * Computes a line-level diff between [contentLeft] and [contentRight] and returns a list of
 * [DiffLine] values ready for rendering.
 *
 * The algorithm works by finding the **Longest Common Subsequence (LCS)** — the longest list
 * of lines that appear in both sides in the same order. Lines in the LCS are marked as
 * [DiffLine.Unchanged]; everything else is [DiffLine.Different]. Each entry also carries
 * 1-based line numbers for the gutter.
 */
internal fun computeLineDiff(contentLeft: String, contentRight: String): PersistentList<DiffLine> {
    val linesA = contentLeft.lines()
    val linesB = contentRight.lines()

    val dp = lcsTable(a = linesA, b = linesB)
    val result = mutableListOf<DiffLine>()
    var i = linesA.size
    var j = linesB.size

    while (i > 0 || j > 0) {
        when {
            i > 0 && j > 0 && linesA[i - 1] == linesB[j - 1] -> {
                result.add(
                    element = DiffLine.Unchanged(
                        text = linesA[i - 1],
                        lineLeft = i,
                        lineRight = j
                    )
                )
                i--
                j--
            }

            j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                result.add(
                    element = DiffLine.Different(
                        textLeft = null,
                        lineLeft = null,
                        textRight = linesB[j - 1],
                        lineRight = j
                    )
                )
                j--
            }

            else -> {
                result.add(
                    element = DiffLine.Different(
                        textLeft = linesA[i - 1],
                        lineLeft = i,
                        textRight = null,
                        lineRight = null
                    )
                )
                i--
            }
        }
    }

    return result.reversed().toPersistentList()
}

/**
 * Returns the length of the Longest Common Subsequence (LCS) between [a] and [b].
 *
 * The LCS is the longest list of lines that appears in both sequences in the same order,
 * not necessarily contiguous. For example, the LCS of `["a","b","c"]` and `["a","c","d"]`
 * is `["a","c"]`, so the length is 2.
 */
@Suppress("DocumentationOverPrivateFunction")
private fun lcsLength(a: List<String>, b: List<String>): Int = lcsTable(
    a = a,
    b = b
)[a.size][b.size]

/**
 * Builds a Dynamic Programming (DP) table to compute the LCS length for every prefix pair
 * of [a] and [b].
 *
 * Dynamic Programming here means we solve the problem bottom-up: `dp[i][j]` holds the LCS
 * length for the first `i` lines of [a] and the first `j` lines of [b]. Each cell is filled
 * in constant time by looking at the cell above, the cell to the left, and the diagonal —
 * so the full table is computed in O(m × n) time and space.
 */
@Suppress("DocumentationOverPrivateFunction")
private fun lcsTable(a: List<String>, b: List<String>): Array<IntArray> {
    val m = a.size
    val n = b.size
    val dp = Array(size = m + 1) { IntArray(size = n + 1) }
    for (i in 1..m) {
        for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1] + 1
            } else {
                maxOf(a = dp[i - 1][j], b = dp[i][j - 1])
            }
        }
    }
    return dp
}

/**
 * Converts a [DiffLine] list into a [DisplayLine] list ready for the UI, collapsing long runs
 * of unchanged lines to keep the view focused on the differences.
 *
 * Runs of [COLLAPSE_THRESHOLD] or more consecutive unchanged lines are trimmed to show only
 * [CONTEXT_LINES] lines on each end, with a [DisplayLine.Collapsed] placeholder in the middle
 * indicating how many lines were hidden.
 */
internal fun List<DiffLine>.toDisplayLines(): PersistentList<DisplayLine> {
    val display = mutableListOf<DisplayLine>()

    // First pass: flatten DiffLine → DisplayLine without collapsing
    val flat = mutableListOf<DisplayLine>()
    forEach { line ->
        when (line) {
            is DiffLine.Unchanged -> flat.add(
                element = DisplayLine.Unchanged(
                    text = line.text,
                    lineLeft = line.lineLeft,
                    lineRight = line.lineRight
                )
            )

            is DiffLine.Different -> {
                if (line.textLeft != null && line.lineLeft != null) {
                    flat.add(element = DisplayLine.Left(text = line.textLeft, line = line.lineLeft))
                }
                if (line.textRight != null && line.lineRight != null) {
                    flat.add(
                        element = DisplayLine.Right(
                            text = line.textRight,
                            line = line.lineRight
                        )
                    )
                }
            }
        }
    }

    // Second pass: find runs of Unchanged and collapse the middle
    var idx = 0
    while (idx < flat.size) {
        val entry = flat[idx]
        if (entry !is DisplayLine.Unchanged) {
            display.add(element = entry)
            idx++
            continue
        }

        // Find the full run of consecutive Unchanged entries
        var runEnd = idx
        while (runEnd + 1 < flat.size && flat[runEnd + 1] is DisplayLine.Unchanged) runEnd++
        val runSize = runEnd - idx + 1

        if (runSize <= COLLAPSE_THRESHOLD) {
            // Short run — show all
            for (k in idx..runEnd) display.add(element = flat[k])
        } else {
            // Long run — show first CONTEXT_LINES, collapse middle, show last CONTEXT_LINES
            for (k in idx until idx + CONTEXT_LINES) display.add(element = flat[k])
            display.add(element = DisplayLine.Collapsed(count = runSize - CONTEXT_LINES * 2))
            for (k in runEnd - CONTEXT_LINES + 1..runEnd) display.add(element = flat[k])
        }
        idx = runEnd + 1
    }

    return display.toPersistentList()
}
