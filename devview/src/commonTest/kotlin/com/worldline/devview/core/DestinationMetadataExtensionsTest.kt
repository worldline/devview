package com.worldline.devview.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.navigation3.runtime.NavKey
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class DestinationMetadataExtensionsTest {

    @Test
    fun `asDestination registers nav key with default metadata`() {
        val key = TestNavKey

        val (registeredKey, metadata) = key.asDestination()

        registeredKey shouldBeSameInstanceAs key
        metadata.title.shouldBeNull()
        metadata.actions.shouldBeEmpty()
    }

    @Test
    fun `withTitle without block sets title and keeps actions empty`() {
        val key = TestNavKey

        val (registeredKey, metadata) = key.withTitle(title = "My Screen")

        registeredKey shouldBeSameInstanceAs key
        metadata.title shouldBe "My Screen"
        metadata.actions.shouldBeEmpty()
    }

    @Test
    fun `withTitle with block sets title and builds actions in order`() {
        val key = TestNavKey
        val firstIcon = Icons.Default.Check
        val secondIcon = Icons.Default.Close
        val popup = ModuleDestinationActionPopup(
            title = "Clear Logs",
            subtitle = "This will remove all logged events.",
            confirmButton = "Clear",
            dismissButton = "Cancel"
        )
        var firstCalls = 0
        var secondCalls = 0

        val (registeredKey, metadata) = key.withTitle(title = "Analytics") {
            action(icon = firstIcon, popup = popup) {
                firstCalls++
            }
            action(icon = secondIcon) {
                secondCalls++
            }
        }

        registeredKey shouldBeSameInstanceAs key
        metadata.title shouldBe "Analytics"
        metadata.actions shouldHaveSize 2

        metadata.actions[0].icon shouldBeSameInstanceAs firstIcon
        metadata.actions[0].popup shouldBe popup
        metadata.actions[1].icon shouldBeSameInstanceAs secondIcon
        metadata.actions[1].popup.shouldBeNull()

        metadata.actions[0].action()
        metadata.actions[1].action()

        firstCalls shouldBe 1
        secondCalls shouldBe 1
    }

    @Test
    fun `withActions with block keeps title null and builds actions`() {
        val key = TestNavKey
        val icon = Icons.Default.Check
        var calls = 0

        val (registeredKey, metadata) = key.withActions {
            action(icon = icon) {
                calls++
            }
        }

        registeredKey shouldBeSameInstanceAs key
        metadata.title.shouldBeNull()
        metadata.actions shouldHaveSize 1
        metadata.actions.single().icon shouldBeSameInstanceAs icon
        metadata.actions.single().popup.shouldBeNull()

        metadata.actions.single().action()

        calls shouldBe 1
    }
}

private data object TestNavKey : NavKey
