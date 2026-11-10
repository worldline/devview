package com.worldline.devview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Clips and fills an item in a connected group using the host theme's large outer corners.
 * Apply outside the item's click or selection modifier so its indication is clipped.
 *
 * @param index Zero-based position in the group.
 * @param count Number of items in the group; must be positive and include [index].
 * @return Modifier with the grouped shape and surface container background.
 */
@Composable
public fun Modifier.devViewGroupedItem(index: Int, count: Int): Modifier {
    require(value = count > 0 && index in 0 until count) { "Invalid grouped item position" }
    val shape = MaterialTheme.shapes.large.copy(
        topStart = if (index == 0) MaterialTheme.shapes.large.topStart else CornerSize(size = 4.dp),
        topEnd = if (index == 0) MaterialTheme.shapes.large.topEnd else CornerSize(size = 4.dp),
        bottomStart = if (index ==
            count - 1
        ) {
            MaterialTheme.shapes.large.bottomStart
        } else {
            CornerSize(
                size = 4.dp
            )
        },
        bottomEnd = if (index == count - 1) {
            MaterialTheme.shapes.large.bottomEnd
        } else {
            CornerSize(
                size = 4.dp
            )
        }
    )
    return clip(shape = shape).background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
}

/**
 * Divider between grouped items. Use [modifier] padding to align it with the group's text.
 *
 * @param modifier Modifier applied to the divider.
 */
@Composable
public fun DevViewGroupedDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier)
}

@Preview
@Composable
private fun DevViewGroupedItemPreview() {
    MaterialTheme {
        Column {
            Text(
                text = "First",
                modifier = Modifier.devViewGroupedItem(index = 0, count = 2).padding(all = 16.dp)
            )
            DevViewGroupedDivider()
            Text(
                text = "Last",
                modifier = Modifier.devViewGroupedItem(index = 1, count = 2).padding(all = 16.dp)
            )
        }
    }
}
