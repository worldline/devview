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
 *  - `ViewModel` subclasses are public API or internal implementation details
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

        test(name = "ViewModel subclasses are public or internal") {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("ViewModel")
                .assertTrue(additionalMessage = "ViewModel subclasses must be public or internal") { clazz ->
                    clazz.hasPublicOrDefaultModifier || clazz.hasInternalModifier
                }
        }
    })
