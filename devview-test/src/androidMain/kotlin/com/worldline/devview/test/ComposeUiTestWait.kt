package com.worldline.devview.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onAllNodesWithTag

public fun ComposeUiTest.waitUntilTagCount(
    tag: String,
    expectedCount: Int,
    timeoutMillis: Long = 10_000L
): Unit {
    waitUntil(timeoutMillis = timeoutMillis) {
        runCatching { onAllNodesWithTag(testTag = tag).fetchSemanticsNodes().size == expectedCount }
            .getOrDefault(false)
    }
}

public fun ComposeUiTest.waitUntilTagExists(
    tag: String,
    timeoutMillis: Long = 10_000L
): Unit = waitUntilTagCount(tag = tag, expectedCount = 1, timeoutMillis = timeoutMillis)

public fun ComposeUiTest.waitUntilTagGone(
    tag: String,
    timeoutMillis: Long = 10_000L
): Unit = waitUntilTagCount(tag = tag, expectedCount = 0, timeoutMillis = timeoutMillis)

