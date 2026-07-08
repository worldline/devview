package com.worldline.devview.networkmock.core.utils

/**
 * Parses the HTTP status code from a response file name.
 *
 * This is the single source of truth for status-code extraction from mock
 * response file names. It is used by both
 * [fromFile][com.worldline.devview.networkmock.core.model.MockResponse.Companion.fromFile] when building
 * a [MockResponse][com.worldline.devview.networkmock.core.model.MockResponse] from disk, and by
 * [Mock.statusCode][com.worldline.devview.networkmock.core.model.EndpointMockState.Mock.statusCode]
 * when a quick status-code lookup is needed without loading the full response.
 *
 * ## File name format
 * Expected format: `{endpointId}-{statusCode}[-{suffix}].json`
 *
 * The status code is located by searching from the right for a `-` followed by
 * exactly three digits, making this robust to endpoint IDs that themselves
 * contain hyphens.
 *
 * ## Extension handling
 * The `.json` extension is stripped before matching via [String.removeSuffix]. If the
 * input has no `.json` extension, `removeSuffix` is a no-op and the regex is applied
 * to the string as-is. A file name without a `.json` extension is therefore still
 * parsed correctly as long as it otherwise follows the naming convention.
 *
 * ## Examples
 * ```kotlin
 * "getUser-200.json".parseStatusCode()        // 200
 * "getUser-404-simple.json".parseStatusCode() // 404
 * "get-user-500.json".parseStatusCode()       // 500
 * "getUser-200".parseStatusCode()             // 200  (no extension — still valid)
 * "getUser.json".parseStatusCode()            // null (no three-digit code)
 * "getUser-json.json".parseStatusCode()       // null (non-numeric code segment)
 * "getUser-20.json".parseStatusCode()         // null (only two digits)
 * "".parseStatusCode()                        // null (empty string)
 * ```
 *
 * @receiver The response file name to parse (e.g. `"getUser-200.json"`)
 * @return The 3-digit HTTP status code, or `null` if the file name does not
 *   contain a `-{3 digits}` segment (with or without a `.json` extension)
 */
internal fun String.parseStatusCode(): Int? {
    val nameWithoutExtension = removeSuffix(suffix = ".json")
    return Regex(pattern = """-(\d{3})(-.+)?$""")
        .find(input = nameWithoutExtension)
        ?.groupValues
        ?.get(index = 1)
        ?.toIntOrNull()
}
