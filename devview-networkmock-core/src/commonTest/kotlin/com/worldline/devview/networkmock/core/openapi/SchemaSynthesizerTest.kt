package com.worldline.devview.networkmock.core.openapi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

class SchemaSynthesizerTest {

    /** No `$ref`s in these fixtures — every schema is passed through unchanged. */
    private val noRefs: suspend (SchemaObject) -> SchemaObject = { it }

    // region primitives

    @Test
    fun `synthesizes a placeholder string for a string schema`() = runTest {
        val result = SchemaSynthesizer.synthesize(
            schema = SchemaObject(type = "string"),
            resolveSchema = noRefs
        )

        result shouldBe JsonPrimitive("string")
    }

    @Test
    fun `synthesizes zero for an integer schema`() = runTest {
        val result = SchemaSynthesizer.synthesize(
            schema = SchemaObject(type = "integer"),
            resolveSchema = noRefs
        )

        result shouldBe JsonPrimitive(0)
    }

    @Test
    fun `synthesizes zero for a number schema`() = runTest {
        val result = SchemaSynthesizer.synthesize(
            schema = SchemaObject(type = "number"),
            resolveSchema = noRefs
        )

        result shouldBe JsonPrimitive(0)
    }

    @Test
    fun `synthesizes false for a boolean schema`() = runTest {
        val result = SchemaSynthesizer.synthesize(
            schema = SchemaObject(type = "boolean"),
            resolveSchema = noRefs
        )

        result shouldBe JsonPrimitive(false)
    }

    // endregion

    // region enum

    @Test
    fun `synthesizes the first enum value instead of a generic placeholder`() = runTest {
        val schema = SchemaObject(type = "string", enum = listOf("ACTIVE", "INACTIVE"))

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe JsonPrimitive("ACTIVE")
    }

    // endregion

    // region object

    @Test
    fun `synthesizes a nested object by recursively synthesizing each property`() = runTest {
        val schema = SchemaObject(
            type = "object",
            properties = mapOf(
                "id" to SchemaObject(type = "integer"),
                "name" to SchemaObject(type = "string")
            )
        )

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe buildJsonObject {
            put("id", JsonPrimitive(0))
            put("name", JsonPrimitive("string"))
        }
    }

    @Test
    fun `treats a schema with properties but no declared type as an object`() = runTest {
        val schema = SchemaObject(properties = mapOf("id" to SchemaObject(type = "integer")))

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe buildJsonObject { put("id", JsonPrimitive(0)) }
    }

    // endregion

    // region array

    @Test
    fun `synthesizes a single-element array from items`() = runTest {
        val schema = SchemaObject(type = "array", items = SchemaObject(type = "string"))

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe buildJsonArray { add(JsonPrimitive("string")) }
    }

    @Test
    fun `throws when an array schema declares no items`() = runTest {
        shouldThrow<IllegalStateException> {
            SchemaSynthesizer.synthesize(schema = SchemaObject(type = "array"), resolveSchema = noRefs)
        }
    }

    // endregion

    // region allOf

    @Test
    fun `allOf merges every members properties into one effective object`() = runTest {
        val schema = SchemaObject(
            allOf = listOf(
                SchemaObject(properties = mapOf("id" to SchemaObject(type = "integer"))),
                SchemaObject(properties = mapOf("name" to SchemaObject(type = "string")))
            )
        )

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe buildJsonObject {
            put("id", JsonPrimitive(0))
            put("name", JsonPrimitive("string"))
        }
    }

    @Test
    fun `allOf throws on conflicting property definitions across members`() = runTest {
        val schema = SchemaObject(
            allOf = listOf(
                SchemaObject(properties = mapOf("id" to SchemaObject(type = "integer"))),
                SchemaObject(properties = mapOf("id" to SchemaObject(type = "string")))
            )
        )

        shouldThrow<IllegalStateException> {
            SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)
        }
    }

    @Test
    fun `allOf tolerates the same property redeclared identically across members`() = runTest {
        val schema = SchemaObject(
            allOf = listOf(
                SchemaObject(properties = mapOf("id" to SchemaObject(type = "integer"))),
                SchemaObject(properties = mapOf("id" to SchemaObject(type = "integer")))
            )
        )

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe buildJsonObject { put("id", JsonPrimitive(0)) }
    }

    // endregion

    // region oneOf

    @Test
    fun `oneOf without a discriminator synthesizes the first declared variant`() = runTest {
        val schema = SchemaObject(oneOf = listOf(SchemaObject(type = "string"), SchemaObject(type = "integer")))

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe JsonPrimitive("string")
    }

    @Test
    fun `oneOf with a discriminator still synthesizes the first declared variant`() = runTest {
        val schema = SchemaObject(
            oneOf = listOf(SchemaObject(type = "string"), SchemaObject(type = "integer")),
            discriminator = DiscriminatorObject(propertyName = "type")
        )

        val result = SchemaSynthesizer.synthesize(schema = schema, resolveSchema = noRefs)

        result shouldBe JsonPrimitive("string")
    }

    @Test
    fun `throws when oneOf declares no variants`() = runTest {
        shouldThrow<IllegalStateException> {
            SchemaSynthesizer.synthesize(schema = SchemaObject(oneOf = emptyList()), resolveSchema = noRefs)
        }
    }

    // endregion

    // region dollar-ref resolution and unrecognized shapes

    @Test
    fun `resolves a dollar-ref via the provided resolveSchema callback before synthesizing`() = runTest {
        val target = SchemaObject(type = "string")
        val schema = SchemaObject(ref = "#/components/schemas/Foo")

        val result = SchemaSynthesizer.synthesize(
            schema = schema,
            resolveSchema = { if (it.ref == "#/components/schemas/Foo") target else it }
        )

        result shouldBe JsonPrimitive("string")
    }

    @Test
    fun `throws when a schema declares no recognizable shape`() = runTest {
        shouldThrow<IllegalStateException> {
            SchemaSynthesizer.synthesize(schema = SchemaObject(), resolveSchema = noRefs)
        }
    }

    // endregion
}

