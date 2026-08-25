# Network Mock Test Files

This directory contains OpenAPI 3.x spec files and mock response files for the DevView Network Mock module.

## 📋 Important: Response Structure Best Practices

All mock responses follow **REST API best practices** with consistent JSON structures across all status codes. This allows clients to use the same data models for both success and error responses.

**See [API_RESPONSE_STRUCTURE.md](./API_RESPONSE_STRUCTURE.md) for detailed explanation.**

**Key principle**: Error responses maintain the same structure as success responses, with data fields set to `null` and an `error` object present.

## File Structure

```
composeResources/files/networkmocks/
├── specs/                                          # One OpenAPI 3.x document per API group
│   ├── jsonplaceholder.json
│   └── sample-api.json
└── responses/                                      # Mock response bodies, referenced from
    └── {specId}/                                   # each spec via examples.<name>.externalValue
        └── {operationId}/
            └── {operationId}-{statusCode}[-{suffix}].json
```

There is no environment tier: a spec's `servers[]` lists every base URL it can be reached at (an app talks to exactly one at a time), and a spec spanning multiple API versions is just multiple operations with different paths in the same document — see `sample-api.json`'s `getUserProfile` (`/api/v1/...`) and `getUserProfileV2` (`/api/v2/...`).

### Current sample layout

```
responses/
├── jsonplaceholder/
│   ├── getUser/
│   │   ├── getUser-200.json               # Success response (example "default")
│   │   ├── getUser-404.json               # Not found (example "default")
│   │   ├── getUser-404-detailed.json      # Not found (example "detailed")
│   │   └── getUser-500.json               # Server error (example "default")
│   ├── listUsers/
│   │   ├── listUsers-200.json             # List with 3 users (example "default")
│   │   └── listUsers-200-empty.json       # Empty list (example "empty")
│   ├── createPost/
│   │   ├── createPost-201.json            # Created successfully
│   │   ├── createPost-400.json            # Validation error
│   │   └── createPost-401.json            # Unauthorized
│   └── getPost/
│       ├── getPost-200.json               # Success response
│       └── getPost-404.json               # Post not found
└── sample-api/
    ├── getUserProfile/                    # /api/v1/profile/{userId}
    │   ├── getUserProfile-200.json
    │   ├── getUserProfile-401.json
    │   └── getUserProfile-404.json
    ├── getUserProfileV2/                  # /api/v2/profile/{userId}
    │   ├── getUserProfile-200.json
    │   ├── getUserProfile-401.json
    │   └── getUserProfile-404.json
    └── updateProfile/
        ├── updateProfile-200.json         # Update successful
        └── updateProfile-400.json         # Validation error
```

## Configured Specs

### 1. JSONPlaceholder (`specs/jsonplaceholder.json`)
- **Spec ID**: `jsonplaceholder` (slugified from `info.title`)
- **Server**: `https://jsonplaceholder.typicode.com`
- **Purpose**: Test with a real public API
- **Operations**:
  - `GET /users/{userId}` (`getUser`) — Get user by ID, 500ms simulated delay via `x-devview`
  - `GET /users` (`listUsers`) — List all users, matches only when the `type=user` query parameter is present
  - `POST /posts` (`createPost`) — Create a post
  - `GET /posts/{postId}` (`getPost`) — Get post by ID

### 2. Sample API (`specs/sample-api.json`)
- **Spec ID**: `sample-api`
- **Servers**: `https://sample.api.staging.com`, `https://sample.api.com` — both map to the same operations
- **Purpose**: Demonstrate a spec-wide default delay (`x-devview.delayMs: 200`) and two API versions in one document
- **Operations**:
  - `GET /api/v1/profile/{userId}` (`getUserProfile`)
  - `GET /api/v2/profile/{userId}` (`getUserProfileV2`)
  - `PUT /api/v1/profile` (`updateProfile`)

## Usage Example

### 1. Add dependency to build.gradle.kts

```kotlin
commonMain.dependencies {
    implementation(projects.devviewNetworkmock)
}
```

### 2. Register the module

```kotlin
module(
    module = NetworkMock(
        resourceLoader = { path -> Res.readBytes(path) },
        specPaths = listOf(
            "files/networkmocks/specs/jsonplaceholder.json",
            "files/networkmocks/specs/sample-api.json"
        )
    )
)
```

The Ktor plugin needs no extra configuration — it resolves its repositories from the same `NetworkMock` registration:

```kotlin
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin)
}
```

### 3. Make requests that can be mocked

```kotlin
// This request can be mocked if configured in DevView UI
val response = client.get("https://jsonplaceholder.typicode.com/users/1")
```

### 4. Control mocking via DevView UI

- Open DevView in your app
- Navigate to "Network Mock" screen
- Toggle global mocking on/off
- Enable specific operation mocks per spec
- Select which response variant to return (200, 404, 500, etc.)

## Testing Different Scenarios

### Success Scenarios
- **Get User (200)**: Returns complete user data with all fields
- **List Users (200, "default")**: Returns array of 3 users
- **List Users (200, "empty")**: Returns empty array
- **Create Post (201)**: Returns created post with ID
- **Get Post (200)**: Returns post with content

### Error Scenarios
- **404, "default"**: Basic error message
- **404, "detailed"**: Comprehensive error with code, message, and timestamp
- **400 Validation**: Shows field-level validation errors
- **401 Unauthorized**: Authentication required message
- **500 Server Error**: Internal server error message

## Path Parameters

The specs include operations with path parameters:
- `/users/{userId}` — matches `/users/1`, `/users/123`, etc.
- `/posts/{postId}` — matches `/posts/1`, `/posts/456`, etc.
- `/api/v1/profile/{userId}`, `/api/v2/profile/{userId}` — match any user ID

These demonstrate the plugin's ability to match requests with dynamic path segments.

## Adding Your Own Mocks

### 1. Add a new spec file

```json
{
  "info": { "title": "Your API" },
  "servers": [{ "url": "https://your-api.com" }],
  "paths": {
    "/api/your/path": {
      "get": {
        "operationId": "yourEndpoint",
        "summary": "Your Endpoint Name",
        "responses": {
          "200": {
            "content": {
              "application/json": {
                "examples": {
                  "default": { "externalValue": "/files/networkmocks/responses/your-api/yourEndpoint/yourEndpoint-200.json" }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

Add the new spec's path to the `specPaths` list passed to `NetworkMock(...)`.

### 2. Create response files

```
responses/your-api/yourEndpoint/
├── yourEndpoint-200.json
├── yourEndpoint-404.json
└── yourEndpoint-500.json
```

### 3. Use in your app

The operation will automatically appear in the DevView Network Mock UI under its spec's tab.

## Notes

- All response files contain raw JSON (response body only)
- Response bodies are declared explicitly via `examples.<name>.externalValue` — there is no filename convention to follow beyond keeping paths readable; discovery reads exactly what the spec declares, nothing is probed
- The primary response for a status code is conventionally named the `"default"` example; any other name shows up as a suffix in the UI (e.g. `"detailed"` → "Not Found - Detailed (404)")
- A spec-wide or per-operation response delay is configured via the `x-devview.delayMs` extension (operation-level overrides spec-level)
