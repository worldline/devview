package com.worldline.devview.crashhistory.core

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class UncaughtHandlerChainingTest {
    private val original: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    private val calls = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> calls += "previous:${throwable.message}" }
    }

    @AfterTest
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(original)
    }

    @Test
    fun `the crash is reported first, then the previous handler runs`() {
        installChainedUncaughtHandler { type, message, trace, thread ->
            calls += "report:$type:$message:$thread"
            (trace?.contains("IllegalStateException") ?: false) shouldBe true
        }

        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), IllegalStateException("boom"))

        calls shouldContainExactly listOf(
            "report:java.lang.IllegalStateException:boom:${Thread.currentThread().name}",
            "previous:boom"
        )
    }

    @Test
    fun `the previous handler still runs when reporting throws`() {
        installChainedUncaughtHandler { _, _, _, _ -> error("reporting failed") }

        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), IllegalStateException("boom"))

        calls shouldContainExactly listOf("previous:boom")
    }
}
