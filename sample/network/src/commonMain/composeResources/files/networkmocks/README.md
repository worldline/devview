# Network Mock Test Files

This directory contains test configuration and mock response files for the DevView Network Mock module.

## 📋 Important: Response Structure Best Practices

All mock responses follow **REST API best practices** with consistent JSON structures across all status codes. This allows clients to use the same data models for both success and error responses.

**See [API_RESPONSE_STRUCTURE.md](./API_RESPONSE_STRUCTURE.md) for detailed explanation.**

**Key principle**: Error responses maintain the same structure as success responses, with data fields set to `null` and an `error` object present.

## File Structure

```
composeResources/files/networkmocks/
├── mocks.json                              # Main configuration file
└── responses/                              # Mock response files organized by endpoint
    ├── getUser/
    │   ├── getUser-200.json               # Success response
    │   ├── getUser-404.json               # Not found (simple)
    │   ├── getUser-404-detailed.json      # Not found (detailed error)
    │   └── getUser-500.json               # Server error
    ├── listUsers/
    │   ├── listUsers-200.json             # List with 3 users
    │   └── listUsers-200-empty.json       # Empty list
    ├── createPost/
    │   ├── createPost-201.json            # Created successfully
    │   ├── createPost-400.json            # Validation error
    │   └── createPost-401.json            # Unauthorized
    ├── getPost/
    │   ├── getPost-200.json               # Success response
    │   └── getPost-404.json               # Post not found
    ├── getUserProfile/
    │   ├── getUserProfile-200.json        # Success response
    │   ├── getUserProfile-401.json        # Unauthorized
    │   └── getUserProfile-404.json        # Profile not found
    └── updateProfile/
        ├── updateProfile-200.json         # Update successful
        └── updateProfile-400.json         # Validation error
```

## Configured Hosts

### 1. JSONPlaceholder (Public API)
- **Host ID**: `jsonplaceholder`
- **URL**: `https://jsonplaceholder.typicode.com`
- **Purpose**: Test with a real public API
- **Endpoints**:
  - `GET /users/{userId}` - Get user by ID
  - `GET /users` - List all users
  - `POST /posts` - Create a post
  - `GET /posts/{postId}` - Get post by ID

### 2. Staging Environment
- **Host ID**: `staging`
- **URL**: `https://staging.api.example.com`
- **Purpose**: Demonstrate multi-host configuration
- **Endpoints**:
  - `GET /api/v1/profile/{userId}` - Get user profile
  - `PUT /api/v1/profile` - Update profile

## Usage Example

### 1. Add dependency to build.gradle.kts

```kotlin
commonMain.dependencies {
    implementation(projects.devviewNetworkmock)
}
```

### 2. Install the plugin in your HttpClient

```kotlin
val dataStore = createDataStore { /* ... */ }

val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin) {
        configPath = "files/networkmocks/mocks.json"
        mockRepository = MockConfigRepository(configPath)
        stateRepository = MockStateRepository(dataStore)
    }
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
- Enable specific endpoint mocks
- Select which response to return (200, 404, 500, etc.)

## Testing Different Scenarios

### Success Scenarios
- **Get User (200)**: Returns complete user data with all fields
- **List Users (200)**: Returns array of 3 users
- **List Users (200-empty)**: Returns empty array
- **Create Post (201)**: Returns created post with ID
- **Get Post (200)**: Returns post with content

### Error Scenarios
- **404 Simple**: Basic error message
- **404 Detailed**: Comprehensive error with code, message, and timestamp
- **400 Validation**: Shows field-level validation errors
- **401 Unauthorized**: Authentication required message
- **500 Server Error**: Internal server error message

## Path Parameters

The configuration includes endpoints with path parameters:
- `/users/{userId}` - matches `/users/1`, `/users/123`, etc.
- `/posts/{postId}` - matches `/posts/1`, `/posts/456`, etc.
- `/api/v1/profile/{userId}` - matches any user ID

These demonstrate the plugin's ability to match requests with dynamic path segments.

## Adding Your Own Mocks

### 1. Add endpoint to mocks.json

```json
{
  "hosts": [
    {
      "id": "your-host",
      "url": "https://your-api.com",
      "endpoints": [
        {
          "id": "yourEndpoint",
          "name": "Your Endpoint Name",
          "path": "/api/your/path",
          "method": "GET"
        }
      ]
    }
  ]
}
```

### 2. Create response files

Create folder: `responses/yourEndpoint/`

Add files following naming convention:
- `yourEndpoint-200.json` - Success
- `yourEndpoint-404.json` - Not found
- `yourEndpoint-500.json` - Server error

### 3. Use in your app

The endpoint will automatically appear in the DevView Network Mock UI.

## Notes

- All response files contain raw JSON (response body only)
- File names follow the pattern: `{endpointId}-{statusCode}[-{suffix}].json`
- Optional suffix helps differentiate multiple responses with the same status code
- The plugin discovers response files automatically based on naming convention


