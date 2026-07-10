import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.screenshot) apply false
    alias(libs.plugins.compose.stability.analyzer) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.jetbrains.idea.ext)
    alias(libs.plugins.kotest.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.room) apply false
}

tasks.register("detektFull") {
    group = "verification"
    description = "Runs detekt on all modules"

    dependsOn(subprojects.map {
        it.tasks.matching { task ->
            task.name == "detektMainAndroid" || task.name == "detektIosMainSourceSet"
        }
    })
}

subprojects {
    pluginManager.withPlugin("org.jetbrains.dokka") {
        the<org.jetbrains.dokka.gradle.DokkaExtension>().apply {
            dokkaSourceSets.configureEach {
                enableJdkDocumentationLink.set(false)
            }
        }
    }
}

tasks.register("preCommitCheck") {
    group = "verification"
    description = "Installs pre-commit and ensures hooks are up to date."

    val rootDirectory = rootDir

    doLast {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        fun run(cmd: List<String>): Int = ProcessBuilder(cmd)
            .directory(rootDirectory)
            .inheritIO()
            .start()
            .waitFor()

        println("Installing pre-commit package")
        if (isWindows) {
            run(listOf("cmd", "/c", "python -m pip install pre-commit"))
        } else {
            run(listOf("/opt/homebrew/bin/brew", "install", "pre-commit"))
        }

        println("Autoupdate pre-commit config")
        val autoupdateExit = if (isWindows) {
            run(listOf("cmd", "/c", "pre-commit autoupdate"))
        } else {
            run(listOf("/opt/homebrew/bin/pre-commit", "autoupdate"))
        }

        println("Installing pre-commit hooks")
        val installExit = if (isWindows) {
            run(listOf("cmd", "/c", "pre-commit install"))
        } else {
            run(listOf("/opt/homebrew/bin/pre-commit", "install"))
        }

        if (autoupdateExit != 0 || installExit != 0) {
            throw GradleException(
                "\"pre-commit\" is not installed or not in PATH. Please install it before syncing."
            )
        }
    }
}

idea.project.settings {
    taskTriggers {
        if (System.getenv("CI") == null) {
            afterSync(tasks.getByName("preCommitCheck"))
        }
    }
}