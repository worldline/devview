@file:Suppress(
    "TooManyFunctions",
    "DocumentationOverPrivateFunction",
    "DocumentationOverPrivateProperty",
    "MatchingDeclarationName"
)

package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.model.MockResponse
import com.worldline.devview.networkmock.utils.fake

// ---------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------

/**
 * Represents a single line in a computed diff between two mock response bodies.
 *
 * - [Unchanged] — the line is identical in both responses; carries 1-based line numbers
 *   for each side
 * - [Different] — the line differs; either side may be null when one response has more
 *   lines than the other. Line numbers are null on the side that has no content for that
 *   position.
 */
internal sealed interface DiffLine {
    data class Unchanged(val text: String, val lineLeft: Int, val lineRight: Int) : DiffLine

    data class Different(
        val textLeft: String?,
        val lineLeft: Int?,
        val textRight: String?,
        val lineRight: Int?
    ) : DiffLine
}

// ---------------------------------------------------------------------------
// Algorithm
// ---------------------------------------------------------------------------

private const val INLINE_DIFF_THRESHOLD = 0.4f

/**
 * Number of unchanged lines shown above and below each changed region before the
 * rest are collapsed into a "… N unchanged lines …" label.
 */
private const val CONTEXT_LINES = 3

/**
 * Minimum run length of unchanged lines that triggers collapsing. Runs shorter than
 * this are shown in full to avoid a collapse label that saves fewer lines than it costs.
 */
private const val COLLAPSE_THRESHOLD = CONTEXT_LINES * 2 + 1

/**
 * Returns `true` when the two content strings are similar enough (≥ 40 % of lines shared)
 * to be displayed as an inline diff rather than a split view.
 */
internal fun shouldUseInlineDiff(contentLeft: String, contentRight: String): Boolean {
    val linesA = contentLeft.lines()
    val linesB = contentRight.lines()
    val lcsLength = lcsLength(a = linesA, b = linesB)
    val maxLines = maxOf(a = linesA.size, b = linesB.size)
    return if (maxLines == 0) true else lcsLength.toFloat() / maxLines >= INLINE_DIFF_THRESHOLD
}

/**
 * Computes a line-level diff between [contentLeft] and [contentRight] using the
 * Longest Common Subsequence algorithm and returns a list of [DiffLine] values
 * suitable for rendering. Each entry carries 1-based source line numbers for the
 * gutter.
 */
internal fun computeLineDiff(contentLeft: String, contentRight: String): List<DiffLine> {
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

    return result.reversed()
}

/** Computes the length of the LCS between two line lists. */
private fun lcsLength(a: List<String>, b: List<String>): Int = lcsTable(
    a = a,
    b = b
)[a.size][b.size]

/** Builds the full LCS DP table. */
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

// ---------------------------------------------------------------------------
// Display model — collapses long unchanged runs for InlineDiffContent
// ---------------------------------------------------------------------------

/**
 * A flattened display entry used by [InlineDiffContent].
 * Long runs of [DiffLine.Unchanged] are collapsed into [DisplayLine.Collapsed].
 */
private sealed interface DisplayLine {
    data class Unchanged(val text: String, val lineLeft: Int, val lineRight: Int) : DisplayLine

    data class Left(val text: String, val line: Int) : DisplayLine

    data class Right(val text: String, val line: Int) : DisplayLine

    /** Placeholder for a collapsed run of unchanged lines. */
    data class Collapsed(val count: Int) : DisplayLine
}

/**
 * Converts a [DiffLine] list into a [DisplayLine] list, collapsing runs of unchanged
 * lines longer than [COLLAPSE_THRESHOLD] to keep the view focused on the differences.
 */
private fun List<DiffLine>.toDisplayLines(): List<DisplayLine> {
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

    return display
}

// ---------------------------------------------------------------------------
// Composables
// ---------------------------------------------------------------------------

/**
 * Renders an inline unified diff for two mock response bodies that are structurally
 * similar (similarity ≥ [INLINE_DIFF_THRESHOLD]).
 *
 * - Unchanged lines are rendered normally with a line-number gutter on both sides.
 * - Differing lines use a `primaryContainer` background for the left response and
 *   `secondaryContainer` for the right. Primary/secondary are used instead of red/green
 *   to avoid implying additions or removals.
 * - Long runs of unchanged lines are collapsed into a "… N unchanged lines …" label,
 *   showing only [CONTEXT_LINES] lines of context on each side of a changed region.
 */
@Composable
internal fun InlineDiffContent(diff: List<DiffLine>, modifier: Modifier = Modifier) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val gutterColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val gutterTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val collapsedColor = MaterialTheme.colorScheme.surfaceContainerLow

    val displayLines = remember(key1 = diff) { diff.toDisplayLines() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState())
    ) {
        displayLines.forEach { line ->
            when (line) {
                is DisplayLine.Unchanged -> {
                    DiffLineRow(
                        lineLeft = line.lineLeft.toString(),
                        lineRight = line.lineRight.toString(),
                        content = line.text,
                        background = surface,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        gutterColor = gutterColor,
                        gutterTextColor = gutterTextColor
                    )
                }

                is DisplayLine.Left -> {
                    DiffLineRow(
                        lineLeft = line.line.toString(),
                        lineRight = "",
                        content = line.text,
                        background = primaryContainer,
                        textColor = onPrimaryContainer,
                        gutterColor = gutterColor,
                        gutterTextColor = gutterTextColor
                    )
                }

                is DisplayLine.Right -> {
                    DiffLineRow(
                        lineLeft = "",
                        lineRight = line.line.toString(),
                        content = line.text,
                        background = secondaryContainer,
                        textColor = onSecondaryContainer,
                        gutterColor = gutterColor,
                        gutterTextColor = gutterTextColor
                    )
                }

                is DisplayLine.Collapsed -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = collapsedColor)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        text = "  ⋯  ${line.count} unchanged lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = gutterTextColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * A single row in the inline diff: left gutter number | right gutter number | content.
 */
@Composable
internal fun DiffLineRow(
    lineLeft: String,
    lineRight: String,
    content: String,
    background: Color,
    textColor: Color,
    gutterColor: Color,
    gutterTextColor: Color,
    modifier: Modifier = Modifier,
    showRightGutter: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Min)
            .background(color = background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left line number
        Text(
            modifier = Modifier
                .width(width = 36.dp)
                .fillMaxHeight()
                .background(color = gutterColor)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            text = lineLeft,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = gutterTextColor,
            textAlign = TextAlign.End
        )
        VerticalDivider(modifier = Modifier.fillMaxHeight())
        // Right line number — hidden in single-response view
        if (showRightGutter) {
            Text(
                modifier = Modifier
                    .width(width = 36.dp)
                    .fillMaxHeight()
                    .background(color = gutterColor)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                text = lineRight,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = gutterTextColor,
                textAlign = TextAlign.End
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
        }
        // Content — horizontally scrollable so long lines don't wrap
        Text(
            modifier = Modifier
                .weight(weight = 1f)
                .horizontalScroll(state = rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 1.dp),
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            softWrap = false
        )
    }
}

/**
 * Renders two mock response bodies in a vertically split view for cases where the
 * responses are too dissimilar for an inline diff (similarity < [INLINE_DIFF_THRESHOLD]).
 *
 * Each half takes equal vertical space, has its own independent [verticalScroll] and
 * [horizontalScroll], and is identified by a small chip header. A [HorizontalDivider]
 * separates the two halves. This layout is safe for portrait phone screens — no
 * side-by-side columns.
 *
 * When only one response is provided ([right] is `null`), only the top half is rendered,
 * making this composable reusable for the single-response preview case.
 */
@Composable
internal fun SplitDiffContent(
    left: MockResponse,
    right: MockResponse?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top half — left response
        Column(
            modifier = Modifier
                .weight(weight = 1f)
                .fillMaxWidth()
        ) {
            ResponseContentPane(response = left)
        }

        if (right != null) {
            HorizontalDivider()

            // Bottom half — right response
            Column(
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth()
            ) {
                ResponseContentPane(response = right)
            }
        }
    }
}

/**
 * A single scrollable pane showing the content of one [MockResponse] with a small
 * chip label header identifying the response by [MockResponse.displayName].
 */
@Composable
private fun ResponseContentPane(response: MockResponse, modifier: Modifier = Modifier) {
    val gutterColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val gutterTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val lines = remember(key1 = response.content) { response.content.lines() }

    Column(modifier = modifier.fillMaxHeight()) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            text = response.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = gutterTextColor
        )
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
        ) {
            lines.forEachIndexed { index, line ->
                val lineNumber = (index + 1).toString()
                DiffLineRow(
                    lineLeft = lineNumber,
                    lineRight = lineNumber,
                    content = line,
                    background = surface,
                    textColor = onSurface,
                    gutterColor = gutterColor,
                    gutterTextColor = gutterTextColor,
                    showRightGutter = false
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val similarLeft = MockResponse(
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
)

private val similarRight = MockResponse(
    statusCode = 200,
    fileName = "endpoint-200-alt.json",
    displayName = "Success Alt (200)",
    content =
        """
        {
          "id": 2,
          "name": "Bob",
          "email": "bob@example.com",
          "role": "user"
        }
        """.trimIndent()
)

private val dissimilarRight = MockResponse(
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

@Preview(name = "Inline diff — similar content", locale = "en")
@Composable
private fun InlineDiffContentPreview() {
    MaterialTheme {
        Surface {
            val diff = remember {
                computeLineDiff(
                    contentLeft = similarLeft.content,
                    contentRight = similarRight.content
                )
            }
            InlineDiffContent(diff = diff)
        }
    }
}

@Preview(name = "Split view — dissimilar content", locale = "en")
@Composable
private fun SplitDiffContentPreview() {
    MaterialTheme {
        Surface {
            SplitDiffContent(left = similarLeft, right = dissimilarRight)
        }
    }
}

@Preview(name = "Split view — single response", locale = "en")
@Composable
private fun SplitDiffContentSinglePreview() {
    MaterialTheme {
        Surface {
            SplitDiffContent(left = MockResponse.fake().first(), right = null)
        }
    }
}
