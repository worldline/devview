# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What the Sample App Demonstrates

A Kotlin Multiplatform sample app (Android + iOS) showing a complete DevView integration: feature flags (`FeatureFlip`), analytics inspection (`Analytics`), network mocking (`NetworkMock`), and a custom module (`TestModule`). It also exercises the Ktor Network Mock plugin end-to-end against two real API groups (JSONPlaceholder and a fictional Sample API).

## Sub-module Roles

```
sample/
├── androidApp   — Android-only entry point; just calls DevViewApp() from MainActivity.
│                  No DevView logic here. All behaviour lives in shared.
├── shared       — KMP library; owns all DevView integration (module registration,
│                  the DevView overlay, feature flags, analytics, TestModule).
│                  Depends on all DevView modules + sample:network.
└── network      — KMP library; owns the Ktor HttpClient, NetworkMockPlugin wiring,
                   and the mocks.json resource tree.
                   Depends only on devview-networkmock-ktor (not the full UI module).
```

`shared` exports `projects.sample.network` from the iOS static framework so Swift code sees both modules as one.

## How DevView Modules Are Registered

`shared/src/commonMain/.../DevViewApp.kt` — the single integration point:

```kotlin
val modules = rememberModules {
    module(module = FeatureFlip)
    module(module = Analytics())
    module(
        module = NetworkMock(
            resourceLoader = { path -> Res.readBytes(path = path) }
        )
    )
    module(module = TestModule)
}
```

`rememberModules` calls `initDataStore()` then `initModule()` on each module in order. The `DevView` composable is rendered as an overlay on top of `App`, sharing the same `MaterialTheme` — so dark-mode toggled via the FeatureFlip panel takes effect on the whole screen, not just the overlay.

`NetworkMock` requires a `resourceLoader` lambda; the lambda receives a resource path string (e.g. `"files/networkmocks/mocks.json"`) and must return raw bytes. The sample wires this directly to `Res.readBytes` from `sample:network`'s generated Compose Resources class.

## How the Ktor Mock Plugin Is Wired

`network/src/commonMain/.../BaseHttpClientConfig.kt` defines the shared client builder:

```kotlin
install(plugin = NetworkMockPlugin)
```

No manual repository wiring is needed. `NetworkMockPlugin` resolves its `MockConfigRepository` and `MockStateRepository` from `NetworkMockInitializer`, which is populated when `NetworkMock` runs `initModule()` inside `rememberModules`. This means `rememberModules { }` must be called (and composed) before any `createHttpClientWithMocking()` call reaches the network layer.

Platform actuals:
- Android (`HttpClientWithMocking.android.kt`): `HttpClient(OkHttp, block = baseHttpClientConfig { ... })`
- iOS (`HttpClientWithMocking.ios.kt`): `HttpClient(Darwin, block = baseHttpClientConfig { ... })`

`rememberHttpClientWithMocking()` wraps `createHttpClientWithMocking()` in `remember { }` so the client survives recompositions.

## Mock Resource Layout

Mock files live under `network/src/commonMain/composeResources/files/networkmocks/`:

```
mocks.json                                       # API groups, endpoints, environments
responses/
  {groupId}/
    {endpointId}/
      {endpointId}-{statusCode}[-{suffix}].json  # Shared across environments
  {groupId}/
    {environmentId}/
      {endpointId}/
        {endpointId}-{statusCode}.json           # Environment-specific override (takes priority)
```

`network/build.gradle.kts` sets `publicResClass = true` and `generateResClass = always` — required so the `Res` class generated from `network`'s resources is accessible from `shared`.

## Custom Module Pattern (TestModule)

`TestModule` demonstrates the minimum required to add a custom tab to DevView:

1. Declare a `sealed interface` extending `NavKey` with `@Serializable` data objects for each screen.
2. Implement `Module`: set `section`, populate `destinations` map using `withTitle` / `asDestination`, set `entryDestination`, register serializers in `registerSerializers`, and provide Compose content in `registerContent`.
3. Pass the object to `rememberModules { module(module = TestModule) }`.

## Non-obvious Patterns

- `ExpectSuccessAttributeKey` in `BaseHttpResponseValidator` lets individual requests opt out of the default exception-on-error behaviour by setting `attributes[ExpectSuccessAttributeKey] = false` on the request builder.
- Environment-specific mock response files under `{groupId}/{environmentId}/` take priority over shared files in `{groupId}/` during discovery — useful to provide prod-specific variants without duplicating every response.
- The `sample-api` group in `mocks.json` shows endpoint path overrides per environment (`endpointOverrides`) — the `prod` environment redirects `getUserProfile` to `/api/v2/profile/{userId}`.
