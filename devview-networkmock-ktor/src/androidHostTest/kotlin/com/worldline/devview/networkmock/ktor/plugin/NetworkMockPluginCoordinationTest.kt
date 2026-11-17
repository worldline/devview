package com.worldline.devview.networkmock.ktor.plugin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.worldline.devview.networkmock.core.NetworkMockResourceLoader
import com.worldline.devview.networkmock.core.model.FailureKind
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import com.worldline.devview.networkmock.core.repository.MockStateRepository
import com.worldline.devview.networkmock.ktor.fixtures.KtorPluginTestData
import com.worldline.devview.test.FakePreferencesDataStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMockPluginCoordinationTest {

    private val key = OperationKey(specId = "example", operationId = "getUser")
    private val sequence = OperationMockState.Sequence(
        responses = listOf(
            OperationMockState.Mock(statusCode = 200, mediaType = "application/json", exampleName = "default"),
            OperationMockState.Mock(statusCode = 404, mediaType = "application/json", exampleName = "default")
        )
    )
    private val responsePath = "files/networkmocks/responses/getUser-200.json"
    private val url = "https://staging.api.example.com/api/users/42"

    @Test
    fun concurrentConsumersReturnDifferentStepsAfterOneWinsTheCommit() = runTest {
        val ready = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val loads = AtomicInteger()
        val config = configuration { path ->
            if (path == responsePath && loads.incrementAndGet() <= 2) {
                if (loads.get() == 2) ready.complete(value = Unit)
                release.await()
            }
            resource(path = path)
        }
        config.loadConfiguration().getOrThrow()
        val repository = repository()
        val before = repository.getStateSnapshot().revision
        val networkCalls = AtomicInteger()
        client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
            val requests = List(size = 2) { async { client.get(urlString = url).status.value } }
            ready.await()
            release.complete(value = Unit)
            requests.awaitAll().shouldContainExactlyInAnyOrder(200, 404)
        }
        networkCalls.get() shouldBe 0
        repository.getStateSnapshot().revision shouldBe before + 1
        repository.getState().getOperationState(key = key) shouldBe sequence.copy(currentIndex = 1)
    }

    @Test
    fun latestOperationSelectionWinsWhileASequenceResponseIsBeingPrepared() = runTest {
        val replacements = listOf(
            OperationMockState.Network,
            OperationMockState.Failure(kind = FailureKind.TIMEOUT),
            sequence.responses[1]
        )
        replacements.forEach { replacement ->
            val ready = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val config = configuration { path ->
                if (path == responsePath) {
                    ready.complete(value = Unit)
                    release.await()
                }
                resource(path = path)
            }
            val repository = repository()
            val networkCalls = AtomicInteger()
            client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
                val request = async {
                    if (replacement is OperationMockState.Failure) {
                        shouldThrow<HttpRequestTimeoutException> { client.get(urlString = url) }
                        null
                    } else {
                        client.get(urlString = url).bodyAsText()
                    }
                }
                ready.await()
                repository.setOperationMockState(key = key, state = replacement)
                val revision = repository.getStateSnapshot().revision
                release.complete(value = Unit)
                request.await() shouldBe when (replacement) {
                    OperationMockState.Network -> "network"
                    is OperationMockState.Failure -> null
                    else -> resource(path = "files/networkmocks/responses/getUser-404.json").decodeToString()
                }
                repository.getStateSnapshot().revision shouldBe revision
            }
            networkCalls.get() shouldBe if (replacement == OperationMockState.Network) 1 else 0
            repository.getState().getOperationState(key = key) shouldBe replacement
        }
    }

    @Test
    fun aReplacementOfAReplacementWinsWhileItsResponseIsLoading() = runTest {
        listOf(OperationMockState.Network, OperationMockState.Failure(kind = FailureKind.TIMEOUT)).forEach { latest ->
            val firstReady = CompletableDeferred<Unit>()
            val firstRelease = CompletableDeferred<Unit>()
            val secondReady = CompletableDeferred<Unit>()
            val secondRelease = CompletableDeferred<Unit>()
            val config = configuration { path ->
                when (path) {
                    responsePath -> {
                        firstReady.complete(value = Unit)
                        firstRelease.await()
                    }
                    "files/networkmocks/responses/getUser-404.json" -> {
                        secondReady.complete(value = Unit)
                        secondRelease.await()
                    }
                }
                resource(path = path)
            }
            val repository = repository()
            val networkCalls = AtomicInteger()
            client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
                val request = async {
                    if (latest is OperationMockState.Failure) {
                        shouldThrow<HttpRequestTimeoutException> { client.get(urlString = url) }
                        null
                    } else {
                        client.get(urlString = url).bodyAsText()
                    }
                }
                firstReady.await()
                repository.setOperationMockState(key = key, state = sequence.responses[1])
                firstRelease.complete(value = Unit)
                secondReady.await()
                repository.setOperationMockState(key = key, state = latest)
                val revision = repository.getStateSnapshot().revision
                secondRelease.complete(value = Unit)
                request.await() shouldBe if (latest == OperationMockState.Network) "network" else null
                repository.getStateSnapshot().revision shouldBe revision
            }
            networkCalls.get() shouldBe if (latest == OperationMockState.Network) 1 else 0
            repository.getState().getOperationState(key = key) shouldBe latest
        }
    }

    @Test
    fun aSelectionChangedDuringConfigurationLoadingWinsBeforeTheProbabilityRoll() = runTest {
        listOf(OperationMockState.Network, OperationMockState.Failure(kind = FailureKind.TIMEOUT)).forEach { latest ->
            val ready = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var rolls = 0
            val random = object : Random() {
                override fun nextBits(bitCount: Int): Int = error("Expected nextDouble")
                override fun nextDouble(): Double {
                    rolls++
                    return 0.1
                }
            }
            val spec = KtorPluginTestData.defaultSpecJson.replace(
                oldValue = "\"operationId\": \"getUser\",",
                newValue = "\"operationId\": \"getUser\", \"x-devview\": { \"failureRate\": 0.5 },"
            )
            val config = configuration { path ->
                if (path == KtorPluginTestData.SPEC_PATH) {
                    ready.complete(value = Unit)
                    release.await()
                    spec.encodeToByteArray()
                } else {
                    resource(path = path)
                }
            }
            val repository = repository()
            val networkCalls = AtomicInteger()
            client(config = config, repository = repository, networkCalls = networkCalls, random = random).use { client ->
                val request = async {
                    if (latest is OperationMockState.Failure) {
                        shouldThrow<HttpRequestTimeoutException> { client.get(urlString = url) }
                        null
                    } else {
                        client.get(urlString = url).bodyAsText()
                    }
                }
                ready.await()
                repository.setOperationMockState(key = key, state = latest)
                val revision = repository.getStateSnapshot().revision
                release.complete(value = Unit)
                request.await() shouldBe if (latest == OperationMockState.Network) "network" else null
                repository.getStateSnapshot().revision shouldBe revision
            }
            rolls shouldBe 0
            networkCalls.get() shouldBe if (latest == OperationMockState.Network) 1 else 0
        }
    }

    @Test
    fun unavailableSelectedMocksNeverSendARealPostOrConsumeASequenceStep() = runTest {
        listOf("missing", "unreadable", "removed").forEach { problem ->
            listOf<OperationMockState>(sequence, sequence.responses[0]).forEach { selection ->
                val spec = KtorPluginTestData.defaultSpecJson.replace(oldValue = "\"get\":", newValue = "\"post\":")
                val config = configuration { path ->
                    when {
                        path == KtorPluginTestData.SPEC_PATH -> spec.encodeToByteArray()
                        path == responsePath && problem == "missing" -> error("Missing selected resource")
                        path == responsePath && problem == "unreadable" -> throw IOException("Unreadable selected resource")
                        else -> resource(path = path)
                    }
                }
                val repository = repository()
                val selected = if (problem == "removed") {
                    when (selection) {
                        is OperationMockState.Sequence -> selection.copy(
                            responses = listOf(selection.responses[0].copy(exampleName = "removed")) + selection.responses.drop(n = 1)
                        )
                        is OperationMockState.Mock -> selection.copy(exampleName = "removed")
                        else -> error("Expected mock or sequence")
                    }
                } else {
                    selection
                }
                repository.setOperationMockState(key = key, state = selected)
                val revision = repository.getStateSnapshot().revision
                val networkCalls = AtomicInteger()
                client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
                    if (problem == "unreadable") {
                        shouldThrow<IOException> { client.post(urlString = url) }
                    } else {
                        shouldThrow<IllegalStateException> { client.post(urlString = url) }
                    }
                }
                networkCalls.get() shouldBe 0
                repository.getStateSnapshot().revision shouldBe revision
                repository.getState().getOperationState(key = key) shouldBe selected
            }
        }
    }

    @Test
    fun disablingGlobalMockingRejectsAnAlreadyPreparedSequenceResponse() = runTest {
        val ready = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val config = configuration { path ->
            if (path == responsePath) {
                ready.complete(value = Unit)
                release.await()
            }
            resource(path = path)
        }
        val repository = repository()
        val networkCalls = AtomicInteger()
        client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
            val request = async { client.get(urlString = url).bodyAsText() }
            ready.await()
            repository.setGlobalMockingEnabled(enabled = false)
            val revision = repository.getStateSnapshot().revision
            release.complete(value = Unit)
            request.await() shouldBe "network"
            repository.getStateSnapshot().revision shouldBe revision
        }
        networkCalls.get() shouldBe 1
        repository.getState().getOperationState(key = key) shouldBe sequence
    }

    @Test
    fun resettingToAnIdenticalSequenceRetriesWithoutAnotherRollOrDelay() = runTest {
        val ready = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val responseLoads = AtomicInteger()
        var rolls = 0
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("Expected nextDouble")
            override fun nextDouble(): Double {
                rolls++
                return 0.9
            }
        }
        val spec = KtorPluginTestData.defaultSpecJson.replace(
            oldValue = "\"operationId\": \"getUser\",",
            newValue = "\"operationId\": \"getUser\", \"x-devview\": { \"delayMs\": 100, \"failureRate\": 0.5 },"
        )
        val config = configuration { path ->
            when (path) {
                KtorPluginTestData.SPEC_PATH -> spec.encodeToByteArray()
                responsePath -> {
                    if (responseLoads.incrementAndGet() == 1) {
                        ready.complete(value = Unit)
                        release.await()
                    }
                    resource(path = path)
                }
                else -> resource(path = path)
            }
        }
        val repository = repository()
        val start = testScheduler.currentTime
        client(config = config, repository = repository, random = random).use { client ->
            val request = async { client.get(urlString = url).status.value }
            ready.await()
            repository.resetOperationSequencePosition(key = key)
            release.complete(value = Unit)
            request.await() shouldBe 200
        }
        responseLoads.get() shouldBe 2
        rolls shouldBe 1
        testScheduler.currentTime - start shouldBe 100L
        repository.getState().getOperationState(key = key) shouldBe sequence.copy(currentIndex = 1)
    }

    @Test
    fun cancellationDuringResourceLoadingNeverConsumesOrHitsTheNetwork() = runTest {
        val ready = CompletableDeferred<Unit>()
        val config = configuration { path ->
            if (path == responsePath) {
                ready.complete(value = Unit)
                CompletableDeferred<Unit>().await()
            }
            resource(path = path)
        }
        val repository = repository()
        val revision = repository.getStateSnapshot().revision
        val networkCalls = AtomicInteger()
        client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
            val request = async { client.get(urlString = url) }
            ready.await()
            request.cancelAndJoin()
        }
        networkCalls.get() shouldBe 0
        repository.getStateSnapshot().revision shouldBe revision
        repository.getState().getOperationState(key = key) shouldBe sequence
    }

    @Test
    fun cancellationDuringConfiguredDelayNeverConsumesOrHitsTheNetwork() = runTest {
        val spec = KtorPluginTestData.defaultSpecJson.replace(
            oldValue = "\"operationId\": \"getUser\",",
            newValue = "\"operationId\": \"getUser\", \"x-devview\": { \"delayMs\": 100 },"
        )
        val ready = CompletableDeferred<Unit>()
        val config = configuration { path ->
            if (path == responsePath) ready.complete(value = Unit)
            if (path == KtorPluginTestData.SPEC_PATH) spec.encodeToByteArray() else resource(path = path)
        }
        val repository = repository()
        val revision = repository.getStateSnapshot().revision
        val networkCalls = AtomicInteger()
        client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
            val request = async { client.get(urlString = url) }
            ready.await()
            advanceTimeBy(delayTimeMillis = 99)
            runCurrent()
            request.cancelAndJoin()
        }
        networkCalls.get() shouldBe 0
        repository.getStateSnapshot().revision shouldBe revision
        repository.getState().getOperationState(key = key) shouldBe sequence
    }

    @Test
    fun configurationAndResponseCancellationPropagateInsteadOfFallingBack() = runTest {
        listOf(KtorPluginTestData.SPEC_PATH, responsePath).forEach { cancelledPath ->
            val config = configuration { path ->
                if (path == cancelledPath) throw CancellationException("Cancelled resource read")
                resource(path = path)
            }
            val repository = repository()
            val revision = repository.getStateSnapshot().revision
            val networkCalls = AtomicInteger()
            client(config = config, repository = repository, networkCalls = networkCalls).use { client ->
                shouldThrow<CancellationException> { client.get(urlString = url) }
            }
            networkCalls.get() shouldBe 0
            repository.getStateSnapshot().revision shouldBe revision
        }
    }

    @Test
    fun failedSequenceCommitDoesNotReturnAMockOrFallBackToTheNetwork() = runTest {
        val store = FailingDataStore()
        val repository = repository(store = store)
        val revision = repository.getStateSnapshot().revision
        store.failWrites = true
        val networkCalls = AtomicInteger()
        client(config = configuration(), repository = repository, networkCalls = networkCalls).use { client ->
            shouldThrow<IOException> { client.get(urlString = url) }.message shouldBe "Simulated write failure"
        }
        networkCalls.get() shouldBe 0
        repository.getStateSnapshot().revision shouldBe revision
        repository.getState().getOperationState(key = key) shouldBe sequence
    }

    @Test
    fun failedSnapshotReadDoesNotFallBackToTheNetwork() = runTest {
        val store = FailingDataStore()
        val repository = repository(store = store)
        store.failReads = true
        val networkCalls = AtomicInteger()
        client(config = configuration(), repository = repository, networkCalls = networkCalls).use { client ->
            shouldThrow<IOException> { client.get(urlString = url) }.message shouldBe "Simulated read failure"
        }
        networkCalls.get() shouldBe 0
    }

    private suspend fun repository(store: DataStore<Preferences> = FakePreferencesDataStore()): MockStateRepository =
        MockStateRepository(dataStore = store).also {
            it.setGlobalMockingEnabled(enabled = true)
            it.setOperationMockState(key = key, state = sequence)
        }

    private fun configuration(loader: NetworkMockResourceLoader = NetworkMockResourceLoader { resource(path = it) }): MockConfigRepository =
        MockConfigRepository(specPaths = listOf(KtorPluginTestData.SPEC_PATH), resourceLoader = loader)

    private fun resource(path: String): ByteArray = KtorPluginTestData.responseResources.getValue(key = path)
        .encodeToByteArray()

    private fun client(
        config: MockConfigRepository,
        repository: MockStateRepository,
        networkCalls: AtomicInteger = AtomicInteger(),
        random: Random = Random.Default
    ): HttpClient = HttpClient(engine = MockEngine {
        networkCalls.incrementAndGet()
        respond(content = "network")
    }) {
        install(plugin = NetworkMockPlugin) {
            mockRepository = config
            stateRepository = repository
            this.random = random
        }
    }

    private class FailingDataStore : DataStore<Preferences> {
        private val delegate = FakePreferencesDataStore()
        var failReads = false
        var failWrites = false

        override val data: Flow<Preferences> = flow {
            if (failReads) throw IOException("Simulated read failure")
            emitAll(flow = delegate.data)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            delegate.updateData { preferences ->
                val updated = transform(preferences)
                if (failWrites) throw IOException("Simulated write failure")
                updated
            }
    }
}
