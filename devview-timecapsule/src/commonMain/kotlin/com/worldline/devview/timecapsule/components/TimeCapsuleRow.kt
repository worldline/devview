package com.worldline.devview.timecapsule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.worldline.devview.timecapsule.FieldChange
import com.worldline.devview.timecapsule.Recorded
import com.worldline.devview.timecapsule.diffLabels
import com.worldline.devview.timecapsule.shownChanges
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

private val RailWidth = 24.dp
private val CurrentNodeSize = 12.dp
private val NodeSize = 8.dp
private val NodeHaloSize = 22.dp
private val CardShape = RoundedCornerShape(size = 12.dp)

/**
 * Single entry in the [com.worldline.devview.timecapsule.TimeCapsuleScreen] timeline.
 *
 * The first line is a change summary against [previousLabel] (e.g. `"count: 3 → 4"`), falling
 * back to [Recorded.label] verbatim when it isn't `key=value`-shaped. Expanding a row reveals
 * the full label below a divider, with changed values highlighted. A rail on the left threads
 * the row into the surrounding timeline, marking [isCurrent] with a small halo.
 *
 * @param previousLabel Label of the entry recorded just before this one, or `null` for the
 *   oldest entry.
 * @param deltaMillis Time elapsed since the previous entry, or `null` for the oldest entry.
 * @param isCurrent Whether this is the most recently recorded entry.
 * @param isLast Whether this is the oldest entry — the rail's connecting line stops here.
 */
@Composable
internal fun TimeCapsuleRow(
    entry: Recorded<*>,
    previousLabel: String?,
    deltaMillis: Long?,
    isCurrent: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val changes = remember(key1 = entry.label, key2 = previousLabel) {
        diffLabels(current = entry.label, previous = previousLabel)
    }
    var summaryOverflows by remember { mutableStateOf(value = false) }
    val isExpandable = changes.isNotEmpty() || summaryOverflows
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    // A Row sized via IntrinsicSize.Min doesn't track AnimatedVisibility's per-frame animated
    // height, and layering animateContentSize() on top just adds a second, independent animation
    // that isn't guaranteed to stay in step with it — close, but visibly not quite synced.
    // Measuring the content for real every frame and forcing the rail to that exact height has
    // no separate clock to drift: the rail can't help but match, because it's the same number.
    Layout(
        modifier = modifier.fillMaxWidth().testTag(tag = "time_capsule_row_${entry.id}"),
        content = {
            TimelineRail(isCurrent = isCurrent, isLast = isLast)
            RowContent(
                entry = entry,
                changes = changes,
                deltaMillis = deltaMillis,
                expanded = expanded,
                isExpandable = isExpandable,
                chevronRotation = chevronRotation,
                onExpandToggle = onExpandToggle,
                onRestoreClick = onRestoreClick,
                onSummaryOverflowChange = { summaryOverflows = it }
            )
        }
    ) { measurables, constraints ->
        val (rail, content) = measurables
        val contentWidth = (constraints.maxWidth - RailWidth.roundToPx()).coerceAtLeast(
            minimumValue = 0
        )
        val contentPlaceable = content.measure(
            constraints = constraints.copy(minWidth = contentWidth, maxWidth = contentWidth)
        )
        val railPlaceable = rail.measure(
            constraints = Constraints.fixed(
                width = RailWidth.roundToPx(),
                height = contentPlaceable.height
            )
        )
        layout(width = constraints.maxWidth, height = contentPlaceable.height) {
            railPlaceable.placeRelative(x = 0, y = 0)
            contentPlaceable.placeRelative(x = railPlaceable.width, y = 0)
        }
    }
}

@Composable
private fun RowContent(
    entry: Recorded<*>,
    changes: List<FieldChange>,
    deltaMillis: Long?,
    expanded: Boolean,
    isExpandable: Boolean,
    chevronRotation: Float,
    onExpandToggle: () -> Unit,
    onRestoreClick: () -> Unit,
    onSummaryOverflowChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(start = 12.dp, top = 4.dp, bottom = 12.dp)
            // Clipping before the background/clickable draw so the ripple (drawn as part of
            // clickable's indication) is bounded to the same rounded shape as the card behind it,
            // instead of the default rectangular ripple spilling past the rounded corners.
            .clip(shape = CardShape)
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = isExpandable, onClick = onExpandToggle)
            .padding(all = 12.dp)
    ) {
        Text(
            text = summaryText(
                label = entry.label,
                changes = changes,
                expanded = expanded,
                mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                emphasisColor = MaterialTheme.colorScheme.primary
            ),
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            // Guarded on `!expanded`: once expanded, layout no longer overflows (maxLines is
            // unbounded), which would otherwise flip this back to false and hide the chevron.
            onTextLayout = { layout ->
                if (!expanded) {
                    onSummaryOverflowChange(
                        layout.hasVisualOverflow
                    )
                }
            }
        )

        AnimatedVisibility(visible = expanded) {
            // Top padding lives inside the animated content (not as Column spacing) so it
            // shrinks away together with the divider and text as this collapses — an
            // Arrangement.spacedBy gap on the parent stays fixed regardless of this child's
            // animated height, leaving dead space until the child is removed outright.
            Column(modifier = Modifier.padding(top = 10.dp)) {
                HorizontalDivider()
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = highlightedLabel(
                        label = entry.label,
                        changes = changes,
                        highlightBackground = MaterialTheme.colorScheme.primaryContainer,
                        highlightContent = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimestamp(millis = entry.atMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DeltaPill(deltaMillis = deltaMillis)
            if (isExpandable) {
                IconButton(
                    modifier = Modifier
                        .size(size = 32.dp)
                        .testTag(tag = "time_capsule_expand_${entry.id}"),
                    onClick = onExpandToggle
                ) {
                    Icon(
                        modifier = Modifier.graphicsLayer(rotationX = chevronRotation),
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            Spacer(modifier = Modifier.weight(weight = 1f))
            TextButton(onClick = onRestoreClick) {
                Text(text = "Restore", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** The node-and-line spine threading rows into a timeline; [isCurrent] gets a halo. */
@Composable
private fun TimelineRail(isCurrent: Boolean, isLast: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(width = RailWidth).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.padding(top = 8.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(size = NodeHaloSize)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )
            }
            Box(
                modifier = Modifier
                    .size(size = if (isCurrent) CurrentNodeSize else NodeSize)
                    .background(
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .width(width = 2.dp)
                    .background(color = MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun DeltaPill(deltaMillis: Long?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraSmall
            ).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = deltaMillis?.let { "+${formatDelta(millis = it)}" } ?: "initial",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Builds the collapsed/expanded headline: `key: old → new` per change, old struck through and
// muted, new emphasized — falling back to `label` verbatim when there's nothing to diff.
private fun summaryText(
    label: String,
    changes: List<FieldChange>,
    expanded: Boolean,
    mutedColor: Color,
    emphasisColor: Color
): AnnotatedString = buildAnnotatedString {
    if (changes.isEmpty()) {
        append(text = label)
        return@buildAnnotatedString
    }

    val shown = changes.shownChanges(expanded = expanded)
    shown.forEachIndexed { index, change ->
        if (index > 0) append(text = ", ")
        withStyle(style = SpanStyle(color = mutedColor)) {
            append(text = "${change.key}: ")
        }
        withStyle(
            style = SpanStyle(color = mutedColor, textDecoration = TextDecoration.LineThrough)
        ) {
            append(text = change.old)
        }
        withStyle(style = SpanStyle(color = mutedColor)) {
            append(text = " → ")
        }
        withStyle(style = SpanStyle(color = emphasisColor, fontWeight = FontWeight.SemiBold)) {
            append(text = change.new)
        }
    }

    val remaining = changes.size - shown.size
    if (remaining > 0) {
        withStyle(style = SpanStyle(color = mutedColor)) {
            append(text = "  +$remaining more")
        }
    }
}

private fun highlightedLabel(
    label: String,
    changes: List<FieldChange>,
    highlightBackground: Color,
    highlightContent: Color
): AnnotatedString = buildAnnotatedString {
    append(text = label)
    changes.forEach { change ->
        addStyle(
            style = SpanStyle(background = highlightBackground, color = highlightContent),
            start = change.newValueRange.first,
            end = change.newValueRange.last + 1
        )
    }
}

private fun formatDelta(millis: Long): String {
    val duration = millis.milliseconds
    return if (millis < 1000) {
        duration.toString(unit = DurationUnit.MILLISECONDS)
    } else {
        duration.toString(unit = DurationUnit.SECONDS, decimals = 1)
    }
}

// ponytail: third copy of this HH:mm:ss formatter (see AnalyticsLog.kt, ConsoleLog.kt).
// Consolidating needs a new public symbol in devview + metalava/doc churn in 3 modules — more
// expensive than the duplication. Extract if a fourth copy appears.
private fun formatTimestamp(millis: Long): String = Instant
    .fromEpochMilliseconds(
        epochMilliseconds = millis
    ).toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
    .time
    .format(
        format = LocalTime.Format {
            hour()
            char(value = ':')
            minute()
            char(value = ':')
            second()
        }
    )
