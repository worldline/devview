package com.worldline.devview.featureflip.model

import com.worldline.devview.test.FakePreferencesDataStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FeatureHandlerTest {

    @Test
    fun `addFeatures persists initial values used by enabled flow`() = runTest {
        val store = FakePreferencesDataStore()
        val local = Feature.LocalFeature(name = "dark_mode", description = null, isEnabled = true)
        val remote = Feature.RemoteFeature(
            name = "new_checkout",
            description = null,
            defaultRemoteValue = false,
            state = FeatureState.LOCAL_ON
        )
        val handler = FeatureHandler(dataStore = store, initialFeatures = emptyList())

        handler.addFeatures(listOf(local, remote))

        handler.isFeatureEnabledFlow("dark_mode").first() shouldBe true
        handler.isFeatureEnabledFlow("new_checkout").first() shouldBe true
    }

    @Test
    fun `setFeatureState updates local and remote features`() = runTest {
        val store = FakePreferencesDataStore()
        val local = Feature.LocalFeature(name = "dark_mode", description = null, isEnabled = false)
        val remote = Feature.RemoteFeature(
            name = "new_checkout",
            description = null,
            defaultRemoteValue = true,
            state = FeatureState.REMOTE
        )
        val handler = FeatureHandler(dataStore = store, initialFeatures = listOf(local, remote))

        handler.setFeatureState("dark_mode", FeatureState.LOCAL_ON)
        handler.setFeatureState("new_checkout", FeatureState.LOCAL_OFF)

        handler.isFeatureEnabledFlow("dark_mode").first() shouldBe true
        handler.isFeatureEnabledFlow("new_checkout").first() shouldBe false
    }

    @Test
    fun `setFeatureState throws when setting REMOTE for local feature`() = runTest {
        val local = Feature.LocalFeature(name = "dark_mode", description = null, isEnabled = false)
        val handler = FeatureHandler(dataStore = FakePreferencesDataStore(), initialFeatures = listOf(local))

        shouldThrow<IllegalArgumentException> {
            handler.setFeatureState("dark_mode", FeatureState.REMOTE)
        }
    }

    @Test
    fun `isFeatureEnabledFlow throws for unknown feature`() = runTest {
        val handler = FeatureHandler(dataStore = FakePreferencesDataStore(), initialFeatures = emptyList())

        shouldThrow<IllegalArgumentException> {
            handler.isFeatureEnabledFlow("missing").first()
        }
    }
}
