package com.worldline.devview.networkmock.core.model

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class StatusCodeFamilyTest {

    @Test
    fun `fromStatusCode maps any integer to the expected family`() = runTest {
        checkAll(iterations = 1_000, genA = Arb.int()) { statusCode ->
            val expected = when (statusCode) {
                in 100..199 -> StatusCodeFamily.INFORMATIONAL
                in 200..299 -> StatusCodeFamily.SUCCESSFUL
                in 300..399 -> StatusCodeFamily.REDIRECTION
                in 400..499 -> StatusCodeFamily.CLIENT_ERROR
                in 500..599 -> StatusCodeFamily.SERVER_ERROR
                else -> StatusCodeFamily.UNKNOWN
            }

            StatusCodeFamily.fromStatusCode(statusCode = statusCode) shouldBe expected
        }
    }

    @Test
    fun `displayName returns expected user-facing labels`() {
        StatusCodeFamily.INFORMATIONAL.displayName shouldBe "Informational"
        StatusCodeFamily.SUCCESSFUL.displayName shouldBe "Successful"
        StatusCodeFamily.REDIRECTION.displayName shouldBe "Redirection"
        StatusCodeFamily.CLIENT_ERROR.displayName shouldBe "Client Error"
        StatusCodeFamily.SERVER_ERROR.displayName shouldBe "Server Error"
        StatusCodeFamily.UNKNOWN.displayName shouldBe "Unknown"
    }
}
