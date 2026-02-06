package com.worldline.devview.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

public enum class Section {
    SETTINGS,
    FEATURES,
    LOGGING,
    CUSTOM
}

public val Section.icon: ImageVector
    get() = when (this) {
        Section.SETTINGS -> Icons.Rounded.Settings
        Section.FEATURES -> Icons.Rounded.DeveloperMode
        Section.LOGGING -> Icons.Rounded.FormatListNumbered
        Section.CUSTOM -> Icons.Rounded.Extension
    }
