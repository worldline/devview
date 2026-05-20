# devview-test

Shared test utilities for DevView modules.

## Current helpers

- `waitUntilTagCount`, `waitUntilTagExists`, `waitUntilTagGone` for Compose device tests.
- `runTestWithDispatchers` and `testDispatchers` for coroutine-based unit tests.
- `Flow.assertEmitsExactly` for concise Turbine assertions.

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

