# Installation

This guide will walk you through adding DevView to your Kotlin Multiplatform project.

## Prerequisites

Before you begin, ensure you have:

- ✅ **Kotlin Multiplatform Project** - A working KMP project
- ✅ **Compose Multiplatform** - Configured in your project
- ✅ **Minimum Versions**:
    - Kotlin 2.3.0+
    - Compose Multiplatform 1.10.0+
    - Android: API 21 (Lollipop) or higher
    - iOS: iOS 14.0 or higher

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
devview = "1.0.0"

[libraries]
devview-core = { module = "com.worldline.devview:devview", version.ref = "devview" }
devview-featureflip = { module = "com.worldline.devview:devview-featureflip", version.ref = "devview" }
devview-analytics = { module = "com.worldline.devview:devview-analytics", version.ref = "devview" }
```

### Step 3: Add Dependencies

In your shared module's `build.gradle.kts`:

=== "Using Version Catalog"

    ```kotlin
    kotlin {
        sourceSets {
            commonMain.dependencies {
                // Core DevView module (required)
                implementation(libs.devview.core)
                
                // Optional: FeatureFlip module
                implementation(libs.devview.featureflip)
                
                // Optional: Analytics module
                implementation(libs.devview.analytics)
            }
        }
    }
    ```

=== "Direct Dependencies"

    ```kotlin
    kotlin {
        sourceSets {
            commonMain.dependencies {
                // Core DevView module (required)
                implementation("com.worldline.devview:devview:1.0.0")
                
                // Optional: FeatureFlip module
                implementation("com.worldline.devview:devview-featureflip:1.0.0")
                
                // Optional: Analytics module
                implementation("com.worldline.devview:devview-analytics:1.0.0")
            }
        }
    }
    ```

## Module Selection

DevView is modular, so you only include what you need:

| Module | Purpose | When to Include |
|--------|---------|----------------|
| **devview-core** | Core framework, module system, navigation | ✅ Always required |
| **devview-featureflip** | Feature flag management with DataStore | When you need feature toggles |
| **devview-analytics** | Analytics event monitoring | When you need analytics debugging |

## Sync Your Project

After adding dependencies, sync your Gradle project:

=== "Android Studio"
    Click the **Sync Now** banner or **File > Sync Project with Gradle Files**

=== "Terminal"
    ```bash
    ./gradlew --refresh-dependencies
    ```

## Verification

Verify the installation by adding this simple import:

```kotlin
import com.worldline.devview.DevView
import com.worldline.devview.core.*
```

## Next Steps

[Quick Start →](quick-start.md){ .md-button .md-button--primary }
