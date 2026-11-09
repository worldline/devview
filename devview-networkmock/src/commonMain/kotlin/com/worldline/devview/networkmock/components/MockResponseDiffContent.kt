package com.worldline.devview.networkmock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.networkmock.PreviewSheetState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.model.DiffLine
import com.worldline.devview.networkmock.model.DisplayLine
import com.worldline.devview.networkmock.preview.PreviewSheetStatePreviewParameterProvider
import com.worldline.devview.networkmock.utils.CONTEXT_LINES
import com.worldline.devview.networkmock.utils.displayContent
import com.worldline.devview.networkmock.utils.toDisplayLines
import kotlinx.collections.immutable.PersistentList

/**
 * Renders an inline unified diff for two mock response bodies that are structurally
 * similar enough to warrant a unified view (as determined by [MockResponseDiffDefaults]).
 *
 * - Unchanged lines are rendered normally with a line-number gutter on both sides.
 * - Differing lines use a [MockResponseDiffColors.leftContainer] background for the left response
 *   and [MockResponseDiffColors.rightContainer] for the right. Their indigo and brown pairs
 *   adapt to the surface luminance without implying additions or removals.
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
    val texts = remember(key1 = displayLines) {
        displayLines.map { line ->
            when (line) {
                is DisplayLine.Unchanged -> line.text
                is DisplayLine.Left -> line.text
                is DisplayLine.Right -> line.text
                is DisplayLine.Collapsed -> "  ⋯  ${line.count} unchanged lines"
            }
        }
    }

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

        ScrollableLinePane(
            lines = texts,
            gutterWidth = 66.dp,
            tag = "preview_inline_pane"
        ) { index ->
            when (val line = displayLines[index]) {
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
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        maxLines = 1
                    )
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
            textAlign = TextAlign.End,
            softWrap = false,
            maxLines = 1
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
                textAlign = TextAlign.End,
                softWrap = false,
                maxLines = 1
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp),
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            softWrap = false,
            maxLines = 1
        )
    }
}

/**
 * Renders two mock response bodies in a vertically split view for cases where the
 * responses are too dissimilar for an inline diff.
 *
 * Each half takes equal vertical space and has independent horizontal and lazy vertical scrolling;
 * identity (which response is which) comes from the caller's chip
 * header, not from a per-pane label. A [HorizontalDivider] separates the two halves. This
 * layout is safe for portrait phone screens — no side-by-side columns.
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
        ResponseContentPane(
            response = first,
            colors = colors,
            modifier = Modifier.weight(weight = 1f),
            tag = "preview_first_pane"
        )
        if (second != null) {
            HorizontalDivider()
            ResponseContentPane(
                response = second,
                colors = colors,
                modifier = Modifier.weight(weight = 1f),
                tag = "preview_second_pane"
            )
        }
    }
}

/**
 * A single scrollable pane showing the content of one [MockResponse]. Its identity
 * ([MockResponse.displayName]) is shown by the caller's chip header, not repeated here.
 */
@Composable
private fun ResponseContentPane(
    response: MockResponse,
    tag: String,
    modifier: Modifier = Modifier,
    colors: MockResponseDiffColors = MockResponseDiffDefaults.colors()
) {
    val lines = remember(key1 = response.content, key2 = response.contentType) {
        response.displayContent().lines()
    }

    ScrollableLinePane(
        lines = lines,
        gutterWidth = 33.dp,
        tag = tag,
        modifier = modifier
    ) { index ->
        val lineNumber = (index + 1).toString()
        DiffLineRow(
            lineLeft = lineNumber,
            lineRight = lineNumber,
            content = lines[index],
            background = colors.surface(),
            textColor = colors.onSurface(),
            gutterColor = colors.gutterContainer(),
            gutterTextColor = colors.onGutterContainer(),
            showRightGutter = false
        )
    }
}

@Suppress("NamedArguments") // Function-type invocation cannot use named arguments.
@Composable
private fun ScrollableLinePane(
    lines: List<String>,
    gutterWidth: Dp,
    tag: String,
    modifier: Modifier = Modifier,
    row: @Composable (Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val listState = rememberLazyListState()
    var textWidth by remember(key1 = lines, key2 = textMeasurer, key3 = textStyle) {
        mutableIntStateOf(value = 0)
    }
    LaunchedEffect(lines, textMeasurer, textStyle, listState) {
        measureLineWidths(
            lines = lines,
            visibleIndices = { listState.layoutInfo.visibleItemsInfo.map { it.index } },
            measure = { line ->
                textMeasurer
                    .measure(
                        text = line,
                        style = textStyle,
                        softWrap = false,
                        maxLines = 1
                    ).size.width
            },
            onWidth = { textWidth = it }
        )
    }
    val contentWidth = with(LocalDensity.current) { textWidth.toDp() } + gutterWidth + 16.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val rowWidth = maxOf(a = maxWidth, b = contentWidth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag = tag)
                .horizontalScroll(state = rememberScrollState())
        ) {
            // Bound the lazy list before the horizontal scroller's infinite width reaches it.
            LazyColumn(
                state = listState,
                modifier = Modifier.width(width = rowWidth).testTag(tag = "${tag}_rows")
            ) {
                items(count = lines.size) { index -> row(index) }
            }
        }
    }
}

/** Measures every line across frames, prioritizing the current viewport without blocking first composition. */
internal suspend fun measureLineWidths(
    lines: List<String>,
    visibleIndices: () -> List<Int>,
    measure: (String) -> Int,
    onWidth: (Int) -> Unit
) {
    val measured = BooleanArray(size = lines.size)
    var count = 0
    var next = 0
    var width = 0
    while (count < lines.size) {
        withFrameNanos { }
        var remaining = 32
        val visible = visibleIndices().iterator()
        while (remaining > 0 && count < lines.size) {
            val index = if (visible.hasNext()) {
                visible.next()
            } else {
                while (next < lines.size && measured[next]) next++
                next
            }
            if (index !in lines.indices || measured[index]) continue
            width = maxOf(a = width, b = measure(lines[index]))
            measured[index] = true
            count++
            remaining--
        }
        onWidth(width)
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
