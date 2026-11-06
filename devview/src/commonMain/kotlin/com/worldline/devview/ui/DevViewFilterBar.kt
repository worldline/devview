package com.worldline.devview.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pinned search field with a collapsible filter panel and visible active-filter count.
 * The caller owns query and filter state and handles IME insets around this bar.
 *
 * @param query Current search text.
 * @param onQueryChange Receives edited search text.
 * @param placeholder Search hint, also used as the field's accessible name.
 * @param activeFilterCount Active non-search filters; must not be negative.
 * @param expanded Whether the panel is visible.
 * @param onExpandedChange Receives the requested panel visibility.
 * @param onClearFilters Resets the panel's filters.
 * @param modifier Modifier applied to the bar.
 * @param bottomPadding Bottom system inset from Module.registerContent.
 * @param panel Module-specific filter controls. The bar supplies the Clear filters action.
 */
@Composable
public fun DevViewFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    activeFilterCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    panel: @Composable () -> Unit
) {
    require(value = activeFilterCount >= 0) { "Active filter count must not be negative" }
    Column(modifier = modifier.padding(bottom = bottomPadding)) {
        HorizontalDivider()
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(state = rememberScrollState())
            ) {
                TextButton(
                    onClick = onClearFilters,
                    enabled = activeFilterCount > 0,
                    modifier = Modifier.align(alignment = Alignment.End)
                ) {
                    Text(text = "Clear filters")
                }
                panel()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(weight = 1f),
                label = { Text(text = placeholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                singleLine = true
            )
            BadgedBox(
                modifier = Modifier.padding(horizontal = 8.dp),
                badge = {
                    if (activeFilterCount > 0) {
                        Badge(modifier = Modifier.clearAndSetSemantics {}) {
                            Text(text = activeFilterCount.toString())
                        }
                    }
                }
            ) {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                        contentDescription = if (expanded) {
                            "Hide filters, $activeFilterCount active"
                        } else {
                            "Show filters, $activeFilterCount active"
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DevViewFilterBarPreview() {
    MaterialTheme {
        DevViewFilterBar(
            query = "",
            onQueryChange = {},
            placeholder = "Search operations",
            activeFilterCount = 1,
            expanded = true,
            onExpandedChange = {},
            onClearFilters = {}
        ) {
            Text(text = "Mocked", modifier = Modifier.padding(all = 16.dp))
        }
    }
}
