# FeatureFlip API Reference

Complete API documentation for the DevView FeatureFlip module.

## Overview

The FeatureFlip module provides feature flag management with persistent storage.

## Key Components

### Feature Sealed Class

```kotlin
sealed class Feature {
    abstract val name: String
    abstract val description: String?
    abstract val isEnabled: Boolean
    
    data class LocalFeature(
        override val name: String,
        override val description: String?,
        override val isEnabled: Boolean
    ) : Feature()
    
    data class RemoteFeature(
        override val name: String,
        override val description: String?,
        val defaultRemoteValue: Boolean,
        val state: FeatureState
    ) : Feature()
}
```

### FeatureState Enum

```kotlin
enum class FeatureState {
    REMOTE,      // Use remote configuration
    LOCAL_OFF,   // Force OFF locally
    LOCAL_ON     // Force ON locally
}
```

### FeatureHandler Class

```kotlin
class FeatureHandler(
    private val dataStore: DataStore<Preferences>,
    initialFeatures: List<Feature>
) {
    fun isFeatureEnabledFlow(featureName: String): Flow<Boolean>
    
    @Composable
    fun isFeatureEnabled(featureName: String): State<Boolean>
    
    suspend fun setFeatureState(featureName: String, state: FeatureState)
    
    suspend fun addFeatures(featuresToAdd: List<Feature>)
    
    val features: State<List<Feature>>
        @Composable get
}
```

### Composables

```kotlin
@Composable
fun FeatureFlipScreen(modifier: Modifier = Modifier)

@Composable
fun rememberFeatureHandler(features: List<Feature>): FeatureHandler

val LocalFeatureHandler: ProvidableCompositionLocal<FeatureHandler>
```

## Detailed Documentation

All APIs are documented with KDoc comments. Generate HTML docs:

```bash
./gradlew dokkaHtml
```

## See Also

- [FeatureFlip Module Guide](../modules/featureflip.md)
- [Feature Flags Example](../examples/feature-flags.md)
