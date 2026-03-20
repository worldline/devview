package com.worldline.devview.networkmock.viewmodel

import io.mockk.clearAllMocks
import kotlin.test.AfterTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

open class ViewModelTest {
    open fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }
}
