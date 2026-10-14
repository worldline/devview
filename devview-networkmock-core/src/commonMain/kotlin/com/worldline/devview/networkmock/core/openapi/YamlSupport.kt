package com.worldline.devview.networkmock.core.openapi

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.encoding.Decoder

/**
 * The sole point of contact with kaml in this module.
 *
 * kaml's repo is archived (0.104.0 is the final release) — YAML support is best-effort. If a
 * future Kotlin/kotlinx-serialization upgrade breaks it, delete this file, the `libs.kaml`
 * dependency (`gradle/libs.versions.toml`, `devview-networkmock-core/build.gradle.kts`), and
 * the two `.yaml`/`.yml` extension checks in [OpenApiParser]'s format sniffing, and the
 * [typeNames] fallback in [SchemaTypeSerializer] — nothing else in this module references kaml.
 *
 * `strictMode = false` mirrors the `ignoreUnknownKeys = true` used for the JSON path: this
 * parser only models a subset of OpenAPI, so unmodeled fields in a real spec must not fail
 * decoding.
 */
internal object YamlSupport {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    /** Names in a YAML schema `type` node: one scalar (3.0) or a sequence (3.1); null for other decoders. */
    fun typeNames(decoder: Decoder): List<String>? =
        when (val node = (decoder as? YamlInput)?.node) {
            is YamlScalar -> listOf(element = node.content)
            is YamlList -> node.items.filterIsInstance<YamlScalar>().map { it.content }
            else -> null
        }

    fun decode(bytes: ByteArray): OpenApiDocument = yaml.decodeFromString(
        deserializer = OpenApiDocument.serializer(),
        string = bytes.decodeToString()
    )
}
