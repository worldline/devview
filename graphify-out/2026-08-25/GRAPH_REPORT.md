# Graph Report - devview  (2026-08-25)

## Corpus Check
- 324 files · ~177,773 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2600 nodes · 4572 edges · 202 communities (152 shown, 50 thin omitted)
- Extraction: 90% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 433 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0202cdc5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Detekt Style Rule Set (Full)
- AnalyticsLogCategory
- Module Overview & Sample Wiring
- Module Overview & Examples
- FeatureTriStateSwitch.kt
- NetworkMockScreen.kt
- NetworkMock Request Matcher Tests
- PreviewSheetState
- OperationUiModel
- MockItem.kt
- Analytics Module API History
- NetworkMock Endpoint UI API History
- Analytics Category Conventions
- DevView Core API (Current)
- Detekt Potential-Bugs Rule Set
- DiffLineUtilsTest
- OperationKey
- OperationMockState
- NetworkMockPlugin.kt
- Detekt Compose Rule Set & Modules
- MockStateRepository
- NetworkMockViewModel
- NetworkMockState
- MockStateRepositoryTest
- MultiplatformLibraryConventionPlugin.kt
- NetworkMock DataStore & Matcher API History
- EndpointMockState (api 0.1.2)
- MockConfigRepository (api 0.1.3)
- Detekt Naming Rule Set
- FakePreferencesDataStore
- Code Style & Pre-commit Hooks
- Detekt Complexity Rule Set
- FeatureHandler
- MockResponseDiffColors
- NetworkMockEndpointViewModel
- Module
- DevView Utils DataStore API History
- Detekt Comments Rule Set
- EndpointConfig (api 0.1.2)
- Analytics
- Feature Handler API
- OpenApiDocument.kt
- EndpointConfig (api 0.1.4)
- NetworkMock Core & Diff Pipeline
- Detekt Empty-Blocks Rule Set
- Renovate Dependency Config
- Highlighted Analytics Log Cards
- DevView Core API v0.1.1
- DevView Core API v0.1.2
- DevView Core API v0.1.3
- DevView Core API v0.1.4
- DisplayLine
- DevView Configuration Guide
- Detekt Global Config & Coroutines Rules
- AnalyticsScreen
- MockConfigRepository (api 0.1.2)
- Preview Sheet State Tests
- TestModule
- FeatureFlipScreen.kt
- Graphify Skill Documentation
- Detekt Exceptions Rule Set
- Feature Sealed Type Model
- NetworkMock.kt
- MockResponse
- iOS App Entry Point
- TimeCapsule Module Design
- FeatureFlip Module API History
- NetworkMock Ktor Plugin API History
- EndpointCardTest
- TestModule
- DevView Test Utilities Module
- Endpoint State Chip Tests
- Getting Started Overview
- NetworkMock Ktor/Core Shared State
- ScreenCapsule
- DestinationMetadata
- ModelUtilsTest
- DevView Utils DataStore Module
- Release Publishing Process
- Section
- add-konsist-rule Skill
- update-docs Skill
- FeatureFlip.kt
- FeatureFlip Screen & Filters
- Feature Tri-State Switch UI
- HomeScreen.kt
- DiffLine
- createDataStore.kt
- TimeRange
- Gradle Module Setup Guide
- Quick Start Guide
- Docs Site Branding Theme JS
- Release Script (release.py)
- Graphify Exports Documentation
- MockResponseDiffContent.kt
- FeatureFlip Module Overview
- KamlSmokeTest
- NetworkMock Destination API History
- StatusCodeFamily
- MockResponseTest
- DevViewTest.kt
- ComposeTest.kt
- ViewModelTest
- Branding Assets Overview
- Platform
- Detekt Performance Rule Set
- PaddingValues
- ModuleDependencyTest.kt
- Troubleshooting & Examples Overview
- Git Workflow Conventions
- Detekt Libraries Rule Set
- PackageNamingTest.kt
- konsist/ViewModelTest.kt
- Detekt Console Reports Settings
- Request Matcher Implementation
- Android DataStore Creation
- NetworkMockEndpointScreen.kt
- Installation & Setup Guide
- CI Publish & Release Workflows
- Test Writing Conventions
- FeatureType
- Feature Sealed Class Tests
- StatusCodeFamily API History
- iOS DataStore Creation
- Prerequisites Overview
- DataStore Convention Plugin
- Device Test Convention Plugin
- Konsist Convention Plugin
- Kover Convention Plugin
- Ktor Convention Plugin
- Metalava Convention Plugin
- Room Convention Plugin
- Unit Test Convention Plugin
- FeatureState Ordinal Tests
- FeatureType Ordinal Tests
- StatusCodeFamily Tests
- MockHttpClientCall API History
- Ktor Plugin Test Data
- Boolean Preview Provider API History
- Gradle Wrapper Script
- NetworkMock Endpoint UI Model
- NetworkMock Class API History
- Flow Test Assertions
- GitHub Issue Templates
- Sample Compose App Test
- NetworkMock Endpoint Destination
- Detekt Dagger-Related Rule
- Poko Annotation Alias Rationale
- Poko Annotation Alias
- TimeCapsule Row Delta Time
- Docs Build Script
- Docs Version Cleanup Script
- GroupEnvironmentUiModel
- NetworkMockDestination.Main
- NetworkMockEndpointUiState
- NetworkMockScreen
- NetworkMockUiState
- TimeCapsuleDestination
- TimeCapsuleDestination.Main
- TimeCapsuleScreen
- DevView Icon (Dark)
- DevView Icon (Light)
- DevView Icon (Mono)
- DevView Logo (Dark)
- DevView Logo (Light)
- DevView Logo (Mono)
- Close Inactive Issues Workflow
- Sample App Icon (mdpi)
- Sample App Round Icon (mdpi)
- Sample App Icon (xhdpi)
- Sample App Icon (xxhdpi)
- DevView iOS App Icon
- Graphify Query/Path/Explain Docs
- Graphify Add/Watch Docs
- Graphify Commit Hook Docs
- Graphify GitHub/Merge Docs
- Graphify Transcribe Docs
- Graphify CLAUDE.md Integration
- Graphify Extraction Spec Docs

## God Nodes (most connected - your core abstractions)
1. `Style Rule Set` - 95 edges
2. `AnalyticsLogCategory` - 60 edges
3. `OperationKey` - 49 edges
4. `AnalyticsLogType` - 48 edges
5. `RequestMatcherTest` - 48 edges
6. `NetworkMockState` - 45 edges
7. `MockResponse` - 42 edges
8. `Potential Bugs Rule Set` - 40 edges
9. `OperationMockState` - 33 edges
10. `MockStateRepository` - 31 edges

## Surprising Connections (you probably didn't know these)
- `NetworkMockDataStoreDelegate (devview-networkmock-core)` --semantically_similar_to--> `TimeCapsule`  [INFERRED] [semantically similar]
  devview-networkmock/CLAUDE.md → devview-timecapsule/api/api.txt
- `Network Mock Test Files README` --semantically_similar_to--> `NetworkMock Workflows Doc`  [INFERRED] [semantically similar]
  sample/network/src/commonMain/composeResources/files/networkmocks/README.md → docs/modules/networkmock-workflows.md
- `Prerequisites` --semantically_similar_to--> `Prerequisites (Installation Recap)`  [INFERRED] [semantically similar]
  docs/getting-started/prerequisites.md → docs/getting-started/installation.md
- `Detekt Compose Rule Set Config` --conceptually_related_to--> `Compose Multiplatform`  [INFERRED]
  config/quality/detekt/compose-config.yml → README.md
- `Module interface` --semantically_similar_to--> `com.worldline.devview.core.Module (external)`  [INFERRED] [semantically similar]
  devview-featureflip/CLAUDE.md → devview-featureflip/api/api.txt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Ktor Request Interception and Mock Lookup Pipeline** — devview_networkmock_ktor_api_api_networkmockplugin, com_worldline_devview_networkmock_core_repository_mockconfigrepository, com_worldline_devview_networkmock_core_repository_mockstaterepository, devview_networkmock_ktor_api_api_mockhttpclientcall [EXTRACTED 1.00]
- **Shared NetworkMock DataStore Singleton Across UI and Ktor Plugin** — com_worldline_devview_networkmock_core_networkmockdatastoredelegate, devview_networkmock_api_api_networkmock, devview_networkmock_ktor_api_api_networkmockconfig [EXTRACTED 1.00]
- **NetworkMock Core/UI/Ktor Three-Module Split** — docs_modules_networkmock_core_networkmock_core, docs_modules_networkmock_ktor_networkmock_ktor, docs_modules_networkmock_ui_networkmock_ui [EXTRACTED 1.00]
- **Per-Screen State Recording and Restore Flow** — devview_timecapsule_api_api_timecapsuleowner, devview_timecapsule_api_api_timecapsuleeffect, devview_timecapsule_claude_screencapsule, devview_timecapsule_api_api_timecapsule [EXTRACTED 1.00]
- **Documentation Synchronization Pipeline** — claude_skills_update_docs_skill, changelog, github_workflows_publish_docs, scripts_build_docs_sh [INFERRED 0.75]
- **CI Quality Gate Pipeline** — github_workflows_build, concept_detekt, concept_konsist, claude_skills_local_ci_skill [INFERRED 0.80]
- **Custom Module Creation Workflow (Guide, Reference Doc, Sample)** — docs_guides_module_development_module_development, docs_modules_custom_modules_custom_modules, sample_claude_testmodule [INFERRED 0.80]
- **DevView Module Scaffolding Workflow** — claude_skills_add_module_skill, claude_skills_add_destination_skill, claude_agents_module_expert, concept_module_interface [INFERRED 0.80]
- **Module Interface Implementations Across Feature Docs** — docs_modules_featureflip_featureflip, docs_modules_analytics_analytics, docs_modules_networkmock_networkmock, docs_modules_timecapsule_timecapsule, docs_modules_custom_modules_custom_modules [INFERRED 0.85]

## Communities (202 total, 50 thin omitted)

### Community 0 - "Detekt Style Rule Set (Full)"
Cohesion: 0.02
Nodes (92): AbstractClassCanBeConcreteClass Rule, AbstractClassCanBeInterface Rule, AlsoCouldBeApply Rule, BracesOnIfStatements Rule, BracesOnWhenStatements Rule, CanBeNonNullable Rule, CascadingCallWrapping Rule, ClassOrdering Rule (+84 more)

### Community 1 - "AnalyticsLogCategory"
Cohesion: 0.05
Nodes (74): Bundle, ComponentActivity, AnalyticsLogger, AnalyticsLogItem(), AnalyticsLogItemPreview(), Modifier, CategoryChip(), CategoryChipPreview() (+66 more)

### Community 2 - "Module Overview & Sample Wiring"
Cohesion: 0.07
Nodes (59): Module interface, Guides Index, Integration Guide, rememberModules, Module Development Guide, Navigation Guide, NavKey, Section-Derived Default Colors (v0.1.4+) (+51 more)

### Community 3 - "Module Overview & Examples"
Cohesion: 0.05
Nodes (59): Analytics module, Advanced Examples, FeatureFlip module, Multi-Module Integration, MyAdvancedModule, NetworkMock module, NetworkMockResourceLoader, rememberModules (+51 more)

### Community 4 - "FeatureTriStateSwitch.kt"
Cohesion: 0.06
Nodes (48): Animatable, AnimationVector1D, BorderStroke, Constraints, FeatureTriStateSwitchTest, FeatureItem(), FeatureItemPreview(), Feature (+40 more)

### Community 5 - "NetworkMockScreen.kt"
Cohesion: 0.06
Nodes (30): GlobalMockToggleTest, NetworkMockScreenTest, EmptyState(), Modifier, ErrorState(), Modifier, GlobalMockToggle(), GlobalMockToggleEnabledPreview() (+22 more)

### Community 7 - "PreviewSheetState"
Cohesion: 0.23
Nodes (11): Modifier, NetworkMockEndpointPreviewBottomSheet(), NetworkMockEndpointPreviewBottomSheetPreview(), PreviewHeader(), PreviewParameterProvider, PreviewSheetStatePreviewParameterProvider, Compare, HasResponse (+3 more)

### Community 8 - "OperationUiModel"
Cohesion: 0.24
Nodes (13): EndpointCard(), EndpointCardPreview(), EndpointCardWithFileNamePreview(), Modifier, EndpointHeaderCard(), EndpointHeaderCardPreview(), Modifier, EndpointStateChip() (+5 more)

### Community 9 - "MockItem.kt"
Cohesion: 0.17
Nodes (17): Modifier, MockItem(), MockItemContent(), MockItemPreview(), MockItemPreviewModePreview(), MockItemSelectedPreview(), NetworkItem(), NetworkItemPreview() (+9 more)

### Community 10 - "Analytics Module API History"
Cohesion: 0.06
Nodes (42): Analytics Module API Surface v0.1.1, Analytics Module API Surface v0.1.2, Analytics Module API Surface v0.1.3, Analytics Module API Surface v0.1.4, Analytics (Module implementation), AnalyticsDestination, AnalyticsDestination.Main, AnalyticsLog (+34 more)

### Community 11 - "NetworkMock Endpoint UI API History"
Cohesion: 0.08
Nodes (42): EndpointUiModel (v0.1.2), GroupEnvironmentUiModel (v0.1.2), NetworkMockEndpointUiState (v0.1.2), NetworkMockEndpointUiState.Content (v0.1.2), NetworkMockEndpointUiState.Error (v0.1.2), NetworkMockEndpointUiState.Loading (v0.1.2), NetworkMockEndpointViewModel (v0.1.2), NetworkMockScreen (v0.1.2) (+34 more)

### Community 12 - "Analytics Category Conventions"
Cohesion: 0.06
Nodes (41): AnalyticsLogCategory.Action, Convention: Adding a new AnalyticsLogType, Rationale: allTypes() must be updated manually (no KMP sealed-class reflection), Analytics (Module entry point), AnalyticsDestination.Main, AnalyticsLog (event record data class), AnalyticsLogCategory (sealed interface), AnalyticsLogger (singleton event store) (+33 more)

### Community 13 - "DevView Core API (Current)"
Cohesion: 0.09
Nodes (41): DevView Public API Signature (current (api.txt)), DestinationMetadata [current (api.txt)], DestinationMetadataBuilder [current (api.txt)], DestinationMetadataExtensionsKt [current (api.txt)], DevViewKt (DevView composable) [current (api.txt)], Home (implements NavKey) [current (api.txt)], Module Interface [current (api.txt)], ModuleDestinationAction [current (api.txt)] (+33 more)

### Community 14 - "Detekt Potential-Bugs Rule Set"
Cohesion: 0.05
Nodes (38): AvoidReferentialEquality Rule, CastNullableToNonNullableType Rule, CastToNullableType Rule, CharArrayToStringCall Rule, Deprecation Rule, DontDowncastCollectionTypes Rule, DoubleMutabilityForCollection Rule, ElseCaseInsteadOfExhaustiveWhen Rule (+30 more)

### Community 15 - "DiffLineUtilsTest"
Cohesion: 0.14
Nodes (3): computeLineDiff(), shouldUseInlineDiff(), DiffLineUtilsTest

### Community 16 - "OperationKey"
Cohesion: 0.14
Nodes (4): OperationKey, ByteArray, MockConfigRepositoryTest, RecordingResourceLoader

### Community 17 - "OperationMockState"
Cohesion: 0.15
Nodes (8): ApiSpec, MockConfiguration, MockMatch, Operation, Mock, Network, OperationMockState, MockTestData

### Community 18 - "NetworkMockPlugin.kt"
Cohesion: 0.07
Nodes (27): createMockHttpClientCall(), HttpClient, HttpStatusCode, MockHttpClientCall, NetworkMockPluginConfig, Exception, HttpClientCall, HttpClientConfig (+19 more)

### Community 19 - "Detekt Compose Rule Set & Modules"
Cohesion: 0.09
Nodes (28): Detekt Compose Rule Set Config, CompositionLocalAllowlist Rule, Material2 Rule, ModifierMissing Rule, ParameterNaming Rule, PreviewPublic Rule, ViewModelInjection Rule, Analytics Module (devview-analytics) (+20 more)

### Community 20 - "MockStateRepository"
Cohesion: 0.14
Nodes (8): DataStore, Preferences, NetworkMockInitializer, MockConfigRepository, Flow, Preferences, MockStateRepository, NetworkMockConfig

### Community 21 - "NetworkMockViewModel"
Cohesion: 0.21
Nodes (10): CoroutineDispatcher, MutableStateFlow, Result, ViewModelTest, NetworkMockViewModelTest, NetworkMockViewModel, collectState(), collectStates() (+2 more)

### Community 22 - "NetworkMockState"
Cohesion: 0.29
Nodes (5): NetworkMockState, HttpClient, HttpStatusCode, NetworkMockPluginTest, MockEngine

### Community 23 - "MockStateRepositoryTest"
Cohesion: 0.17
Nodes (5): DataStore, Flow, Preferences, ThrowingPreferencesDataStore, MockStateRepositoryTest

### Community 24 - "MultiplatformLibraryConventionPlugin.kt"
Cohesion: 0.09
Nodes (20): AndroidApplicationConventionPlugin, Plugin, Project, configureAndroidMultiplatformLibrary(), configureDetekt(), configureJava(), java(), configureKotlinCompiler() (+12 more)

### Community 25 - "NetworkMock DataStore & Matcher API History"
Cohesion: 0.09
Nodes (26): NetworkMockDataStoreDelegateKt (api 0.1.1), RequestMatcher (api 0.1.1), NetworkMockDataStoreDelegateKt (api 0.1.2), RequestMatcher (api 0.1.2), NetworkMockDataStoreDelegateKt (api 0.1.3), RequestMatcher (api 0.1.3), NetworkMockDataStoreDelegateKt (api 0.1.4), RequestMatcher (api 0.1.4) (+18 more)

### Community 26 - "EndpointMockState (api 0.1.2)"
Cohesion: 0.14
Nodes (26): EndpointMockState (api 0.1.1), EndpointMockState.Mock (api 0.1.1), EndpointMockState.Network (api 0.1.1), MockStateRepository (api 0.1.1), NetworkMockState (api 0.1.1), EndpointMockState (api 0.1.2), EndpointMockState.Mock (api 0.1.2), EndpointMockState.Network (api 0.1.2) (+18 more)

### Community 27 - "MockConfigRepository (api 0.1.3)"
Cohesion: 0.16
Nodes (24): EndpointDescriptor (api 0.1.3), EndpointKey (api 0.1.3), MockConfigRepository (api 0.1.3), MockMatch (api 0.1.3), MockResponse (api 0.1.3), NetworkMockInitializer (api 0.1.3), NetworkMockResourceLoader (api 0.1.3), EndpointDescriptor (api 0.1.4) (+16 more)

### Community 28 - "Detekt Naming Rule Set"
Cohesion: 0.09
Nodes (23): BooleanPropertyNaming Rule, ClassNaming Rule, ConstructorParameterNaming Rule, EnumNaming Rule, ForbiddenClassName Rule, FunctionNameMaxLength Rule, FunctionNameMinLength Rule, FunctionParameterNaming Rule (+15 more)

### Community 29 - "FakePreferencesDataStore"
Cohesion: 0.22
Nodes (10): FeatureFlipScreenTest, FeatureFlipScreen(), waitUntilTagCount(), waitUntilTagExists(), waitUntilTagGone(), FakePreferencesDataStore, DataStore, Flow (+2 more)

### Community 30 - "Code Style & Pre-commit Hooks"
Cohesion: 0.11
Nodes (23): Code of Conduct, Compose List Keys Rule Rationale, Detekt, Code Style Guide, ktlint, LazyColumn/LazyRow key Argument, Pre-commit Hook (code style enforcement), Android Studio Ladybug (+15 more)

### Community 31 - "Detekt Complexity Rule Set"
Cohesion: 0.10
Nodes (21): CognitiveComplexMethod Rule, ComplexCondition Rule, ComplexInterface Rule, Complexity Rule Set, Jetpack Compose @Composable Annotation, CyclomaticComplexMethod Rule, **/DependencyInjection.kt File Pattern (Detekt Exclude), DocumentationOverPrivateFunction Rule (+13 more)

### Community 32 - "FeatureHandler"
Cohesion: 0.22
Nodes (7): FeatureHandler, Feature, Flow, Preferences, State, rememberFeatureHandler(), FeatureHandlerTest

### Community 33 - "MockResponseDiffColors"
Cohesion: 0.22
Nodes (3): Color, MockResponseDiffColors, MockResponseDiffDefaults

### Community 34 - "NetworkMockEndpointViewModel"
Cohesion: 0.29
Nodes (7): MutableStateFlow, Result, ViewModelTest, NetworkMockEndpointViewModelTest, StateFlow, ViewModel, NetworkMockEndpointViewModel

### Community 35 - "Module"
Cohesion: 0.31
Nodes (9): Color, Dp, ImageVector, KClass, NavKey, PersistentMap, Module, previewModule() (+1 more)

### Community 36 - "DevView Utils DataStore API History"
Cohesion: 0.18
Nodes (20): CreateDataStore_androidKt (v0.1.1), CreateDataStore_iosKt (v0.1.1), CreateDataStoreKt (v0.1.1), DataStoreDelegate (v0.1.1), RequiresDataStore (v0.1.1), CreateDataStore_androidKt (v0.1.2), CreateDataStore_iosKt (v0.1.2), CreateDataStoreKt (v0.1.2) (+12 more)

### Community 38 - "Detekt Comments Rule Set"
Cohesion: 0.13
Nodes (18): AbsentOrWrongFileLicense Rule, Comments Rule Set, DeprecatedBlockTag Rule, DocumentationOverPrivateProperty Rule, EndOfSentenceFormat Rule, InstanceOfCheckForException Rule, KDocReferencesNonPublicProperty Rule, KMP Test Source-Set Exclusion Glob Pattern (+10 more)

### Community 39 - "EndpointConfig (api 0.1.2)"
Cohesion: 0.18
Nodes (22): ApiGroupConfig (api 0.1.1), EndpointConfig (api 0.1.1), EndpointDefinition (api 0.1.1), EndpointOverride (api 0.1.1), EnvironmentConfig (api 0.1.1), MockConfiguration (api 0.1.1), MockConfigurationKt (effectiveEndpoints) (api 0.1.1), ApiGroupConfig (api 0.1.2) (+14 more)

### Community 40 - "Analytics"
Cohesion: 0.11
Nodes (13): Analytics, AnalyticsDestination, Dp, KClass, Module, NavKey, PersistentMap, Main (+5 more)

### Community 41 - "Feature Handler API"
Cohesion: 0.18
Nodes (17): FeatureHandler.addFeatures, FeatureHandler class, FeatureHandler.isFeatureEnabled, FeatureHandler.isFeatureEnabledFlow, rememberFeatureHandler (FeatureHandlerKt), FeatureHandler.addFeatures, FeatureHandler class, Rationale: featureRegistry keyed by Feature instance but looked up by stable name (+9 more)

### Community 42 - "OpenApiDocument.kt"
Cohesion: 0.09
Nodes (21): ByteArray, NetworkMockResourceLoader, ComponentsObject, DevViewExtension, ExampleObject, InfoObject, MediaTypeObject, OpenApiDocument (+13 more)

### Community 43 - "EndpointConfig (api 0.1.4)"
Cohesion: 0.28
Nodes (15): ApiGroupConfig (api 0.1.4), EndpointConfig (api 0.1.4), EndpointDefinition (api 0.1.4), EndpointOverride (api 0.1.4), EnvironmentConfig (api 0.1.4), MockConfigurationKt (effectiveEndpoints) (api 0.1.4), ApiGroupConfig (api current), EndpointConfig (api current) (+7 more)

### Community 44 - "NetworkMock Core & Diff Pipeline"
Cohesion: 0.17
Nodes (16): NetworkMockInitializer (devview-networkmock-core), NetworkMockResourceLoader (devview-networkmock-core), MockConfigRepository (devview-networkmock-core), MockStateRepository (devview-networkmock-core), NetworkMock Legacy Constructor (pre-0.1.3, Function2 resourceLoader), NetworkMock, NetworkMockDestination, NetworkMockEndpointViewModel (+8 more)

### Community 45 - "Detekt Empty-Blocks Rule Set"
Cohesion: 0.12
Nodes (16): Empty Blocks Rule Set, EmptyCatchBlock Rule, EmptyClassBlock Rule, EmptyDefaultConstructor Rule, EmptyDoWhileBlock Rule, EmptyElseBlock Rule, EmptyFinallyBlock Rule, EmptyForBlock Rule (+8 more)

### Community 46 - "Renovate Dependency Config"
Cohesion: 0.12
Nodes (15): * * * * 0,6, * 22-23,0-4 * * *, config:recommended, group:all, mergeConfidence:all-badges, commitMessagePrefix, customManagers, extends (+7 more)

### Community 47 - "Highlighted Analytics Log Cards"
Cohesion: 0.27
Nodes (12): HighlightedAnalyticsLogCard(), HighlightedAnalyticsLogCardPreview(), Modifier, HighlightedAnalyticsLogsHeader(), HighlightedAnalyticsLogsHeaderPreview(), Modifier, PersistentList, HighlightedAnalyticsLog (+4 more)

### Community 48 - "DevView Core API v0.1.1"
Cohesion: 0.23
Nodes (15): DevView Public API Signature (0.1.1), DestinationMetadata [0.1.1], DestinationMetadataBuilder [0.1.1], DestinationMetadataExtensionsKt [0.1.1], DevViewKt (DevView composable) [0.1.1], Home (implements NavKey) [0.1.1], Module Interface [0.1.1], ModuleDestinationAction [0.1.1] (+7 more)

### Community 49 - "DevView Core API v0.1.2"
Cohesion: 0.23
Nodes (15): DevView Public API Signature (0.1.2), DestinationMetadata [0.1.2], DestinationMetadataBuilder [0.1.2], DestinationMetadataExtensionsKt [0.1.2], DevViewKt (DevView composable) [0.1.2], Home (implements NavKey) [0.1.2], Module Interface [0.1.2], ModuleDestinationAction [0.1.2] (+7 more)

### Community 50 - "DevView Core API v0.1.3"
Cohesion: 0.23
Nodes (15): DevView Public API Signature (0.1.3), DestinationMetadata [0.1.3], DestinationMetadataBuilder [0.1.3], DestinationMetadataExtensionsKt [0.1.3], DevViewKt (DevView composable) [0.1.3], Home (implements NavKey) [0.1.3], Module Interface [0.1.3], ModuleDestinationAction [0.1.3] (+7 more)

### Community 51 - "DevView Core API v0.1.4"
Cohesion: 0.23
Nodes (15): DevView Public API Signature (0.1.4), DestinationMetadata [0.1.4], DestinationMetadataBuilder [0.1.4], DestinationMetadataExtensionsKt [0.1.4], DevViewKt (DevView composable) [0.1.4], Home (implements NavKey) [0.1.4], Module Interface [0.1.4], ModuleDestinationAction [0.1.4] (+7 more)

### Community 52 - "DisplayLine"
Cohesion: 0.21
Nodes (10): Collapsed, DisplayLine, Left, Right, Unchanged, PersistentList, lcsLength(), lcsTable() (+2 more)

### Community 53 - "DevView Configuration Guide"
Cohesion: 0.16
Nodes (15): Build Errors (Issue), Conditional Modules, Configuration, Debug Menu Button, DevView Not Appearing (Issue), Feature Flags Not Working (Issue), FeatureFlip Module, Gesture Detection (Opening DevView) (+7 more)

### Community 54 - "Detekt Global Config & Coroutines Rules"
Cohesion: 0.14
Nodes (14): CoroutineLaunchedInTestWithoutRunTest Rule, Coroutines Rule Set, Detekt Default Config (default-config.yml), Detekt Global Config Settings, Detekt Processors Settings, DetektProgressListener Processor (excluded), GlobalCoroutineUsage Rule, InjectDispatcher Rule (+6 more)

### Community 55 - "AnalyticsScreen"
Cohesion: 0.29
Nodes (6): AnalyticsScreenTest, AnalyticsLogScreenPreview(), AnalyticsScreen(), Dp, Modifier, PersistentList

### Community 56 - "MockConfigRepository (api 0.1.2)"
Cohesion: 0.30
Nodes (12): EndpointDescriptor (api 0.1.1), EndpointKey (api 0.1.1), MockConfigRepository (api 0.1.1), MockMatch (api 0.1.1), MockResponse (api 0.1.1), NetworkMockInitializer (api 0.1.1), EndpointDescriptor (api 0.1.2), EndpointKey (api 0.1.2) (+4 more)

### Community 58 - "TestModule"
Cohesion: 0.09
Nodes (25): androidx, KClass, Module, NavKey, PersistentMap, ModuleRegistryUiTest, TrackingModule, UiTestDestination (+17 more)

### Community 59 - "FeatureFlipScreen.kt"
Cohesion: 0.18
Nodes (9): FeatureFilter, LOCAL, OFF, ON, REMOTE, FeaturesScreenPreview(), Dp, Feature (+1 more)

### Community 60 - "Graphify Skill Documentation"
Cohesion: 0.08
Nodes (24): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+16 more)

### Community 61 - "Detekt Exceptions Rule Set"
Cohesion: 0.15
Nodes (13): ErrorUsageWithThrowable Rule, ExceptionRaisedInUnexpectedLocation Rule, Exceptions Rule Set, NotImplementedDeclaration Rule, ObjectExtendsThrowable Rule, PrintStackTrace Rule, RethrowCaughtException Rule, ReturnFromFinally Rule (+5 more)

### Community 62 - "Feature Sealed Type Model"
Cohesion: 0.21
Nodes (13): Feature sealed interface, Feature.Companion, Feature.LocalFeature, Feature.RemoteFeature, DevView FeatureFlip Module (CLAUDE.md overview), Feature (sealed class), Feature.LocalFeature, Feature.RemoteFeature (+5 more)

### Community 63 - "NetworkMock.kt"
Cohesion: 0.12
Nodes (16): Endpoint, Dp, KClass, Module, NavKey, PersistentMap, Main, NetworkMock (+8 more)

### Community 64 - "MockResponse"
Cohesion: 0.11
Nodes (7): OperationDescriptor, MockResponse, Result, MockScreenTestData, ApiSpecUiModel, StateFlow, ViewModel

### Community 65 - "iOS App Entry Point"
Cohesion: 0.15
Nodes (13): App, ComposeView, ContentView, .body, Context, UIViewController, iOSApp, .body (+5 more)

### Community 66 - "TimeCapsule Module Design"
Cohesion: 0.24
Nodes (12): Module (devview core interface), TimeCapsule, TimeCapsuleEffect, TimeCapsuleOwner, devview-timecapsule Module Documentation, Dedup/Replay Is StateFlow's Job, Not ScreenCapsule's, No CompositionLocal — Single Consumer Doesn't Justify Indirection, Recorded<S> (internal entry model) (+4 more)

### Community 67 - "FeatureFlip Module API History"
Cohesion: 0.21
Nodes (12): FeatureFlip Public API Surface v0.1.1, FeatureFlip Public API Surface v0.1.2, FeatureFlip Public API Surface v0.1.3, FeatureFlip Public API Surface v0.1.4, FeatureFlip Public API Surface (current), FeatureFlipDestination sealed interface, FeatureFlipDestination.Main, FeatureType enum (+4 more)

### Community 68 - "NetworkMock Ktor Plugin API History"
Cohesion: 0.32
Nodes (12): NetworkMockConfig (v0.1.1), NetworkMockPlugin (v0.1.1), NetworkMockPluginConfig (v0.1.1), NetworkMockConfig (v0.1.2), NetworkMockPlugin (v0.1.2), NetworkMockPluginConfig (v0.1.2), NetworkMockConfig (v0.1.3), NetworkMockPlugin (v0.1.3) (+4 more)

### Community 70 - "TestModule"
Cohesion: 0.19
Nodes (8): HomeScreenTest, ModuleItemUiTest, Dp, KClass, Module, NavKey, PersistentMap, TestModule

### Community 71 - "DevView Test Utilities Module"
Cohesion: 0.21
Nodes (12): devview-test Module Documentation, ComposeUiTestWait.kt extensions (waitUntilTagCount/Exists/Gone), FakePreferencesDataStore, assertEmitsExactly (FlowAssertions.kt), collectState/collectStates (StateFlowCollectors.kt), TestDispatchers / testDispatchers() / runTestWithDispatchers, ViewModelTest, ViewModelTest Uses Unconfined, Not Main, Dispatcher (+4 more)

### Community 73 - "Getting Started Overview"
Cohesion: 0.18
Nodes (12): Analytics Module, App Composable, DevView Composable, FeatureFlip Module, GitHub Discussions, GitHub Issues, Getting Started with DevView, Early Integration Tip (+4 more)

### Community 74 - "NetworkMock Ktor/Core Shared State"
Cohesion: 0.18
Nodes (11): MockResponse (devview-networkmock-core), NetworkMockDataStoreDelegate (devview-networkmock-core), Shared DataStore Singleton Across NetworkMock UI and Ktor Plugin, MockHttpClientCall, NetworkMockPlugin, NetworkMockPluginConfig, devview-networkmock-ktor Module Documentation, Why MockHttpClientCall Is Public (Ktor Internals Constraint) (+3 more)

### Community 75 - "ScreenCapsule"
Cohesion: 0.05
Nodes (34): For --cluster-only, For --update (incremental re-extraction), graphify reference: incremental update and cluster-only, formatDelta(), Modifier, TimeCapsuleRow(), S, Recorded (+26 more)

### Community 76 - "DestinationMetadata"
Cohesion: 0.26
Nodes (10): DestinationMetadata, DestinationMetadataBuilder, ImageVector, PersistentList, asDestination(), KClass, NavKey, withActions() (+2 more)

### Community 78 - "DevView Utils DataStore Module"
Cohesion: 0.25
Nodes (11): BooleanPreviewParameterProvider, createDataStore, DataStoreDelegate, rememberDataStore (Android actual), rememberDataStore (iOS actual), RequiresDataStore, devview-utils Module Documentation, @Suppress("ComposableNaming") on init/initDataStore (+3 more)

### Community 79 - "Release Publishing Process"
Cohesion: 0.22
Nodes (11): CHANGELOG.md, --no-configuration-cache Rationale, Publishing a Release Guide, GitHub Repository Secrets (Publishing), GPG Artifact Signing, gradle.properties (VERSION_NAME, POM_*), com.worldline Namespace Verification, publish.yml GitHub Actions Workflow (+3 more)

### Community 80 - "Section"
Cohesion: 0.12
Nodes (16): FeatureFlipModuleTest, Section, CUSTOM, FEATURES, LOGGING, NETWORK, SETTINGS, Detail (+8 more)

### Community 81 - "add-konsist-rule Skill"
Cohesion: 0.27
Nodes (12): architecture-reviewer Agent, add-konsist-rule Skill, local-ci Skill, verify-module Skill, Detekt Static Analysis, Konsist Architecture Enforcement, config/gitleaks/.gitleaks.toml, config/quality/detekt/default-config.yml (+4 more)

### Community 82 - "update-docs Skill"
Cohesion: 0.14
Nodes (20): kmp-advisor Agent, module-expert Agent, add-destination Skill, add-module Skill, update-docs Skill, ComposeUIViewController iOS Embedding Pattern, Module Interface Contract, NavKey Destination Pattern (+12 more)

### Community 83 - "FeatureFlip.kt"
Cohesion: 0.33
Nodes (8): FeatureFlip, FeatureFlipDestination, Dp, KClass, Module, NavKey, PersistentMap, Main

### Community 84 - "FeatureFlip Screen & Filters"
Cohesion: 0.22
Nodes (10): FeatureFlipScreen (FeatureFlipScreenKt), LocalFeatureHandler (FeatureHandlerKt), FeatureFilter (adaptive filter chips), Rationale: FeatureFilter hides LOCAL/REMOTE chips when all features share one type, Rationale: filter chips use OR within a dimension, AND across dimensions, FeatureFlipScreen composable, LocalFeatureHandler CompositionLocal, FeatureFlipScreen composable (README variant with onStateChange) (+2 more)

### Community 85 - "Feature Tri-State Switch UI"
Cohesion: 0.20
Nodes (10): FeatureState enum, FeatureState.Companion.fromOrdinal, FeatureState enum, Rationale: FeatureState ordinals are load-bearing (persisted as raw ints; reordering breaks data), FeatureTriStateSwitch composable, Rationale: FeatureTriStateSwitch segment order mirrors FeatureState ordinal order, SegmentedButtonContentMeasurePolicy, Rationale: hand-rolled because Material3 lacks icon-only segmented buttons with per-segment container colors (+2 more)

### Community 86 - "HomeScreen.kt"
Cohesion: 0.18
Nodes (15): Home, HomeScreen(), HomeScreenPreview(), Modifier, Module, Modifier, Module, ModuleItem() (+7 more)

### Community 87 - "DiffLine"
Cohesion: 0.29
Nodes (4): Different, DiffLine, Unchanged, PersistentList

### Community 88 - "createDataStore.kt"
Cohesion: 0.60
Nodes (4): createDataStore(), DataStore, Preferences, rememberDataStore()

### Community 89 - "TimeRange"
Cohesion: 0.40
Nodes (5): TimeRange, All, Last15Min, Last30Min, Last5Min

### Community 90 - "Gradle Module Setup Guide"
Cohesion: 0.31
Nodes (10): build.gradle.kts (Shared Module), devview-analytics Module, devview (Core Module), devview-featureflip Module, devview-networkmock Module, Gradle Setup, gradle/libs.versions.toml, Module Selection Table (+2 more)

### Community 91 - "Quick Start Guide"
Cohesion: 0.24
Nodes (10): Analytics Module, Complete Example, DevView Composable, FeatureFlip Module, NetworkMock Module, Quick Start Guide, rememberModules, Step 1: Create Your App Structure (+2 more)

### Community 92 - "Docs Site Branding Theme JS"
Cohesion: 0.44
Nodes (9): applyBrandingTheme(), getAssetPrefix(), init(), isDarkScheme(), observeColorScheme(), observePageChanges(), scheduleApplyBrandingTheme(), setHeaderLogo() (+1 more)

### Community 93 - "Release Script (release.py)"
Cohesion: 0.49
Nodes (9): Path, extract_changelog_section(), find_published_api_files(), get_property(), main(), run(), set_property(), update_changelog() (+1 more)

### Community 94 - "Graphify Exports Documentation"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 95 - "MockResponseDiffContent.kt"
Cohesion: 0.51
Nodes (9): DiffLineRow(), InlineDiffContent(), Color, Modifier, PersistentList, LegendDot(), MockResponseDiffContentPreview(), ResponseContentPane() (+1 more)

### Community 96 - "FeatureFlip Module Overview"
Cohesion: 0.25
Nodes (9): FeatureFlip class, com.worldline.devview.core.Module (external), com.worldline.devview.utils.RequiresDataStore (external), feature_flip_datastore.preferences_pb file, devview-utils module, FeatureFlip module object, rememberModules DSL, RequiresDataStore interface (+1 more)

### Community 98 - "NetworkMock Destination API History"
Cohesion: 0.33
Nodes (9): NetworkMockDestination (v0.1.2), NetworkMockDestination.Endpoint (v0.1.2), NetworkMockDestination.Main (v0.1.2), NetworkMockDestination (v0.1.3), NetworkMockDestination.Endpoint (v0.1.3), NetworkMockDestination.Main (v0.1.3), NetworkMockDestination (v0.1.4), NetworkMockDestination.Endpoint (v0.1.4) (+1 more)

### Community 99 - "StatusCodeFamily"
Cohesion: 0.22
Nodes (7): StatusCodeFamily, CLIENT_ERROR, INFORMATIONAL, REDIRECTION, SERVER_ERROR, SUCCESSFUL, UNKNOWN

### Community 101 - "DevViewTest.kt"
Cohesion: 0.13
Nodes (18): DevViewDestination, DevViewModule, DevViewTest, NavigationEventHandler, Dp, KClass, Module, NavigationEventHandler (+10 more)

### Community 103 - "ViewModelTest"
Cohesion: 0.31
Nodes (5): ViewModelTest, runTestWithDispatchers(), TestDispatchers, TestCoroutineScheduler, TestResult

### Community 104 - "Branding Assets Overview"
Cohesion: 0.39
Nodes (9): devview-icon-dark.svg, devview-icon-light.svg, devview-icon-mono.svg, devview-logo-dark.svg, devview-logo-light.svg, devview-logo-mono.svg, Branding Assets README, Documentation Site (+1 more)

### Community 105 - "Platform"
Cohesion: 0.24
Nodes (7): AndroidPlatform, getPlatform(), Greeting, getPlatform(), Platform, getPlatform(), IOSPlatform

### Community 106 - "Detekt Performance Rule Set"
Cohesion: 0.25
Nodes (8): ArrayPrimitive Rule, CouldBeSequence Rule, ForEachOnRange Rule, Performance Rule Set, SpreadOperator Rule, UnnecessaryPartOfBinaryExpression Rule, UnnecessaryTemporaryInstantiation Rule, UnnecessaryTypeCasting Rule

### Community 107 - "PaddingValues"
Cohesion: 0.70
Nodes (4): Dp, Modifier, TimeCapsuleScreen(), PaddingValues

### Community 110 - "Troubleshooting & Examples Overview"
Cohesion: 0.29
Nodes (8): Examples Overview, DevView GitHub Repository, Compose Multiplatform Target Compatibility, Where To Find More Examples, Localisation Support, Troubleshooting & FAQ, Best Practices Guide, Common Pitfalls Guide

### Community 111 - "Git Workflow Conventions"
Cohesion: 0.38
Nodes (6): CLAUDE.md Project Guidance, DevView Module Dependency Graph, Gitmoji Commit Convention, Semantic PR Title Convention, PR Hygiene Workflow, scripts/release.sh

### Community 112 - "Detekt Libraries Rule Set"
Cohesion: 0.38
Nodes (7): Detekt (static analysis tool), Ktlint Ruleset Config (android_studio style, maxLineLength=120), ktlint (Kotlin linter), ForbiddenPublicDataClass rule (inactive, ignores *.internal), Libraries Ruleset Config (Detekt), LibraryCodeMustSpecifyReturnType rule (active, allowOmitUnit=false), LibraryEntitiesShouldNotBePublic rule (inactive)

### Community 115 - "Detekt Console Reports Settings"
Cohesion: 0.33
Nodes (6): ComplexityReport Console Report (excluded), Detekt Console Reports Settings, FileBasedIssuesReport Console Report (excluded), IssuesReport Console Report (excluded), NotificationReport Console Report (excluded), ProjectStatisticsReport Console Report (excluded)

### Community 120 - "Android DataStore Creation"
Cohesion: 0.73
Nodes (5): createDataStore(), Context, DataStore, Preferences, rememberDataStore()

### Community 121 - "NetworkMockEndpointScreen.kt"
Cohesion: 0.29
Nodes (11): Dp, Modifier, NetworkMockEndpointScreen(), NetworkMockEndpointScreenContent(), NetworkMockEndpointScreenPreview(), Content, EndpointLoadingState, Error (+3 more)

### Community 122 - "Installation & Setup Guide"
Cohesion: 0.33
Nodes (6): Installation, Prerequisites (Installation Recap), Sync Your Project, Installation Verification, How Do I Add A Custom Module?, Creating Custom Modules Guide

### Community 123 - "CI Publish & Release Workflows"
Cohesion: 0.40
Nodes (6): GitHub Release Notes Categorization Config, Publish Workflow, Publish Docs Workflow, Release Comment Workflow, gradle.properties, scripts/build_docs.sh

### Community 125 - "Test Writing Conventions"
Cohesion: 0.70
Nodes (5): test-writer Agent, write-tests Skill, Kotest Infix Assertion Convention, Test Source Set Selection Convention, ViewModelTest Base Class Convention

### Community 127 - "FeatureType"
Cohesion: 0.40
Nodes (3): FeatureType, LOCAL, REMOTE

### Community 129 - "StatusCodeFamily API History"
Cohesion: 0.40
Nodes (5): StatusCodeFamily (api 0.1.1), StatusCodeFamily (api 0.1.2), StatusCodeFamily (api 0.1.3), StatusCodeFamily (api 0.1.4), StatusCodeFamily (api current)

### Community 130 - "iOS DataStore Creation"
Cohesion: 0.90
Nodes (4): createDataStore(), DataStore, Preferences, rememberDataStore()

### Community 131 - "Prerequisites Overview"
Cohesion: 0.40
Nodes (5): Prerequisites (Overview Summary), Development Tools, Minimum Supported Versions, Prerequisites, Project Requirements

### Community 132 - "DataStore Convention Plugin"
Cohesion: 0.70
Nodes (3): DatastoreConventionPlugin, Plugin, Project

### Community 133 - "Device Test Convention Plugin"
Cohesion: 0.70
Nodes (3): DeviceTestConventionPlugin, Plugin, Project

### Community 134 - "Konsist Convention Plugin"
Cohesion: 0.70
Nodes (3): KonsistConventionPlugin, Plugin, Project

### Community 135 - "Kover Convention Plugin"
Cohesion: 0.70
Nodes (3): KoverConventionPlugin, Plugin, Project

### Community 136 - "Ktor Convention Plugin"
Cohesion: 0.70
Nodes (3): Plugin, Project, KtorConventionPlugin

### Community 137 - "Metalava Convention Plugin"
Cohesion: 0.70
Nodes (3): Plugin, Project, MetalavaConventionPlugin

### Community 138 - "Room Convention Plugin"
Cohesion: 0.70
Nodes (3): Plugin, Project, RoomConventionPlugin

### Community 139 - "Unit Test Convention Plugin"
Cohesion: 0.70
Nodes (3): Plugin, Project, UnitTestConventionPlugin

### Community 144 - "MockHttpClientCall API History"
Cohesion: 0.50
Nodes (4): MockHttpClientCall (v0.1.1), MockHttpClientCall (v0.1.2), MockHttpClientCall (v0.1.3), MockHttpClientCall (v0.1.4)

### Community 146 - "Boolean Preview Provider API History"
Cohesion: 0.50
Nodes (4): BooleanPreviewParameterProvider (v0.1.1), BooleanPreviewParameterProvider (v0.1.2), BooleanPreviewParameterProvider (v0.1.3), BooleanPreviewParameterProvider (v0.1.4)

### Community 148 - "Gradle Wrapper Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 149 - "NetworkMock Endpoint UI Model"
Cohesion: 0.67
Nodes (3): EndpointDescriptor (devview-networkmock-core), EndpointMockState (devview-networkmock-core), EndpointUiModel

### Community 150 - "NetworkMock Class API History"
Cohesion: 0.67
Nodes (3): NetworkMock (v0.1.2), NetworkMock (v0.1.3), NetworkMock (v0.1.4)

### Community 154 - "GitHub Issue Templates"
Cohesion: 0.67
Nodes (3): Bug Report Issue Template, Issue Template Config, Feature Request Issue Template

### Community 213 - "Graphify Query/Path/Explain Docs"
Cohesion: 0.33
Nodes (5): For /graphify explain, For /graphify path, graphify reference: query, path, explain, Step 0 — Constrained query expansion (REQUIRED before traversal), Step 1 — Traversal

### Community 214 - "Graphify Add/Watch Docs"
Cohesion: 0.50
Nodes (3): For /graphify add, For --watch, graphify reference: add a URL and watch a folder

### Community 215 - "Graphify Commit Hook Docs"
Cohesion: 0.50
Nodes (3): For git commit hook, For native CLAUDE.md integration, graphify reference: commit hook and native CLAUDE.md integration

## Ambiguous Edges - Review These
- `CounterScreen.kt (sample)` → `TimeCapsule Module Doc`  [AMBIGUOUS]
  docs/modules/timecapsule.md · relation: references
- `FeatureHandler class` → `FeatureHandler.getFeatures()`  [AMBIGUOUS]
  devview-featureflip/api/api.txt · relation: semantically_similar_to
- `FeatureHandler.isFeatureEnabled` → `FeatureHandler.isFeatureEnabled (README variant, used as Flow)`  [AMBIGUOUS]
  devview-featureflip/api/api.txt · relation: semantically_similar_to
- `FeatureHandler.isFeatureEnabled` → `FeatureHandler.isFeatureEnabled (README variant, used as Flow)`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `setFeatureState (internal)` → `FeatureHandler.setFeatureState (README shows as publicly callable)`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `FeatureFlipScreen composable` → `FeatureFlipScreen composable (README variant with onStateChange)`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `LocalFeatureHandler CompositionLocal` → `LocalFeatures CompositionLocal`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `createDataStore(producePath)` → `devview-utils module`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: references

## Knowledge Gaps
- **528 isolated node(s):** `Main`, `All`, `Last5Min`, `Last15Min`, `Last30Min` (+523 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **50 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `CounterScreen.kt (sample)` and `TimeCapsule Module Doc`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `FeatureHandler class` and `FeatureHandler.getFeatures()`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `FeatureHandler.isFeatureEnabled` and `FeatureHandler.isFeatureEnabled (README variant, used as Flow)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `FeatureHandler.isFeatureEnabled` and `FeatureHandler.isFeatureEnabled (README variant, used as Flow)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `setFeatureState (internal)` and `FeatureHandler.setFeatureState (README shows as publicly callable)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `FeatureFlipScreen composable` and `FeatureFlipScreen composable (README variant with onStateChange)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `LocalFeatureHandler CompositionLocal` and `LocalFeatures CompositionLocal`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._