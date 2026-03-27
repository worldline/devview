package com.worldline.devview.featureflip.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FeatureStateTest {

    @Test
    fun `fromOrdinal maps valid ordinals`() {
        FeatureState.fromOrdinal(0) shouldBe FeatureState.REMOTE
        FeatureState.fromOrdinal(1) shouldBe FeatureState.LOCAL_OFF
        FeatureState.fromOrdinal(2) shouldBe FeatureState.LOCAL_ON
    }

    @Test
    fun `fromOrdinal throws for unknown ordinal`() {
        shouldThrow<IllegalArgumentException> {
            FeatureState.fromOrdinal(3)
        }
    }
}

