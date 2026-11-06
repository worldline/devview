package com.worldline.devview.internal.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldline.devview.core.Module
import com.worldline.devview.core.Section
import com.worldline.devview.core.previewModule
import com.worldline.devview.internal.sectionTileColors
import com.worldline.devview.internal.sectionTileContent
import com.worldline.devview.ui.DevViewGroupedDivider
import com.worldline.devview.ui.devViewGroupedItem

@Composable
internal fun ModuleItem(
    module: Module,
    index: Int,
    count: Int,
    openModule: (Module) -> Unit,
    modifier: Modifier = Modifier
) {
    val (container, content) = sectionTileColors(section = module.section)
    val containerColor by animateColorAsState(
        targetValue = module.containerColor.takeOrElse { container },
        label = "Module tile container"
    )
    val contentColor by animateColorAsState(
        targetValue = module.contentColor.takeOrElse {
            sectionTileContent(container = module.containerColor, defaultContent = content)
        },
        label = "Module tile icon"
    )
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .testTag(tag = "module_item_${module.moduleName}")
            .devViewGroupedItem(index = index, count = count)
            .clickable(role = Role.Button, onClick = { openModule(module) })
            .minimumInteractiveComponentSize()
    ) {
        if (index > 0) {
            DevViewGroupedDivider(modifier = Modifier.padding(start = 56.dp, end = 12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(shape = RoundedCornerShape(size = 8.dp))
                    .background(color = containerColor)
                    .padding(all = 6.dp)
                    .size(size = 20.dp),
                imageVector = module.icon,
                contentDescription = null,
                tint = contentColor
            )
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Text(
                    modifier = Modifier.testTag(tag = "module_name_${module.moduleName}"),
                    text = module.moduleName,
                    style = MaterialTheme.typography.titleSmall
                )
                module.subtitle?.let { subtitle ->
                    Text(
                        modifier = Modifier.testTag(tag = "module_subtitle_${module.moduleName}"),
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(locale = "en")
@Composable
private fun ModuleItemPreview() {
    MaterialTheme {
        Column {
            ModuleItem(
                module = previewModule(name = "Preview Module", section = Section.SETTINGS),
                index = 0,
                count = 2,
                openModule = {}
            )
            ModuleItem(
                module = previewModule(name = "Preview Module 2", section = Section.CUSTOM),
                index = 1,
                count = 2,
                openModule = {}
            )
        }
    }
}
