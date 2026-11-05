package com.worldline.devview.crashhistory.core

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Starts the recorder before `Application.onCreate`, so crashes during app startup are recorded
 * too, without any code in the host app.
 *
 * It only starts when `CrashHistory.initModule()` has run once before (it leaves an `enabled`
 * marker file), which is how the first launch after integrating the module still works lazily.
 * Everything is wrapped in `try/catch`: a failure here must never prevent the app from starting.
 */
internal class CrashHistoryInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        try {
            val appContext = context?.applicationContext
            if (appContext != null) {
                startIfEnabled(platform = AndroidCrashPlatform(context = appContext))
            }
        } catch (ignored: Throwable) {
            // Never block app startup because of crash bookkeeping.
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}

/**
 * Calls [start] when the `enabled` marker exists in [platform]'s storage directory. [start] is a
 * parameter only so tests do not have to touch the global [CrashRecorder].
 */
internal fun startIfEnabled(
    platform: CrashPlatform,
    start: (CrashPlatform, Int) -> Unit = CrashRecorder::start
) {
    val maxRecords = CrashStore(directory = platform.storageDirectory)
        .readState(name = CrashStore.ENABLED)
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?: return
    start(platform, maxRecords)
}
