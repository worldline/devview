package com.worldline.devview.networkmock.core

import com.charleskorn.kaml.Yaml
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.serialization.Serializable

/**
 * Gate for kaml (YAML support in the upcoming OpenAPI parser, see #73).
 *
 * kaml's repo is archived and its final release (0.104.0) was built against an older
 * Kotlin/kotlinx-serialization than this project uses. This test — together with the iOS
 * cross-compile check run alongside it in CI — is the fail-fast signal: if either breaks,
 * YAML support is dropped from the OpenAPI parser and this file (plus the `libs.kaml`
 * dependency) is deleted, per the epic's documented JSON-only fallback.
 */
class KamlSmokeTest {

    @Serializable
    private data class Sample(val name: String, val count: Int)

    @Test
    fun `kaml parses a trivial YAML document via kotlinx-serialization`() {
        val yaml = """
            name: getUser
            count: 2
        """.trimIndent()

        val result = Yaml.default.decodeFromString(Sample.serializer(), yaml)

        result shouldBe Sample(name = "getUser", count = 2)
    }
}
