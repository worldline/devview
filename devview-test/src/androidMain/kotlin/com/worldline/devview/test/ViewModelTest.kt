package com.worldline.devview.test

import io.mockk.clearAllMocks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

public open class ViewModelTest {
    protected lateinit var dispatchers: TestDispatchers

    public open fun setup() {
        dispatchers = testDispatchers()
        Dispatchers.setMain(dispatchers.unconfined)
    }

    public open fun tearDown() {
        clearAllMocks()
        Dispatchers.resetMain()
    }
}
