package com.worldline.devview.networkmock.viewmodel

import com.worldline.devview.networkmock.core.model.OperationDescriptor
import com.worldline.devview.networkmock.core.model.OperationKey
import com.worldline.devview.networkmock.core.repository.MockConfigRepository
import kotlinx.collections.immutable.toPersistentList

internal class OperationDetailsLoader(private val configRepository: MockConfigRepository) {
    suspend fun load(key: OperationKey): LoadedOperation {
        val configuration = configRepository.loadConfiguration().getOrThrow()
        val operation = configuration.specs
            .firstOrNull { it.id == key.specId }
            ?.operations
            ?.firstOrNull { it.operationId == key.operationId }
        checkNotNull(value = operation) { "Operation configuration not found" }
        return LoadedOperation(
            key = key,
            descriptor = OperationDescriptor(key = key, config = operation),
            responses = configRepository.discoverResponseFiles(key = key).toPersistentList()
        )
    }
}
