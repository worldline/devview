# devview-test

Shared test utilities for DevView modules.

## Current helpers

- `waitUntilTagCount`, `waitUntilTagExists`, `waitUntilTagGone` for Compose device tests.
- `runTestWithDispatchers` and `testDispatchers` for coroutine-based unit tests.
- `Flow.assertEmitsExactly` for concise Turbine assertions.
- `FakePreferencesDataStore` for in-memory `DataStore<Preferences>` tests.

## DataStore fixtures

- Use `FakePreferencesDataStore` when your test needs a normal in-memory Preferences DataStore.
- Keep module-local fakes only for special behavior (for example, a DataStore that throws `IOException` to test recovery paths).

## Usage

Add to a test source set dependency:

```kotlin
androidDeviceTest {
    dependencies {
        implementation(projects.devviewTest)
    }
}
```

```kotlin
commonTest {
    dependencies {
        implementation(projects.devviewTest)
    }
}
```
