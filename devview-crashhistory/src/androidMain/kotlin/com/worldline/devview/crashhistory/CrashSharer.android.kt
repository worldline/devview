package com.worldline.devview.crashhistory

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

private const val EXPORT_DIRECTORY = "devview-crashhistory-export"
private const val EXPORT_RETENTION_MILLIS = 60 * 60 * 1_000L

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

    override fun shareText(text: String): Boolean = try {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        launchChooser(send = send)
        true
    } catch (ignored: ActivityNotFoundException) {
        false
    }

    override fun shareFile(fileName: String, content: String): Boolean {
        val file = writeCrashExport(
            directory = File(context.cacheDir, EXPORT_DIRECTORY),
            fileName = fileName,
            content = content
        ) ?: return false
        return try {
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
            true
        } catch (ignored: IllegalArgumentException) {
            false
        } catch (ignored: ActivityNotFoundException) {
            false
        }
    }

    private fun launchChooser(send: Intent) {
        val chooser = Intent.createChooser(send, null)
        // A non-Activity context (e.g. the application context) cannot start an activity otherwise.
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

/** Keeps exports alive for delayed recipients; removes only files older than one hour. */
internal fun writeCrashExport(directory: File, fileName: String, content: String): File? {
    var file: File? = null
    return try {
        directory.mkdirs()
        val cutoff = System.currentTimeMillis() - EXPORT_RETENTION_MILLIS
        directory.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }?.forEach {
            it
                .delete()
        }
        file = File.createTempFile(
            fileName.removeSuffix(suffix = ".json") + "-",
            ".json",
            directory
        )
        file.writeText(text = content)
        file
    } catch (ignored: IOException) {
        file?.delete()
        null
    } catch (ignored: IllegalArgumentException) {
        file?.delete()
        null
    }
}

@Composable
internal actual fun rememberCrashSharer(): CrashSharer {
    val context = LocalContext.current
    return remember(key1 = context) { AndroidCrashSharer(context = context) }
}
