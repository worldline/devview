package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

/**
 * UI model for a single API group + environment combination.
 *
 * Each tab in the Network Mock screen represents one [GroupEnvironmentUiModel],
 * showing the resolved endpoints for that specific group and environment.
 *
 * @property groupId The [com.worldline.devview.networkmock.model.ApiGroupConfig] identifier
 * @property environmentId The [com.worldline.devview.networkmock.model.EnvironmentConfig] identifier
 * @property name Human-readable display name, e.g. `"My Backend — Staging"`
 * @property url The base URL for this group in this environment
 * @property endpoints The resolved endpoints with their current mock states
 */
@Immutable
public data class GroupEnvironmentUiModel(
    val groupId: String,
    val environmentId: String,
    val name: String,
    val url: String,
    val endpoints: PersistentList<EndpointUiModel>
) {
    public companion object
}
