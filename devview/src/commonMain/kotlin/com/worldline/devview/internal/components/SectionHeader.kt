package com.worldline.devview.internal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldline.devview.Section
import com.worldline.devview.icon

@Composable
internal fun SectionHeader(section: Section, modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            value = LocalContentColor provides contentColorFor(backgroundColor = backgroundColor)
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null
            )

            Text(
                text = when (section) {
                    Section.SETTINGS -> "Settings"
                    Section.FEATURES -> "Features"
                    Section.LOGGING -> "Logging"
                    Section.APP_SPECIFIC -> "App Specific"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
