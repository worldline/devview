package com.worldline.devview.core

import androidx.compose.ui.graphics.vector.ImageVector

public data class ModuleDestinationAction(
    val icon: ImageVector,
    val action: () -> Unit,
    val popup: ModuleDestinationActionPopup? = null
)
