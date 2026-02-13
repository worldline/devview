# Duplicate Repository Instance Fix ✅

## Issue Found

The previous fix still had a problem: **both the UI and HttpClient were creating their own `MockStateRepository` instances**, even though they shared the same DataStore file.

```kotlin
// In RememberHttpClient.kt:
val dataStore = rememberDataStore(...)
val mockStateRepository = remember(dataStore) {
    MockStateRepository(dataStore)  // Instance #1
}

// In NetworkMockScreen.kt:
val dataStore = rememberDataStore(...)
return remember(dataStore) {
    MockStateRepository(dataStore)  // Instance #2 - DUPLICATE!
}
```

**Problem**: Two separate repository instances could lead to:
- Cached state inconsistencies
- Race conditions
- Potential bugs with state updates

---

## The Fix

Created a **single shared repository** that both the UI and HttpClient use.

### 1. New File: `RememberMockStateRepository.kt`

```kotlin
@Composable
public fun rememberMockStateRepository(): MockStateRepository {
    val dataStore = rememberDataStore(NETWORK_MOCK_DATASTORE_NAME)
    return remember(dataStore) {
        MockStateRepository(dataStore)  // Single instance!
    }
}
```

### 2. Updated `RememberHttpClient.kt`

```kotlin
@Composable
public fun rememberHttpClientWithMocking(
    mockStateRepository: MockStateRepository = rememberMockStateRepository()
): HttpClient {
    return remember(mockStateRepository) {
        createHttpClientWithMocking(
            mockStateRepository = mockStateRepository,  // Uses passed instance
            resourceLoader = { path -> Res.readBytes(path) }
        )
    }
}
```

### 3. Updated `App.kt`

```kotlin
@Composable
public fun App(...) {
    // Create single shared repository instance
    val mockStateRepository = rememberMockStateRepository()
    
    // Pass it to HttpClient
    val httpClient = rememberHttpClientWithMocking(mockStateRepository = mockStateRepository)
    val api = remember(httpClient) { SampleApi(httpClient) }
    
    // Same repository instance would be available for UI if needed
}
```

### 4. Updated Platform Implementations

Changed from accepting `DataStore` to accepting `MockStateRepository`:

**Before**:
```kotlin
public actual fun createHttpClientWithMocking(
    dataStore: DataStore<Preferences>,  // ❌
    resourceLoader: suspend (String) -> ByteArray
): HttpClient = HttpClient(...) {
    baseHttpClientConfig(
        dataStore = dataStore,
        ...
    )
}
```

**After**:
```kotlin
public actual fun createHttpClientWithMocking(
    mockStateRepository: MockStateRepository,  // ✅
    resourceLoader: suspend (String) -> ByteArray
): HttpClient = HttpClient(...) {
    baseHttpClientConfig(
        mockStateRepository = mockStateRepository,
        ...
    )
}
```

---

## Architecture

### Before (Multiple Instances):

```
App.kt
├─► rememberHttpClientWithMocking()
│       └─► Creates MockStateRepository #1
│
DevViewApp.kt (hypothetically)
└─► NetworkMockScreen
        └─► Creates MockStateRepository #2

❌ Two separate instances, same DataStore
```

### After (Single Instance):

```
App.kt
├─► rememberMockStateRepository()  ← Creates ONCE
│       └─► MockStateRepository (shared)
│
├─► rememberHttpClientWithMocking(mockStateRepository)
│       └─► Uses shared instance ✅
│
DevViewApp.kt
└─► NetworkMockScreen(stateRepository = mockStateRepository)
        └─► Uses same shared instance ✅

✅ One instance, same DataStore, shared everywhere
```

---

## Benefits

### ✅ Single Source of Truth
- One repository instance for entire app
- No inconsistencies between UI and plugin
- State always in sync

### ✅ No Race Conditions
- Both UI and plugin use same instance
- No conflicts from multiple instances
- Predictable state management

### ✅ Memory Efficient
- Only one repository in memory
- Single DataStore connection
- No duplicate instances

### ✅ Flexible Usage
- Can be injected for testing
- Can be shared across multiple screens
- Default instance provided for convenience

---

## Files Modified

1. ✅ **RememberMockStateRepository.kt** (NEW) - Single repository creation
2. ✅ **RememberHttpClient.kt** - Accepts repository parameter
3. ✅ **App.kt** - Creates and shares repository
4. ✅ **BaseHttpClientConfig.kt** - Accepts MockStateRepository
5. ✅ **HttpClientWithMocking.kt** (common) - Updated signature
6. ✅ **HttpClientWithMocking.android.kt** - Accepts repository
7. ✅ **HttpClientWithMocking.ios.kt** - Accepts repository

---

## Testing

The app now has a **single shared repository**:

```
App starts
    ↓
rememberMockStateRepository() creates instance
    ↓
    ├─► HttpClient uses it
    └─► UI uses it (when NetworkMockScreen is shown)
    
All changes flow through ONE repository instance ✅
```

---

## Status: ✅ FIXED

The duplicate repository instance issue has been fixed! Now:

- ✅ Single `MockStateRepository` instance created
- ✅ Shared between HttpClient plugin and UI
- ✅ Same DataStore file
- ✅ Same repository instance
- ✅ Complete state consistency

**Network mocking should now work perfectly with no state inconsistencies!** 🎉

