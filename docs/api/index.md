# API Reference
Complete API documentation for all DevView modules.
## Modules
- [Core API](core.md) - Framework, module system, navigation
- [FeatureFlip API](featureflip.md) - Feature flag management  
- [Analytics API](analytics.md) - Analytics monitoring
## Quick Links
### Core
- `Module` interface
- `Section` enum
- `rememberModules()` function
- `DevView` composable
### FeatureFlip
- `Feature` sealed class
- `FeatureState` enum
- `FeatureHandler` class
- `FeatureFlipScreen` composable
### Analytics
- `AnalyticsLog` data class
- `AnalyticsLogType` enum
- `AnalyticsLogger` object
- `AnalyticsScreen` composable
## Documentation
All APIs are documented with KDoc. Generate HTML docs:
```bash
./gradlew dokkaHtml
```
