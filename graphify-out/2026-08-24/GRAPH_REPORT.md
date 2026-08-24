# Graph Report - devview  (2026-08-24)

## Corpus Check
- 1 files · ~0 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2522 nodes · 3923 edges · 212 communities (167 shown, 45 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 557 edges (avg confidence: 0.86)
- Token cost: 0 input · 79,992 output

## Community Hubs (Navigation)
- Detekt Style Rule Set (Full)
- Analytics Log Categories & Sample App
- Developer Guides & Navigation
- Module Overview & Examples
- FeatureFlip UI Animation Tests
- NetworkMock Global Toggle Tests
- NetworkMock Request Matcher Tests
- Mock Response Diff Colors
- Endpoint Card Tests
- NetworkMock Item & Error State UI
- Analytics Module API History
- NetworkMock Endpoint UI API History
- Analytics Category Conventions
- DevView Core API (Current)
- Detekt Potential-Bugs Rule Set
- Mock Response Diff Lines
- NetworkMock Resource Loader & Config Tests
- Mock Configuration Model
- NetworkMock Ktor Plugin
- Detekt Compose Rule Set & Modules
- Mock Response & Initializer
- NetworkMock ViewModel Tests
- NetworkMock Plugin State Tests
- Mock State Repository Tests
- Android Build Convention Plugins
- NetworkMock DataStore & Matcher API History
- NetworkMock State API v0.1.1-0.1.2
- NetworkMock Endpoint API v0.1.3-0.1.4
- Detekt Naming Rule Set
- FeatureFlip Screen Tests
- Code Style & Conduct Guides
- Detekt Complexity Rule Set
- Analytics Logger Core
- Mock State Repository (Endpoint)
- NetworkMock Endpoint ViewModel Tests
- Module Interface Definition
- DevView Utils DataStore API History
- Konsist Architecture Tests
- Detekt Comments Rule Set
- NetworkMock ApiGroupConfig API v0.1.1-0.1.2
- Analytics Module Registration
- Feature Handler API
- Feature Handler Implementation
- NetworkMock Config API v0.1.3-0.1.4
- NetworkMock Core & Legacy API
- Detekt Empty-Blocks Rule Set
- Renovate Dependency Config
- Highlighted Analytics Log Cards
- DevView Core API v0.1.1
- DevView Core API v0.1.2
- DevView Core API v0.1.3
- DevView Core API v0.1.4
- Mock File Name Parsing Tests
- DevView Configuration Guide
- Detekt Global Config & Coroutines Rules
- Analytics Screen Tests
- NetworkMock Core Config API v0.1.1-0.1.2
- Preview Sheet State Tests
- Module Registry UI Tests
- Module Destination Action
- Module Registry Implementation
- Detekt Exceptions Rule Set
- Feature Sealed Type Model
- FeatureFlip Module Registration
- Module Registry Tests
- Sample Android App Entry Point
- TimeCapsule Module Design
- FeatureFlip Module API History
- NetworkMock Ktor Plugin API History
- NetworkMock Module Registration
- Module Item UI Tests
- DevView Test Utilities Module
- DataStore Delegate UI Tests
- Getting Started Overview
- NetworkMock Ktor/Core Shared State
- DevView Navigation Tests
- Destination Metadata Builder
- ScreenCapsule Tests
- DevView Utils DataStore Module
- Release Publishing Process
- Sample Test Module
- CI & Architecture Tooling
- KMP & Module Dev Agents
- Module Scaffolding Skills
- FeatureFlip Screen & Filters
- Feature Tri-State Switch UI
- Home Screen & Tests
- TimeCapsule Row & Screen UI
- TimeCapsule Module Registration
- TimeCapsule Registry Tests
- Gradle Module Setup Guide
- Quick Start Guide
- Docs Site Branding Theme JS
- Release Script (release.py)
- Sample App API & Entry Point
- Sample Counter Screen
- FeatureFlip Module Overview
- FeatureFlip Screen Filter Logic
- NetworkMock Destination API History
- HTTP Status Code Family
- Mock Response Tests
- DevView Module Registration Tests
- Module Item UI Component
- Test ViewModel & Dispatchers
- Branding Assets Overview
- Platform Detection (expect/actual)
- Detekt Performance Rule Set
- Analytics Log Item & Chip UI
- TimeCapsule Effect & Owner
- Boolean Preview Provider Tests
- Troubleshooting & Examples Overview
- Git Workflow Conventions
- Detekt Libraries Rule Set
- FeatureState Enum
- ScreenCapsule Recording Model
- Detekt Console Reports Settings
- Analytics Logger Tests
- Request Matcher Implementation
- Mock Screen Test Data
- DevView Overlay Navigation
- Android DataStore Creation
- Common DataStore Creation
- Installation & Setup Guide
- CI Publish & Release Workflows
- Sample HttpClient Mocking Setup
- Test Writing Conventions
- Analytics Time Range Filter
- FeatureType Enum
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
- FeatureFlip Module Metadata Tests
- FeatureState Ordinal Tests
- FeatureType Ordinal Tests
- StatusCodeFamily Tests
- MockHttpClientCall API History
- Ktor Plugin Test Data
- Boolean Preview Provider API History
- Project Build Type Enum
- Gradle Wrapper Script
- NetworkMock Endpoint UI Model
- NetworkMock Class API History
- NetworkMock Loading State UI
- HasTitle Navigation Interface
- Flow Test Assertions
- GitHub Issue Templates
- Sample Compose App Test
- NetworkMock Endpoint Destination
- Detekt Dagger-Related Rule
- Poko Annotation Alias Rationale
- Poko Annotation Alias
- TimeCapsule Row Delta Time
- Build Versions Constants
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

## God Nodes (most connected - your core abstractions)
1. `Style Rule Set` - 95 edges
2. `AnalyticsLogCategory` - 51 edges
3. `RequestMatcherTest` - 48 edges
4. `AnalyticsLogType` - 41 edges
5. `Potential Bugs Rule Set` - 40 edges
6. `NetworkMockState` - 39 edges
7. `EndpointKey` - 25 edges
8. `MockConfigRepositoryTest` - 25 edges
9. `FeatureHandler` - 23 edges
10. `Naming Rule Set` - 22 edges

## Surprising Connections (you probably didn't know these)
- `NetworkMockDataStoreDelegate (devview-networkmock-core)` --semantically_similar_to--> `TimeCapsule`  [INFERRED] [semantically similar]
  devview-networkmock/CLAUDE.md → devview-timecapsule/api/api.txt
- `Network Mock Test Files README` --semantically_similar_to--> `NetworkMock Workflows Doc`  [INFERRED] [semantically similar]
  sample/network/src/commonMain/composeResources/files/networkmocks/README.md → docs/modules/networkmock-workflows.md
- `DevViewApp()` --calls--> `Analytics`  [INFERRED]
  sample/shared/src/commonMain/kotlin/com/worldline/devview/sample/DevViewApp.kt → devview-analytics/src/commonMain/kotlin/com/worldline/devview/analytics/Analytics.kt
- `DevViewApp()` --calls--> `AnalyticsLog`  [INFERRED]
  sample/shared/src/commonMain/kotlin/com/worldline/devview/sample/DevViewApp.kt → devview-analytics/src/commonMain/kotlin/com/worldline/devview/analytics/model/AnalyticsLog.kt
- `DevViewApp()` --calls--> `rememberFeatureHandler()`  [INFERRED]
  sample/shared/src/commonMain/kotlin/com/worldline/devview/sample/DevViewApp.kt → devview-featureflip/src/commonMain/kotlin/com/worldline/devview/featureflip/model/FeatureHandler.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **CI Quality Gate Pipeline** — github_workflows_build, concept_detekt, concept_konsist, claude_skills_local_ci_skill [INFERRED 0.80]
- **DevView Module Scaffolding Workflow** — claude_skills_add_module_skill, claude_skills_add_destination_skill, claude_agents_module_expert, concept_module_interface [INFERRED 0.80]
- **Documentation Synchronization Pipeline** — claude_skills_update_docs_skill, changelog, github_workflows_publish_docs, scripts_build_docs_sh [INFERRED 0.75]
- **Shared NetworkMock DataStore Singleton Across UI and Ktor Plugin** — com_worldline_devview_networkmock_core_networkmockdatastoredelegate, devview_networkmock_api_api_networkmock, devview_networkmock_ktor_api_api_networkmockconfig [EXTRACTED 1.00]
- **Per-Screen State Recording and Restore Flow** — devview_timecapsule_api_api_timecapsuleowner, devview_timecapsule_api_api_timecapsuleeffect, devview_timecapsule_claude_screencapsule, devview_timecapsule_api_api_timecapsule [EXTRACTED 1.00]
- **Ktor Request Interception and Mock Lookup Pipeline** — devview_networkmock_ktor_api_api_networkmockplugin, com_worldline_devview_networkmock_core_repository_mockconfigrepository, com_worldline_devview_networkmock_core_repository_mockstaterepository, devview_networkmock_ktor_api_api_mockhttpclientcall [EXTRACTED 1.00]
- **Module Interface Implementations Across Feature Docs** — docs_modules_featureflip_featureflip, docs_modules_analytics_analytics, docs_modules_networkmock_networkmock, docs_modules_timecapsule_timecapsule, docs_modules_custom_modules_custom_modules [INFERRED 0.85]
- **NetworkMock Core/UI/Ktor Three-Module Split** — docs_modules_networkmock_core_networkmock_core, docs_modules_networkmock_ktor_networkmock_ktor, docs_modules_networkmock_ui_networkmock_ui [EXTRACTED 1.00]
- **Custom Module Creation Workflow (Guide, Reference Doc, Sample)** — docs_guides_module_development_module_development, docs_modules_custom_modules_custom_modules, sample_claude_testmodule [INFERRED 0.80]

## Communities (212 total, 45 thin omitted)

### Community 0 - "Detekt Style Rule Set (Full)"
Cohesion: 0.02
Nodes (92): AbstractClassCanBeConcreteClass Rule, AbstractClassCanBeInterface Rule, AlsoCouldBeApply Rule, BracesOnIfStatements Rule, BracesOnWhenStatements Rule, CanBeNonNullable Rule, CascadingCallWrapping Rule, ClassOrdering Rule (+84 more)

### Community 1 - "Analytics Log Categories & Sample App"
Cohesion: 0.08
Nodes (65): App, Action, AddToCart, AnalyticsLogCategory, AudioPlay, Checkout, Click, Comment (+57 more)

### Community 2 - "Developer Guides & Navigation"
Cohesion: 0.07
Nodes (59): Module interface, Guides Index, Integration Guide, rememberModules, Module Development Guide, Navigation Guide, NavKey, Section-Derived Default Colors (v0.1.4+) (+51 more)

### Community 3 - "Module Overview & Examples"
Cohesion: 0.05
Nodes (59): Analytics module, Advanced Examples, FeatureFlip module, Multi-Module Integration, MyAdvancedModule, NetworkMock module, NetworkMockResourceLoader, rememberModules (+51 more)

### Community 4 - "FeatureFlip UI Animation Tests"
Cohesion: 0.06
Nodes (47): Animatable, AnimationVector1D, BorderStroke, Constraints, FeatureTriStateSwitchTest, FeatureItem(), FeatureItemPreview(), Feature (+39 more)

### Community 5 - "NetworkMock Global Toggle Tests"
Cohesion: 0.06
Nodes (23): GlobalMockToggleTest, NetworkMockScreenTest, EmptyState(), Modifier, GlobalMockToggle(), GlobalMockToggleEnabledPreview(), Modifier, ContentState() (+15 more)

### Community 7 - "Mock Response Diff Colors"
Cohesion: 0.07
Nodes (27): Color, MockResponseDiffColors, MockResponseDiffDefaults, DiffLineRow(), InlineDiffContent(), Color, Modifier, PersistentList (+19 more)

### Community 8 - "Endpoint Card Tests"
Cohesion: 0.09
Nodes (15): EndpointCardTest, EndpointStateChipTest, EndpointCard(), EndpointCardPreview(), EndpointCardWithFileNamePreview(), Modifier, EndpointHeaderCard(), EndpointHeaderCardPreview() (+7 more)

### Community 9 - "NetworkMock Item & Error State UI"
Cohesion: 0.07
Nodes (29): ErrorState(), Modifier, Modifier, MockItem(), MockItemContent(), MockItemPreview(), MockItemPreviewModePreview(), MockItemSelectedPreview() (+21 more)

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

### Community 15 - "Mock Response Diff Lines"
Cohesion: 0.09
Nodes (13): Collapsed, DisplayLine, Left, Right, Unchanged, computeLineDiff(), PersistentList, lcsLength() (+5 more)

### Community 16 - "NetworkMock Resource Loader & Config Tests"
Cohesion: 0.14
Nodes (5): ByteArray, NetworkMockResourceLoader, ByteArray, MockConfigRepositoryTest, RecordingResourceLoader

### Community 17 - "Mock Configuration Model"
Cohesion: 0.12
Nodes (12): ApiGroupConfig, effectiveEndpoints(), EndpointConfig, EndpointDefinition, EndpointOverride, EnvironmentConfig, MockConfiguration, MockMatch (+4 more)

### Community 18 - "NetworkMock Ktor Plugin"
Cohesion: 0.09
Nodes (22): createMockHttpClientCall(), install(), HttpClient, HttpStatusCode, MockHttpClientCall, NetworkMockPluginConfig, prepare(), Exception (+14 more)

### Community 19 - "Detekt Compose Rule Set & Modules"
Cohesion: 0.09
Nodes (28): Detekt Compose Rule Set Config, CompositionLocalAllowlist Rule, Material2 Rule, ModifierMissing Rule, ParameterNaming Rule, PreviewPublic Rule, ViewModelInjection Rule, Analytics Module (devview-analytics) (+20 more)

### Community 20 - "Mock Response & Initializer"
Cohesion: 0.10
Nodes (11): fromFile(), generateDisplayName(), MockResponse, DataStore, Preferences, NetworkMockInitializer, Result, MockConfigRepository (+3 more)

### Community 21 - "NetworkMock ViewModel Tests"
Cohesion: 0.19
Nodes (12): CoroutineDispatcher, MutableStateFlow, Result, ViewModelTest, NetworkMockViewModelTest, StateFlow, ViewModel, NetworkMockViewModel (+4 more)

### Community 22 - "NetworkMock Plugin State Tests"
Cohesion: 0.28
Nodes (6): NetworkMockState, Flow, HttpClient, HttpStatusCode, NetworkMockPluginTest, MockEngine

### Community 23 - "Mock State Repository Tests"
Cohesion: 0.13
Nodes (5): DataStore, Flow, Preferences, ThrowingPreferencesDataStore, MockStateRepositoryTest

### Community 24 - "Android Build Convention Plugins"
Cohesion: 0.08
Nodes (16): AndroidApplicationConventionPlugin, Plugin, Project, configureAndroidMultiplatformLibrary(), configureDetekt(), configureJava(), java(), configureKotlinCompiler() (+8 more)

### Community 25 - "NetworkMock DataStore & Matcher API History"
Cohesion: 0.09
Nodes (26): NetworkMockDataStoreDelegateKt (api 0.1.1), RequestMatcher (api 0.1.1), NetworkMockDataStoreDelegateKt (api 0.1.2), RequestMatcher (api 0.1.2), NetworkMockDataStoreDelegateKt (api 0.1.3), RequestMatcher (api 0.1.3), NetworkMockDataStoreDelegateKt (api 0.1.4), RequestMatcher (api 0.1.4) (+18 more)

### Community 26 - "NetworkMock State API v0.1.1-0.1.2"
Cohesion: 0.14
Nodes (25): EndpointMockState (api 0.1.1), EndpointMockState.Mock (api 0.1.1), EndpointMockState.Network (api 0.1.1), MockStateRepository (api 0.1.1), NetworkMockState (api 0.1.1), EndpointMockState (api 0.1.2), EndpointMockState.Mock (api 0.1.2), EndpointMockState.Network (api 0.1.2) (+17 more)

### Community 27 - "NetworkMock Endpoint API v0.1.3-0.1.4"
Cohesion: 0.16
Nodes (25): EndpointDescriptor (api 0.1.3), EndpointKey (api 0.1.3), MockConfigRepository (api 0.1.3), MockMatch (api 0.1.3), MockResponse (api 0.1.3), MockStateRepository (api 0.1.3), NetworkMockInitializer (api 0.1.3), NetworkMockResourceLoader (api 0.1.3) (+17 more)

### Community 28 - "Detekt Naming Rule Set"
Cohesion: 0.09
Nodes (23): BooleanPropertyNaming Rule, ClassNaming Rule, ConstructorParameterNaming Rule, EnumNaming Rule, ForbiddenClassName Rule, FunctionNameMaxLength Rule, FunctionNameMinLength Rule, FunctionParameterNaming Rule (+15 more)

### Community 29 - "FeatureFlip Screen Tests"
Cohesion: 0.17
Nodes (12): FeatureFlipScreenTest, FeatureFlipScreen(), Dp, Modifier, waitUntilTagCount(), waitUntilTagExists(), waitUntilTagGone(), FakePreferencesDataStore (+4 more)

### Community 30 - "Code Style & Conduct Guides"
Cohesion: 0.11
Nodes (23): Code of Conduct, Compose List Keys Rule Rationale, Detekt, Code Style Guide, ktlint, LazyColumn/LazyRow key Argument, Pre-commit Hook (code style enforcement), Android Studio Ladybug (+15 more)

### Community 31 - "Detekt Complexity Rule Set"
Cohesion: 0.10
Nodes (21): CognitiveComplexMethod Rule, ComplexCondition Rule, ComplexInterface Rule, Complexity Rule Set, Jetpack Compose @Composable Annotation, CyclomaticComplexMethod Rule, **/DependencyInjection.kt File Pattern (Detekt Exclude), DocumentationOverPrivateFunction Rule (+13 more)

### Community 32 - "Analytics Logger Core"
Cohesion: 0.10
Nodes (9): AnalyticsLogger, AnalyticsLog, AnalyticsLogListPreviewParameterProvider, PreviewParameterProvider, AnalyticsLogPreviewParameterProvider, PreviewParameterProvider, AnalyticsLoggerSamples, AnalyticsLogSamples (+1 more)

### Community 33 - "Mock State Repository (Endpoint)"
Cohesion: 0.14
Nodes (5): EndpointDescriptor, EndpointKey, Preferences, MockStateRepository, GroupEnvironmentUiModel

### Community 34 - "NetworkMock Endpoint ViewModel Tests"
Cohesion: 0.25
Nodes (7): MutableStateFlow, Result, ViewModelTest, NetworkMockEndpointViewModelTest, StateFlow, ViewModel, NetworkMockEndpointViewModel

### Community 35 - "Module Interface Definition"
Cohesion: 0.14
Nodes (15): Color, Dp, ImageVector, KClass, NavKey, PersistentMap, Module, previewModule() (+7 more)

### Community 36 - "DevView Utils DataStore API History"
Cohesion: 0.18
Nodes (20): CreateDataStore_androidKt (v0.1.1), CreateDataStore_iosKt (v0.1.1), CreateDataStoreKt (v0.1.1), DataStoreDelegate (v0.1.1), RequiresDataStore (v0.1.1), CreateDataStore_androidKt (v0.1.2), CreateDataStore_iosKt (v0.1.2), CreateDataStoreKt (v0.1.2) (+12 more)

### Community 37 - "Konsist Architecture Tests"
Cohesion: 0.11
Nodes (10): add-konsist-rule Skill, konsist/build.gradle.kts, ComposeTest, FunSpec, FunSpec, ModuleDependencyTest, FunSpec, PackageNamingTest (+2 more)

### Community 38 - "Detekt Comments Rule Set"
Cohesion: 0.13
Nodes (18): AbsentOrWrongFileLicense Rule, Comments Rule Set, DeprecatedBlockTag Rule, DocumentationOverPrivateProperty Rule, EndOfSentenceFormat Rule, InstanceOfCheckForException Rule, KDocReferencesNonPublicProperty Rule, KMP Test Source-Set Exclusion Glob Pattern (+10 more)

### Community 39 - "NetworkMock ApiGroupConfig API v0.1.1-0.1.2"
Cohesion: 0.24
Nodes (18): ApiGroupConfig (api 0.1.1), EndpointConfig (api 0.1.1), EndpointDefinition (api 0.1.1), EndpointOverride (api 0.1.1), EnvironmentConfig (api 0.1.1), MockConfigurationKt (effectiveEndpoints) (api 0.1.1), ApiGroupConfig (api 0.1.2), EndpointConfig (api 0.1.2) (+10 more)

### Community 40 - "Analytics Module Registration"
Cohesion: 0.14
Nodes (9): Analytics, AnalyticsDestination, Dp, KClass, Module, NavKey, PersistentMap, Main (+1 more)

### Community 41 - "Feature Handler API"
Cohesion: 0.18
Nodes (17): FeatureHandler.addFeatures, FeatureHandler class, FeatureHandler.isFeatureEnabled, FeatureHandler.isFeatureEnabledFlow, rememberFeatureHandler (FeatureHandlerKt), FeatureHandler.addFeatures, FeatureHandler class, Rationale: featureRegistry keyed by Feature instance but looked up by stable name (+9 more)

### Community 42 - "Feature Handler Implementation"
Cohesion: 0.20
Nodes (7): FeatureHandler, Feature, Flow, Preferences, State, rememberFeatureHandler(), FeatureHandlerTest

### Community 43 - "NetworkMock Config API v0.1.3-0.1.4"
Cohesion: 0.24
Nodes (17): MockConfiguration (api 0.1.3), ApiGroupConfig (api 0.1.4), EndpointConfig (api 0.1.4), EndpointDefinition (api 0.1.4), EndpointOverride (api 0.1.4), EnvironmentConfig (api 0.1.4), MockConfiguration (api 0.1.4), MockConfigurationKt (effectiveEndpoints) (api 0.1.4) (+9 more)

### Community 44 - "NetworkMock Core & Legacy API"
Cohesion: 0.17
Nodes (16): NetworkMockInitializer (devview-networkmock-core), NetworkMockResourceLoader (devview-networkmock-core), MockConfigRepository (devview-networkmock-core), MockStateRepository (devview-networkmock-core), NetworkMock Legacy Constructor (pre-0.1.3, Function2 resourceLoader), NetworkMock, NetworkMockDestination, NetworkMockEndpointViewModel (+8 more)

### Community 45 - "Detekt Empty-Blocks Rule Set"
Cohesion: 0.12
Nodes (16): Empty Blocks Rule Set, EmptyCatchBlock Rule, EmptyClassBlock Rule, EmptyDefaultConstructor Rule, EmptyDoWhileBlock Rule, EmptyElseBlock Rule, EmptyFinallyBlock Rule, EmptyForBlock Rule (+8 more)

### Community 46 - "Renovate Dependency Config"
Cohesion: 0.12
Nodes (15): * * * * 0,6, * 22-23,0-4 * * *, config:recommended, group:all, mergeConfidence:all-badges, commitMessagePrefix, customManagers, extends (+7 more)

### Community 47 - "Highlighted Analytics Log Cards"
Cohesion: 0.20
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

### Community 53 - "DevView Configuration Guide"
Cohesion: 0.16
Nodes (15): Build Errors (Issue), Conditional Modules, Configuration, Debug Menu Button, DevView Not Appearing (Issue), Feature Flags Not Working (Issue), FeatureFlip Module, Gesture Detection (Opening DevView) (+7 more)

### Community 54 - "Detekt Global Config & Coroutines Rules"
Cohesion: 0.14
Nodes (14): CoroutineLaunchedInTestWithoutRunTest Rule, Coroutines Rule Set, Detekt Default Config (default-config.yml), Detekt Global Config Settings, Detekt Processors Settings, DetektProgressListener Processor (excluded), GlobalCoroutineUsage Rule, InjectDispatcher Rule (+6 more)

### Community 55 - "Analytics Screen Tests"
Cohesion: 0.24
Nodes (6): AnalyticsScreenTest, AnalyticsLogScreenPreview(), AnalyticsScreen(), Dp, Modifier, PersistentList

### Community 56 - "NetworkMock Core Config API v0.1.1-0.1.2"
Cohesion: 0.25
Nodes (14): EndpointDescriptor (api 0.1.1), EndpointKey (api 0.1.1), MockConfigRepository (api 0.1.1), MockConfiguration (api 0.1.1), MockMatch (api 0.1.1), MockResponse (api 0.1.1), NetworkMockInitializer (api 0.1.1), EndpointDescriptor (api 0.1.2) (+6 more)

### Community 58 - "Module Registry UI Tests"
Cohesion: 0.19
Nodes (8): androidx, KClass, Module, NavKey, PersistentMap, ModuleRegistryUiTest, TrackingModule, UiTestDestination

### Community 59 - "Module Destination Action"
Cohesion: 0.16
Nodes (6): ImageVector, ModuleDestinationAction, ModuleDestinationActionPopup, DestinationMetadataExtensionsTest, NavKey, TestNavKey

### Community 60 - "Module Registry Implementation"
Cohesion: 0.29
Nodes (6): buildModules(), ImmutableList, Module, ModuleRegistry, rememberModules(), ModuleRegistryTest

### Community 61 - "Detekt Exceptions Rule Set"
Cohesion: 0.15
Nodes (13): ErrorUsageWithThrowable Rule, ExceptionRaisedInUnexpectedLocation Rule, Exceptions Rule Set, NotImplementedDeclaration Rule, ObjectExtendsThrowable Rule, PrintStackTrace Rule, RethrowCaughtException Rule, ReturnFromFinally Rule (+5 more)

### Community 62 - "Feature Sealed Type Model"
Cohesion: 0.21
Nodes (13): Feature sealed interface, Feature.Companion, Feature.LocalFeature, Feature.RemoteFeature, DevView FeatureFlip Module (CLAUDE.md overview), Feature (sealed class), Feature.LocalFeature, Feature.RemoteFeature (+5 more)

### Community 63 - "FeatureFlip Module Registration"
Cohesion: 0.17
Nodes (9): FeatureFlip, FeatureFlipDestination, Dp, KClass, Module, NavKey, PersistentMap, Main (+1 more)

### Community 64 - "Module Registry Tests"
Cohesion: 0.27
Nodes (11): DummyDestination, androidx, KClass, Module, NavKey, PersistentMap, ModuleA, ModuleB (+3 more)

### Community 65 - "Sample Android App Entry Point"
Cohesion: 0.17
Nodes (8): Bundle, ComponentActivity, MainActivity, AppFeatures, DARK_MODE, DevViewApp(), UIViewController, MainViewController()

### Community 66 - "TimeCapsule Module Design"
Cohesion: 0.24
Nodes (12): Module (devview core interface), TimeCapsule, TimeCapsuleEffect, TimeCapsuleOwner, devview-timecapsule Module Documentation, Dedup/Replay Is StateFlow's Job, Not ScreenCapsule's, No CompositionLocal — Single Consumer Doesn't Justify Indirection, Recorded<S> (internal entry model) (+4 more)

### Community 67 - "FeatureFlip Module API History"
Cohesion: 0.21
Nodes (12): FeatureFlip Public API Surface v0.1.1, FeatureFlip Public API Surface v0.1.2, FeatureFlip Public API Surface v0.1.3, FeatureFlip Public API Surface v0.1.4, FeatureFlip Public API Surface (current), FeatureFlipDestination sealed interface, FeatureFlipDestination.Main, FeatureType enum (+4 more)

### Community 68 - "NetworkMock Ktor Plugin API History"
Cohesion: 0.32
Nodes (12): NetworkMockConfig (v0.1.1), NetworkMockPlugin (v0.1.1), NetworkMockPluginConfig (v0.1.1), NetworkMockConfig (v0.1.2), NetworkMockPlugin (v0.1.2), NetworkMockPluginConfig (v0.1.2), NetworkMockConfig (v0.1.3), NetworkMockPlugin (v0.1.3) (+4 more)

### Community 69 - "NetworkMock Module Registration"
Cohesion: 0.20
Nodes (9): Endpoint, Dp, KClass, Module, NavKey, PersistentMap, Main, NetworkMock (+1 more)

### Community 70 - "Module Item UI Tests"
Cohesion: 0.18
Nodes (7): ModuleItemUiTest, Dp, KClass, Module, NavKey, PersistentMap, TestModule

### Community 71 - "DevView Test Utilities Module"
Cohesion: 0.21
Nodes (12): devview-test Module Documentation, ComposeUiTestWait.kt extensions (waitUntilTagCount/Exists/Gone), FakePreferencesDataStore, assertEmitsExactly (FlowAssertions.kt), collectState/collectStates (StateFlowCollectors.kt), TestDispatchers / testDispatchers() / runTestWithDispatchers, ViewModelTest, ViewModelTest Uses Unconfined, Not Main, Dispatcher (+4 more)

### Community 72 - "DataStore Delegate UI Tests"
Cohesion: 0.23
Nodes (6): DataStoreDelegateUiTest, InitDelegate(), DataStoreDelegate, DataStore, Preferences, DataStoreDelegateTest

### Community 73 - "Getting Started Overview"
Cohesion: 0.18
Nodes (12): Analytics Module, App Composable, DevView Composable, FeatureFlip Module, GitHub Discussions, GitHub Issues, Getting Started with DevView, Early Integration Tip (+4 more)

### Community 74 - "NetworkMock Ktor/Core Shared State"
Cohesion: 0.18
Nodes (11): MockResponse (devview-networkmock-core), NetworkMockDataStoreDelegate (devview-networkmock-core), Shared DataStore Singleton Across NetworkMock UI and Ktor Plugin, MockHttpClientCall, NetworkMockPlugin, NetworkMockPluginConfig, devview-networkmock-ktor Module Documentation, Why MockHttpClientCall Is Public (Ktor Internals Constraint) (+3 more)

### Community 75 - "DevView Navigation Tests"
Cohesion: 0.22
Nodes (7): DevViewTest, NavigationEventHandler, NavigationEventHandler, DevView(), ImmutableList, Modifier, Module

### Community 76 - "Destination Metadata Builder"
Cohesion: 0.33
Nodes (8): DestinationMetadata, DestinationMetadataBuilder, PersistentList, asDestination(), KClass, NavKey, withActions(), withTitle()

### Community 77 - "ScreenCapsule Tests"
Cohesion: 0.40
Nodes (4): CounterState, FakeOwner, StateFlow, ScreenCapsuleTest

### Community 78 - "DevView Utils DataStore Module"
Cohesion: 0.25
Nodes (11): BooleanPreviewParameterProvider, createDataStore, DataStoreDelegate, rememberDataStore (Android actual), rememberDataStore (iOS actual), RequiresDataStore, devview-utils Module Documentation, @Suppress("ComposableNaming") on init/initDataStore (+3 more)

### Community 79 - "Release Publishing Process"
Cohesion: 0.22
Nodes (11): CHANGELOG.md, --no-configuration-cache Rationale, Publishing a Release Guide, GitHub Repository Secrets (Publishing), GPG Artifact Signing, gradle.properties (VERSION_NAME, POM_*), com.worldline Namespace Verification, publish.yml GitHub Actions Workflow (+3 more)

### Community 80 - "Sample Test Module"
Cohesion: 0.20
Nodes (9): Detail, Dp, KClass, Module, NavKey, PersistentMap, Main, TestModule (+1 more)

### Community 81 - "CI & Architecture Tooling"
Cohesion: 0.33
Nodes (10): architecture-reviewer Agent, local-ci Skill, verify-module Skill, Detekt Static Analysis, Konsist Architecture Enforcement, config/gitleaks/.gitleaks.toml, config/quality/detekt/default-config.yml, Get AVD Info Composite Action (+2 more)

### Community 82 - "KMP & Module Dev Agents"
Cohesion: 0.24
Nodes (10): kmp-advisor Agent, module-expert Agent, add-destination Skill, ComposeUIViewController iOS Embedding Pattern, Module Interface Contract, NavKey Destination Pattern, @Poko Annotation, RequiresDataStore Lifecycle (+2 more)

### Community 83 - "Module Scaffolding Skills"
Cohesion: 0.27
Nodes (10): add-module Skill, update-docs Skill, docs/contributing/development.md, docs/guides/module-development.md, docs/modules/analytics.md, docs/modules/featureflip.md, internal/dokka/build.gradle.kts, README.md (+2 more)

### Community 84 - "FeatureFlip Screen & Filters"
Cohesion: 0.22
Nodes (10): FeatureFlipScreen (FeatureFlipScreenKt), LocalFeatureHandler (FeatureHandlerKt), FeatureFilter (adaptive filter chips), Rationale: FeatureFilter hides LOCAL/REMOTE chips when all features share one type, Rationale: filter chips use OR within a dimension, AND across dimensions, FeatureFlipScreen composable, LocalFeatureHandler CompositionLocal, FeatureFlipScreen composable (README variant with onStateChange) (+2 more)

### Community 85 - "Feature Tri-State Switch UI"
Cohesion: 0.20
Nodes (10): FeatureState enum, FeatureState.Companion.fromOrdinal, FeatureState enum, Rationale: FeatureState ordinals are load-bearing (persisted as raw ints; reordering breaks data), FeatureTriStateSwitch composable, Rationale: FeatureTriStateSwitch segment order mirrors FeatureState ordinal order, SegmentedButtonContentMeasurePolicy, Rationale: hand-rolled because Material3 lacks icon-only segmented buttons with per-segment container colors (+2 more)

### Community 86 - "Home Screen & Tests"
Cohesion: 0.24
Nodes (6): HomeScreenTest, Home, HomeScreen(), HomeScreenPreview(), Modifier, Module

### Community 87 - "TimeCapsule Row & Screen UI"
Cohesion: 0.22
Nodes (7): formatDelta(), Modifier, TimeCapsuleRow(), Dp, Dp, Modifier, TimeCapsuleScreen()

### Community 88 - "TimeCapsule Module Registration"
Cohesion: 0.22
Nodes (7): KClass, Module, NavKey, PersistentMap, Main, TimeCapsule, TimeCapsuleDestination

### Community 89 - "TimeCapsule Registry Tests"
Cohesion: 0.27
Nodes (3): FakeOwner, StateFlow, TimeCapsuleTest

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

### Community 94 - "Sample App API & Entry Point"
Cohesion: 0.20
Nodes (4): SampleApi, App(), Modifier, Greeting

### Community 95 - "Sample Counter Screen"
Cohesion: 0.24
Nodes (5): CounterScreen(), Modifier, CounterState, CounterViewModel, StateFlow

### Community 96 - "FeatureFlip Module Overview"
Cohesion: 0.25
Nodes (9): FeatureFlip class, com.worldline.devview.core.Module (external), com.worldline.devview.utils.RequiresDataStore (external), feature_flip_datastore.preferences_pb file, devview-utils module, FeatureFlip module object, rememberModules DSL, RequiresDataStore interface (+1 more)

### Community 97 - "FeatureFlip Screen Filter Logic"
Cohesion: 0.25
Nodes (8): availableEntries(), FeatureFilter, LOCAL, OFF, ON, REMOTE, FeaturesScreenPreview(), Feature

### Community 98 - "NetworkMock Destination API History"
Cohesion: 0.33
Nodes (9): NetworkMockDestination (v0.1.2), NetworkMockDestination.Endpoint (v0.1.2), NetworkMockDestination.Main (v0.1.2), NetworkMockDestination (v0.1.3), NetworkMockDestination.Endpoint (v0.1.3), NetworkMockDestination.Main (v0.1.3), NetworkMockDestination (v0.1.4), NetworkMockDestination.Endpoint (v0.1.4) (+1 more)

### Community 99 - "HTTP Status Code Family"
Cohesion: 0.25
Nodes (8): fromStatusCode(), StatusCodeFamily, CLIENT_ERROR, INFORMATIONAL, REDIRECTION, SERVER_ERROR, SUCCESSFUL, UNKNOWN

### Community 101 - "DevView Module Registration Tests"
Cohesion: 0.25
Nodes (7): DevViewDestination, DevViewModule, Dp, KClass, Module, NavKey, PersistentMap

### Community 102 - "Module Item UI Component"
Cohesion: 0.25
Nodes (8): Modifier, Module, ModuleItem(), ModulePosition, FIRST, LAST, MIDDLE, SINGLE

### Community 103 - "Test ViewModel & Dispatchers"
Cohesion: 0.25
Nodes (5): ViewModelTest, runTestWithDispatchers(), TestDispatchers, TestCoroutineScheduler, TestResult

### Community 104 - "Branding Assets Overview"
Cohesion: 0.39
Nodes (9): devview-icon-dark.svg, devview-icon-light.svg, devview-icon-mono.svg, devview-logo-dark.svg, devview-logo-light.svg, devview-logo-mono.svg, Branding Assets README, Documentation Site (+1 more)

### Community 105 - "Platform Detection (expect/actual)"
Cohesion: 0.36
Nodes (6): AndroidPlatform, getPlatform(), getPlatform(), Platform, getPlatform(), IOSPlatform

### Community 106 - "Detekt Performance Rule Set"
Cohesion: 0.25
Nodes (8): ArrayPrimitive Rule, CouldBeSequence Rule, ForEachOnRange Rule, Performance Rule Set, SpreadOperator Rule, UnnecessaryPartOfBinaryExpression Rule, UnnecessaryTemporaryInstantiation Rule, UnnecessaryTypeCasting Rule

### Community 107 - "Analytics Log Item & Chip UI"
Cohesion: 0.32
Nodes (6): AnalyticsLogItem(), AnalyticsLogItemPreview(), Modifier, CategoryChip(), CategoryChipPreview(), Modifier

### Community 108 - "TimeCapsule Effect & Owner"
Cohesion: 0.29
Nodes (5): S, TimeCapsuleEffect(), S, StateFlow, TimeCapsuleOwner

### Community 109 - "Boolean Preview Provider Tests"
Cohesion: 0.29
Nodes (3): BooleanPreviewParameterProvider, PreviewParameterProvider, BooleanPreviewParameterProviderTest

### Community 110 - "Troubleshooting & Examples Overview"
Cohesion: 0.29
Nodes (8): Examples Overview, DevView GitHub Repository, Compose Multiplatform Target Compatibility, Where To Find More Examples, Localisation Support, Troubleshooting & FAQ, Best Practices Guide, Common Pitfalls Guide

### Community 111 - "Git Workflow Conventions"
Cohesion: 0.38
Nodes (6): CLAUDE.md Project Guidance, DevView Module Dependency Graph, Gitmoji Commit Convention, Semantic PR Title Convention, PR Hygiene Workflow, scripts/release.sh

### Community 112 - "Detekt Libraries Rule Set"
Cohesion: 0.38
Nodes (7): Detekt (static analysis tool), Ktlint Ruleset Config (android_studio style, maxLineLength=120), ktlint (Kotlin linter), ForbiddenPublicDataClass rule (inactive, ignores *.internal), Libraries Ruleset Config (Detekt), LibraryCodeMustSpecifyReturnType rule (active, allowOmitUnit=false), LibraryEntitiesShouldNotBePublic rule (inactive)

### Community 113 - "FeatureState Enum"
Cohesion: 0.33
Nodes (5): FeatureState, LOCAL_OFF, LOCAL_ON, REMOTE, fromOrdinal()

### Community 114 - "ScreenCapsule Recording Model"
Cohesion: 0.43
Nodes (3): S, Recorded, ScreenCapsule

### Community 115 - "Detekt Console Reports Settings"
Cohesion: 0.33
Nodes (6): ComplexityReport Console Report (excluded), Detekt Console Reports Settings, FileBasedIssuesReport Console Report (excluded), IssuesReport Console Report (excluded), NotificationReport Console Report (excluded), ProjectStatisticsReport Console Report (excluded)

### Community 119 - "DevView Overlay Navigation"
Cohesion: 0.33
Nodes (4): NavigationEventHandler, OverlayBackHandler(), NavigationEventHandler, NavigationEventDispatcher

### Community 120 - "Android DataStore Creation"
Cohesion: 0.53
Nodes (5): createDataStore(), Context, DataStore, Preferences, rememberDataStore()

### Community 121 - "Common DataStore Creation"
Cohesion: 0.47
Nodes (4): createDataStore(), DataStore, Preferences, rememberDataStore()

### Community 122 - "Installation & Setup Guide"
Cohesion: 0.33
Nodes (6): Installation, Prerequisites (Installation Recap), Sync Your Project, Installation Verification, How Do I Add A Custom Module?, Creating Custom Modules Guide

### Community 123 - "CI Publish & Release Workflows"
Cohesion: 0.40
Nodes (6): GitHub Release Notes Categorization Config, Publish Workflow, Publish Docs Workflow, Release Comment Workflow, gradle.properties, scripts/build_docs.sh

### Community 124 - "Sample HttpClient Mocking Setup"
Cohesion: 0.33
Nodes (4): createHttpClientWithMocking(), HttpClient, HttpClient, rememberHttpClientWithMocking()

### Community 125 - "Test Writing Conventions"
Cohesion: 0.70
Nodes (5): test-writer Agent, write-tests Skill, Kotest Infix Assertion Convention, Test Source Set Selection Convention, ViewModelTest Base Class Convention

### Community 126 - "Analytics Time Range Filter"
Cohesion: 0.40
Nodes (5): TimeRange, All, Last15Min, Last30Min, Last5Min

### Community 127 - "FeatureType Enum"
Cohesion: 0.50
Nodes (4): FeatureType, LOCAL, REMOTE, fromOrdinal()

### Community 129 - "StatusCodeFamily API History"
Cohesion: 0.40
Nodes (5): StatusCodeFamily (api 0.1.1), StatusCodeFamily (api 0.1.2), StatusCodeFamily (api 0.1.3), StatusCodeFamily (api 0.1.4), StatusCodeFamily (api current)

### Community 130 - "iOS DataStore Creation"
Cohesion: 0.70
Nodes (4): createDataStore(), DataStore, Preferences, rememberDataStore()

### Community 131 - "Prerequisites Overview"
Cohesion: 0.40
Nodes (5): Prerequisites (Overview Summary), Development Tools, Minimum Supported Versions, Prerequisites, Project Requirements

### Community 132 - "DataStore Convention Plugin"
Cohesion: 0.50
Nodes (3): DatastoreConventionPlugin, Plugin, Project

### Community 133 - "Device Test Convention Plugin"
Cohesion: 0.50
Nodes (3): DeviceTestConventionPlugin, Plugin, Project

### Community 134 - "Konsist Convention Plugin"
Cohesion: 0.50
Nodes (3): KonsistConventionPlugin, Plugin, Project

### Community 135 - "Kover Convention Plugin"
Cohesion: 0.50
Nodes (3): KoverConventionPlugin, Plugin, Project

### Community 136 - "Ktor Convention Plugin"
Cohesion: 0.50
Nodes (3): Plugin, Project, KtorConventionPlugin

### Community 137 - "Metalava Convention Plugin"
Cohesion: 0.50
Nodes (3): Plugin, Project, MetalavaConventionPlugin

### Community 138 - "Room Convention Plugin"
Cohesion: 0.50
Nodes (3): Plugin, Project, RoomConventionPlugin

### Community 139 - "Unit Test Convention Plugin"
Cohesion: 0.50
Nodes (3): Plugin, Project, UnitTestConventionPlugin

### Community 144 - "MockHttpClientCall API History"
Cohesion: 0.50
Nodes (4): MockHttpClientCall (v0.1.1), MockHttpClientCall (v0.1.2), MockHttpClientCall (v0.1.3), MockHttpClientCall (v0.1.4)

### Community 146 - "Boolean Preview Provider API History"
Cohesion: 0.50
Nodes (4): BooleanPreviewParameterProvider (v0.1.1), BooleanPreviewParameterProvider (v0.1.2), BooleanPreviewParameterProvider (v0.1.3), BooleanPreviewParameterProvider (v0.1.4)

### Community 147 - "Project Build Type Enum"
Cohesion: 0.50
Nodes (3): ProjectBuildType, DEBUG, RELEASE

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

## Ambiguous Edges - Review These
- `LocalFeatureHandler CompositionLocal` → `LocalFeatures CompositionLocal`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `FeatureFlipScreen composable` → `FeatureFlipScreen composable (README variant with onStateChange)`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `setFeatureState (internal)` → `FeatureHandler.setFeatureState (README shows as publicly callable)`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `devview-utils module` → `createDataStore(producePath)`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: references
- `FeatureHandler.isFeatureEnabled` → `FeatureHandler.isFeatureEnabled (README variant, used as Flow)`  [AMBIGUOUS]
  devview-featureflip/README.md · relation: semantically_similar_to
- `FeatureHandler.isFeatureEnabled (README variant, used as Flow)` → `FeatureHandler.isFeatureEnabled`  [AMBIGUOUS]
  devview-featureflip/api/api.txt · relation: semantically_similar_to
- `FeatureHandler.getFeatures()` → `FeatureHandler class`  [AMBIGUOUS]
  devview-featureflip/api/api.txt · relation: semantically_similar_to
- `TimeCapsule Module Doc` → `CounterScreen.kt (sample)`  [AMBIGUOUS]
  docs/modules/timecapsule.md · relation: references

## Knowledge Gaps
- **482 isolated node(s):** `Main`, `All`, `Last5Min`, `Last15Min`, `Last30Min` (+477 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **45 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `LocalFeatureHandler CompositionLocal` and `LocalFeatures CompositionLocal`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `FeatureFlipScreen composable` and `FeatureFlipScreen composable (README variant with onStateChange)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `setFeatureState (internal)` and `FeatureHandler.setFeatureState (README shows as publicly callable)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `devview-utils module` and `createDataStore(producePath)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `FeatureHandler.isFeatureEnabled` and `FeatureHandler.isFeatureEnabled (README variant, used as Flow)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `FeatureHandler.isFeatureEnabled (README variant, used as Flow)` and `FeatureHandler.isFeatureEnabled`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `FeatureHandler.getFeatures()` and `FeatureHandler class`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._