package com.worldline.devview.networkmock

import com.worldline.devview.core.Section
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
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
    fun `toolbar registers refresh and sort menu and reset actions in order`() {
        val module = module()

        val mainMetadata = module.destinations[NetworkMockDestination.Main::class].shouldNotBeNull()
        mainMetadata.title shouldBe "Network Mock"
        mainMetadata.actions shouldHaveSize 3

        val (refreshAction, sortAction, resetAction) = mainMetadata.actions

        refreshAction.menuItems.shouldBeNull()
        resetAction.menuItems.shouldBeNull()

        val menuItems = sortAction.menuItems.shouldNotBeNull()
        menuItems shouldHaveSize OperationSort.entries.size
        menuItems.map { it.label } shouldContainExactly OperationSort.entries.map { it.label }
    }

    @Test
    fun `sort menu items and plain actions can be invoked without throwing`() {
        val module = module()
        val mainMetadata = module.destinations[NetworkMockDestination.Main::class].shouldNotBeNull()
        val (refreshAction, sortAction, resetAction) = mainMetadata.actions

        refreshAction.action()
        resetAction.action()
        sortAction.menuItems.shouldNotBeNull().forEach { it.onClick() }
    }
}
