package com.worldline.devview.crashhistory.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ReportUnhandledExceptionHook
import kotlin.native.setUnhandledExceptionHook
import kotlin.native.terminateWithUnhandledException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.staticCFunction
import kotlinx.io.files.Path
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSException
import platform.Foundation.NSFileManager
import platform.Foundation.NSGetUncaughtExceptionHandler
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

private const val STORAGE_DIRECTORY = "devview-crashhistory"

// Hook installed before ours. Top-level: the hook lambda cannot reference a val still being initialised.
@OptIn(ExperimentalNativeApi::class)
private var previousKotlinHook: ReportUnhandledExceptionHook? = null

// Receiver of NSException crashes. Top-level because a `staticCFunction` cannot capture.
private var nsExceptionSink: CrashSink? = null

// Handler installed before ours (Crashlytics, Sentry, ...). Always called afterwards.
@OptIn(ExperimentalForeignApi::class)
private var previousNsExceptionHandler: NSUncaughtExceptionHandler? = null

/** iOS implementation: Kotlin unhandled-exception hook + NSException handler + MetricKit. */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
internal class IosCrashPlatform : CrashPlatform {
    override val storageDirectory: Path = Path(applicationSupportPath(), STORAGE_DIRECTORY)

    override val processId: Int = NSProcessInfo.processInfo.processIdentifier

    override val metadata: CrashMetadata by lazy {
        val bundle = NSBundle.mainBundle
        CrashMetadata(
            platform = "ios",
            osVersion = UIDevice.currentDevice.systemVersion,
            deviceModel = UIDevice.currentDevice.model,
            appVersion = bundle.objectForInfoDictionaryKey(
                "CFBundleShortVersionString"
            ) as? String ?: "unknown",
            buildNumber = bundle.objectForInfoDictionaryKey(
                "CFBundleVersion"
            ) as? String ?: "unknown"
        )
    }

    override fun installUncaughtHandlers(onCrash: CrashSink) {
        previousKotlinHook = setUnhandledExceptionHook { throwable ->
            try {
                onCrash(
                    throwable::class.qualifiedName,
                    throwable.message,
                    throwable.stackTraceToString(),
                    null
                )
            } catch (ignored: Throwable) {
                // Never let our bookkeeping hide the real crash from the previous hook.
            }
            // A hook that returns does not terminate the app: terminate like the default would.
            previousKotlinHook?.invoke(throwable) ?: terminateWithUnhandledException(throwable)
        }
        nsExceptionSink = onCrash
        previousNsExceptionHandler = NSGetUncaughtExceptionHandler()
        NSSetUncaughtExceptionHandler(
            staticCFunction { exception: NSException? ->
                try {
                    nsExceptionSink?.invoke(
                        exception?.name,
                        exception?.reason,
                        exception?.callStackSymbols?.joinToString("\n"),
                        null
                    )
                } catch (ignored: Throwable) {
                    // Same as above: the previous handler must still run.
                }
                previousNsExceptionHandler?.invoke(exception)
            }
        )
    }

    override fun importHistory(
        store: CrashStore,
        existing: List<CrashRecord>,
        onImported: (CrashRecord) -> Unit
    ) {
        subscribeToMetricKit(store, metadata, onImported)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportPath(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    return requireNotNull(url?.path) { "Application Support directory unavailable" }
}

@Composable
internal actual fun rememberCrashPlatform(): CrashPlatform = remember { IosCrashPlatform() }
