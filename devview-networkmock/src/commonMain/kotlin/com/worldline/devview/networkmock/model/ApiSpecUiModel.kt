package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

/**
 * UI model for a single OpenAPI spec — one tab in the Network Mock screen.
 *
 * There is no environment axis: a spec's tab shows every operation declared in its document,
 * regardless of which of the spec's declared servers the app currently talks to.
 *
 * @property specId The [com.worldline.devview.networkmock.core.model.ApiSpec.id] identifier
 * @property name Human-readable display name, taken from the spec's `info.title`
 * @property operations The operations declared in this spec, with their current mock states
 */
@Immutable
public data class ApiSpecUiModel(
    val specId: String,
    val name: String,
    val operations: PersistentList<OperationUiModel>
) {
    public companion object
}
