package com.worldline.devview.networkmock

import com.worldline.devview.core.Section
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NetworkMockModuleTest {

    private fun module() = NetworkMock(
        resourceLoader = { ByteArray(size = 0) },
        specPaths = listOf("files/networkmocks/specs/example.json")
    )

    @Test
    fun `network mock module exposes expected metadata and destinations`() {
        val module = module()

        module.section shouldBe Section.NETWORK
        module.destinations.keys.shouldContain(NetworkMockDestination.Main::class)
        module.entryDestination::class shouldBe NetworkMockDestination.Main::class
    }

    @Test
    fun `toolbar registers reload and reset while sorting belongs to the filter panel`() {
        val module = module()

        val mainMetadata = module.destinations[NetworkMockDestination.Main::class].shouldNotBeNull()
        mainMetadata.title shouldBe null
        module.moduleName shouldBe "Network Mock"
        mainMetadata.actions shouldHaveSize 2

        val (refreshAction, resetAction) = mainMetadata.actions
        refreshAction.menuItems.shouldBeNull()
        resetAction.menuItems.shouldBeNull()
        refreshAction.label shouldBe "Reload specs"
        resetAction.label shouldBe "Reset all to network"
        resetAction.popup.shouldNotBeNull().title shouldBe "Reset all operations to network?"
    }

    @Test
    fun `toolbar actions can be invoked without throwing`() {
        val module = module()
        val mainMetadata = module.destinations[NetworkMockDestination.Main::class].shouldNotBeNull()
        val (refreshAction, resetAction) = mainMetadata.actions

        refreshAction.action(NetworkMockDestination.Main)
        resetAction.action(NetworkMockDestination.Main)
    }
}
