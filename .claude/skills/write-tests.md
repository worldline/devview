---
name: write-tests
description: Write tests for DevView modules following the correct source set, naming, and assertion conventions
---

You are writing tests for a DevView module. The conventions here are NOT obvious — the wrong style still compiles and runs but diverges from the codebase. Read this fully before writing a single test.

## Step 1: Choose the source set

| What you're testing | Source set | Runs via |
|---------------------|------------|----------|
| Pure Kotlin logic, no Android classes | `commonTest` | `testAndroidHostTest` (JVM) |
| ViewModel, needs MockK or Android classes | `androidHostTest` | `testAndroidHostTest` (JVM) |
| Compose UI, visual rendering | `androidDeviceTest` | `connectedAndroidDeviceTest` (emulator) |

When in doubt: if it can be in `commonTest`, put it there. Host tests run 10× faster than device tests.

## Step 2: Test class style

**NOT Kotest FunSpec.** FunSpec is used ONLY in `konsist/`. Feature module tests use plain JUnit-style `@Test`.

```kotlin
// CORRECT — commonTest or androidHostTest
class MyRepositoryTest {

    @Test
    fun `setEnabled true persists to DataStore`() = runTest {
        // ...
    }
}

// WRONG — do not use FunSpec in feature modules
class MyRepositoryTest : FunSpec({
    test("...") { }
})
```

## Step 3: Naming conventions

**commonTest** — backtick natural language:
```kotlin
fun `getConfig returns default when DataStore is empty`() = runTest { }
fun `setGlobalMockingEnabled true persists to DataStore`() = runTest { }
fun `init with empty DataStore emits loading then default state`() = runTest { }
```

**androidHostTest** (ViewModels) — camelCase with underscores as separators:
```kotlin
fun initialUiState_isLoading_whileConfigIsStillLoading() = runTest { }
fun onRetry_reloadsConfig() = runTest { }
```

## Step 4: Assertions

Always use Kotest infix matchers. Never `assertEquals`, `assertNotNull`, or `assertTrue`.

```kotlin
// CORRECT
result shouldBe "expected"
list shouldHaveSize 3
list shouldContainExactly listOf("a", "b")
obj shouldBeInstanceOf MyClass::class
value shouldNotBe null

// WRONG
assertEquals("expected", result)
assertNotNull(value)
assertTrue(list.isNotEmpty())
```

## Step 5: Coroutines

Always wrap async tests in `runTest { }`:
```kotlin
@Test
fun `emits updated state after toggle`() = runTest {
    val stateFlow = repository.stateFlow
    repository.setEnabled(true)
    stateFlow.value shouldBe MyState(enabled = true)
}
```

For flows that emit over time, use Turbine:
```kotlin
@Test
fun `emits values in order`() = runTest {
    repository.events.test {
        repository.trigger()
        awaitItem() shouldBe Event.Triggered
        cancelAndIgnoreRemainingEvents()
    }
}
```

Use `assertEmitsExactly` from `devview-test` for ordered multi-item assertions:
```kotlin
stateFlow.assertEmitsExactly(State.Loading, State.Success(data))
```

## Step 6: DataStore

Use `FakePreferencesDataStore` from `devview-test`:
```kotlin
// In test setup
private val dataStore = FakePreferencesDataStore()
private val repository = MyRepository(dataStore)
```

Only create a custom fake (e.g. `ThrowingPreferencesDataStore`) when testing error paths that `FakePreferencesDataStore` can't simulate.

## Step 7: ViewModel tests (androidHostTest)

Extend `ViewModelTest` base class:
```kotlin
class MyViewModelTest : ViewModelTest() {

    private lateinit var viewModel: MyViewModel

    @BeforeTest
    override fun setup() {
        super.setup()
        viewModel = MyViewModel(
            repository = FakeMyRepository(),
            dispatchers = dispatchers,
        )
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun initialUiState_isLoading() = runTest {
        viewModel.uiState.value shouldBe MyUiState.Loading
    }
}
```

The base class wires `dispatchers.unconfined` as the Main dispatcher and cancels the view model scope in tearDown.

## Step 8: Compose UI tests (androidDeviceTest)

```kotlin
@Test
fun myScreen_showsTitle() = runComposeUiTest {
    setContent {
        MyScreen(state = MyState.Success(items = fakeItems))
    }
    waitUntilTagExists("my_screen_title")
    onNodeWithTag("my_screen_title").assertIsDisplayed()
    onNodeWithTag("item_list").performScrollToIndex(5)
    onNodeWithTag("item_5").performClick()
    onNodeWithTag("detail_screen").assertExists()
}
```

Use hand-written `Fake*` repositories/dependencies (not MockK) in device tests. Keep fixtures in a `fixtures/` package alongside the test file.

## Step 9: Mocking

| Source set | Library | Notes |
|------------|---------|-------|
| `androidHostTest` | MockK | Full mocking support |
| `commonTest` | Mokkery | KMP-compatible mocking |
| `androidDeviceTest` | Hand-written Fakes | No reflection-based mocking |

```kotlin
// androidHostTest — MockK
val repo = mockk<MyRepository>()
every { repo.getConfig() } returns flowOf(Config.Default)

// commonTest — prefer hand-written Fakes over Mokkery for simplicity
class FakeMyRepository : MyRepository {
    var returnValue: Config = Config.Default
    override fun getConfig() = flowOf(returnValue)
}
```

## Step 10: Organization

For files with many tests, group with regions:
```kotlin
class MyRepositoryTest {

    // region getConfig

    @Test
    fun `getConfig returns default when DataStore is empty`() { }

    @Test
    fun `getConfig returns persisted value`() { }

    // endregion

    // region setEnabled

    @Test
    fun `setEnabled persists to DataStore`() { }

    // endregion
}
```

Use private factory functions to keep test bodies focused:
```kotlin
private fun createRepository(
    dataStore: PreferencesDataStore = FakePreferencesDataStore(),
    config: Config = Config.Default,
) = MyRepository(dataStore, config)
```

## Run the tests

```shell
# Host tests (fast, no device):
.\gradlew.bat :devview-myfeature:testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration

# Single test class:
.\gradlew.bat :devview-myfeature:testAndroidHostTest --tests "com.worldline.devview.myfeature.MyRepositoryTest" -Pandroidx.baselineprofile.skipgeneration

# Device tests (requires emulator):
.\gradlew.bat :devview-myfeature:connectedAndroidDeviceTest -Pandroidx.baselineprofile.skipgeneration
```
