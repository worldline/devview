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
import com.worldline.devview.networkmock.core.model.EndpointDescriptor
import com.worldline.devview.networkmock.core.model.EndpointMockState
import com.worldline.devview.networkmock.core.model.MockResponse
import com.worldline.devview.networkmock.viewmodel.EndpointUiModel
import com.worldline.devview.networkmock.viewmodel.HostUiModel
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
    fun `endpoint state extension properties use network defaults`() {
        val state = EndpointMockState.Network

        state.icon shouldBe Icons.Rounded.Wifi
        state.contentColor shouldBe Color(color = 0xFF0D1F3A)
        state.containerColor shouldBe Color(color = 0xFFABC4ED)
    }

    @Test
    fun `endpoint state extension properties use mock status code mapping`() {
        val state = EndpointMockState.Mock(responseFile = "response-404.json")

        state.icon shouldBe Icons.Rounded.ErrorOutline
        state.contentColor shouldBe Color(color = 0xFF6F1111)
        state.containerColor shouldBe Color(color = 0xFFECB7B7)
    }

    @Test
    fun `fake HostUiModel creates requested amount with nested endpoints`() {
        val hosts = HostUiModel.fake(amount = 3)

        hosts shouldHaveSize 3
        hosts[0].id shouldBe "host-0"
        hosts[0].name shouldBe "Host 0"
        hosts[0].url shouldBe "https://api.host0.com"
        hosts[0].endpoints shouldHaveSize 7
    }

    @Test
    fun `fake EndpointDescriptor creates requested amount and response count`() {
        val descriptors = EndpointDescriptor.fake(amount = 2, availableResponsesAmount = 4, hostId = "qa")

        descriptors shouldHaveSize 2
        descriptors[0].hostId shouldBe "qa"
        descriptors[0].endpointId shouldBe "endpoint-0"
        descriptors[0].config.path shouldBe "/endpoint0"
        descriptors[0].availableResponses shouldHaveSize 4
    }

    @Test
    fun `fake EndpointUiModel and MockResponse create requested amount`() {
        val endpoints = EndpointUiModel.fake(amount = 5, availableResponsesAmount = 2)
        val responses = MockResponse.fake(amount = 4)

        endpoints shouldHaveSize 5
        endpoints[0].descriptor.availableResponses shouldHaveSize 2
        endpoints[0].currentState shouldBe EndpointMockState.Mock(responseFile = "response-100.json")

        responses shouldHaveSize 4
        responses[1].fileName shouldBe "response1.json"
        responses[1].statusCode shouldBe 200
        responses[1].displayName shouldBe "Response 1"
    }
}
