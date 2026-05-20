package com.worldline.devview.test

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow

public suspend fun <T> Flow<T>.assertEmitsExactly(
	vararg expected: T,
	message: String = "Flow emitted unexpected values"
): Unit {
	test {
		expected.forEach { expectedItem ->
			val actualItem = awaitItem()
			if (actualItem != expectedItem) {
				throw AssertionError("$message: expected=$expectedItem, actual=$actualItem")
			}
		}
		cancelAndIgnoreRemainingEvents()
	}
}
