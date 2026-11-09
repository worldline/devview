package com.worldline.devview.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

/**
 * Verifies ViewModel conventions across all DevView modules.
 *
 * Conventions:
 *  - `ViewModel` subclasses live in a `viewmodel` package
 *  - `ViewModel` subclasses are internal implementation details
 */
class ViewModelTest :
    FunSpec(body = {
        test(name = "ViewModel subclasses reside in a viewmodel package") {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("ViewModel")
                .assertTrue(
                    additionalMessage = "ViewModel subclasses must be in a 'viewmodel' package"
                ) { clazz ->
                    clazz.packagee?.name?.endsWith(".viewmodel") ?: false
                }
        }

        test(name = "Built-in ViewModels are internal") {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("ViewModel")
                .filterNot { it.packagee?.hasNameStartingWith("com.worldline.devview.sample") ?: false }
                .assertTrue(additionalMessage = "Built-in ViewModels belong to module registration") { clazz ->
                    clazz.hasInternalModifier
                }
        }
    })
