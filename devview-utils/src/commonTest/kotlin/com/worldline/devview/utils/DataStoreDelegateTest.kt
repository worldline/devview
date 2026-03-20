package com.worldline.devview.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class DataStoreDelegateTest {

    @Test
    fun `get throws when datastore is not initialized`() {
        val delegate = DataStoreDelegate()

        val exception = shouldThrow<IllegalStateException> {
            delegate.get()
        }

        exception.message.shouldContain("DataStore not initialised")
    }
}

