plugins {
    alias(libs.plugins.convention.multiplatform.library)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.test"
    }

    sourceSets {
        all {
            languageSettings.optIn("androidx.compose.ui.test.ExperimentalTestApi")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.androidx.datastore.preferences.core)
            }
        }
        androidMain {
            dependencies {
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.compose.ui.test.junit4.android)
                implementation(libs.mockk)
            }
        }
    }
}
