package com.worldline.devview.networkmock.core.repository

import com.worldline.devview.networkmock.core.model.OperationKey
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/**
 * Loads the real, shipped sample specs (`sample/network`'s `composeResources/files/networkmocks/`)
 * through [MockConfigRepository] itself — every other test in this module uses hand-built inline
 * JSON/YAML fixtures, so nothing previously guarded the actual sample app against silently
 * drifting out of sync with what this parser accepts (see #91).
 *
 * Reads the sample's spec/response files directly from disk via [File] rather than depending on
 * the `sample:network` module at compile time (which would invert this module's place in the
 * dependency graph — `sample` depends on the `networkmock` family, not the reverse). The absolute
 * path is supplied by `devview-networkmock-core/build.gradle.kts` as the
 * `devview.sampleNetworkResourcesDir` system property, computed once at Gradle configuration time
 * so this test doesn't depend on the JVM working directory at run time.
 *
 * `androidHostTest`-only (not `commonTest`): `java.io.File` isn't available on Kotlin/Native, and
 * this is a plain JVM sanity check — it doesn't need multiplatform coverage.
 */
class RealSampleSpecTest {

    @Test
    fun `sample-api spec parses successfully through the real repository`() = runTest {
        val repository = repositoryFor(specPath = "files/networkmocks/specs/sample-api.json")

        val config = repository.loadConfiguration().getOrThrow()

        val spec = config.specs.single()
        spec.id shouldBe "sample-api"
        spec.operations.map { it.operationId } shouldContainExactlyInAnyOrder listOf(
            "getUserProfile",
            "getUserProfileV2",
            "updateProfile"
        )
    }

    @Test
    fun `sample-api's declared response files all load successfully`() = runTest {
        val repository = repositoryFor(specPath = "files/networkmocks/specs/sample-api.json")

        val responses = repository.discoverResponseFiles(
            key = OperationKey(specId = "sample-api", operationId = "getUserProfile")
        )

        responses.map { it.statusCode }.sorted() shouldBe listOf(200, 401, 404)
    }

    @Test
    fun `jsonplaceholder spec parses successfully through the real repository`() = runTest {
        val repository = repositoryFor(specPath = "files/networkmocks/specs/jsonplaceholder.json")

        val config = repository.loadConfiguration().getOrThrow()

        val spec = config.specs.single()
        spec.id shouldBe "jsonplaceholder"
        spec.operations.map { it.operationId } shouldContainExactlyInAnyOrder listOf(
            "getUser",
            "listUsers",
            "createPost",
            "getPost",
            "updateUser",
            "deleteUser",
            "createUser",
            "listPosts",
            "updatePost",
            "deletePost",
            "getPostComments",
            "listComments",
            "createComment",
            "getComment",
            "listAlbums",
            "createAlbum",
            "getAlbum",
            "getAlbumPhotos",
            "listPhotos",
            "getPhoto",
            "listTodos",
            "createTodo",
            "getTodo",
            "updateTodo"
        )
    }

    @Test
    fun `jsonplaceholder's declared response files all load successfully`() = runTest {
        val repository = repositoryFor(specPath = "files/networkmocks/specs/jsonplaceholder.json")

        val responses = repository.discoverResponseFiles(
            key = OperationKey(specId = "jsonplaceholder", operationId = "getUser")
        )

        // 200 (1 example) + 404 (2 examples: default, detailed) + 500 (1 example) = 4.
        responses shouldHaveSize 4
        responses.map { it.statusCode }.sorted() shouldBe listOf(200, 404, 404, 500)
    }

    private fun repositoryFor(specPath: String): MockConfigRepository = MockConfigRepository(
        specPaths = listOf(specPath),
        resourceLoader = { path -> readSampleResource(path = path) }
    )

    private fun readSampleResource(path: String): ByteArray {
        val resourcesRoot = System.getProperty("devview.sampleNetworkResourcesDir")
            ?: error(
                message = "devview.sampleNetworkResourcesDir system property is not set - " +
                    "check devview-networkmock-core/build.gradle.kts's tasks.withType<Test> block."
            )
        return File(resourcesRoot, path).readBytes()
    }
}

