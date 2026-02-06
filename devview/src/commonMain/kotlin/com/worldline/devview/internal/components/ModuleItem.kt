package com.worldline.devview.internal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.previewModule

/**
 * Internal composable that displays a single module as a card in the home screen.
 *
 * This component renders a tappable Material card containing:
 * - A circular icon with customizable colors
 * - Module name
 * - Optional subtitle
 * - Conditional divider (for middle/last positions)
 *
 * The card shape adapts based on its position within a section group to create
 * a visually connected appearance.
 *
 * ## Layout Structure
 * ```
 * Card (shape varies by position)
 * └── Column
 *     ├── HorizontalDivider (if hasDivider)
 *     └── Row
 *         ├── Icon (circular background)
 *         └── Column (module info)
 *             ├── Text (module name)
 *             └── Text (subtitle, optional)
 * ```
 *
 * ## Shape Adaptation
 * The card shape changes based on [position] to create grouped appearance:
 * - [ModulePosition.SINGLE]: All corners rounded (standalone module)
 * - [ModulePosition.FIRST]: Top rounded, bottom squared (start of group)
 * - [ModulePosition.MIDDLE]: All squared with top divider (middle of group)
 * - [ModulePosition.LAST]: Bottom rounded, top squared with divider (end of group)
 *
 * ## Icon Styling
 * - Background: [Module.containerColor] in a circle
 * - Icon: [Module.icon] tinted with [Module.contentColor]
 * - Size: 20dp icon in 32dp circular container
 *
 * ## Usage
 * This is an internal component used by the [HomeScreen][com.worldline.devview.HomeScreen]. Not intended for direct use.
 *
 * @param module The module to display.
 * @param position The position of this module within its section group.
 * @param openModule Callback invoked when the module card is tapped.
 * @param modifier Optional [Modifier] to apply to the card.
 *
 * @see ModulePosition
 * @see Module
 */
@Composable
internal fun ModuleItem(
    module: Module,
    position: ModulePosition,
    openModule: (Module) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseShape = MaterialTheme.shapes.medium

    val shape = when (position) {
        ModulePosition.SINGLE -> baseShape
        ModulePosition.FIRST -> baseShape.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize
        )

        ModulePosition.MIDDLE -> RectangleShape
        ModulePosition.LAST -> baseShape.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize
        )
    }

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp),
        onClick = { openModule(module) },
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .padding(
                    horizontal = 12.dp
                )
        ) {
            if (position.hasDivider) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(start = 44.dp) // 20 (icon size) + 12 (padding end) + 12 (padding horizontal)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(shape = CircleShape)
                        .background(color = module.containerColor)
                        .padding(all = 6.dp)
                        .size(size = 20.dp),
                    imageVector = module.icon,
                    contentDescription = null,
                    tint = module.contentColor
                )
                Column(
                    modifier = Modifier
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${module::class.simpleName}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Normal
                        )
                    )
                    module.subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Light
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ModuleItemPreview() {
    Column {
        ModuleItem(
            module = previewModule(
                name = "Preview Module",
                section = Section.SETTINGS
            ),
            position = ModulePosition.FIRST,
            openModule = {}
        )
        ModuleItem(
            module = previewModule(
                name = "Preview Module 2",
                section = Section.CUSTOM
            ),
            position = ModulePosition.LAST,
            openModule = {}
        )
    }
}

/**
 * Defines the position of a module within a section group.
 *
 * This enum is used to determine the visual styling of module cards in the
 * home screen, specifically:
 * - Card shape (which corners are rounded vs squared)
 * - Whether to show a divider at the top
 *
 * The positioning creates a visually grouped appearance for modules within
 * the same section.
 *
 * ## Visual Effect
 * ```
 * ┌─────────┐  SINGLE (or FIRST)
 * │ Module  │  Rounded top corners
 * ├─────────┤
 * │ Module  │  MIDDLE - Squared, has divider
 * ├─────────┤
 * │ Module  │  LAST - Rounded bottom, has divider
 * └─────────┘
 * ```
 *
 * ## Usage
 * ```kotlin
 * ModuleItem(
 *     module = myModule,
 *     position = when {
 *         modulesInSection.size == 1 -> ModulePosition.SINGLE
 *         index == 0 -> ModulePosition.FIRST
 *         index == modulesInSection.lastIndex -> ModulePosition.LAST
 *         else -> ModulePosition.MIDDLE
 *     },
 *     openModule = { }
 * )
 * ```
 *
 * @property hasDivider Whether this position requires a top divider line.
 *
 * @see ModuleItem
 */
public enum class ModulePosition(public val hasDivider: Boolean) {
    /**
     * Single module in a section (not part of a group).
     *
     * Visual: All corners rounded, no divider.
     */
    SINGLE(false),

    /**
     * First module in a section group.
     *
     * Visual: Top corners rounded, bottom corners squared, no divider.
     */
    FIRST(false),

    /**
     * Middle module in a section group.
     *
     * Visual: All corners squared, has top divider.
     */
    MIDDLE(true),

    /**
     * Last module in a section group.
     *
     * Visual: Top corners squared, bottom corners rounded, has top divider.
     */
    LAST(true)
}
