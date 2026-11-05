package com.worldline.devview.crashhistory

import androidx.compose.runtime.Composable

/**
 * Gets crash text off the device: clipboard and the system share sheet. Platform-specific
 * because neither exists in common code.
 */
internal interface CrashSharer {
    /** Puts [text] on the clipboard. */
    fun copy(text: String)

    /** Opens the share sheet with [text], returning false when it cannot be opened. */
    fun shareText(text: String): Boolean

    /** Exports [content] using [fileName] as its name, returning false on failure. */
    fun shareFile(fileName: String, content: String): Boolean
}

@Composable
internal expect fun rememberCrashSharer(): CrashSharer
