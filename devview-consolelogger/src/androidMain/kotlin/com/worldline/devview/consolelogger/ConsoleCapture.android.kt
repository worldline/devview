package com.worldline.devview.consolelogger

import co.touchlab.kermit.Logger
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tails `logcat` scoped to this process's PID.
 *
 * Reading your own app's logcat needs no permission and works on an untethered, released
 * build. Some OEM ROMs block `logcat` for third-party apps entirely — in that case the
 * process fails to start or its output stream ends immediately, and this function simply
 * returns without native capture; [ConsoleLogger.log] and [DevViewLogWriter] still work.
 */
@Suppress("InjectDispatcher") // platform actual: no DI framework to inject a test dispatcher into
internal actual suspend fun tailNativeConsole(): Unit = withContext(context = Dispatchers.IO) {
    val process = try {
        ProcessBuilder("logcat", "-v", "brief", "--pid=${android.os.Process.myPid()}")
            .redirectErrorStream(true)
            .start()
    } catch (exception: IOException) {
        Logger.w(throwable = exception, tag = "DevViewConsole") {
            "logcat process failed to start; native console capture disabled for this session"
        }
        return@withContext
    }

    try {
        collectLogcatProcess(process = process)
    } finally {
        process.destroy()
    }
}

@Suppress("InjectDispatcher")
internal suspend fun collectLogcatProcess(process: Process): Unit = withContext(
    context = Dispatchers.IO
) {
    // Install before blocking: cancellation must terminate logcat to unblock its reader.
    val stopOnCancellation = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            process.destroy()
        }
    }
    try {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                currentCoroutineContext().ensureActive()
                ConsoleLogger.log(log = parseLogcatLine(line = line))
            }
        }
    } catch (exception: IOException) {
        currentCoroutineContext().ensureActive()
        throw exception
    } finally {
        stopOnCancellation.cancel()
        process.destroy()
    }
}
