package com.worldline.devview.crashhistory.core

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.io.files.Path

private const val STORAGE_DIRECTORY = "devview-crashhistory"
private const val UNKNOWN = "unknown"

/** Android implementation: chained `Thread` default handler + `ApplicationExitInfo` (API 30+). */
internal class AndroidCrashPlatform(private val context: Context) : CrashPlatform {
    override val storageDirectory: Path =
        Path(base = context.noBackupFilesDir.absolutePath + "/" + STORAGE_DIRECTORY)

    override val processId: Int = Process.myPid()

    override val metadata: CrashMetadata by lazy {
        val info = packageInfo()
        CrashMetadata(
            platform = "android",
            osVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = info?.versionName ?: UNKNOWN,
            buildNumber = info?.let { versionCodeOf(info = it) } ?: UNKNOWN
        )
    }

    override fun installUncaughtHandlers(onCrash: CrashSink) {
        installChainedUncaughtHandler(onCrash = onCrash)
    }

    override suspend fun importHistory(
        store: CrashStore,
        existing: List<CrashRecord>,
        onImported: suspend (CrashRecord) -> Boolean,
        enqueueImport: (suspend () -> Unit) -> Unit,
        withStorage: suspend (suspend () -> Unit) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        withStorage {
            importExitReasons(store = store, existing = existing, onImported = onImported)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun importExitReasons(
        store: CrashStore,
        existing: List<CrashRecord>,
        onImported: suspend (CrashRecord) -> Boolean
    ) {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        val cursor = store.readState(name = CrashStore.EXIT_INFO_CURSOR)?.toLongOrNull() ?: 0L
        val exits = manager
            .getHistoricalProcessExitReasons(context.packageName, 0, 0)
            .filter { it.timestamp > cursor }
            .sortedBy { it.timestamp }
        // Commit a timestamp only when every exit at that timestamp has been handled.
        for ((timestamp, group) in exits.groupBy { it.timestamp }) {
            for (exit in group) {
                val record = exitInfoToRecord(
                    reason = exit.reason,
                    description = exit.description,
                    timestampMillis = exit.timestamp,
                    processId = exit.pid,
                    trace = readTrace(exit = exit),
                    metadata = metadata
                )
                if (record != null && !isAlreadyRecorded(record = record, existing = existing)) {
                    if (!onImported(record)) return
                }
            }
            if (!store.writeState(
                    name = CrashStore.EXIT_INFO_CURSOR,
                    value = timestamp.toString()
                )
            ) {
                return
            }
        }
    }

    // ANR traces are plain text. A native crash trace is a protobuf tombstone (API 31+) that may be
    // missing or already overwritten, so only its presence is noted.
    @RequiresApi(Build.VERSION_CODES.R)
    private fun readTrace(exit: ApplicationExitInfo): String? = try {
        when (exit.reason) {
            ApplicationExitInfo.REASON_ANR -> exit.traceInputStream?.bufferedReader()?.use {
                it
                    .readText()
            }
            ApplicationExitInfo.REASON_CRASH_NATIVE ->
                exit.traceInputStream?.use {
                    "Native tombstone available from the OS (protobuf, not shown)."
                }
            else -> null
        }
    } catch (ignored: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(): PackageInfo? = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (ignored: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: PackageInfo): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toString()
        } else {
            info.versionCode.toString()
        }
}

@Composable
internal actual fun rememberCrashPlatform(): CrashPlatform {
    val context = LocalContext.current.applicationContext
    return remember(key1 = context) { AndroidCrashPlatform(context = context) }
}
