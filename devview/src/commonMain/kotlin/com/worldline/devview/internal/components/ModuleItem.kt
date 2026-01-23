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
import com.worldline.devview.Module

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
                        .padding(start = 44.dp) // 24 (icon size) + 12 (padding end) + 8 (padding horizontal)
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
            module = Module.FeatureFlip,
            position = ModulePosition.FIRST,
            openModule = {}
        )
        ModuleItem(
            module = Module.Console,
            position = ModulePosition.LAST,
            openModule = {}
        )
    }
}

public enum class ModulePosition(public val hasDivider: Boolean) {
    SINGLE(false),
    FIRST(false),
    MIDDLE(true),
    LAST(true)
}
