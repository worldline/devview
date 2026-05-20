package com.worldline.devview.test

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

public data class TestDispatchers(
    val main: TestDispatcher,
    val io: TestDispatcher,
    val default: TestDispatcher,
    val unconfined: TestDispatcher
)

public fun testDispatchers(
    scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
): TestDispatchers =
    TestDispatchers(
        main = StandardTestDispatcher(scheduler = scheduler),
        io = StandardTestDispatcher(scheduler = scheduler),
        default = StandardTestDispatcher(scheduler = scheduler),
        unconfined = UnconfinedTestDispatcher(scheduler = scheduler)
    )

public fun runTestWithDispatchers(
    dispatchers: TestDispatchers = testDispatchers(),
    testBody: suspend TestScope.(TestDispatchers) -> Unit
): TestResult =
    runTest(context = dispatchers.main) {
        testBody(dispatchers)
    }

