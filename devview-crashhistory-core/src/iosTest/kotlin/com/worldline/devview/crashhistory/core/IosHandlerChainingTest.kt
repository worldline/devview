package com.worldline.devview.crashhistory.core

import io.kotest.matchers.collections.shouldContainExactly
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ReportUnhandledExceptionHook
import kotlin.native.getUnhandledExceptionHook
import kotlin.native.setUnhandledExceptionHook
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSException
import platform.Foundation.NSGetUncaughtExceptionHandler
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler

/** Top-level: a `staticCFunction` cannot capture. */
private val calls = mutableListOf<String>()

/**
 * Checks that both iOS handlers report first and then call the handler that was there before.
 * Invokes the installed handlers directly: a real crash would kill the test process.
 */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
class IosHandlerChainingTest {
    private var originalHook: ReportUnhandledExceptionHook? = null
    private var originalNsHandler: NSUncaughtExceptionHandler? = null

    @BeforeTest
    fun setUp() {
        calls.clear()
        originalHook = getUnhandledExceptionHook()
        originalNsHandler = NSGetUncaughtExceptionHandler()
        setUnhandledExceptionHook { calls += "previousHook:${it.message}" }
        NSSetUncaughtExceptionHandler(staticCFunction { exception: NSException? -> calls += "previousNs:${exception?.reason}" })
    }

    @AfterTest
    fun tearDown() {
        setUnhandledExceptionHook(originalHook)
        NSSetUncaughtExceptionHandler(originalNsHandler)
    }

    @Test
    fun `the kotlin hook reports then calls the previous hook`() {
        IosCrashPlatform().installUncaughtHandlers { type, message, _, _ -> calls += "report:$type:$message" }

        getUnhandledExceptionHook()!!.invoke(IllegalStateException("boom"))

        calls shouldContainExactly listOf("report:kotlin.IllegalStateException:boom", "previousHook:boom")
    }

    @Test
    fun `the NSException handler reports then calls the previous handler`() {
        IosCrashPlatform().installUncaughtHandlers { type, message, _, _ -> calls += "report:$type:$message" }

        NSGetUncaughtExceptionHandler()!!.invoke(NSException(name = "TestException", reason = "boom", userInfo = null))

        calls shouldContainExactly listOf("report:TestException:boom", "previousNs:boom")
    }
}
