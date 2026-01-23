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
    implementation(projects.sample.shared)
    implementation(projects.devview)
    implementation(projects.devviewFeatureflip)

    implementation(libs.androidx.activity.compose)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.material3)

    implementation(libs.jetbrains.androidx.navigation3.ui)

    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.jetbrains.compose.ui.tooling)
    debugImplementation(libs.jetbrains.compose.ui.tooling.preview)
}