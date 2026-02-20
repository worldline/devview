plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.convention.datastore)
    alias(libs.plugins.convention.ktor)
}

kotlin {
    androidLibrary {
        namespace = "com.worldline.devview.sample.network"

        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "network"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.components.resources)

            // DevView modules
            implementation(projects.devviewNetworkmock)
            implementation(projects.devviewUtils)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}