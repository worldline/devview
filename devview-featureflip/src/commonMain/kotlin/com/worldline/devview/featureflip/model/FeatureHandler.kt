@file:Suppress("CommentOverPrivateFunction", "CommentOverPrivateProperty")

package com.worldline.devview.featureflip.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

/**
 * Handler for managing feature flag state persistence and retrieval.
 *
 * This class provides the core functionality for the FeatureFlip module, handling:
 * - Registration of feature flags
 * - Persistent storage using DataStore
 * - State queries (both Flow and Compose State)
 * - State updates with validation
 *
 * ## Architecture
 * The handler maintains an internal registry mapping each [Feature] to its corresponding
 * DataStore preference key:
 * - [Feature.LocalFeature] uses `booleanPreferencesKey` to store the enabled state
 * - [Feature.RemoteFeature] uses `intPreferencesKey` to store the state ordinal
 *
 * ## Usage
 *
 * ### Creating a Handler
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     val features = remember {
 *         listOf(
 *             Feature.LocalFeature(
 *                 name = "dark_mode",
 *                 description = "Enable dark theme",
 *                 isEnabled = false
 *             ),
 *             Feature.RemoteFeature(
 *                 name = "new_feature",
 *                 description = "Experimental feature",
 *                 defaultRemoteValue = true,
 *                 state = FeatureState.REMOTE
 *             )
 *         )
 *     }
 *
 *     val featureHandler = rememberFeatureHandler(features)
 *
 *     CompositionLocalProvider(LocalFeatureHandler provides featureHandler) {
 *         Content()
 *     }
 * }
 * ```
 *
 * ### Checking Feature State in Composables
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val featureHandler = LocalFeatureHandler.current
 *     val isDarkMode by featureHandler.isFeatureEnabled("dark_mode")
 *
 *     if (isDarkMode) {
 *         DarkThemeContent()
 *     } else {
 *         LightThemeContent()
 *     }
 * }
 * ```
 *
 * ### Checking Feature State with Flow
 * ```kotlin
 * class MyViewModel(private val featureHandler: FeatureHandler) : ViewModel() {
 *     val isDarkModeEnabled: Flow<Boolean> =
 *         featureHandler.isFeatureEnabledFlow("dark_mode")
 * }
 * ```
 *
 * ### Updating Feature State
 * ```kotlin
 * viewModelScope.launch {
 *     featureHandler.setFeatureState("new_feature", FeatureState.LOCAL_ON)
 * }
 * ```
 *
 * ## Thread Safety
 * All operations are thread-safe. DataStore handles concurrent access internally,
 * and the handler's internal state is protected.
 *
 * @property dataStore The DataStore instance used for persistence.
 * @param initialFeatures The initial list of features to register and manage.
 *
 * @see Feature
 * @see FeatureState
 * @see rememberFeatureHandler
 * @see LocalFeatureHandler
 */
public class FeatureHandler(
    private val dataStore: DataStore<Preferences>,
    initialFeatures: List<Feature>
) {
    /**
     * Internal registry mapping features to their corresponding DataStore preference keys.
     *
     * This mutable list maintains pairs of [Feature] instances and their associated
     * [Preferences.Key]. Local features use Boolean keys, while remote features use Int keys
     * to store their state ordinal values.
     */
    private val featuresAndPreferenceKeys: MutableList<Pair<Feature, Preferences.Key<*>>> =
        initialFeatures
            .map { feature ->
                val preferenceKey = when (feature) {
                    is Feature.LocalFeature -> booleanPreferencesKey(name = feature.name)
                    is Feature.RemoteFeature -> intPreferencesKey(name = feature.name)
                }
                feature to preferenceKey
            }.toMutableList()

    /**
     * Checks whether a feature is currently enabled.
     *
     * For [Feature.LocalFeature], returns the stored boolean value.
     * For [Feature.RemoteFeature], resolves the enabled state based on the current [FeatureState]:
     * - [FeatureState.REMOTE]: Returns the default remote value
     * - [FeatureState.LOCAL_ON]: Returns true
     * - [FeatureState.LOCAL_OFF]: Returns false
     *
     * @param featureName The name of the feature to check
     * @return A Flow emitting the current enabled state of the feature
     * @throws IllegalArgumentException if no feature with the given name is registered
     */
    public fun isFeatureEnabledFlow(featureName: String): Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(value = emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val featureAndPreferenceKey =
                featuresAndPreferenceKeys.firstOrNull { it.first.name == featureName }
                    ?: throw IllegalArgumentException("Feature with name $featureName not found")

            when (val feature = featureAndPreferenceKey.first) {
                is Feature.LocalFeature -> {
                    @Suppress("UNCHECKED_CAST")
                    val preferenceKey =
                        featureAndPreferenceKey.second as Preferences.Key<Boolean>
                    preferences[preferenceKey] ?: false
                }

                is Feature.RemoteFeature -> {
                    @Suppress("UNCHECKED_CAST")
                    val preferenceKey = featureAndPreferenceKey.second as Preferences.Key<Int>
                    val state = FeatureState.fromOrdinal(
                        ordinal = preferences[preferenceKey] ?: FeatureState.REMOTE.ordinal
                    )
                    when (state) {
                        FeatureState.REMOTE -> feature.defaultRemoteValue
                        FeatureState.LOCAL_OFF -> false
                        FeatureState.LOCAL_ON -> true
                    }
                }
            }
        }

    /**
     * Checks whether a feature is currently enabled as a Compose [State].
     *
     * This composable function returns a State that automatically updates when the
     * feature's enabled status changes. It uses [isFeatureEnabledFlow] internally
     * and collects it as lifecycle-aware state.
     *
     * The initial value is determined from the registered feature's default state,
     * or false if the feature is not found.
     *
     * @param featureName The name of the feature to check
     * @return A [State] object emitting the current enabled state of the feature
     */
    @Composable
    public fun isFeatureEnabled(featureName: String): State<Boolean> =
        isFeatureEnabledFlow(featureName = featureName)
            .collectAsStateWithLifecycle(
                initialValue = featuresAndPreferenceKeys
                    .map { it.first }
                    .firstOrNull { feature ->
                        feature.name == featureName
                    }?.isEnabled ?: false
            )

    /**
     * Updates the state of a feature flag.
     *
     * For [Feature.LocalFeature], sets the enabled boolean:
     * - [FeatureState.LOCAL_ON] sets to true
     * - [FeatureState.LOCAL_OFF] sets to false
     * - [FeatureState.REMOTE] throws an exception (not applicable to local features)
     *
     * For [Feature.RemoteFeature], sets the state ordinal to control the feature.
     *
     * @param featureName The name of the feature to update
     * @param state The new state to set
     * @throws IllegalArgumentException if no feature with the given name is registered,
     *         or if trying to set REMOTE state for a local feature
     */
    internal suspend fun setFeatureState(featureName: String, state: FeatureState) {
        dataStore.edit { preferences ->
            val featureAndPreferenceKey =
                featuresAndPreferenceKeys.firstOrNull { it.first.name == featureName }
                    ?: throw IllegalArgumentException("Feature with name $featureName not found")

            when (featureAndPreferenceKey.first) {
                is Feature.LocalFeature -> {
                    @Suppress("UNCHECKED_CAST")
                    val preferenceKey = featureAndPreferenceKey.second as Preferences.Key<Boolean>
                    when (state) {
                        FeatureState.REMOTE -> throw IllegalArgumentException(
                            "Cannot set remote state for local feature"
                        )

                        FeatureState.LOCAL_OFF -> preferences[preferenceKey] = false
                        FeatureState.LOCAL_ON -> preferences[preferenceKey] = true
                    }
                }

                is Feature.RemoteFeature -> {
                    @Suppress("UNCHECKED_CAST")
                    val preferenceKey = featureAndPreferenceKey.second as Preferences.Key<Int>
                    preferences[preferenceKey] = state.ordinal
                }
            }
        }
    }

    /**
     * Registers and persists a list of features.
     *
     * This method adds the features to the internal registry (if not already present)
     * and stores their initial state in DataStore. Features with the same name will
     * not be added twice.
     *
     * @param featuresToAdd The list of features to register and persist
     */
    public suspend fun addFeatures(featuresToAdd: List<Feature>) {
        val newFeaturesAndPreferenceKeys = featuresToAdd.zip(
            other = featuresToAdd.map {
                when (it) {
                    is Feature.LocalFeature -> booleanPreferencesKey(name = it.name)
                    is Feature.RemoteFeature -> intPreferencesKey(name = it.name)
                }
            }
        )

        newFeaturesAndPreferenceKeys.forEach { (feature, preferenceKey) ->
            if (!featuresAndPreferenceKeys.any { feature.name == it.first.name }) {
                featuresAndPreferenceKeys.add(element = feature to preferenceKey)
            }

            dataStore.edit { preferences ->
                when (feature) {
                    is Feature.LocalFeature -> {
                        @Suppress("UNCHECKED_CAST")
                        val castPreferenceKey = preferenceKey as Preferences.Key<Boolean>
                        preferences[castPreferenceKey] = feature.isEnabled
                    }

                    is Feature.RemoteFeature -> {
                        @Suppress("UNCHECKED_CAST")
                        val castPreferenceKey = preferenceKey as Preferences.Key<Int>
                        preferences[castPreferenceKey] = feature.state.ordinal
                    }
                }
            }
        }
    }

    /**
     * Retrieves all registered features with their current persisted state.
     *
     * The returned features will have their state/enabled properties updated
     * based on what's stored in DataStore.
     *
     * @return A Flow emitting the list of all registered features with current state
     */
    private fun getFeatures(): Flow<List<Feature>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(value = emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            featuresAndPreferenceKeys.map { (feature, preferenceKey) ->
                when (feature) {
                    is Feature.LocalFeature -> {
                        @Suppress("UNCHECKED_CAST")
                        val castPreferenceKey = preferenceKey as Preferences.Key<Boolean>
                        feature.copy(
                            isEnabled = preferences[castPreferenceKey] ?: feature.isEnabled
                        )
                    }

                    is Feature.RemoteFeature -> {
                        @Suppress("UNCHECKED_CAST")
                        val castPreferenceKey = preferenceKey as Preferences.Key<Int>
                        val state = FeatureState.fromOrdinal(
                            ordinal = preferences[castPreferenceKey] ?: feature.state.ordinal
                        )
                        feature.copy(state = state)
                    }
                }
            }
        }

    /**
     * Provides access to all registered features as a Compose [State].
     *
     * This property returns a lifecycle-aware State containing the current list of all
     * registered features with their persisted state values. The State automatically
     * updates when any feature's state changes in DataStore.
     *
     * This is an internal property used primarily for feature management UI components.
     *
     * @return A [State] containing the list of all features with their current state
     */
    internal val features: State<List<Feature>>
        @Composable get() = getFeatures()
            .collectAsStateWithLifecycle(
                initialValue = featuresAndPreferenceKeys.map { it.first }
            )
}

/**
 * Remembers and returns a [FeatureHandler] instance for the current composition.
 *
 * This composable creates a FeatureHandler backed by a platform-specific DataStore
 * and automatically initializes it with the provided features. The instance is
 * remembered across recompositions and tied to the composition lifecycle.
 *
 * ## Platform-Specific Storage
 * - **Android**: Stored in `{appFilesDir}/feature_flip_datastore.preferences_pb`
 * - **iOS**: Stored in `{documentDirectory}/feature_flip_datastore.preferences_pb`
 *
 * ## Usage
 *
 * ### Basic Setup
 * ```kotlin
 * @Composable
 * fun App() {
 *     val features = remember {
 *         listOf(
 *             Feature.LocalFeature(
 *                 name = "dark_mode",
 *                 description = "Enable dark theme",
 *                 isEnabled = false
 *             ),
 *             Feature.RemoteFeature(
 *                 name = "new_checkout",
 *                 description = "New checkout flow",
 *                 defaultRemoteValue = true,
 *                 state = FeatureState.REMOTE
 *             )
 *         )
 *     }
 *
 *     val featureHandler = rememberFeatureHandler(features)
 *
 *     CompositionLocalProvider(LocalFeatureHandler provides featureHandler) {
 *         NavigationHost()
 *     }
 * }
 * ```
 *
 * ### With Dynamic Features
 * ```kotlin
 * @Composable
 * fun App(remoteConfig: RemoteConfig) {
 *     val features = remember(remoteConfig) {
 *         buildList {
 *             add(Feature.LocalFeature("dark_mode", null, false))
 *
 *             // Add remote features based on remote config
 *             remoteConfig.getAllKeys().forEach { key ->
 *                 add(Feature.RemoteFeature(
 *                     name = key,
 *                     description = null,
 *                     defaultRemoteValue = remoteConfig.getBoolean(key),
 *                     state = FeatureState.REMOTE
 *                 ))
 *             }
 *         }
 *     }
 *
 *     val handler = rememberFeatureHandler(features)
 *     // Use handler...
 * }
 * ```
 *
 * @param features The list of features to initialize the handler with.
 *                 These features will be registered and their initial state persisted.
 * @return A remembered [FeatureHandler] instance bound to the composition lifecycle.
 *
 * @see FeatureHandler
 * @see LocalFeatureHandler
 * @see rememberDataStore
 */
@Composable
public fun rememberFeatureHandler(features: List<Feature>): FeatureHandler {
    val dataStore = rememberDataStore()

    return remember(key1 = dataStore) {
        FeatureHandler(
            dataStore = dataStore,
            initialFeatures = features
        )
    }
}

/**
 * CompositionLocal providing access to the current [FeatureHandler].
 *
 * This allows components deep in the composition tree to access the FeatureHandler
 * instance for checking and managing feature flags without explicit parameter passing.
 *
 * ## Usage
 *
 * ### Providing the Handler
 * ```kotlin
 * @Composable
 * fun App() {
 *     val features = remember { /* your features */ }
 *     val featureHandler = rememberFeatureHandler(features)
 *
 *     CompositionLocalProvider(LocalFeatureHandler provides featureHandler) {
 *         Content()
 *     }
 * }
 * ```
 *
 * ### Consuming the Handler
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val featureHandler = LocalFeatureHandler.current
 *     val isDarkMode by featureHandler.isFeatureEnabled("dark_mode")
 *
 *     // Use the feature state...
 * }
 * ```
 *
 * ### In ViewModels (via dependency injection)
 * ```kotlin
 * class MyViewModel(
 *     private val featureHandler: FeatureHandler
 * ) : ViewModel() {
 *     val newFeatureEnabled = featureHandler
 *         .isFeatureEnabledFlow("new_feature")
 *         .stateIn(viewModelScope, SharingStarted.Eagerly, false)
 * }
 * ```
 *
 * @throws IllegalStateException if accessed without being provided in the composition tree.
 *
 * @see FeatureHandler
 * @see rememberFeatureHandler
 */
public val LocalFeatureHandler: ProvidableCompositionLocal<FeatureHandler> =
    staticCompositionLocalOf {
        error(message = "No FeatureHandler provided")
    }
