package com.worldline.devview.ui

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

internal actual fun createPlainTextClipEntry(text: String): ClipEntry =
    ClipEntry(clipData = ClipData.newPlainText("plain text", text))
