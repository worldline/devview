plugins {
    alias(libs.plugins.convention.multiplatform.library)
    alias(libs.plugins.convention.compose.multiplatform)
    alias(libs.plugins.convention.unitTest)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.metalava)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    addDefaultDevViewTargets()

    android {
        namespace = "com.worldline.devview.networkmock.core"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.devviewUtils)
                implementation(libs.kotlinx.serialization.json)
                // ponytail: kaml's repo is archived (0.104.0 is final) - best-effort YAML support,
                // quarantined behind the openapi parser's format-detection branch. If a future
                // Kotlin/serialization bump breaks it, drop this line and the YAML branch together.
                implementation(libs.kaml)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.devviewTest)
            }
        }

    }
}

tasks.withType<Test> {
    failOnNoDiscoveredTests.set(false)
    // Points RealSampleSpecTest (androidHostTest) at the sample app's real, shipped OpenAPI
    // specs/response files without a compile-time dependency on the sample module - this is a
    // pure file-system read at test-run-time, guarding against the shipped sample silently
    // drifting out of sync with what this parser actually accepts.
    systemProperty(
        "devview.sampleNetworkResourcesDir",
        rootProject.file("sample/network/src/commonMain/composeResources").absolutePath
    )
}
