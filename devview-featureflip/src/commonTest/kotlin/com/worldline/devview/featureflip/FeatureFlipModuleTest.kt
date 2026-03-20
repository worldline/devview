package com.worldline.devview.featureflip

import com.worldline.devview.core.Section
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FeatureFlipModuleTest {

    @Test
    fun `feature flip module exposes expected metadata`() {
        FeatureFlip.section shouldBe Section.FEATURES
        FeatureFlip.dataStoreName shouldBe FEATURE_FLIP_DATASTORE_NAME

        FeatureFlip.destinations.keys.shouldContain(FeatureFlipDestination.Main)

        val metadata = FeatureFlip.destinations[FeatureFlipDestination.Main].shouldNotBeNull()
        metadata.title shouldBe "Feature Flip"
        metadata.actions shouldHaveSize 0
    }

    @Test
    fun `feature flip module has a datastore delegate instance`() {
        FeatureFlip.dataStoreDelegate.shouldNotBeNull()
    }
}

