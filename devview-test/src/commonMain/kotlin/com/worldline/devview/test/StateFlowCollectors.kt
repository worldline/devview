package com.worldline.devview.test

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

public fun <T> TestScope.collectState(
    stateFlow: StateFlow<T>,
    dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
) {
    backgroundScope.launch(dispatcher) {
        stateFlow.collect { }
    }
}

public fun TestScope.collectStates(
    vararg stateFlows: StateFlow<*>,
    dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
) {
    stateFlows.forEach { stateFlow ->
        backgroundScope.launch(dispatcher) {
            stateFlow.collect { }
        }
    }
}
