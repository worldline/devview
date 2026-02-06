plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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

    implementation(libs.androidx.activity.compose)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.material3)

    implementation(libs.kotlinx.collections.immutable)

    debugImplementation(libs.jetbrains.compose.ui.tooling)
    debugImplementation(libs.jetbrains.compose.ui.tooling.preview)
}