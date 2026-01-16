import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.worldline.buildlogic"

java {
    toolchain {
        // Build with JDK 21
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.compose.multiplatform.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.convention.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("composeMultiplatform") {
            id = libs.plugins.convention.compose.multiplatform.get().pluginId
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("datastore") {
            id = libs.plugins.convention.datastore.get().pluginId
            implementationClass = "DatastoreConventionPlugin"
        }
        register("konsist") {
            id = libs.plugins.convention.konsist.get().pluginId
            implementationClass = "KonsistConventionPlugin"
        }
        register("kover") {
            id = libs.plugins.convention.kover.get().pluginId
            implementationClass = "KoverConventionPlugin"
        }
        register("ktor") {
            id = libs.plugins.convention.ktor.get().pluginId
            implementationClass = "KtorConventionPlugin"
        }
        register("multiplatformLibrary") {
            id = libs.plugins.convention.multiplatform.library.get().pluginId
            implementationClass = "MultiplatformLibraryConventionPlugin"
        }
        register("room") {
            id = libs.plugins.convention.room.get().pluginId
            implementationClass = "RoomConventionPlugin"
        }
        register("unitTest") {
            id = libs.plugins.convention.unitTest.get().pluginId
            implementationClass = "UnitTestConventionPlugin"
        }
    }
}
