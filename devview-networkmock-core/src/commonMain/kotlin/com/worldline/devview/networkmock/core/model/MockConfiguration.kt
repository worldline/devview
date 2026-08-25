package com.worldline.devview.networkmock.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Root configuration for network mocking, parsed from one or more OpenAPI 3.x documents.
 *
 * Each configured spec file (JSON or YAML) becomes one [ApiSpec] — see
 * [com.worldline.devview.networkmock.core.repository.MockConfigRepository] for how these are
 * loaded, and [com.worldline.devview.networkmock.NetworkMock] (in `devview-networkmock`) for
 * how integrators register spec files.
 *
 * @property specs The parsed API specs, one per configured spec file
 * @see ApiSpec
 * @see com.worldline.devview.networkmock.core.repository.MockConfigRepository
 */
@Serializable
public data class MockConfiguration(val specs: List<ApiSpec>)

/**
 * The parsed contents of a single OpenAPI 3.x document — one spec file, one [ApiSpec].
 *
 * There is no environment axis: a running app talks to exactly one base URL at a time, so
 * [servers] simply lists every base URL declared in the document's `servers[]` array, and a
 * request matches this spec if its hostname matches any of them (see
 * [com.worldline.devview.networkmock.core.repository.MockConfigRepository.findMatchingMock]).
 * An OpenAPI document spanning multiple API versions (e.g. both `/api/v1/...` and
 * `/api/v2/...` paths) is simply multiple [Operation] entries in the same spec — there is no
 * separate version dimension in this type.
 *
 * @property id Stable identifier for this spec, slugified from `info.title`
 *   (e.g. `"Sample API"` → `"sample-api"`). Used as the response-file path prefix and in
 *   [OperationKey.compositeKey].
 * @property name Human-readable name displayed in the UI, taken verbatim from `info.title`.
 * @property servers Base URLs declared in the document's `servers[].url`. A request matches
 *   this spec if its hostname matches any entry.
 * @property operations The operations declared across all of this document's `paths`.
 * @property delayMs Spec-wide default response delay in milliseconds, from the document-root
 *   `x-devview.delayMs` extension (see #94). Falls back to `null` (no delay) when absent, and
 *   is itself overridden by [Operation.delayMs] when the operation declares its own.
 * @see Operation
 * @see MockConfiguration
 */
@Serializable
public data class ApiSpec(
    val id: String,
    val name: String,
    val servers: List<String>,
    val operations: List<Operation>,
    val delayMs: Long? = null
)

/**
 * A single mockable API operation, parsed from one `paths.<path>.<method>` entry.
 *
 * @property operationId The OpenAPI `operationId` — required by the parser (a spec with a
 *   missing `operationId` fails to load with a clear error, since every DevView identifier
 *   depends on it). Stable across parses as long as the spec author doesn't rename it.
 * @property name Display name, taken from the operation's `summary`, falling back to
 *   [operationId] when absent.
 * @property path API path, may contain `{param}` placeholders (e.g. `"/v1/users/{userId}"`).
 * @property method HTTP method (GET, POST, PUT, DELETE, PATCH, etc.), uppercase.
 * @property queryParameters Required query-parameter values for this operation to match an
 *   incoming request, or `null` if this operation has none. Sourced from `parameters` entries
 *   with `in: query` that declare a literal `example` value — see
 *   [com.worldline.devview.networkmock.core.openapi.ParameterObject].
 * @property delayMs Response delay in milliseconds for this operation specifically, from the
 *   operation-level `x-devview.delayMs` extension. Overrides [ApiSpec.delayMs] when present.
 * @see ApiSpec
 * @see com.worldline.devview.networkmock.core.repository.RequestMatcher
 */
@Immutable
@Serializable
public data class Operation(
    val operationId: String,
    val name: String,
    val path: String,
    val method: String,
    val queryParameters: Map<String, String>? = null,
    val delayMs: Long? = null
)

/**
 * A stable, value-type identifier for the pair `(specId, operationId)`.
 *
 * Used everywhere these two identifiers travel together — as a map key, as a lookup token,
 * and as the carrier inside [MockMatch] and [OperationDescriptor].
 *
 * @property specId The [ApiSpec.id] (e.g. `"sample-api"`)
 * @property operationId The [Operation.operationId] (e.g. `"getUser"`)
 * @see MockMatch
 * @see OperationDescriptor
 * @see com.worldline.devview.networkmock.core.model.NetworkMockState
 */
@Immutable
@Serializable
public data class OperationKey(val specId: String, val operationId: String) {
    /**
     * The canonical `"{specId}-{operationId}"` string used as a DataStore preference key
     * suffix and as the key in
     * [com.worldline.devview.networkmock.core.model.NetworkMockState.operationStates].
     */
    public val compositeKey: String get() = "$specId-$operationId"

    public companion object
}

/**
 * A matched operation for an incoming HTTP request, returned by
 * [com.worldline.devview.networkmock.core.repository.MockConfigRepository.findMatchingMock].
 *
 * @property key The [OperationKey] carrying the matched spec and operation identifiers
 * @property config The matched [Operation]
 * @property delayMs The resolved response delay for this match — [Operation.delayMs] falling
 *   back to the owning [ApiSpec.delayMs], or `null` if neither declares one
 * @see OperationKey
 * @see Operation
 */
@Immutable
public data class MockMatch(
    val key: OperationKey,
    val config: Operation,
    val delayMs: Long? = null
) {
    /** The [ApiSpec.id] of the matched spec. Convenience accessor for [OperationKey.specId]. */
    public val specId: String get() = key.specId

    /** The [Operation.operationId] of the match. Convenience accessor for [OperationKey.operationId]. */
    public val operationId: String get() = key.operationId
}

/**
 * The static descriptor for an available operation and its discovered mock responses.
 *
 * This combines an [Operation] with the [MockResponse] variants declared for it, giving the
 * UI layer a complete, immutable view of an operation's mocking capabilities. Runtime
 * selection state is intentionally excluded — see
 * [com.worldline.devview.networkmock.core.model.OperationMockState].
 *
 * @property key The [OperationKey] uniquely identifying this operation within its spec
 * @property config The matched [Operation]
 * @property availableResponses The response variants declared for this operation, one per
 *   `(statusCode, exampleName)` pair found in the spec's `responses.<code>.content.*.examples`
 * @see MockResponse
 * @see OperationKey
 * @see Operation
 */
@Immutable
@Serializable
public data class OperationDescriptor(
    val key: OperationKey,
    val config: Operation,
    val availableResponses: List<MockResponse>
) {
    /** The [ApiSpec.id] this operation belongs to. Convenience accessor for [OperationKey.specId]. */
    public val specId: String get() = key.specId

    /** The [Operation.operationId] for this operation. Convenience accessor for [OperationKey.operationId]. */
    public val operationId: String get() = key.operationId

    public companion object
}
