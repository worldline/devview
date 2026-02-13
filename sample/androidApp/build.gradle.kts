plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.plugin.serialization)
}

android {
    namespace = "com.worldline.devview.sample"

    defaultConfig {
        applicationId = "com.worldline.devview.sample"
        versionCode = 100_000_000
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
        }
    }
}

dependencies {
    // Shared module contains all DevView integration
    implementation(projects.sample.shared)

    // Android-specific dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.material3)

    // Debug tooling
    debugImplementation(libs.jetbrains.compose.ui.tooling)
    debugImplementation(libs.jetbrains.compose.ui.tooling.preview)
}