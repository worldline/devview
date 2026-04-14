package com.worldline.devview.featureflip.model

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FeatureTest {

    @Test
    fun `remote feature isEnabled reflects remote state`() {
        Feature.RemoteFeature(
            name = "new_checkout",
            description = null,
            defaultRemoteValue = true,
            state = FeatureState.REMOTE
        ).isEnabled shouldBe true

        Feature.RemoteFeature(
            name = "new_checkout",
            description = null,
            defaultRemoteValue = false,
            state = FeatureState.REMOTE
        ).isEnabled shouldBe false
    }

    @Test
    fun `remote feature local overrides take precedence`() {
        Feature.RemoteFeature(
            name = "new_checkout",
            description = null,
            defaultRemoteValue = false,
            state = FeatureState.LOCAL_ON
        ).isEnabled shouldBe true

        Feature.RemoteFeature(
            name = "new_checkout",
            description = null,
            defaultRemoteValue = true,
            state = FeatureState.LOCAL_OFF
        ).isEnabled shouldBe false
    }

    @Test
    fun `local feature isEnabled returns stored value`() {
        Feature.LocalFeature(
            name = "dark_mode",
            description = null,
            isEnabled = true
        ).isEnabled shouldBe true

        Feature.LocalFeature(
            name = "dark_mode",
            description = null,
            isEnabled = false
        ).isEnabled shouldBe false
    }
}

