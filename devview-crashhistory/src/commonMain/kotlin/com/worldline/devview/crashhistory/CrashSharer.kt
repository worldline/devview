package com.worldline.devview.crashhistory

import androidx.compose.runtime.Composable

/**
 * Gets crash text off the device: clipboard and the system share sheet. Platform-specific
 * because neither exists in common code.
 */
internal interface CrashSharer {
    /** Puts [text] on the clipboard. */
    fun copy(text: String)

    /** Opens the share sheet with [text]. */
    fun shareText(text: String)

    /** Writes [content] to a temporary file called [fileName] and opens the share sheet with it. */
    fun shareFile(fileName: String, content: String)
}

@Composable
internal expect fun rememberCrashSharer(): CrashSharer
