package com.worldline.devview.crashhistory

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

private const val EXPORT_DIRECTORY = "devview-crashhistory-export"

/**
 * `FileProvider` that serves the exported crash JSON. A subclass of its own so it cannot clash
 * with a `FileProvider` declared by the host app.
 */
internal class CrashHistoryFileProvider : FileProvider()

private class AndroidCrashSharer(private val context: Context) : CrashSharer {
    override fun copy(text: String) {
        context
            .getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("DevView crash", text))
    }

    override fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        launchChooser(send = send)
    }

    override fun shareFile(fileName: String, content: String) {
        // Cleared first so old exports (which may hold stack traces) never pile up in the cache.
        val directory = File(context.cacheDir, EXPORT_DIRECTORY)
        directory.deleteRecursively()
        directory.mkdirs()
        val file = File(directory, fileName).apply { writeText(text = content) }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.devview.crashhistory.fileprovider",
            file
        )
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/json")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        launchChooser(send = send)
    }

    private fun launchChooser(send: Intent) {
        val chooser = Intent.createChooser(send, null)
        // A non-Activity context (e.g. the application context) cannot start an activity otherwise.
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

@Composable
internal actual fun rememberCrashSharer(): CrashSharer {
    val context = LocalContext.current
    return remember(key1 = context) { AndroidCrashSharer(context = context) }
}
