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
}
