package com.worldline.devview.consolelogger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.worldline.devview.consolelogger.components.ConsoleLogRow
import com.worldline.devview.consolelogger.components.ConsoleNoiseFilterPanel
import com.worldline.devview.consolelogger.model.ConsoleLog
import com.worldline.devview.consolelogger.model.LogLevel
import com.worldline.devview.consolelogger.preview.ConsoleLogListPreviewParameterProvider
import com.worldline.devview.consolelogger.theme.LocalLogColorScheme
import com.worldline.devview.consolelogger.theme.LogColorScheme
import com.worldline.devview.consolelogger.theme.rememberLogColorScheme
import com.worldline.devview.ui.DevViewEmptyState
import com.worldline.devview.ui.DevViewFilterBar
import com.worldline.devview.ui.DevViewSectionHeader
import com.worldline.devview.utils.rememberDataStore
import okio.IOException

/**
 * Module-owned console history with persisted noise exclusions and newest-first rows.
 * Scrolling or expanding pauses follow mode; search and reading state are saveable.
 */
@Composable
internal fun ConsoleScreen(
    dataStore: DataStore<Preferences>,
    defaults: List<String>,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    logs: List<ConsoleLog> = ConsoleLogger.logs
) {
    val repository = remember(key1 = dataStore, key2 = defaults) {
        ConsoleNoiseRepository(dataStore = dataStore, defaults = defaults)
    }
    var overrides by remember(key1 = repository) { mutableStateOf(value = ConsoleNoiseOverrides()) }
    var loaded by remember(key1 = repository) { mutableStateOf(value = false) }
    var readFailed by remember(key1 = repository) { mutableStateOf(value = false) }
    var writeFailed by remember(key1 = repository) { mutableStateOf(value = false) }
    var pending by remember(
        key1 = repository
    ) { mutableStateOf(value = emptyList<ConsoleNoiseChange>()) }
    var retry by remember(key1 = repository) { mutableIntStateOf(value = 0) }
    LaunchedEffect(key1 = repository, key2 = retry) {
        loaded = false
        readFailed = false
        try {
            repository.overrides.collect {
                overrides = it
                loaded = true
            }
        } catch (_: IOException) {
            readFailed = true
        }
    }
    // Serialize writes; a failed edit stays first until explicitly retried.
    LaunchedEffect(key1 = repository, key2 = pending.isNotEmpty(), key3 = retry) {
        while (pending.isNotEmpty()) {
            try {
                repository.update(change = pending.first())
                writeFailed = false
                pending = pending.drop(n = 1)
            } catch (_: IOException) {
                writeFailed = true
                break
            }
        }
    }
    val noiseControlsEnabled = loaded && !readFailed && !writeFailed && pending.isEmpty()
    val noiseLoading = !loaded && !readFailed
    val noiseError = when {
        readFailed -> "Could not load noise filters. Your saved rules have not been changed."
        writeFailed -> "Could not save noise filters. Your last saved rules are still in use."
        else -> null
    }
    val onNoiseChange: (ConsoleNoiseChange) -> Unit = { pending = pending + it }
    val onRetryNoise: () -> Unit = { retry++ }
    val noiseOverrides = overrides
    val colors = rememberLogColorScheme()
    var query by rememberSaveable { mutableStateOf(value = "") }
    var visibleLevels by rememberSaveable {
        mutableStateOf(value = LogLevel.entries.map { it.name })
    }
    var panelExpanded by rememberSaveable { mutableStateOf(value = false) }
    var expandedIds by rememberSaveable { mutableStateOf(value = emptyList<String>()) }
    var following by rememberSaveable { mutableStateOf(value = true) }
    val levels = LogLevel.entries.filter { it.name in visibleLevels }.toSet()
    val excludedPatterns = noiseOverrides.enabledPatterns(defaults = defaults)
    val matching = filterConsoleLogs(
        logs = logs,
        query = query,
        levels = levels,
        excludedPatterns = excludedPatterns
    )
    val filtered = matching.asReversed()
    val keys = consoleRowKeys(logs = matching).asReversed()
    val sourceKeys = consoleRowKeys(logs = logs).toSet()
    LaunchedEffect(key1 = sourceKeys) {
        expandedIds = expandedIds.filter { it in sourceKeys }
    }
    val hiddenCount = LogLevel.entries.size - levels.size + excludedPatterns.size
    val clearFilters = {
        visibleLevels = LogLevel.entries.map { it.name }
        onNoiseChange(ConsoleNoiseChange.DisableAll)
    }
    val listState = rememberLazyListState()
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) following = false
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(key1 = keys.firstOrNull(), key2 = following) {
        if (following && filtered.isNotEmpty()) {
            listState.scrollToItem(index = 0)
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        floatingActionButton = {
            AnimatedVisibility(visible = !following) {
                FloatingActionButton(
                    onClick = { following = true },
                    modifier = Modifier.testTag(tag = "console_resume_follow")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Resume following latest log"
                    )
                }
            }
        },
        bottomBar = {
            DevViewFilterBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Filter by tag or message",
                activeFilterCount = hiddenCount,
                expanded = panelExpanded,
                onExpandedChange = { panelExpanded = it },
                onClearFilters = clearFilters,
                clearFiltersEnabled = noiseControlsEnabled,
                bottomPadding = bottomPadding,
                modifier = Modifier.testTag(tag = "console_filter_bar")
            ) {
                DevViewSectionHeader(text = "Visible levels")
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    LogLevel.entries.forEach { level ->
                        FilterChip(
                            selected = level.name in visibleLevels,
                            onClick = {
                                visibleLevels = if (level.name in visibleLevels) {
                                    visibleLevels - level.name
                                } else {
                                    visibleLevels + level.name
                                }
                            },
                            label = { Text(text = level.displayName) },
                            leadingIcon = {
                                Surface(
                                    color = colors[level].label,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.size(size = 8.dp)
                                ) {}
                            },
                            modifier = Modifier.testTag(tag = "console_level_chip_${level.name}")
                        )
                    }
                }
                ConsoleNoiseFilterPanel(
                    defaults = defaults,
                    overrides = noiseOverrides,
                    onChange = onNoiseChange,
                    hiddenEntries = logs.count {
                        it.matchesConsoleNoise(
                            patterns = excludedPatterns
                        )
                    },
                    controlsEnabled = noiseControlsEnabled,
                    loading = noiseLoading,
                    error = noiseError,
                    onRetry = onRetryNoise
                )
            }
        }
    ) { padding ->
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues = padding),
                contentAlignment = Alignment.Center
            ) {
                DevViewEmptyState(
                    icon = Icons.Rounded.Terminal,
                    title = if (logs.isEmpty()) "No logs yet" else "No logs match your filters",
                    message = if (logs.isEmpty()) {
                        "Captured console output will appear here."
                    } else {
                        "Change your search or clear the level and noise filters."
                    },
                    modifier = Modifier.testTag(tag = "console_empty_state"),
                    action = if (query.isNotEmpty() || hiddenCount > 0) {
                        {
                            TextButton(
                                enabled = noiseControlsEnabled,
                                onClick = {
                                    query = ""
                                    clearFilters()
                                }
                            ) {
                                Text(text = "Clear search and filters")
                            }
                        }
                    } else {
                        null
                    }
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding)
                    .testTag(tag = "console_log_list")
                    .nestedScroll(connection = scrollConnection)
            ) {
                itemsIndexed(items = filtered, key = { index, _ -> keys[index] }) { index, log ->
                    ConsoleLogRow(
                        log = log,
                        colors = colors,
                        expanded = keys[index] in expandedIds,
                        onExpandedChange = {
                            following = false
                            expandedIds = if (keys[index] in expandedIds) {
                                expandedIds - keys[index]
                            } else {
                                expandedIds + keys[index]
                            }
                        },
                        modifier = Modifier.testTag(tag = "console_log_item_${log.id}")
                    )
                    if (index != filtered.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConsoleScreenPreview(
    @PreviewParameter(ConsoleLogListPreviewParameterProvider::class) logs: List<ConsoleLog>
) {
    MaterialTheme {
        CompositionLocalProvider(
            values = arrayOf(LocalLogColorScheme provides LogColorScheme.Light)
        ) {
            ConsoleScreen(
                logs = logs,
                defaults = Console.DEFAULT_EXCLUDED_PATTERNS,
                dataStore = rememberDataStore(dataStoreName = "console_preview.preferences_pb")
            )
        }
    }
}
