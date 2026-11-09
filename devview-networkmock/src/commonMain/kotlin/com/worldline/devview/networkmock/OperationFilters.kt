package com.worldline.devview.networkmock

import com.worldline.devview.networkmock.core.model.HttpMethod
import com.worldline.devview.networkmock.core.model.OperationMockState
import com.worldline.devview.networkmock.model.OperationUiModel
import kotlinx.serialization.Serializable

@Serializable
internal data class NetworkMockListState(
    val selectedSpecId: String? = null,
    val expanded: Boolean = false,
    val filters: Map<String, OperationFilters> = emptyMap()
)

@Serializable
internal data class OperationFilters(
    val query: String = "",
    val states: Set<MockStateFilter> = emptySet(),
    val version: String? = null,
    val methods: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val sort: OperationSort = OperationSort.SPEC_ORDER
) {
    val activeCount: Int
        get() = states.size + (if (version == null) 0 else 1) + methods.size + tags.size +
            if (sort == OperationSort.SPEC_ORDER) 0 else 1
}

@Serializable
internal enum class MockStateFilter(val label: String) {
    MOCKED(label = "Mocked"),
    NETWORK(label = "Network")
}

@Serializable
internal enum class OperationSort(val label: String) {
    SPEC_ORDER(label = "Default"),
    PATH(label = "Path (A-Z)"),
    METHOD(label = "Method"),
    TAG(label = "Tag"),
    MOCKED_FIRST(label = "Mocked first")
}

internal fun filterOperations(
    operations: List<OperationUiModel>,
    filters: OperationFilters
): List<OperationUiModel> = operations
    .filter { operation ->
        val config = operation.descriptor.config
        val matchesQuery = filters.query.isBlank() ||
            config.name.contains(other = filters.query, ignoreCase = true) ||
            config.path.contains(other = filters.query, ignoreCase = true) ||
            config.operationId.contains(other = filters.query, ignoreCase = true)
        val matchesVersion = filters.version == null || config.version == filters.version
        val matchesMethod = filters.methods.isEmpty() || config.method.value in filters.methods
        val matchesTags = filters.tags.isEmpty() || config.tags.any { it in filters.tags }
        val state = if (operation.currentState is OperationMockState.Network) {
            MockStateFilter.NETWORK
        } else {
            MockStateFilter.MOCKED
        }
        matchesQuery && matchesVersion && matchesMethod && matchesTags &&
            (filters.states.isEmpty() || state in filters.states)
    }.sortedByOption(sort = filters.sort)

internal fun List<OperationUiModel>.sortedByOption(sort: OperationSort): List<OperationUiModel> =
    when (sort) {
        OperationSort.SPEC_ORDER -> this
        OperationSort.PATH -> sortedBy { it.descriptor.config.path }
        OperationSort.MOCKED_FIRST -> sortedBy { it.currentState is OperationMockState.Network }
        OperationSort.METHOD -> sortedBy { operation ->
            HttpMethod.DefaultMethods
                .indexOf(element = operation.descriptor.config.method)
                .takeIf { it >= 0 } ?: Int.MAX_VALUE
        }
        OperationSort.TAG -> sortedBy {
            it.descriptor.config.tags
                .firstOrNull()
                .orEmpty()
        }
    }
