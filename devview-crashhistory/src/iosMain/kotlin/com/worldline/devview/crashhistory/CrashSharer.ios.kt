@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("CAST_NEVER_SUCCEEDS")

package com.worldline.devview.crashhistory

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

// The directory is wiped before each export so only the latest file is kept in the temp folder.
private const val EXPORT_DIRECTORY = "devview-crashhistory-export"

private object IosCrashSharer : CrashSharer {
    override fun copy(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun shareText(text: String) {
        present(item = text)
    }

    override fun shareFile(fileName: String, content: String) {
        val directory = NSTemporaryDirectory() + EXPORT_DIRECTORY
        val files = NSFileManager.defaultManager
        files.removeItemAtPath(path = directory, error = null)
        files.createDirectoryAtPath(
            path = directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        val path = "$directory/$fileName"
        val written = (content as NSString).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        if (written) present(item = NSURL.fileURLWithPath(path = path))
    }

    // shortcut: `keyWindow` is deprecated (iOS 13) but is the only window lookup that works for
    // single-scene apps without scene plumbing; replace it if multi-window support is needed.
    @Suppress("DEPRECATION")
    private fun present(item: Any) {
        var top: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        val presenter = top ?: return
        val sheet = UIActivityViewController(
            activityItems = listOf(item),
            applicationActivities = null
        )
        // iPad shows the sheet as a popover and crashes without an anchor.
        sheet.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = presenter.view.bounds
            permittedArrowDirections = 0u
        }
        presenter.presentViewController(
            viewControllerToPresent = sheet,
            animated = true,
            completion = null
        )
    }
}

@Composable
internal actual fun rememberCrashSharer(): CrashSharer = IosCrashSharer
