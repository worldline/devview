# NetworkMock Workflows

Step-by-step guides for common network mocking tasks.

## Adding a new mock endpoint

### 1. Add the endpoint to mocks.json

```json
{
  "apiGroups": [{
    "id": "my-backend",
    "name": "My Backend",
    "endpoints": [
      { "id": "getUser", "name": "Get User", "path": "/v1/users/{userId}", "method": "GET" }
    ],
    "environments": [
      { "id": "staging", "name": "Staging", "url": "https://staging.api.example.com" }
    ]
  }]
}
```

### 2. Create response files

Place response files under `composeResources/files/networkmocks/responses/`:

```
responses/my-backend/getUser/getUser-200.json      ← shared (all environments)
responses/my-backend/staging/getUser/getUser-200.json  ← staging-specific (takes priority)
```

File naming: `{endpointId}-{statusCode}[-{suffix}].json`

```
getUser-200.json         ← default 200 response
getUser-200-simple.json  ← alternate 200 response (simple variant)
getUser-404.json         ← 404 error response
getUser-500.json         ← server error response
```

### 3. Launch the app

Open DevView → Network Mock. Your new endpoint appears in the list for its group/environment tab.

## Testing an error scenario

1. Open DevView → Network Mock → tap your endpoint.
2. Tap a 4xx or 5xx response file to activate it.
3. The endpoint chip turns red/orange. The Ktor plugin now returns that response for matching requests.
4. After testing, tap "No mock" or use the "Reset to Network" toolbar action to restore pass-through.

## Using environment-specific responses

To serve a different response for production vs staging, place environment-specific files at higher priority:

```
responses/my-backend/getUser/getUser-200.json          ← shared fallback
responses/my-backend/production/getUser/getUser-200.json  ← production-specific
```

The environment is determined at interception time from the request hostname — no manual selection needed.

## Using endpoint path overrides per environment

In `mocks.json`, add `endpointOverrides` to a specific environment:

```json
{
  "id": "production",
  "url": "https://api.example.com",
  "endpointOverrides": [
    { "id": "getUser", "path": "/v2/users/{userId}" }
  ]
}
```

The production environment now uses `/v2/users/{userId}` for `getUser` while staging keeps `/v1/users/{userId}`.

## Resetting all mocks

- **UI**: Open DevView → Network Mock → tap the restore icon in the top toolbar.
- **All mocks are reset to `Network` state**, including endpoints the user has never explicitly touched.

## Related Modules

- [NetworkMock](networkmock.md): Overview and installation.
- [NetworkMock Core](networkmock-core.md): Config format details, request matching.
- [NetworkMock UI](networkmock-ui.md): Screen descriptions.
- [NetworkMock Ktor](networkmock-ktor.md): Plugin installation.
