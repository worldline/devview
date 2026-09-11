package com.worldline.devview.networkmock.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.v2.runComposeUiTest
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.theme.LocalMockColorScheme
import com.worldline.devview.networkmock.theme.MockColorScheme
import io.kotest.matchers.shouldBe
import org.junit.Test

// contentColorForStatusCode/containerColorForStatusCode and the OperationMockState color
// extensions are @Composable (they read LocalMockColorScheme), so they need a composition —
// see ModelUtilsTest in commonTest for the remaining pure (icon) coverage.
class ModelUtilsColorTest {

    @Test
    fun contentColorForStatusCode_maps_HTTP_families_and_fallback() = runComposeUiTest {
        val colors = mutableMapOf<Int?, Color>()

        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMockColorScheme provides MockColorScheme.Light) {
                    colors[150] = contentColorForStatusCode(statusCode = 150)
                    colors[250] = contentColorForStatusCode(statusCode = 250)
                    colors[350] = contentColorForStatusCode(statusCode = 350)
                    colors[450] = contentColorForStatusCode(statusCode = 450)
                    colors[550] = contentColorForStatusCode(statusCode = 550)
                    colors[null] = contentColorForStatusCode(statusCode = null)
                    colors[700] = contentColorForStatusCode(statusCode = 700)
                }
            }
        }
        waitForIdle()

        colors[150] shouldBe MockColorScheme.Light.informational.content
        colors[250] shouldBe MockColorScheme.Light.successful.content
        colors[350] shouldBe MockColorScheme.Light.redirection.content
        colors[450] shouldBe MockColorScheme.Light.clientError.content
        colors[550] shouldBe MockColorScheme.Light.serverError.content
        colors[null] shouldBe MockColorScheme.Light.unknown.content
        colors[700] shouldBe MockColorScheme.Light.unknown.content
    }

    @Test
    fun containerColorForStatusCode_maps_HTTP_families_and_fallback() = runComposeUiTest {
        val colors = mutableMapOf<Int?, Color>()

        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMockColorScheme provides MockColorScheme.Light) {
                    colors[150] = containerColorForStatusCode(statusCode = 150)
                    colors[250] = containerColorForStatusCode(statusCode = 250)
                    colors[350] = containerColorForStatusCode(statusCode = 350)
                    colors[450] = containerColorForStatusCode(statusCode = 450)
                    colors[550] = containerColorForStatusCode(statusCode = 550)
                    colors[null] = containerColorForStatusCode(statusCode = null)
                    colors[700] = containerColorForStatusCode(statusCode = 700)
                }
            }
        }
        waitForIdle()

        colors[150] shouldBe MockColorScheme.Light.informational.container
        colors[250] shouldBe MockColorScheme.Light.successful.container
        colors[350] shouldBe MockColorScheme.Light.redirection.container
        colors[450] shouldBe MockColorScheme.Light.clientError.container
        colors[550] shouldBe MockColorScheme.Light.serverError.container
        colors[null] shouldBe MockColorScheme.Light.unknown.container
        colors[700] shouldBe MockColorScheme.Light.unknown.container
    }

    @Test
    fun operation_state_extensions_use_network_defaults() = runComposeUiTest {
        var content: Color? = null
        var container: Color? = null

        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMockColorScheme provides MockColorScheme.Light) {
                    val state = OperationMockState.Network
                    content = state.contentColor
                    container = state.containerColor
                }
            }
        }
        waitForIdle()

        content shouldBe MockColorScheme.Light.network.content
        container shouldBe MockColorScheme.Light.network.container
    }

    @Test
    fun operation_state_extensions_use_mock_status_code_mapping() = runComposeUiTest {
        var content: Color? = null
        var container: Color? = null

        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMockColorScheme provides MockColorScheme.Light) {
                    val state = OperationMockState.Mock(statusCode = 404, exampleName = "default")
                    content = state.contentColor
                    container = state.containerColor
                }
            }
        }
        waitForIdle()

        content shouldBe MockColorScheme.Light.clientError.content
        container shouldBe MockColorScheme.Light.clientError.container
    }
}
