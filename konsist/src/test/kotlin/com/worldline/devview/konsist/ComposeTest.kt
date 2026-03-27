package com.worldline.devview.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAnnotationOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

/**
 * Verifies Compose conventions across all DevView modules.
 *
 * Conventions:
 *  - Composables in `components` packages are `internal`
 *  - `@Preview` composable functions are `private`
 *  - `PreviewParameterProvider` subclasses are `internal` and live in a `preview` package
 */
class ComposeTest : FunSpec(body = {
    test(name = "Composables in components packages are internal") {
        Konsist
            .scopeFromProject()
            .functions()
            .withAnnotationOf(androidx.compose.runtime.Composable::class)
            .withPackage("..components..")
            .assertFalse(additionalMessage = "Composables in a 'components' package must be 'internal'") { function ->
                function.hasPublicOrDefaultModifier
            }
    }

    test(name = "Preview composables are private") {
        Konsist
            .scopeFromProject()
            .functions()
            .withAnnotationOf(androidx.compose.ui.tooling.preview.Preview::class)
            .assertTrue(additionalMessage = "@Preview composables must be 'private'") { function ->
                function.hasPrivateModifier
            }
    }

    test(name = "PreviewParameterProvider subclasses are internal (except in utils module)") {
        Konsist
            .scopeFromProject()
            .classes()
            .filterNot {
                it.packagee?.hasNameStartingWith("com.worldline.devview.utils") ?: false
            }
            .withNameEndingWith("PreviewParameterProvider")
            .assertTrue(additionalMessage = "PreviewParameterProvider subclasses must be 'internal'") { clazz ->
                clazz.hasInternalModifier
            }
    }

    test(name = "PreviewParameterProvider subclasses reside in a preview package") {
        Konsist
            .scopeFromProject()
            .classes()
            .withNameEndingWith("PreviewParameterProvider")
            .assertTrue(additionalMessage = "PreviewParameterProvider subclasses must be in a 'preview' package") { clazz ->
                clazz.packagee?.name?.endsWith(".preview") ?: false
            }
    }
})
