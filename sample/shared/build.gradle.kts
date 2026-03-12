plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.datastore)
    alias(libs.plugins.convention.ktor)
}

kotlin {
    android {
        namespace = "com.worldline.devview.sample.shared"
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true

            export(projects.sample.network)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.jetbrains.compose.ui.tooling)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)

            // DevView modules
            implementation(projects.devview)
            implementation(projects.devviewFeatureflip)
            implementation(projects.devviewAnalytics)
            implementation(projects.devviewNetworkmock)
            implementation(projects.devviewUtils)

            api(projects.sample.network)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
