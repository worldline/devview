package com.worldline.devview.networkmock.model

import androidx.compose.runtime.Immutable
import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationMockState

/**
 * UI model pairing a static [OperationDescriptor] with its live [OperationMockState].
 *
 * @property descriptor The immutable operation configuration and available responses.
 * @property currentState The current runtime mock state for this operation.
 * @see OperationDescriptor
 * @see OperationMockState
 */
@Immutable
public data class OperationUiModel(
    val descriptor: OperationDescriptor,
    val currentState: OperationMockState
) {
    public companion object
}
