package com.worldline.devview.networkmock.utils

/**
 * Parses the HTTP status code from a response file name.
 *
 * This is the single source of truth for status-code extraction from mock
 * response file names. It is used by both
 * [fromFile][com.worldline.devview.networkmock.model.MockResponse.fromFile] when building
 * a [MockResponse][com.worldline.devview.networkmock.model.MockResponse] from disk, and by
 * [Mock.statusCode][com.worldline.devview.networkmock.model.EndpointMockState.Mock.statusCode]
 * when a quick status-code lookup is needed without loading the full response.
 *
 * ## File name format
 * Expected format: `{endpointId}-{statusCode}[-{suffix}].json`
 *
 * The status code is located by searching from the right for a `-` followed by
 * exactly three digits, making this robust to endpoint IDs that themselves
 * contain hyphens.
 *
 * ## Examples
 * ```kotlin
 * "getUser-200.json".parseStatusCode()        // 200
 * "getUser-404-simple.json".parseStatusCode() // 404
 * "get-user-500.json".parseStatusCode()       // 500
 * "malformed.json".parseStatusCode()          // null
 * ```
 *
 * @receiver The response file name to parse (e.g. `"getUser-200.json"`)
 * @return The 3-digit HTTP status code, or `null` if the file name does not
 *   match the expected `{endpointId}-{statusCode}[-{suffix}].json` format
 */
internal fun String.parseStatusCode(): Int? {
    val nameWithoutExtension = removeSuffix(suffix = ".json")
    return Regex(pattern = """-(\d{3})(-.+)?$""")
        .find(input = nameWithoutExtension)
        ?.groupValues
        ?.get(index = 1)
        ?.toIntOrNull()
}
