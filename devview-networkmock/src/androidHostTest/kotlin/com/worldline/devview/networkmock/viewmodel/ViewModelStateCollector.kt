package com.worldline.devview.networkmock.viewmodel

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

fun <T> TestScope.collectState(stateFlow: StateFlow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher()) {
        stateFlow.collect { }
    }
}

fun TestScope.collectStates(vararg stateFlows: StateFlow<*>) {
    for (stateFlow in stateFlows) {
        backgroundScope.launch(UnconfinedTestDispatcher()) {
            stateFlow.collect { }
        }
    }
}
