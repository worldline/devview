package com.worldline.devview.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.components.AnalyticsLogItem
import com.worldline.devview.analytics.components.HighlightedAnalyticsLogsHeader
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogCategory
import com.worldline.devview.analytics.model.AnalyticsLogType
import com.worldline.devview.analytics.model.HighlightedAnalyticsLog
import com.worldline.devview.analytics.preview.AnalyticsLogListPreviewParameterProvider
import kotlin.time.Clock
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

/**
 * Main UI component for displaying analytics logs in a tabular format.
 *
 * This composable renders a scrollable list of analytics events with a sticky header
 * displaying highlighted statistics, followed by a filterable and scrollable log list.
 * The bottom bar provides three complementary filtering mechanisms: a category chip row,
 * a time range segmented button, and a text field for filtering by tag or screen class.
 *
 * ## Features
 * - Sticky header with highlighted analytics log statistics
 * - Formatted timestamps (HH:mm:ss)
 * - Category colour-coded chips per log entry
 * - Animated item transitions when filters change
 * - Scroll-to-top FAB that appears when the latest entry is out of view
 * - Bottom bar with category filter chips, time range selector, and text filter field
 * - Empty state message when no logs are present or no logs match the current filter
 *
 * ## Usage
 *
 * ### Basic Usage
 * ```kotlin
 * CompositionLocalProvider(LocalAnalytics provides AnalyticsLogger.logs) {
 *     AnalyticsScreen(
 *         highlightedAnalyticsLogTypes = persistentListOf(
 *             AnalyticsLogCategory.Action.Click,
 *             AnalyticsLogCategory.Performance.Error
 *         ),
 *         modifier = Modifier.fillMaxSize()
 *     )
 * }
 * ```
 *
 * @param highlightedAnalyticsLogTypes The list of [AnalyticsLogType] entries to highlight in the
 *        sticky header. Each type is displayed as a summary card showing its event count alongside
 *        a total count card. Typically contains two or three types of interest (e.g. clicks, errors).
 * @param modifier Modifier to be applied to the root [Scaffold].
 * @param bottomPadding Additional bottom padding applied to the bottom bar content, used to avoid
 *        overlap with system UI elements when this screen is nested inside another [Scaffold].
 *
 * @see AnalyticsLogger
 * @see LocalAnalytics
 * @see AnalyticsLog
 * @see HighlightedAnalyticsLogsHeader
 */
@Composable
public fun AnalyticsScreen(
    highlightedAnalyticsLogTypes: PersistentList<AnalyticsLogType>,
    modifier: Modifier = Modifier,
    bottomPadding: Dp
) {
    val analytics = LocalAnalytics.current

    val spacing = 8.dp

    var filterQuery by remember { mutableStateOf(value = "") }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTopFab by remember(key1 = lazyListState) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val headerItem =
                layoutInfo.visibleItemsInfo.firstOrNull { it.key == "highlighted_logs_header" }
            val firstLogItem =
                layoutInfo.visibleItemsInfo.firstOrNull { it.key != "highlighted_logs_header" }
            val headerBottom = (headerItem?.offset ?: 0) + (headerItem?.size ?: 0)
            firstLogItem == null || firstLogItem.offset < headerBottom
        }
    }

    var selectedCategories by remember { mutableStateOf(value = emptySet<AnalyticsLogCategory>()) }
    var selectedTimeRange by remember { mutableStateOf(value = TimeRange.All) }
    var filtersExpanded by remember { mutableStateOf(value = false) }

    val iconRotation by animateFloatAsState(
        targetValue = if (filtersExpanded) 180f else 0f
    )

    val availableCategories by remember(key1 = analytics) {
        derivedStateOf {
            analytics.map { it.type.category }.distinct()
        }
    }

    val filteredAnalytics by remember(
        analytics,
        filterQuery,
        selectedCategories,
        selectedTimeRange
    ) {
        derivedStateOf {
            val now = Clock.System.now().toEpochMilliseconds()
            analytics.filter {
                val matchesQuery = filterQuery.isBlank() ||
                    it.tag.contains(other = filterQuery, ignoreCase = true) ||
                    it.screenClass.contains(other = filterQuery, ignoreCase = true)
                val matchesCategory = selectedCategories.isEmpty() ||
                    it.type.category in selectedCategories
                val matchesTimeRange = selectedTimeRange.durationMillis?.let { duration ->
                    now - it.timestamp <= duration
                } ?: true
                matchesQuery && matchesCategory && matchesTimeRange
            }
        }
    }

    val highlightedAnalyticsLogs: PersistentList<HighlightedAnalyticsLog> by remember(
        key1 = analytics,
        key2 = highlightedAnalyticsLogTypes
    ) {
        derivedStateOf {
            buildList {
                add(element = HighlightedAnalyticsLog.Total(count = analytics.size))
                highlightedAnalyticsLogTypes.mapTo(destination = this) { type ->
                    HighlightedAnalyticsLog.Type(
                        type = type,
                        count = analytics.count { it.type == type }
                    )
                }
            }.toPersistentList()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTopFab,
                enter = slideInHorizontally(initialOffsetX = { it * 2 }),
                exit = slideOutHorizontally(targetOffsetX = { it * 2 })
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(index = 0)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to latest"
                    )
                }
            }
        },
        bottomBar = {
            Surface {
                Column {
                    AnimatedVisibility(visible = filtersExpanded) {
                        Column {
                            HorizontalDivider()
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                TimeRange.entries.forEachIndexed { index, timeRange ->
                                    SegmentedButton(
                                        selected = selectedTimeRange == timeRange,
                                        onClick = { selectedTimeRange = timeRange },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = TimeRange.entries.size
                                        ),
                                        label = { Text(text = timeRange.label) }
                                    )
                                }
                            }
                            HorizontalDivider()
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(items = availableCategories) { category ->
                                    FilterChip(
                                        selected = category in selectedCategories,
                                        onClick = {
                                            selectedCategories =
                                                if (category in selectedCategories) {
                                                    selectedCategories - category
                                                } else {
                                                    selectedCategories + category
                                                }
                                        },
                                        label = { Text(text = category.displayName) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(
                                                    size = FilterChipDefaults.IconSize
                                                )
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = category.containerColor,
                                            selectedLabelColor = category.contentColor,
                                            selectedLeadingIconColor = category.contentColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .height(intrinsicSize = IntrinsicSize.Min)
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(weight = 1f)
                                .padding(vertical = 8.dp)
                                .padding(bottom = bottomPadding),
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = { Text(text = "Filter by tag or screen...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                AnimatedVisibility(visible = filterQuery.isNotEmpty()) {
                                    IconButton(onClick = { filterQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear filter"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        IconButton(
                            modifier = Modifier
                                .padding(bottom = bottomPadding),
                            onClick = { filtersExpanded = !filtersExpanded }
                        ) {
                            Icon(
                                modifier = Modifier
                                    .graphicsLayer(
                                        rotationX = iconRotation
                                    ),
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Expand filter"
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(space = spacing)
        ) {
            stickyHeader(
                key = "highlighted_logs_header"
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    HighlightedAnalyticsLogsHeader(
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                        highlightedAnalyticsLogs = highlightedAnalyticsLogs
                    )
                }
            }
            if (filteredAnalytics.isEmpty()) {
                item(key = "empty_state") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                            .wrapContentWidth(align = Alignment.CenterHorizontally),
                        text = if (filterQuery.isBlank()) {
                            "Analytics logs will appear here"
                        } else {
                            "No logs match your filter"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(
                items = filteredAnalytics,
                key = { _, log -> log.hashCode() }
            ) { index, log ->
                Column(
                    modifier = Modifier
                        .animateItem()
                ) {
                    AnalyticsLogItem(
                        analyticsLog = log
                    )
                    if (index != filteredAnalytics.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(top = spacing)
                        )
                    }
                }
            }
            item(
                key = "bottom_spacer"
            ) {
                Spacer(
                    modifier = Modifier
                        .height(
                            height = paddingValues.calculateBottomPadding()
                        )
                )
            }
        }
    }
}

/**
 * Defines preset time ranges for filtering analytics logs based on their timestamp.
 *
 * Each enum entry represents a specific time range, with a user-friendly label and an optional
 * duration in milliseconds.
 * The `durationMillis` value is used to calculate the cutoff timestamp for filtering logs relative
 * to the current time. A `null` value for `durationMillis` indicates that there is no time limit
 * (i.e., all logs should be included regardless of their timestamp).
 *
 * ## Enum Entries
 * - `All`: Represents all time, with no filtering based on timestamp.
 * - `Last5Min`: Represents the last 5 minutes, filtering logs that occurred within the last 5 minutes.
 * - `Last15Min`: Represents the last 15 minutes, filtering logs that occurred within the last 15 minutes.
 * - `Last30Min`: Represents the last 30 minutes, filtering logs that occurred within the last 30 minutes.
 *
 * ## Usage
 * The `TimeRange` enum can be used in the `AnalyticsScreen` composable to provide users with predefined
 * time range options for filtering analytics logs. When a user selects a time range, the corresponding
 * `durationMillis` value can be used to calculate the cutoff timestamp and filter the logs accordingly
 * ```kotlin
 * val selectedTimeRange: TimeRange = TimeRange.Last15Min
 * val cutoffTimestamp = Clock.System.now().toEpochMilliseconds() - (selectedTimeRange.durationMillis ?: 0L)
 * val filteredLogs = analyticsLogs.filter { it.timestamp >= cutoffTimestamp }
 * ```
 *
 * @property label A user-friendly label for the time range, used in the UI (e.g., "All", "5m", "15m", "30m").
 * @property durationMillis The duration of the time range in milliseconds, used for filtering logs
 * based on their timestamp. A `null` value indicates no time limit (include all logs).
 * @see AnalyticsScreen
 */
private enum class TimeRange(val label: String, val durationMillis: Long?) {
    All(label = "All", durationMillis = null),
    Last5Min(label = "5m", durationMillis = 5 * 60 * 1000L),
    Last15Min(label = "15m", durationMillis = 15 * 60 * 1000L),
    Last30Min(label = "30m", durationMillis = 30 * 60 * 1000L)
}

@Preview(locale = "en")
@Composable
private fun AnalyticsLogScreenPreview(
    @PreviewParameter(
        AnalyticsLogListPreviewParameterProvider::class
    ) analyticsLogs: List<AnalyticsLog>
) {
    MaterialTheme {
        Scaffold {
            CompositionLocalProvider(value = LocalAnalytics provides analyticsLogs) {
                AnalyticsScreen(
                    highlightedAnalyticsLogTypes = persistentListOf(
                        AnalyticsLogCategory.Action.Click,
                        AnalyticsLogCategory.Performance.Error
                    ),
                    modifier = Modifier
                        .padding(paddingValues = it),
                    bottomPadding = it.calculateBottomPadding()
                )
            }
        }
    }
}
