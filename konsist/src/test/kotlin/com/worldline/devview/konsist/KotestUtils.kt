package com.worldline.devview.konsist

import io.kotest.core.test.TestScope

val TestScope.koTestName: String
    get() = this.testCase.name.name
