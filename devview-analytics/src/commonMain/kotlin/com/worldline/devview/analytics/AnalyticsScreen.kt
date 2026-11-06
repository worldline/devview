package com.worldline.devview.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldline.devview.analytics.components.AnalyticsLogItem
import com.worldline.devview.analytics.components.HighlightedAnalyticsLogsHeader
import com.worldline.devview.analytics.model.AnalyticsLog
import com.worldline.devview.analytics.model.AnalyticsLogType
import com.worldline.devview.analytics.model.HighlightedAnalyticsLog
import com.worldline.devview.ui.DevViewEmptyState
import com.worldline.devview.ui.DevViewFilterBar
import com.worldline.devview.ui.DevViewSectionHeader
import kotlin.time.Clock
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Module-owned analytics history; event details are separate registered destinations.
 * Search and the inspected event anchor survive saved-state restoration and navigation.
 */
@Composable
internal fun AnalyticsScreen(
    highlightedAnalyticsLogTypes: PersistentList<AnalyticsLogType>,
    onEventClick: (AnalyticsLog) -> Unit,
    modifier: Modifier = Modifier,
    logs: List<AnalyticsLog> = AnalyticsLogger.logs,
    bottomPadding: Dp = 0.dp
) {
    val analytics = logs
    var query by rememberSaveable { mutableStateOf(value = "") }
    var selectedCategories by rememberSaveable { mutableStateOf(value = emptyList<String>()) }
    var range by rememberSaveable { mutableStateOf(value = TimeRange.All) }
    var expanded by rememberSaveable { mutableStateOf(value = false) }
    val now by produceState(initialValue = Clock.System.now().toEpochMilliseconds(), key1 = range) {
        value = Clock.System.now().toEpochMilliseconds()
        while (range != TimeRange.All) {
            delay(timeMillis = 5_000)
            value = Clock.System.now().toEpochMilliseconds()
        }
    }
    val matching = filterLogs(
        logs = analytics,
        query = query,
        categories = selectedCategories.toSet(),
        range = range,
        now = now
    )
    val filtered = matching.asReversed()
    val rowKeys = eventRowKeys(logs = matching).asReversed()
    val categories = analytics.map { it.type.category }.distinctBy { it.displayName }
    val count = selectedCategories.size + if (range == TimeRange.All) 0 else 1
    val clearFilters = {
        selectedCategories = emptyList()
        range = TimeRange.All
    }
    var anchorKey by rememberSaveable { mutableStateOf<String?>(value = null) }
    var anchorOffset by rememberSaveable { mutableIntStateOf(value = 0) }
    val listState = remember {
        val rowIndex = rowKeys.indexOf(element = anchorKey)
        LazyListState(
            firstVisibleItemIndex = if (rowIndex >= 0) rowIndex + 1 else 0,
            firstVisibleItemScrollOffset =
                if (rowIndex >= 0 || anchorKey == "highlighted_logs_header") anchorOffset else 0
        )
    }
    val currentKeys by rememberUpdatedState(newValue = rowKeys)
    val readAnchor = {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull {
                it.index == listState.firstVisibleItemIndex
            }?.let { item ->
                (item.key as? String)
                    ?.takeIf {
                        currentKeys.isNotEmpty() &&
                            (it == "highlighted_logs_header" || it in currentKeys)
                    }?.let { it to listState.firstVisibleItemScrollOffset }
            }
    }
    LaunchedEffect(key1 = listState) {
        snapshotFlow { readAnchor() }.collect { position ->
            if (position != null) {
                anchorKey = position.first
                anchorOffset = position.second
            }
        }
    }
    val scope = rememberCoroutineScope()
    val summaries = buildList {
        add(element = HighlightedAnalyticsLog.Total(count = filtered.size))
        highlightedAnalyticsLogTypes.forEach { type ->
            add(
                element = HighlightedAnalyticsLog.Type(
                    type = type,
                    count = filtered.count { it.type == type }
                )
            )
        }
    }.toPersistentList()

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        floatingActionButton = {
            AnimatedVisibility(visible = listState.firstVisibleItemIndex > 1) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(index = 0) } }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Latest events"
                    )
                }
            }
        },
        bottomBar = {
            DevViewFilterBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Filter by tag or screen",
                activeFilterCount = count,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onClearFilters = clearFilters,
                bottomPadding = bottomPadding,
                modifier = Modifier.testTag(tag = "analytics_filter_bar")
            ) {
                DevViewSectionHeader(text = "Time range")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    TimeRange.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = range == option,
                            onClick = { range = option },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = TimeRange.entries.size
                            ),
                            modifier = Modifier.testTag(tag = "time_range_${option.label}"),
                            label = { Text(text = option.label) }
                        )
                    }
                }
                DevViewSectionHeader(text = "Categories")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(items = categories, key = { it.displayName }) { category ->
                        FilterChip(
                            selected = category.displayName in selectedCategories,
                            onClick = {
                                selectedCategories =
                                    if (category.displayName in selectedCategories) {
                                        selectedCategories - category.displayName
                                    } else {
                                        selectedCategories + category.displayName
                                    }
                            },
                            label = { Text(text = category.displayName) },
                            modifier = Modifier.testTag(
                                tag = "category_chip_${category.displayName}"
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (filtered.isEmpty()) {
            Column(modifier = Modifier.padding(paddingValues = padding).fillMaxSize()) {
                HighlightedAnalyticsLogsHeader(highlightedAnalyticsLogs = summaries)
                Box(
                    modifier = Modifier.weight(weight = 1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DevViewEmptyState(
                        icon = Icons.Rounded.Insights,
                        title = if (analytics.isEmpty()) {
                            "No logs yet"
                        } else {
                            "No logs match your filters"
                        },
                        message = if (analytics.isEmpty()) {
                            "Record events with AnalyticsLogger.log()."
                        } else {
                            "Change your search or clear the filters."
                        },
                        modifier = Modifier.testTag(tag = "empty_state_message"),
                        action = if (query.isNotEmpty() || count > 0) {
                            {
                                TextButton(
                                    onClick = {
                                        query = ""
                                        clearFilters()
                                    }
                                ) { Text(text = "Clear search and filters") }
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding)
                    .testTag(tag = "analytics_event_list")
            ) {
                stickyHeader(key = "highlighted_logs_header") {
                    Surface { HighlightedAnalyticsLogsHeader(highlightedAnalyticsLogs = summaries) }
                }
                itemsIndexed(items = filtered, key = { index, _ -> rowKeys[index] }) { index, log ->
                    Column(
                        modifier = Modifier
                            .animateItem()
                            .testTag(
                                tag = "analytics_log_item_${log.tag}"
                            ).clickable(
                                role = Role.Button,
                                onClickLabel = "Open event details",
                                onClick = {
                                    val clickedRow = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == rowKeys[index] }
                                    if (listState.firstVisibleItemIndex == 0 &&
                                        clickedRow != null
                                    ) {
                                        // Anchor the inspected content, not the stationary summary header.
                                        anchorKey = rowKeys[index]
                                        anchorOffset = -clickedRow.offset
                                    } else {
                                        readAnchor()?.let { position ->
                                            anchorKey = position.first
                                            anchorOffset = position.second
                                        }
                                    }
                                    onEventClick(log)
                                }
                            )
                    ) {
                        AnalyticsLogItem(analyticsLog = log)
                        if (index != filtered.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AnalyticsLogScreenPreview() {
    MaterialTheme {
        AnalyticsScreen(
            highlightedAnalyticsLogTypes = persistentListOf(),
            onEventClick = {},
            logs = emptyList()
        )
    }
}
