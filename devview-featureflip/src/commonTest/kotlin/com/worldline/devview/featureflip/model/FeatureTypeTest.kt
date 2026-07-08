package com.worldline.devview.featureflip.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FeatureTypeTest {

    @Test
    fun `fromOrdinal maps valid ordinals`() {
        FeatureType.fromOrdinal(0) shouldBe FeatureType.REMOTE
        FeatureType.fromOrdinal(1) shouldBe FeatureType.LOCAL
    }

    @Test
    fun `fromOrdinal throws for unknown ordinal`() {
        shouldThrow<IllegalArgumentException> {
            FeatureType.fromOrdinal(2)
        }
    }
}

