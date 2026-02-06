# Network Mock Implementation Plan

## Overview
Enable KMP/CMP developers to mock individual API calls on the fly by defining JSON configuration files that specify which APIs to mock, their HTTP methods, paths, and the host(s) for which mocking should be active.

## Current State
- Module has basic structure with DataStore integration for persistence
- Empty NetworkMockScreen composable
- Navigation setup completed
- Dependencies: DataStore, Compose Multiplatform, utils module

## Project Context
- **Technology Stack**: Kotlin Multiplatform (KMP), Compose Multiplatform (CMP)
- **Network Library**: Ktor (version 3.3.3 available)
- **Persistence**: DataStore (already integrated)
- **UI**: Jetbrains Compose Material3

---

## Recent Updates

### ✅ Architecture Finalized (2026-02-06)

**Package Structure:**
- Screen files (`NetworkMockScreen.kt`) at root package level
- Supporting packages (`components/`, `preview/`, `plugin/`, `model/`, `repository/`, `viewmodel/`) at same level
- Follows existing DevView module structure (e.g., `devview-featureflip`)

**Build Configuration:**
- Using `convention.ktor` plugin (already added to project)
- Convention plugin handles all Ktor dependencies automatically
- No need for manual `ktor-client-core` dependency declarations

**Key Decisions:**
- ✅ Host-centric JSON structure with naming conventions for response files
- ✅ Ktor Plugin/Interceptor integration approach
- ✅ Two-level toggle system (global + per-endpoint)
- ✅ Tabbed UI when multiple hosts exist
- ✅ Simple path parameter matching for MVP
- ✅ Comprehensive DataStore persistence

---

## Questions & Clarifications

### 1. Configuration File Format & Location ✅ ANSWERED
**Question**: Where should the JSON configuration file be located?

**Answer**: Configuration file will be located at `composeResources/files/networkmocks/mocks.json` in the integrator's project.

**Status**: ✅ Fully answered

---

### 2. Integration Mechanism ✅ ANSWERED
**Question**: How should integrators hook into their network layer?

**Answer**: Integrators should be able to keep their existing HttpClient definitions for each platform, and the module should plug into it. This avoids redefining the network layer.

**Validated Solution**: Ktor Plugin/Interceptor approach (see Q3 in follow-up section)

**Status**: ✅ Fully answered and validated

---

### 3. Mock Response Definition ✅ ANSWERED
**Question**: How should mock responses be defined?

**Answer**: Separate files - integrators should choose from a list of available mock responses.

**Validated Solution**: Host-centric JSON structure with convention-based response file discovery (see Q1 in follow-up section)

**Status**: ✅ Fully answered and validated

---

### 4. Runtime Configuration ✅ ANSWERED
**Question**: What should be configurable at runtime via the UI?

**Answer**: 
- Toggle between actual network call or one of the available mock responses
- Changes should persist via DataStore
- Should be able to reset to all network calls

**Validated Solution**: Two-level toggle system (global + per-mock) (see Q7 in follow-up section)

**Status**: ✅ Fully answered and validated

---

### 5. Path Matching ✅ ANSWERED
**Question**: How should path matching work?

**Answer**: Path parameters (e.g., `/api/users/{userId}`) - most versatile solution while avoiding regex issues.

**Validated Solution**: Simple parameter matching for MVP, constraints for future (see Q6 in follow-up section)

**Status**: ✅ Fully answered and validated

---

### 6. Multi-Host Support ✅ ANSWERED
**Question**: Should we support mocking for multiple hosts simultaneously?

**Answer**: 
- Allow integrators to change mocks on a per-host basis
- Global host switching deferred to future iterations

**Validated Solution**: Configuration supports multiple hosts, matching based on actual request host (see Q2 in follow-up section)

**Status**: ✅ Fully answered and validated

---

### 7. Response Scenarios ✅ ANSWERED
**Question**: Should we support multiple response scenarios per endpoint?

**Answer**: Integrators should be able to choose one response at a time, whatever that response may be (200, 404, 401, 500, etc.)

**Status**: ✅ Fully answered

---

### 8. Developer Experience ❓ CLARIFICATION NEEDED
**Question**: What level of convenience vs. flexibility?

**Answer**: Since JSON contents is text, unclear what is being asked here.

**Status**: ❓ Question not applicable - skipped

---

### 9. Edge Cases & Error Handling ⚠️ PARTIALLY ANSWERED
**Question**: How should we handle edge cases?

**Answer**: 
- Invalid configuration and mock definitions should be handled
- Network timeouts simulation, error simulation (connection refused, timeout) are out of scope for now but should be kept for future consideration

**Status**: ⚠️ Partially answered - specific error handling strategies to be defined during implementation

---

### 10. Platform-Specific Considerations ✅ ANSWERED
**Question**: Are there platform-specific requirements?

**Answer**: 
- Target platforms: Android and iOS only
- May have file access nuances between platforms

**Status**: ✅ Answered - details to be discovered during implementation

---

### 11. Security & Production Builds ✅ ANSWERED
**Question**: How to prevent accidental inclusion in production?

**Answer**: Up to integrators to choose how they want to handle this.

**Status**: ✅ Fully answered

---

### 12. State Management ✅ ANSWERED
**Question**: How should mock state be managed?

**Answer**: 
- DataStore persistence for the state of each API mock
- Import/export functionality kept for future consideration

**Validated Solution**: Store all necessary state including global toggle, per-endpoint selection, and metadata (see Q4 in follow-up section)

**Status**: ✅ Fully answered and validated

---

## Follow-up Questions for Clarification

### Q1: Configuration File Structure with Separate Response Files ✅ VALIDATED
Since mock responses will be in separate files, should the structure be:

**VALIDATED ANSWER**: Option B (host-centric) with naming convention-based response discovery.

**Structure:**
```json
// composeResources/files/networkmocks/mocks.json
{
  "hosts": [
    {
      "id": "staging",
      "url": "https://staging.api.example.com",
      "endpoints": [
        {
          "id": "getUser",
          "name": "Get User Profile",
          "path": "/api/v1/user/{userId}",
          "method": "GET"
        }
      ]
    },
    {
      "id": "production",
      "url": "https://api.example.com",
      "endpoints": [...]
    }
  ]
}
```

**File Organization Convention:**
```
composeResources/files/networkmocks/
├── mocks.json
└── responses/
    ├── getUser/
    │   ├── getUser-200.json
    │   ├── getUser-404-simple.json
    │   ├── getUser-404-detailed.json
    │   └── getUser-500.json
    ├── createUser/
    │   ├── createUser-201.json
    │   └── createUser-400.json
    └── ...
```

**Naming Convention:**
- Folder name matches endpoint `id`
- File format: `{endpointId}-{statusCode}[-{suffix}].json`
- Suffix is optional to differentiate between responses with same status code
- Examples: `getUser-200.json`, `getUser-404-simple.json`, `getUser-404-notFound.json`

**Status**: ✅ Fully validated

---

### Q2: Host Switching Mechanism ✅ VALIDATED
When you say "change hosts on the fly," do you mean:

**VALIDATED ANSWER**: This feature (global host switching for API calls) is **OUT OF SCOPE** for the current implementation.

**Current Scope:**
- Configuration can specify mocks for different hosts
- Only the currently used host (as defined in the underlying Ktor request) will be matched against available mocks
- No runtime host switching capability needed

**Future Consideration:**
- Global host switching capability (e.g., switch all API calls from production to staging)
- This would require deeper integration with the network layer
- To be revisited in future iterations

**Status**: ✅ Fully validated - feature deferred

---

### Q3: HttpClient Integration Strategy ✅ VALIDATED
You mentioned integrators keeping their existing HttpClient. Here are possible approaches:

**VALIDATED ANSWER**: Option A - Ktor Plugin/Interceptor

**Implementation Approach:**
```kotlin
// Integrator's code
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin) {
        configPath = "files/networkmocks/mocks.json"
    }
}
```

**Benefits:**
- Clean, idiomatic Ktor approach
- Automatic interception of requests
- Follows Ktor plugin patterns
- Minimal code changes for integrators
- Easy to add to existing HttpClient configurations

**Integration Points:**
- Plugin will intercept requests before they're sent
- Match against configured mocks based on host, path, and method
- Return mock response if match found and mock is enabled
- Otherwise, proceed with actual network call

**Status**: ✅ Fully validated

---

### Q4: Runtime State Persistence ✅ VALIDATED
For DataStore persistence, should we store:

**VALIDATED ANSWER**: Store as much as needed (Option D - All of the above)

**Persisted State:**
1. **Per-endpoint mock state:**
   - Which response file is selected (if any)
   - Whether the mock is enabled (vs. using actual network)
   
2. **Global settings:**
   - Global mocking enable/disable flag
   - Currently active host configuration (if applicable)

3. **Additional useful state:**
   - Last modified timestamp
   - User preferences (e.g., UI display settings)

**DataStore Schema (conceptual):**
```kotlin
data class NetworkMockState(
    val globalMockingEnabled: Boolean,
    val endpointStates: Map<String, EndpointMockState>, // key: "hostId-endpointId"
    val lastModified: Long
)

data class EndpointMockState(
    val mockEnabled: Boolean, // true = use mock, false = use network
    val selectedResponseFile: String?, // e.g., "getUser-200.json"
)
```

**Status**: ✅ Fully validated

---

### Q5: UI Navigation Flow ✅ VALIDATED
For the NetworkMockScreen, what should the navigation hierarchy be?

**VALIDATED ANSWER**: Option C - Tabbed approach (when multiple hosts exist)

**UI Structure:**
```
NetworkMockScreen
├── Global Controls (if multiple hosts)
│   ├── Master enable/disable toggle
│   └── Reset all button
├── Host Tabs (if > 1 host)
│   ├── Tab: Staging
│   ├── Tab: Production
│   └── Tab: Development
└── Endpoint List (per tab)
    ├── Endpoint Card 1
    │   ├── Name & path display
    │   ├── Toggle: Network / Mock
    │   └── Response selector (if mock enabled)
    │       ├── getUser-200.json
    │       ├── getUser-404-simple.json
    │       └── getUser-500.json
    └── Endpoint Card 2...
```

**Adaptive Behavior:**
- Single host: No tabs, just show endpoints directly
- Multiple hosts: Show tabs for each host
- Empty state: Helpful message about adding mocks

**Status**: ✅ Fully validated

---

### Q6: Path Parameter Matching Details ✅ VALIDATED
For path parameters like `/api/users/{userId}`, should:

**VALIDATED ANSWER**: Option C - Start with simple matching (A), add constraints later (B)

**MVP Implementation:**
- Path parameters like `{userId}` match any value
- Examples:
  - Config: `/api/users/{userId}` 
  - Matches: `/api/users/123`, `/api/users/abc`, `/api/users/user-456`
- Simple string substitution and matching logic

**Future Enhancement:**
- Parameter constraints: `{userId:\d+}` (only numbers)
- Type validation: `{userId:int}`, `{email:email}`
- Pattern matching for more complex scenarios

**Matching Algorithm (MVP):**
1. Split config path and request path by `/`
2. Compare segments
3. If segment starts with `{` and ends with `}`, it's a parameter → always matches
4. Otherwise, exact string match required

**Status**: ✅ Fully validated

---

### Q7: "Actual Network Call" Toggle Mechanism ✅ VALIDATED
When toggling between mock and actual network, should this be:

**VALIDATED ANSWER**: Option C - Both levels of control

**Two-Level Toggle System:**

**1. Global Level:**
- Master switch to enable/disable ALL mocking
- When OFF: All requests go to actual network (ignores per-mock settings)
- When ON: Respects individual mock settings
- Useful for quickly testing with/without any mocking

**2. Per-Mock Level:**
- Each endpoint has its own state:
  - **"Use Network"** - Make actual network call
  - **"Use Mock"** - Return selected mock response
- When "Use Mock" is selected, user chooses which response file to use

**Interaction Logic:**
```
if (globalMockingEnabled) {
    if (endpoint.mockEnabled && endpoint.selectedResponse != null) {
        return mockResponse
    } else {
        return actualNetworkCall
    }
} else {
    return actualNetworkCall // Global override
}
```

**UI Implications:**
- Global toggle at top of screen (prominent)
- Per-endpoint toggles in each card
- Response selector enabled only when mock is enabled
- Visual feedback when global toggle overrides individual settings

**Status**: ✅ Fully validated

---

## ✅ Technical Architecture

All follow-up questions have been validated. Below is the complete technical architecture for the Network Mock feature.

---

## 1. Component Breakdown

### 1.1 Core Components

```
devview-networkmock/src/commonMain/kotlin/com/worldline/devview/networkmock/
├── NetworkMock.kt                     # Module entry point (existing)
├── NetworkMockScreen.kt               # Main screen (existing, to be implemented)
├── plugin/
│   ├── NetworkMockPlugin.kt           # Ktor plugin implementation
│   ├── NetworkMockConfig.kt           # Plugin configuration
│   └── RequestMatcher.kt              # Path matching logic
├── model/
│   ├── MockConfiguration.kt           # Data models for mocks.json
│   ├── NetworkMockState.kt            # DataStore state models
│   ├── MockResponse.kt                # Response metadata
│   └── rememberDataStore.kt           # Existing DataStore composable
├── repository/
│   ├── MockConfigRepository.kt        # Load mocks.json & responses
│   ├── MockStateRepository.kt         # DataStore persistence
│   └── ResourceFileReader.kt          # Platform-specific file reading
├── viewmodel/
│   └── NetworkMockViewModel.kt        # UI state management (kept due to module complexity)
├── components/
│   ├── GlobalMockToggle.kt            # Master enable/disable switch
│   ├── HostTabs.kt                    # Host tab navigation
│   ├── EndpointCard.kt                # Individual endpoint UI
│   └── ResponseSelector.kt            # Response file picker
└── preview/
    └── NetworkMockPreviews.kt         # Preview samples
```

**Note on ViewModel**: This module uses a ViewModel despite other DevView modules not using one. This is kept due to the complexity of combining configuration loading, file discovery, and state management. This may be revisited in the future for consistency.

---

## 2. Data Models

### 2.1 Configuration Models (for mocks.json)

```kotlin
@Serializable
data class MockConfiguration(
    val hosts: List<HostConfig>
)

@Serializable
data class HostConfig(
    val id: String,
    val url: String,
    val endpoints: List<EndpointConfig>
)

@Serializable
data class EndpointConfig(
    val id: String,
    val name: String,
    val path: String,
    val method: String // GET, POST, PUT, DELETE, PATCH
)
```

### 2.2 State Models (for DataStore persistence)

```kotlin
@Serializable
data class NetworkMockState(
    val globalMockingEnabled: Boolean = false,
    val endpointStates: Map<String, EndpointMockState> = emptyMap(),
    val lastModified: Long = 0L
)

@Serializable
data class EndpointMockState(
    val mockEnabled: Boolean = false,
    val selectedResponseFile: String? = null
)
```

### 2.3 Runtime Models

```kotlin
data class MockResponse(
    val statusCode: Int,
    val fileName: String,
    val displayName: String, // e.g., "Success (200)" or "Not Found - Simple (404)"
    val content: ByteArray
)

data class AvailableEndpointMock(
    val hostId: String,
    val endpointId: String,
    val config: EndpointConfig,
    val availableResponses: List<MockResponse>,
    val currentState: EndpointMockState
)
```

---

## 3. Ktor Plugin Implementation

### 3.1 Plugin Definition

```kotlin
val NetworkMockPlugin = createClientPlugin(
    name = "NetworkMockPlugin",
    createConfiguration = ::NetworkMockConfig
) {
    val config = pluginConfig
    val mockRepository = MockConfigRepository(config.configPath)
    val stateRepository = MockStateRepository()
    
    onRequest { request, content ->
        val currentState = stateRepository.getState()
        
        if (!currentState.globalMockingEnabled) {
            return@onRequest // Pass through to actual network
        }
        
        val mockMatch = mockRepository.findMatchingMock(
            host = request.url.host,
            path = request.url.encodedPath,
            method = request.method.value
        )
        
        mockMatch?.let { match ->
            val endpointKey = "${match.hostId}-${match.endpointId}"
            val endpointState = currentState.endpointStates[endpointKey]
            
            if (endpointState?.mockEnabled == true && endpointState.selectedResponseFile != null) {
                val mockResponse = mockRepository.loadMockResponse(
                    endpointId = match.endpointId,
                    fileName = endpointState.selectedResponseFile
                )
                
                mockResponse?.let { response ->
                    // Intercept and return mock response
                    request.cancel() // Prevent actual network call
                    return@onRequest HttpResponse(
                        status = HttpStatusCode.fromValue(response.statusCode),
                        content = ByteReadChannel(response.content),
                        headers = response.headers
                    )
                }
            }
        }
        
        // No mock matched or not enabled - proceed with actual network call
    }
}

class NetworkMockConfig {
    var configPath: String = "files/networkmocks/mocks.json"
}
```

### 3.2 Request Matching Logic

```kotlin
object RequestMatcher {
    fun matchesPath(configPath: String, requestPath: String): Boolean {
        val configSegments = configPath.split("/")
        val requestSegments = requestPath.split("/")
        
        if (configSegments.size != requestSegments.size) {
            return false
        }
        
        return configSegments.zip(requestSegments).all { (config, request) ->
            if (config.startsWith("{") && config.endsWith("}")) {
                true // Parameter segment - matches anything
            } else {
                config == request // Exact match required
            }
        }
    }
}
```

---

## 4. Repository Layer

### 4.1 MockConfigRepository

**Responsibilities:**
- Load and parse `mocks.json`
- Discover available response files based on naming convention
- Load response file contents
- Match requests to configured mocks

**Key Methods:**
```kotlin
class MockConfigRepository(private val configPath: String) {
    suspend fun loadConfiguration(): Result<MockConfiguration>
    
    suspend fun findMatchingMock(
        host: String,
        path: String,
        method: String
    ): MockMatch?
    
    suspend fun discoverResponseFiles(endpointId: String): List<MockResponse>
    
    suspend fun loadMockResponse(
        endpointId: String,
        fileName: String
    ): MockResponse?
}
```

### 4.2 MockStateRepository

**Responsibilities:**
- Read/write DataStore state
- Provide Flow<NetworkMockState> for UI observation
- Update individual endpoint states
- Reset functionality

**Key Methods:**
```kotlin
class MockStateRepository(private val dataStore: DataStore<Preferences>) {
    fun observeState(): Flow<NetworkMockState>
    
    suspend fun getState(): NetworkMockState
    
    suspend fun setGlobalMockingEnabled(enabled: Boolean)
    
    suspend fun setEndpointMockState(
        hostId: String,
        endpointId: String,
        state: EndpointMockState
    )
    
    suspend fun resetAllToNetwork()
}
```

---

## 5. UI/UX Design

### 5.1 Screen Hierarchy

**NetworkMockScreen**
- Top App Bar: "Network Mocks"
- Global Controls Section
  - Global Enable/Disable Switch
  - "Reset All to Network" button
- Host Tabs (conditional)
  - Show only if multiple hosts exist
  - Tab for each host from config
- Endpoint List (LazyColumn)
  - One card per endpoint
  - Filter by selected host tab (if applicable)

### 5.2 Endpoint Card Layout

```
┌─────────────────────────────────────────┐
│ GET /api/users/{userId}                 │
│ Get User Profile                        │
│                                         │
│ ○ Use Network  ● Use Mock              │  <-- Toggle
│                                         │
│ Response:                               │
│ ▼ getUser-200.json            [200] ✓  │  <-- Dropdown
│   getUser-404-simple.json     [404]    │
│   getUser-404-detailed.json   [404]    │
│   getUser-500.json            [500]    │
└─────────────────────────────────────────┘
```

### 5.3 Empty State

When no `mocks.json` found or no endpoints configured:
```
┌─────────────────────────────────────────┐
│           📄 No Mocks Configured        │
│                                         │
│  Add a mocks.json file to:             │
│  composeResources/files/networkmocks/  │
│                                         │
│  [View Documentation]                  │
└─────────────────────────────────────────┘
```

---

## 6. Implementation Phases

### Phase 1: Core Infrastructure ✅ MVP
**Goal**: Basic plugin + config loading + state management

**Tasks:**
1. Define data models (MockConfiguration, NetworkMockState, etc.)
2. Implement MockConfigRepository
   - Load mocks.json from resources
   - Parse JSON into data models
3. Implement MockStateRepository
   - DataStore read/write operations
   - State observation Flow
4. Implement RequestMatcher
   - Path parameter matching logic
5. Create NetworkMockPlugin skeleton
   - Plugin registration
   - Basic request interception (no-op initially)

**Deliverable**: Plugin can be installed, config loads, state persists

---

### Phase 2: Response Discovery & Loading ✅ MVP
**Goal**: Discover and load mock response files

**Tasks:**
1. Implement file discovery logic
   - Scan `responses/{endpointId}/` folders
   - Parse filenames to extract status codes and suffixes
2. Implement response file loading
   - Platform-specific resource reading (Android/iOS)
   - Cache loaded responses in memory
3. Create MockResponse model
   - Include metadata (status, filename, display name)
   - Store file content as ByteArray

**Deliverable**: System can discover and load all available responses per endpoint

---

### Phase 3: Request Interception & Mocking ✅ MVP
**Goal**: Intercept requests and return mock responses

**Tasks:**
1. Complete NetworkMockPlugin implementation
   - Match incoming request to configured endpoint
   - Check DataStore state for selected mock
   - Load and return mock response if enabled
   - Pass through to network if not mocked
2. Implement two-level toggle logic
   - Global enable/disable check
   - Per-endpoint enable/disable check
3. Error handling
   - Missing response files
   - Invalid JSON in responses
   - Configuration errors

**Deliverable**: Plugin successfully mocks requests end-to-end

---

### Phase 4: UI Implementation ✅ MVP
**Goal**: Complete NetworkMockScreen with all controls

**Tasks:**
1. Implement NetworkMockViewModel
   - Load configuration
   - Observe DataStore state
   - Expose UI state (hosts, endpoints, selections)
   - Handle user actions (toggle mock, select response)
2. Build UI components
   - GlobalMockToggle
   - HostTabs (conditional rendering)
   - EndpointCard
   - ResponseSelector dropdown
3. Implement empty state
4. Add loading & error states

**Deliverable**: Fully functional UI for managing mocks

---

### Phase 5: Polish & Documentation 🔄 POST-MVP
**Goal**: Refine UX, add tests, write docs

**Tasks:**
1. Add animations and transitions
2. Improve error messages
3. Write unit tests for:
   - RequestMatcher
   - Repositories
   - ViewModel
4. Write integration tests
5. Create documentation:
   - Setup guide for integrators
   - JSON schema documentation
   - Example configurations
6. Add search/filter in UI

**Deliverable**: Production-ready feature with full documentation

---

### Phase 6: Future Enhancements 🔮 FUTURE
**Ideas for later iterations:**
- Import/export mock configurations
- Network error simulation (timeouts, connection refused)
- Response delay simulation
- Path parameter constraints (regex patterns)
- Global host switching feature
- UI to create/edit mocks without JSON
- Response body preview in UI
- Request/response history logging
- Multiple mock configurations (profiles)

---

## 7. File Structure Proposal

### 7.1 Source Code Structure

```
devview-networkmock/src/commonMain/kotlin/com/worldline/devview/networkmock/
├── NetworkMock.kt                             # Module entry point (existing)
├── NetworkMockScreen.kt                       # Main screen (existing)
├── plugin/
│   ├── NetworkMockPlugin.kt
│   ├── NetworkMockConfig.kt
│   └── RequestMatcher.kt
├── model/
│   ├── MockConfiguration.kt
│   ├── NetworkMockState.kt
│   ├── MockResponse.kt
│   ├── AvailableEndpointMock.kt
│   └── rememberDataStore.kt                   # Existing
├── repository/
│   ├── MockConfigRepository.kt
│   ├── MockStateRepository.kt
│   └── ResourceFileReader.kt
├── viewmodel/
│   └── NetworkMockViewModel.kt
├── components/
│   ├── GlobalMockToggle.kt
│   ├── HostTabs.kt
│   ├── EndpointCard.kt
│   └── ResponseSelector.kt
└── preview/
    └── NetworkMockPreviews.kt
```

**Note**: This structure follows the same pattern as other DevView modules (e.g., `devview-featureflip`), with:
- Screen files at the root package level
- Supporting packages (`plugin/`, `model/`, `repository/`, `viewmodel/`, `components/`, `preview/`) at the same level

### 7.2 Integrator's Project Structure (Example)

```
integrator-app/composeResources/files/networkmocks/
├── mocks.json
└── responses/
    ├── getUser/
    │   ├── getUser-200.json
    │   ├── getUser-404-simple.json
    │   ├── getUser-404-detailed.json
    │   └── getUser-500.json
    ├── createUser/
    │   ├── createUser-201.json
    │   ├── createUser-400.json
    │   └── createUser-401.json
    └── getProducts/
        ├── getProducts-200-empty.json
        ├── getProducts-200-withData.json
        └── getProducts-500.json
```

---

## 8. Dependencies to Add

Based on the proposed architecture, ensure these dependencies are available:

```kotlin
// build.gradle.kts (devview-networkmock)
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Existing
                api(projects.devview)
                implementation(projects.devviewUtils)
                implementation(libs.kotlinx.collections.immutable)
                
                // New additions needed
                implementation(libs.ktor.client.core)          // For plugin
                implementation(libs.kotlinx.serialization.json) // For JSON parsing
                implementation(libs.kotlinx.io.core)           // For file I/O
                
                // Compose & ViewModel (likely already included)
                implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
            }
        }
    }
}
```

---

## 9. Integrator Setup Guide (Draft)

### 9.1 Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.worldline.devview:devview-networkmock:<version>")
}
```

### 9.2 Configuration

**Step 1**: Create configuration file
```
src/commonMain/composeResources/files/networkmocks/mocks.json
```

**Step 2**: Define your mocks
```json
{
  "hosts": [
    {
      "id": "staging",
      "url": "https://staging.api.example.com",
      "endpoints": [
        {
          "id": "getUser",
          "name": "Get User Profile",
          "path": "/api/v1/user/{userId}",
          "method": "GET"
        }
      ]
    }
  ]
}
```

**Step 3**: Add response files
```
responses/getUser/getUser-200.json
responses/getUser/getUser-404.json
```

**Step 4**: Install plugin in HttpClient
```kotlin
val client = HttpClient(OkHttp) {
    install(NetworkMockPlugin) {
        configPath = "files/networkmocks/mocks.json"
    }
    // ... your existing config
}
```

**Note**: The `NetworkMockPlugin` is provided by the `devview-networkmock` module and integrates seamlessly with Ktor's plugin system.

**Step 5**: Add NetworkMock module to DevView
```kotlin
DevView(
    modules = listOf(
        NetworkMock,
        // ... other modules
    )
)
```

### 9.3 Usage

- Open DevView in your app
- Navigate to "Logging" section → "Network Mock"
- Toggle global mocking on/off
- Select individual endpoints and choose mock responses
- Changes persist across app restarts

---

## 10. Error Handling Strategy

### 10.1 Configuration Errors

| Error | Handling Strategy |
|-------|------------------|
| Missing mocks.json | Show empty state in UI, log warning, allow app to function |
| Invalid JSON syntax | Show error in UI, log error, disable mocking for safety |
| Duplicate endpoint IDs | Log warning, use first occurrence |
| Invalid HTTP method | Log warning, skip endpoint |

### 10.2 Response File Errors

| Error | Handling Strategy |
|-------|------------------|
| Missing response file | Show error in UI for that endpoint, allow other mocks to work |
| Invalid JSON in response | Log error, fall back to network call |
| File read error | Log error, disable mock for that endpoint |

### 10.3 Runtime Errors

| Error | Handling Strategy |
|-------|------------------|
| DataStore write failure | Log error, continue with in-memory state |
| Plugin installation error | Log error, disable mocking, allow normal network calls |
| Path matching exception | Log error, fall back to network call |

---

## 11. Testing Strategy

### 11.1 Unit Tests

- **RequestMatcher**: Test path matching with various patterns
- **MockConfigRepository**: Test JSON parsing, file discovery
- **MockStateRepository**: Test DataStore operations
- **NetworkMockViewModel**: Test state transformations, user actions

### 11.2 Integration Tests

- **Plugin Integration**: Test request interception end-to-end
- **UI Integration**: Test user interactions update state correctly
- **Cross-platform**: Test resource loading on Android and iOS

### 11.3 Manual Testing Checklist

- [ ] Install plugin in sample app
- [ ] Load valid configuration
- [ ] Toggle global mocking on/off
- [ ] Select different responses for endpoints
- [ ] Verify actual requests are mocked
- [ ] Verify persistence across app restarts
- [ ] Test with missing files
- [ ] Test with invalid JSON
- [ ] Test with multiple hosts
- [ ] Test UI on different screen sizes

---

## 12. Implementation Details & Clarifications

### 12.1 Response File Format ✅ CLARIFIED

**Decision**: Raw JSON only for MVP

**Implementation:**
- Response files contain only the response body content
- Example: `getUser-200.json` contains just `{"id": "123", "name": "John Doe"}`
- No metadata (headers, delays) in response files for MVP

**Future Enhancement:**
- Add optional metadata in the main `mocks.json` configuration file
- Support custom headers per response
- Response delay simulation

### 12.2 Host Matching ✅ CLARIFIED

**Decision**: Match hostname only (simple matching)

**Implementation:**
- Extract hostname from request URL: `request.url.host`
- Extract hostname from config URL: parse `https://staging.api.example.com` → `staging.api.example.com`
- Compare hostnames for equality (case-insensitive recommended)

**Future Enhancement:**
- Port matching (e.g., `localhost:8080` vs `localhost:9090`)
- Scheme matching (http vs https)
- Wildcard/pattern matching for hosts

### 12.3 Error Logging ✅ CLARIFIED

**Decision**: Use `println` for MVP

**Implementation:**
- Use `println()` for all error and warning messages
- Prefix messages with module identifier: `[NetworkMock]`
- Example: `println("[NetworkMock] ERROR: Failed to load mocks.json - ${e.message}")`

**Future Enhancement:**
- Integrate with proper logging library (to be defined for entire DevView library)
- Use structured logging with levels (DEBUG, INFO, WARN, ERROR)
- Potentially use Kermit (already available in project dependencies)

### 12.4 Resource Loading

**Question**: How to robustly load files from composeResources on both platforms?

**Options:**
- Use Compose Resources API (if available for files)
- Platform-specific implementations with expect/actual
- Use kotlinx-io for cross-platform file access

**Decision needed**: Research best KMP approach for resource file loading during implementation

### 12.5 DataStore Access in Ktor Plugin

**Question**: How to access DataStore from within Ktor plugin?

**Options:**
- Pass DataStore instance to plugin config
- Use singleton/global state (not ideal)
- Use dependency injection (Koin)

**Decision needed**: Determine cleanest approach for state access in plugin during implementation

**Note**: The `convention.ktor` plugin already handles Ktor client setup, so we can focus on the plugin implementation itself.

---

## Summary

This implementation plan provides a complete blueprint for the Network Mock feature:

✅ **All questions validated**
✅ **Architecture defined**
✅ **Data models specified**
✅ **Plugin approach confirmed**
✅ **UI/UX designed**
✅ **Phased implementation plan**
✅ **File structure proposed**
✅ **Error handling strategy**
✅ **Testing approach**

**Next step**: Review this plan and validate the technical architecture before proceeding to implementation.

---

## Appendix: Example Configuration Files

### Example mocks.json

```json
{
  "hosts": [
    {
      "id": "staging",
      "url": "https://staging.api.example.com",
      "endpoints": [
        {
          "id": "getUser",
          "name": "Get User Profile",
          "path": "/api/v1/user/{userId}",
          "method": "GET"
        },
        {
          "id": "updateUser",
          "name": "Update User",
          "path": "/api/v1/user/{userId}",
          "method": "PUT"
        },
        {
          "id": "listProducts",
          "name": "List Products",
          "path": "/api/v1/products",
          "method": "GET"
        }
      ]
    },
    {
      "id": "production",
      "url": "https://api.example.com",
      "endpoints": [
        {
          "id": "getUser",
          "name": "Get User Profile",
          "path": "/api/v1/user/{userId}",
          "method": "GET"
        }
      ]
    }
  ]
}
```

### Example Response Files

**getUser-200.json**:
```json
{
  "id": "user-123",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "admin"
}
```

**getUser-404-simple.json**:
```json
{
  "error": "User not found"
}
```

**getUser-404-detailed.json**:
```json
{
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "No user exists with the provided ID",
    "timestamp": "2026-02-06T10:30:00Z"
  }
}
```

**listProducts-200-empty.json**:
```json
{
  "products": [],
  "total": 0,
  "page": 1
}
```

**listProducts-200-withData.json**:
```json
{
  "products": [
    {
      "id": "prod-1",
      "name": "Widget",
      "price": 29.99
    },
    {
      "id": "prod-2",
      "name": "Gadget",
      "price": 49.99
    }
  ],
  "total": 2,
  "page": 1
}
```
