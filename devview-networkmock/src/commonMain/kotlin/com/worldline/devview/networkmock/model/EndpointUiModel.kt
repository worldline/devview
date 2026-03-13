package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable

/**
 * UI model pairing a static [EndpointDescriptor] with its live [EndpointMockState].
 *
 * @property descriptor The immutable endpoint configuration and available responses.
 * @property currentState The current runtime mock state for this endpoint.
 * @see EndpointDescriptor
 * @see EndpointMockState
 */
@Immutable
public data class EndpointUiModel(
    val descriptor: EndpointDescriptor,
    val currentState: EndpointMockState
) {
    public companion object
}
