# Installation

This guide will walk you through adding DevView to your Kotlin Multiplatform project.

## Prerequisites

Before you begin, ensure you have:

- ✅ **Kotlin Multiplatform Project** - A working KMP project
- ✅ **Compose Multiplatform** - Configured in your project
- ✅ **Minimum Versions**:
    <!-- renovate: datasource=maven depName=org.jetbrains.kotlin:kotlin-stdlib -->
    - Kotlin 2.4.0+
    <!-- renovate: datasource=maven depName=org.jetbrains.compose:compose-gradle-plugin -->
    - Compose Multiplatform 1.11.1+
    - Android: API 26 (Oreo) or higher
    - iOS: iOS 16.0 or higher

## Gradle Setup

### Step 1: Add Repository

Ensure you have the required repositories in your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### Step 2: Add Version Catalog (Recommended)

Add DevView dependencies to your `gradle/libs.versions.toml`:

```toml
[versions]
devview = "0.0.1-SNAPSHOT"

[libraries]
devview = { module = "com.worldline.devview:devview", version.ref = "devview" }
devview-featureflip = { module = "com.worldline.devview:devview-featureflip", version.ref = "devview" }
devview-analytics = { module = "com.worldline.devview:devview-analytics", version.ref = "devview" }
```

### Step 3: Add Dependencies

In your shared module's `build.gradle.kts`:

**Using Version Catalog**

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core DevView module (required)
            implementation(libs.devview)
            
            // Optional: FeatureFlip module
            implementation(libs.devview.featureflip)
            
            // Optional: Analytics module
            implementation(libs.devview.analytics)
        }
    }
}
```

--- 

**Direct Dependencies**

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core DevView module (required)
            implementation("com.worldline.devview:devview:0.0.1-SNAPSHOT")
            
            // Optional: FeatureFlip module
            implementation("com.worldline.devview:devview-featureflip:0.0.1-SNAPSHOT")
            
            // Optional: Analytics module
            implementation("com.worldline.devview:devview-analytics:0.0.1-SNAPSHOT")
        }
    }
}
```

---

## Module Selection

DevView is modular, so you only include what you need:

| Module | Purpose | When to Include |
|--------|---------|----------------|
| **devview** | Core framework, module system, navigation | ✅ Always required |
| **devview-featureflip** | Feature flag management with DataStore | When you need feature toggles |
| **devview-analytics** | Analytics event monitoring | When you need analytics debugging |
| **devview-networkmock** | Network mocking for Ktor and other platforms | When you need to simulate network responses |

## Sync Your Project

After adding dependencies, sync your Gradle project:

**Android Studio**

- Click the **Sync Now** banner or **File > Sync Project with Gradle Files**

**Terminal**

```bash
./gradlew --refresh-dependencies
```

## Verification

Verify the installation by adding this simple import:

```kotlin
import com.worldline.devview.DevView
import com.worldline.devview.core.*
```

## Custom Modules

See [Creating Custom Modules](../modules/custom-modules.md) for a full guide on extending DevView with your own modules.

## Troubleshooting

If you encounter issues during installation:
- Ensure all repositories are correctly configured in your Gradle files.
- For dependency resolution errors, run `./gradlew --refresh-dependencies` in the terminal.
- If Compose Multiplatform is not detected, verify plugin versions and compatibility.
- For further help, consult the [troubleshooting section](troubleshooting-faq.md).

## Next Steps

[Quick Start →](quick-start.md){ .md-button .md-button--primary }
