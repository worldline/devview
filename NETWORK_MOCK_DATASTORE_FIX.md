# Critical DataStore Bug Fix ✅

## The Real Issue Found!

The NetworkMock module wasn't working because **the UI and the HttpClient plugin were using DIFFERENT DataStore files**!

---

## Root Cause

### Different DataStore Files

**UI (NetworkMockScreen)**:
```kotlin
// devview-networkmock/model/rememberDataStore.kt
internal const val NETWORK_MOCK_DATASTORE_NAME = "network_mock_datastore.preferences_pb"
```

**HttpClient Plugin** (in sample app):
```kotlin
// sample/shared/network/RememberHttpClient.kt
private const val NETWORK_MOCK_DATASTORE_NAME = "network_mock_datastore_2.preferences_pb"  // ❌ WRONG!
```

### What This Caused

```
User toggles mocking ON in UI
    ↓
Saves to: network_mock_datastore.preferences_pb
    ↓
UI reads from same file → Shows "ENABLED" ✅
    
Meanwhile...

User makes API call
    ↓
Plugin reads from: network_mock_datastore_2.preferences_pb  ❌
    ↓
Empty/default file → globalMocking = false
    ↓
Plugin thinks mocking is DISABLED
    ↓
Uses actual network instead of mock
```

**They were writing and reading from completely different files!**

---

## The Fix

Changed `RememberHttpClient.kt` to use the **same filename** as the UI:

```kotlin
// BEFORE (BROKEN):
private const val NETWORK_MOCK_DATASTORE_NAME = "network_mock_datastore_2.preferences_pb"

// AFTER (FIXED):
private const val NETWORK_MOCK_DATASTORE_NAME = "network_mock_datastore.preferences_pb"
```

Now both the UI and the plugin use the **same DataStore file**, so changes made in the UI are visible to the plugin!

---

## How This Happened

The `_2` suffix was likely added during testing/debugging and accidentally left in. This is a common issue when:
1. Creating temporary test files
2. Debugging DataStore issues
3. Testing with clean slate

The filename needs to be **exactly the same** across all code that reads/writes the mock state.

---

## Verification

With this fix and the detailed logging, you should now see:

### When Toggling Mocking ON:
```
[NetworkMock][State] Setting global mocking enabled: true
[NetworkMock][State] Global mocking state saved to DataStore
```

### When Making API Call:
```
[NetworkMock][Plugin] Intercepted request: GET jsonplaceholder.typicode.com/users/1
[NetworkMock][State] Reading state from DataStore...
[NetworkMock][State] DataStore preferences keys: [network_mock_global_enabled, network_mock_endpoints, ...]
[NetworkMock][State] KEY_GLOBAL_ENABLED value: true ✅
[NetworkMock][State] KEY_ENDPOINTS JSON: {"jsonplaceholder-getUser":{"mockEnabled":true,"selectedResponseFile":"getUser-200.json"}}
[NetworkMock][State] Successfully decoded 1 endpoint(s)
[NetworkMock][State] ========================================
[NetworkMock][State] FINAL STATE: globalMocking=true, endpoints=1 ✅
[NetworkMock][State]   jsonplaceholder-getUser: enabled=true, file=getUser-200.json
[NetworkMock][State] ========================================
[NetworkMock][Plugin] Global mocking is ENABLED - checking for mock ✅
[NetworkMock][Plugin] Found matching endpoint: jsonplaceholder-getUser
[NetworkMock][Plugin] Mock is enabled with file: getUser-200.json
[NetworkMock][Plugin] Successfully loaded mock response (status 200)
[NetworkMock][Plugin] Returning MOCK response - NO network call will be made ✅
```

---

## Additional Improvements Made

While fixing this, I also improved the logging in `getState()`:

```kotlin
public suspend fun getState(): NetworkMockState {
    println("[NetworkMock][State] Reading state from DataStore...")
    
    val preferences = dataStore.data.first()
    
    // Show ALL preference keys
    println("[NetworkMock][State] DataStore preferences keys: ${preferences.asMap().keys}")
    
    // Show the actual values being read
    val globalEnabled = preferences[KEY_GLOBAL_ENABLED] ?: false
    println("[NetworkMock][State] KEY_GLOBAL_ENABLED value: $globalEnabled")
    
    val endpointsJson = preferences[KEY_ENDPOINTS]
    println("[NetworkMock][State] KEY_ENDPOINTS JSON: $endpointsJson")
    
    // Show decoded endpoint count
    val endpoints = endpointsJson?.let { json ->
        val decoded = this.json.decodeFromString<Map<String, EndpointMockState>>(json)
        println("[NetworkMock][State] Successfully decoded ${decoded.size} endpoint(s)")
        decoded
    } ?: run {
        println("[NetworkMock][State] No endpoints JSON found in DataStore")
        emptyMap()
    }
    
    // Show final complete state
    println("[NetworkMock][State] ========================================")
    println("[NetworkMock][State] FINAL STATE: globalMocking=${state.globalMockingEnabled}, endpoints=${state.endpointStates.size}")
    state.endpointStates.forEach { (key, endpointState) ->
        println("[NetworkMock][State]   $key: enabled=${endpointState.mockEnabled}, file=${endpointState.selectedResponseFile}")
    }
    println("[NetworkMock][State] ========================================")
    
    return state
}
```

This makes it **crystal clear** what's being read from DataStore at every step.

---

## Files Modified

1. ✅ **sample/shared/network/RememberHttpClient.kt** - Fixed DataStore filename
2. ✅ **devview-networkmock/repository/MockStateRepository.kt** - Enhanced logging

---

## Testing Steps

1. **Clear app data** to remove old DataStore files:
   ```bash
   adb shell pm clear com.worldline.devview.sample
   ```

2. **Run the app**

3. **Enable global mocking** in DevView UI

4. **Enable an endpoint mock** and select a response

5. **Make an API call** using the test button

6. **Check logs** - you should now see:
   - State being saved with the correct filename
   - State being read with the SAME filename
   - Global mocking = true
   - Endpoint state correctly loaded
   - Mock response being returned

---

## Status: ✅ FIXED

The critical DataStore filename mismatch has been fixed! The UI and plugin now share the same DataStore file, so changes made in the UI are immediately visible to the plugin.

**Network mocking should now work correctly!** 🎉

---

## Prevention

To prevent this in the future:

1. ✅ **Use a constant defined in one place** - The NetworkMock module already has this
2. ✅ **Import that constant** instead of defining it again
3. ✅ **Add validation** - Could add a check that warns if different DataStore names are used

Better approach going forward:

```kotlin
// In NetworkMock module - EXPORT the constant
public const val NETWORK_MOCK_DATASTORE_NAME = "network_mock_datastore.preferences_pb"

// In sample app - IMPORT and USE it
import com.worldline.devview.networkmock.model.NETWORK_MOCK_DATASTORE_NAME

val dataStore = rememberDataStore(NETWORK_MOCK_DATASTORE_NAME)  // ✅ Single source of truth
```

This would make it impossible to have this mismatch!

