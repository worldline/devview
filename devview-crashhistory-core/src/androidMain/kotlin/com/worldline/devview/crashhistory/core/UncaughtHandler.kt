package com.worldline.devview.crashhistory.core

import android.os.Process
import kotlin.system.exitProcess

private const val EXIT_CODE_CRASH = 10

/**
 * Installs a default uncaught-exception handler that reports to [onCrash] and then **always**
 * calls the handler that was installed before (Crashlytics, the OS's own crash dialog, ...).
 *
 * Reporting comes first so DevView still records the crash if a later handler never returns.
 * [onCrash] must not throw; failures here are swallowed so the previous handler always runs.
 */
internal fun installChainedUncaughtHandler(onCrash: CrashSink) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            onCrash(
                throwable.javaClass.name,
                throwable.message,
                throwable.stackTraceToString(),
                thread.name
            )
        } catch (ignored: Throwable) {
            // Never let our bookkeeping hide the real crash from the previous handler.
        }
        if (previous != null) {
            previous.uncaughtException(thread, throwable)
        } else {
            // No handler to terminate the process: swallowing the crash would leave a zombie app.
            Process.killProcess(Process.myPid())
            exitProcess(status = EXIT_CODE_CRASH)
        }
    }
}
