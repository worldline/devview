package com.worldline.devview.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun createPlainTextClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(
    text = text
)
