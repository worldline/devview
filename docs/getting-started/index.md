# Getting Started with DevView

Welcome to DevView! This guide will help you get up and running with the framework in your Kotlin Multiplatform project.

## Prerequisites

Before you begin, make sure you have:

- ✅ **Kotlin Multiplatform Project** - A working KMP project targeting Android and/or iOS
- ✅ **Compose Multiplatform** - Compose Multiplatform configured in your project
- ✅ **Minimum Versions**:
    - Kotlin 2.3.0+
    - Compose Multiplatform 1.10.0+
    - Android: API 21 (Lollipop) or higher
    - iOS: iOS 14.0 or higher

## Setup Overview

Setting up DevView involves three main steps:

1. **[Installation](installation.md)** - Add dependencies to your project
2. **[Quick Start](quick-start.md)** - Integrate DevView into your app
3. **[Configuration](configuration.md)** - Customize modules and settings

## What You'll Learn

### Installation
Learn how to add DevView dependencies to your Gradle configuration for both shared and platform-specific code.

[Go to Installation →](installation.md){ .md-button }

### Quick Start
Follow a step-by-step tutorial to integrate DevView into your application with working code examples.

[Go to Quick Start →](quick-start.md){ .md-button }

### Configuration
Discover advanced configuration options, module selection, and customization techniques.

[Go to Configuration →](configuration.md){ .md-button }

## Quick Preview

Here's a minimal example of what DevView integration looks like:

```kotlin
@Composable
fun App() {
    var isDevViewOpen by remember { mutableStateOf(false) }
    
    val modules = rememberModules {
        module(FeatureFlip)
        module(Analytics)
    }
    
    Box {
        MainAppContent()
        
        DevView(
            devViewIsOpen = isDevViewOpen,
            closeDevView = { isDevViewOpen = false },
            modules = modules
        )
    }
}
```

## Next Steps

Ready to get started? Head to the [Installation Guide](installation.md) to begin!

## Need Help?

If you encounter any issues:

- 📖 Check the [troubleshooting section](configuration.md#troubleshooting)
- 💬 Ask in [GitHub Discussions](https://github.com/worldline/devview/discussions)
- 🐛 Report bugs in [GitHub Issues](https://github.com/worldline/devview/issues)
