package com.worldline.devview.networkmock.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.ApiSpecUiModel
import com.worldline.devview.networkmock.model.OperationUiModel
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// contentColorForStatusCode/containerColorForStatusCode and OperationMockState.contentColor/
// containerColor are @Composable (they read LocalMockColorScheme) — covered in the
// androidDeviceTest ModelUtilsColorTest instead, which can provide a composition.
class ModelUtilsTest {

    @Test
    fun `iconForStatusCode maps HTTP families and fallback`() {
        iconForStatusCode(statusCode = 101) shouldBe Icons.Rounded.Info
        iconForStatusCode(statusCode = 204) shouldBe Icons.Rounded.CheckCircleOutline
        iconForStatusCode(statusCode = 302) shouldBe Icons.AutoMirrored.Rounded.Redo
        iconForStatusCode(statusCode = 404) shouldBe Icons.Rounded.ErrorOutline
        iconForStatusCode(statusCode = 503) shouldBe Icons.Rounded.CloudOff

        iconForStatusCode(statusCode = null) shouldBe Icons.AutoMirrored.Rounded.HelpOutline
        iconForStatusCode(statusCode = 42) shouldBe Icons.AutoMirrored.Rounded.HelpOutline
    }

    @Test
    fun `fake ApiSpecUiModel creates requested amount with nested operations`() {
        val specs = ApiSpecUiModel.fake(amount = 3)

        specs shouldHaveSize 3
        specs[0].specId shouldBe "spec"
        specs[0].name shouldBe "Spec"
        specs[0].operations shouldHaveSize 7
    }

    @Test
    fun `fake OperationDescriptor creates requested amount`() {
        val descriptors = OperationDescriptor.fake(
            amount = 2,
            specId = "qa"
        )

        descriptors shouldHaveSize 2
        descriptors[0].specId shouldBe "qa"
        descriptors[0].operationId shouldBe "operation-1"
        descriptors[0].config.path shouldBe "/operation1"
    }

    @Test
    fun `fake OperationUiModel and MockResponse create requested amount`() {
        val operations = OperationUiModel.fake(amount = 5)
        val responses = MockResponse.fake(amount = 4)

        operations shouldHaveSize 5
        operations[0].currentState shouldBe OperationMockState.Mock(statusCode = 100, exampleName = "default")

        responses shouldHaveSize 4
        responses[1].exampleName shouldBe "default"
        responses[1].statusCode shouldBe 200
        responses[1].displayName shouldBe "Response 1"
    }
}
