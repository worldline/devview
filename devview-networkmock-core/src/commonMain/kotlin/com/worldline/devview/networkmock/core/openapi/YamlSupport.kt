package com.worldline.devview.networkmock.core.openapi

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

/**
 * The sole point of contact with kaml in this module.
 *
 * kaml's repo is archived (0.104.0 is the final release) — YAML support is best-effort. If a
 * future Kotlin/kotlinx-serialization upgrade breaks it, delete this file, the `libs.kaml`
 * dependency (`gradle/libs.versions.toml`, `devview-networkmock-core/build.gradle.kts`), and
 * the two `.yaml`/`.yml` extension checks in [OpenApiParser]'s format sniffing — nothing else
 * in this module references kaml.
 *
 * `strictMode = false` mirrors the `ignoreUnknownKeys = true` used for the JSON path: this
 * parser only models a subset of OpenAPI, so unmodeled fields in a real spec must not fail
 * decoding.
 */
internal object YamlSupport {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    fun decode(bytes: ByteArray): OpenApiDocument = yaml.decodeFromString(
        deserializer = OpenApiDocument.serializer(),
        string = bytes.decodeToString()
    )
}
