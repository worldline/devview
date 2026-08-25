package com.worldline.devview.networkmock.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.Color
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.ApiSpecUiModel
import com.worldline.devview.networkmock.model.OperationUiModel
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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
    fun `contentColorForStatusCode maps HTTP families and fallback`() {
        contentColorForStatusCode(statusCode = 150) shouldBe Color(color = 0xFF184559)
        contentColorForStatusCode(statusCode = 250) shouldBe Color(color = 0xFF103C13)
        contentColorForStatusCode(statusCode = 350) shouldBe Color(color = 0xFF603610)
        contentColorForStatusCode(statusCode = 450) shouldBe Color(color = 0xFF6F1111)
        contentColorForStatusCode(statusCode = 550) shouldBe Color(color = 0xFF611A59)

        contentColorForStatusCode(statusCode = null) shouldBe Color(color = 0xFF3D3D3D)
        contentColorForStatusCode(statusCode = 700) shouldBe Color(color = 0xFF3D3D3D)
    }

    @Test
    fun `containerColorForStatusCode maps HTTP families and fallback`() {
        containerColorForStatusCode(statusCode = 150) shouldBe Color(color = 0xFFB7DCEC)
        containerColorForStatusCode(statusCode = 250) shouldBe Color(color = 0xFFB7ECBA)
        containerColorForStatusCode(statusCode = 350) shouldBe Color(color = 0xFFF0CAA7)
        containerColorForStatusCode(statusCode = 450) shouldBe Color(color = 0xFFECB7B7)
        containerColorForStatusCode(statusCode = 550) shouldBe Color(color = 0xFFECB7E6)

        containerColorForStatusCode(statusCode = null) shouldBe Color(color = 0xFFD1D1D1)
        containerColorForStatusCode(statusCode = 700) shouldBe Color(color = 0xFFD1D1D1)
    }

    @Test
    fun `operation state extension properties use network defaults`() {
        val state = OperationMockState.Network

        state.icon shouldBe Icons.Rounded.Wifi
        state.contentColor shouldBe Color(color = 0xFF0D1F3A)
        state.containerColor shouldBe Color(color = 0xFFABC4ED)
    }

    @Test
    fun `operation state extension properties use mock status code mapping`() {
        val state = OperationMockState.Mock(statusCode = 404, exampleName = "default")

        state.icon shouldBe Icons.Rounded.ErrorOutline
        state.contentColor shouldBe Color(color = 0xFF6F1111)
        state.containerColor shouldBe Color(color = 0xFFECB7B7)
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
    fun `fake OperationDescriptor creates requested amount and response count`() {
        val descriptors = OperationDescriptor.fake(
            amount = 2,
            availableResponsesAmount = 4,
            specId = "qa"
        )

        descriptors shouldHaveSize 2
        descriptors[0].specId shouldBe "qa"
        descriptors[0].operationId shouldBe "operation-1"
        descriptors[0].config.path shouldBe "/operation1"
        descriptors[0].availableResponses shouldHaveSize 4
    }

    @Test
    fun `fake OperationUiModel and MockResponse create requested amount`() {
        val operations = OperationUiModel.fake(amount = 5, availableResponsesAmount = 2)
        val responses = MockResponse.fake(amount = 4)

        operations shouldHaveSize 5
        operations[0].descriptor.availableResponses shouldHaveSize 2
        operations[0].currentState shouldBe OperationMockState.Mock(statusCode = 100, exampleName = "default")

        responses shouldHaveSize 4
        responses[1].exampleName shouldBe "default"
        responses[1].statusCode shouldBe 200
        responses[1].displayName shouldBe "Response 1"
    }
}
