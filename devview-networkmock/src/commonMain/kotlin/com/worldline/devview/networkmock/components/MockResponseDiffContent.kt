package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.PreviewSheetState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.model.DiffLine
import com.worldline.devview.networkmock.model.DisplayLine
import com.worldline.devview.networkmock.preview.PreviewSheetStatePreviewParameterProvider
import com.worldline.devview.networkmock.utils.CONTEXT_LINES
import com.worldline.devview.networkmock.utils.toDisplayLines
import kotlinx.collections.immutable.PersistentList

/**
 * Renders an inline unified diff for two mock response bodies that are structurally
 * similar enough to warrant a unified view (as determined by [MockResponseDiffDefaults]).
 *
 * - Unchanged lines are rendered normally with a line-number gutter on both sides.
 * - Differing lines use a [MockResponseDiffColors.leftContainer] background for the left response
 *   and [MockResponseDiffColors.rightContainer] for the right. These map to `primaryContainer` and
 *   `secondaryContainer` by default to avoid implying additions or removals.
 * - Long runs of unchanged lines are collapsed into a "… N unchanged lines …" label,
 *   showing only [CONTEXT_LINES] lines of context on each side of a changed region.
 * - When [leftLabel] and [rightLabel] are provided, a colour legend is rendered above the diff
 *   rows to identify which colour corresponds to which response.
 *
 * @param diff The pre-computed list of [DiffLine]s to render.
 * @param leftLabel Optional label for the left (first) response shown in the colour legend.
 * @param rightLabel Optional label for the right (second) response shown in the colour legend.
 * @param colors Colours used to render the diff. Defaults to [MockResponseDiffDefaults.colors].
 * @param modifier [Modifier] to be applied to the root [Column].
 */
@Composable
internal fun InlineDiffContent(
    diff: PersistentList<DiffLine>,
    modifier: Modifier = Modifier,
    leftLabel: String? = null,
    rightLabel: String? = null,
    colors: MockResponseDiffColors = MockResponseDiffDefaults.colors()
) {
    val displayLines = remember(key1 = diff) { diff.toDisplayLines() }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (leftLabel != null && rightLabel != null) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(
                    color = colors.leftContainer(),
                    label = leftLabel,
                    colors = colors
                )
                LegendDot(
                    color = colors.rightContainer(),
                    label = rightLabel,
                    colors = colors
                )
            }
            HorizontalDivider()
        }

        Column(
            modifier = Modifier
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
                            background = colors.surface(),
                            textColor = colors.onSurface(),
                            gutterColor = colors.gutterContainer(),
                            gutterTextColor = colors.onGutterContainer()
                        )
                    }

                    is DisplayLine.Left -> {
                        DiffLineRow(
                            lineLeft = line.line.toString(),
                            lineRight = "",
                            content = line.text,
                            background = colors.leftContainer(),
                            textColor = colors.onLeftContainer(),
                            gutterColor = colors.gutterContainer(),
                            gutterTextColor = colors.onGutterContainer()
                        )
                    }

                    is DisplayLine.Right -> {
                        DiffLineRow(
                            lineLeft = "",
                            lineRight = line.line.toString(),
                            content = line.text,
                            background = colors.rightContainer(),
                            textColor = colors.onRightContainer(),
                            gutterColor = colors.gutterContainer(),
                            gutterTextColor = colors.onGutterContainer()
                        )
                    }

                    is DisplayLine.Collapsed -> {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = colors.collapsedContainer())
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            text = "  ⋯  ${line.count} unchanged lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onGutterContainer(),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * A small coloured dot with a text label, used in the [InlineDiffContent] colour legend.
 *
 * @param color The background colour of the dot.
 * @param label The text label displayed next to the dot.
 * @param colors The diff colours used to derive the label text colour.
 * @param modifier [Modifier] to be applied to the root [Row].
 */
@Composable
private fun LegendDot(
    color: Color,
    label: String,
    colors: MockResponseDiffColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 10.dp)
                .clip(shape = MaterialTheme.shapes.extraSmall)
                .background(color = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onGutterContainer()
        )
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
                .width(width = 32.dp)
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
                    .width(width = 32.dp)
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
 * responses are too dissimilar for an inline diff.
 *
 * Each half takes equal vertical space, has its own independent [verticalScroll] and
 * [horizontalScroll], and is identified by a small chip header. A [HorizontalDivider]
 * separates the two halves. This layout is safe for portrait phone screens — no
 * side-by-side columns.
 *
 * When only one response is provided ([second] is `null`), only the top half is rendered,
 * making this composable reusable for the single-response preview case.
 */
@Composable
internal fun SplitDiffContent(
    first: MockResponse,
    second: MockResponse?,
    modifier: Modifier = Modifier,
    colors: MockResponseDiffColors = MockResponseDiffDefaults.colors()
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ResponseContentPane(response = first, colors = colors)
        }

        if (second != null) {
            HorizontalDivider()

            Column(modifier = Modifier.fillMaxWidth()) {
                ResponseContentPane(response = second, colors = colors)
            }
        }
    }
}

/**
 * A single scrollable pane showing the content of one [MockResponse] with a small
 * chip label header identifying the response by [MockResponse.displayName].
 */
@Composable
private fun ResponseContentPane(
    response: MockResponse,
    modifier: Modifier = Modifier,
    colors: MockResponseDiffColors = MockResponseDiffDefaults.colors()
) {
    val lines = remember(key1 = response.content) { response.content.lines() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            text = response.fileName,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onGutterContainer()
        )
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
        ) {
            lines.forEachIndexed { index, line ->
                val lineNumber = (index + 1).toString()
                DiffLineRow(
                    lineLeft = lineNumber,
                    lineRight = lineNumber,
                    content = line,
                    background = colors.surface(),
                    textColor = colors.onSurface(),
                    gutterColor = colors.gutterContainer(),
                    gutterTextColor = colors.onGutterContainer(),
                    showRightGutter = false
                )
            }
        }
    }
}

@Preview(name = "MockResponseDiffContent", locale = "en")
@Composable
private fun MockResponseDiffContentPreview(
    @PreviewParameter(PreviewSheetStatePreviewParameterProvider::class)
    previewSheetState: PreviewSheetState.HasResponse
) {
    MaterialTheme {
        Surface {
            when (previewSheetState) {
                is PreviewSheetState.Single -> SplitDiffContent(
                    first = previewSheetState.response,
                    second = null
                )

                is PreviewSheetState.Compare -> if (previewSheetState.useInlineDiff) {
                    InlineDiffContent(
                        diff = previewSheetState.lineDiff,
                        leftLabel = previewSheetState.first.displayName,
                        rightLabel = previewSheetState.second.displayName
                    )
                } else {
                    SplitDiffContent(
                        first = previewSheetState.first,
                        second = previewSheetState.second
                    )
                }
            }
        }
    }
}
