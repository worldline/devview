# Network Mock Logging Implementation ✅

## Summary

Added comprehensive logging throughout the NetworkMock module to help debug why selected mocks aren't being used. All logs use the `[NetworkMock]` prefix with subcategories for easy filtering.

---

## Logging Categories

All logs follow this pattern: `[NetworkMock][Category] Message`

### Categories:
- **`[Config]`** - Configuration loading
- **`[Matching]`** - Request matching logic
- **`[Discovery]`** - Response file discovery
- **`[Loading]`** - Response file loading
- **`[State]`** - State repository operations
- **`[Plugin]`** - Plugin interception logic

---

## Added Logs by Component

### 1. **MockConfigRepository** (6 log points)

#### Configuration Loading
```kotlin
[NetworkMock][Config] Loading configuration from: files/networkmocks/mocks.json
[NetworkMock][Config] Successfully loaded configuration:
[NetworkMock][Config]   Host: jsonplaceholder (https://jsonplaceholder.typicode.com) with 4 endpoint(s)
[NetworkMock][Config]     - GET /users/{userId} (getUser)
[NetworkMock][Config]     - GET /users (listUsers)
[NetworkMock][Config] Using cached configuration with 2 host(s)
[NetworkMock][Config] ERROR: Failed to load configuration - File not found
```

#### Request Matching
```kotlin
[NetworkMock][Matching] Looking for match: GET jsonplaceholder.typicode.com/users/1
[NetworkMock][Matching] Comparing against 2 host(s):
[NetworkMock][Matching]   Host 'jsonplaceholder': jsonplaceholder.typicode.com vs jsonplaceholder.typicode.com = true
[NetworkMock][Matching] Host matched: 'jsonplaceholder' - checking 4 endpoint(s)
[NetworkMock][Matching]   Endpoint 'getUser':
[NetworkMock][Matching]     Path: /users/{userId} vs /users/1 = true
[NetworkMock][Matching]     Method: GET vs GET = true
[NetworkMock][Matching] SUCCESS: Matched endpoint 'getUser' on host 'jsonplaceholder'
[NetworkMock][Matching] ERROR: No matching host found for 'unknown.api.com'
[NetworkMock][Matching] ERROR: No matching endpoint found for POST /users
```

#### Response Discovery
```kotlin
[NetworkMock][Discovery] Discovering response files for endpoint: getUser
[NetworkMock][Discovery]   Found: getUser-200.json (status 200)
[NetworkMock][Discovery]   Found: getUser-404.json (status 404)
[NetworkMock][Discovery]   Found: getUser-500.json (status 500)
[NetworkMock][Discovery] Discovered 3 response file(s) for 'getUser'
```

#### Response Loading
```kotlin
[NetworkMock][Loading] Loading response: getUser/getUser-200.json
[NetworkMock][Loading] Successfully loaded: getUser-200.json (status 200)
[NetworkMock][Loading] ERROR: Failed to load: getUser-999.json
```

---

### 2. **MockStateRepository** (3 log points)

#### Reading State
```kotlin
[NetworkMock][State] Current state: globalMocking=true, endpoints=2
[NetworkMock][State]   jsonplaceholder-getUser: enabled=true, file=getUser-200.json
[NetworkMock][State]   staging-getUserProfile: enabled=false, file=null
```

#### Setting Global Mocking
```kotlin
[NetworkMock][State] Setting global mocking enabled: true
[NetworkMock][State] Setting global mocking enabled: false
```

#### Setting Endpoint State
```kotlin
[NetworkMock][State] Setting endpoint state: jsonplaceholder-getUser, enabled=true, file=getUser-200.json
[NetworkMock][State] Setting endpoint state: staging-getUserProfile, enabled=false, file=null
```

---

### 3. **NetworkMockPlugin** (15+ log points) - MOST IMPORTANT

#### Plugin Installation
```kotlin
[NetworkMock][Plugin] NetworkMock plugin installed successfully
```

#### Request Interception Flow
```kotlin
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: GET jsonplaceholder.typicode.com/users/1
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock
[NetworkMock][Plugin] Found matching endpoint: jsonplaceholder-getUser
[NetworkMock][Plugin] Endpoint state: enabled=true, file=getUser-200.json
[NetworkMock][Plugin] Mock is enabled with file: getUser-200.json
[NetworkMock][Plugin] Successfully loaded mock response (status 200)
[NetworkMock][Plugin] Returning MOCK response - NO network call will be made
[NetworkMock][Plugin] ========================================
```

#### Global Mocking Disabled
```kotlin
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: GET api.example.com/users
[NetworkMock][Plugin] Global mocking is DISABLED - using actual network
[NetworkMock][Plugin] ========================================
```

#### No Endpoint Match
```kotlin
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: POST unknown.api.com/data
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock
[NetworkMock][Matching] Looking for match: POST unknown.api.com/data
[NetworkMock][Matching] ERROR: No matching host found for 'unknown.api.com'
[NetworkMock][Plugin] No matching endpoint config found
[NetworkMock][Plugin] Using actual network
[NetworkMock][Plugin] ========================================
```

#### Endpoint Not Enabled
```kotlin
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: GET staging.api.com/profile/123
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock
[NetworkMock][Plugin] Found matching endpoint: staging-getUserProfile
[NetworkMock][Plugin] Endpoint state: enabled=false, file=null
[NetworkMock][Plugin] Endpoint mock not enabled or no response selected
[NetworkMock][Plugin]   shouldUseMock() = false
[NetworkMock][Plugin] Using actual network
[NetworkMock][Plugin] ========================================
```

#### No State for Endpoint
```kotlin
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: GET jsonplaceholder.typicode.com/posts/1
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock
[NetworkMock][Plugin] Found matching endpoint: jsonplaceholder-getPost
[NetworkMock][Plugin] No state found for endpoint key: jsonplaceholder-getPost
[NetworkMock][Plugin] Available endpoint states: [jsonplaceholder-getUser, staging-getUserProfile]
[NetworkMock][Plugin] Using actual network
[NetworkMock][Plugin] ========================================
```

#### Error Loading Response
```kotlin
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: GET jsonplaceholder.typicode.com/users/1
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock
[NetworkMock][Plugin] Found matching endpoint: jsonplaceholder-getUser
[NetworkMock][Plugin] Endpoint state: enabled=true, file=invalid-file.json
[NetworkMock][Plugin] Mock is enabled with file: invalid-file.json
[NetworkMock][Loading] Loading response: getUser/invalid-file.json
[NetworkMock][Loading] ERROR: Failed to load: invalid-file.json
[NetworkMock][Plugin] ERROR: Mock response loaded as null
[NetworkMock][Plugin] Falling back to actual network
[NetworkMock][Plugin] ========================================
```

---

## How to Use the Logs

### Finding Why Mocks Aren't Used

1. **Look for the separator lines**: `========================================`
   - Each request creates a clear block of logs

2. **Check the flow**:
   ```
   Intercepted request → Global mocking check → Matching → State check → Load response → Return mock/network
   ```

3. **Common Issues and Logs**:

| Issue | Look For |
|-------|----------|
| Global mocking off | `Global mocking is DISABLED` |
| Wrong host | `No matching host found for 'xxx'` |
| Wrong path/method | `No matching endpoint found for xxx` |
| Endpoint not enabled | `Endpoint mock not enabled or no response selected` |
| No state configured | `No state found for endpoint key: xxx` |
| Response file missing | `ERROR: Failed to load: xxx.json` |
| Wrong response selected | `Mock is enabled with file: xxx` (check filename) |

### Filtering Logs

Use logcat filters (Android) or console filters:

```bash
# Show only NetworkMock logs
adb logcat | grep "\[NetworkMock\]"

# Show only Plugin logs (request interception)
adb logcat | grep "\[NetworkMock\]\[Plugin\]"

# Show only Matching logs (why requests don't match)
adb logcat | grep "\[NetworkMock\]\[Matching\]"

# Show only State logs (configuration issues)
adb logcat | grep "\[NetworkMock\]\[State\]"
```

---

## Example Debug Session

### Scenario: Mock not being returned

**Logs**:
```
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: GET jsonplaceholder.typicode.com/users/1
[NetworkMock][State] Current state: globalMocking=true, endpoints=1
[NetworkMock][State]   jsonplaceholder-getUser: enabled=true, file=getUser-200.json
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock
[NetworkMock][Matching] Looking for match: GET jsonplaceholder.typicode.com/users/1
[NetworkMock][Matching] Comparing against 2 host(s):
[NetworkMock][Matching]   Host 'jsonplaceholder': jsonplaceholder.typicode.com vs jsonplaceholder.typicode.com = true
[NetworkMock][Matching] Host matched: 'jsonplaceholder' - checking 4 endpoint(s)
[NetworkMock][Matching]   Endpoint 'getUser':
[NetworkMock][Matching]     Path: /users/{userId} vs /users/1 = true
[NetworkMock][Matching]     Method: GET vs GET = true
[NetworkMock][Matching] SUCCESS: Matched endpoint 'getUser' on host 'jsonplaceholder'
[NetworkMock][Plugin] Found matching endpoint: jsonplaceholder-getUser
[NetworkMock][Plugin] Endpoint state: enabled=true, file=getUser-200.json
[NetworkMock][Plugin] Mock is enabled with file: getUser-200.json
[NetworkMock][Loading] Loading response: getUser/getUser-200.json
[NetworkMock][Loading] Successfully loaded: getUser-200.json (status 200)
[NetworkMock][Plugin] Successfully loaded mock response (status 200)
[NetworkMock][Plugin] Returning MOCK response - NO network call will be made
[NetworkMock][Plugin] ========================================
```

**Analysis**: Everything working correctly! Mock is returned.

---

### Scenario: Wrong host

**Logs**:
```
[NetworkMock][Plugin] ========================================
[NetworkMock][Plugin] Intercepted request: GET wrong.api.com/users/1
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock
[NetworkMock][Matching] Looking for match: GET wrong.api.com/users/1
[NetworkMock][Matching] Comparing against 2 host(s):
[NetworkMock][Matching]   Host 'jsonplaceholder': jsonplaceholder.typicode.com vs wrong.api.com = false
[NetworkMock][Matching]   Host 'staging': staging.api.example.com vs wrong.api.com = false
[NetworkMock][Matching] ERROR: No matching host found for 'wrong.api.com'
[NetworkMock][Plugin] No matching endpoint config found
[NetworkMock][Plugin] Using actual network
[NetworkMock][Plugin] ========================================
```

**Analysis**: Request going to different host than configured in mocks.json

**Fix**: Either update mocks.json or fix the request URL

---

## Files Modified

1. ✅ **MockConfigRepository.kt** - Added 4 logging sections
2. ✅ **MockStateRepository.kt** - Added 3 logging methods
3. ✅ **NetworkMockPlugin.kt** - Added comprehensive request flow logging

---

## Total Log Points Added: 30+

The logging is **comprehensive** and will help identify exactly where and why mocks aren't being used!

---

## Status: ✅ COMPLETE

All necessary logging has been added. You can now:
1. ✅ See when requests are intercepted
2. ✅ Track global mocking state
3. ✅ See matching logic step-by-step
4. ✅ Identify endpoint state issues
5. ✅ Debug response loading problems
6. ✅ Know exactly when mocks are used vs network

**Ready for debugging!** 🔍

