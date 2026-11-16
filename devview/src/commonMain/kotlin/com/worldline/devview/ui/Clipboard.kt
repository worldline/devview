package com.worldline.devview.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

/**
 * Remembers a callback that copies plain text to the system clipboard asynchronously.
 * Pending copies are cancelled when the calling composition leaves the screen.
 *
 * @return Callback accepting the text to copy.
 */
@Composable
public fun rememberCopyToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember<(String) -> Unit>(key1 = clipboard, key2 = scope) {
        { text ->
            scope.launch {
                clipboard.setClipEntry(
                    clipEntry = createPlainTextClipEntry(text = text)
                )
            }
        }
    }
}

internal expect fun createPlainTextClipEntry(text: String): ClipEntry
