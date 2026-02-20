package com.worldline.devview.networkmock.repository

/**
 * Utility object for matching HTTP request paths against configured endpoint paths.
 *
 * This matcher supports path parameters using curly braces notation (e.g., `/users/{userId}`)
 * and performs intelligent matching to determine if an incoming request matches a configured
 * endpoint pattern. This is a core component of the network mock feature's request interception
 * and is intentionally agnostic of any specific HTTP client implementation.
 *
 * ## Path Parameter Syntax
 * Path parameters are defined using curly braces:
 * - `/api/users/{userId}` matches `/api/users/123`, `/api/users/abc`, etc.
 * - `/api/posts/{postId}/comments/{commentId}` matches any values for both IDs
 * - `/api/v1/products/{productId}` matches `/api/v1/products/prod-456`
 *
 * ## Matching Rules
 * 1. Paths must have the same number of segments (separated by `/`)
 * 2. Non-parameter segments must match exactly
 * 3. Parameter segments (in curly braces) match any value
 * 4. Matching is case-sensitive for non-parameter segments
 * 5. Empty segments are ignored (leading/trailing slashes handled automatically)
 *
 * ## Usage
 * ```kotlin
 * val configPath = "/api/v1/user/{userId}"
 * val requestPath = "/api/v1/user/123"
 *
 * if (RequestMatcher.matchesPath(configPath, requestPath)) {
 *     // Request matches the configured endpoint
 *     // Proceed with mock response lookup
 * }
 * ```
 *
 * ## Examples
 *
 * ### Simple Paths (No Parameters)
 * ```kotlin
 * RequestMatcher.matchesPath("/api/users", "/api/users")        // true
 * RequestMatcher.matchesPath("/api/users", "/api/posts")        // false
 * RequestMatcher.matchesPath("/api/users", "/api/users/123")    // false (different segment count)
 * ```
 *
 * ### Single Parameter
 * ```kotlin
 * RequestMatcher.matchesPath("/api/users/{id}", "/api/users/123")     // true
 * RequestMatcher.matchesPath("/api/users/{id}", "/api/users/abc")     // true
 * RequestMatcher.matchesPath("/api/users/{id}", "/api/users/user-1")  // true
 * RequestMatcher.matchesPath("/api/users/{id}", "/api/posts/123")     // false
 * ```
 *
 * ### Multiple Parameters
 * ```kotlin
 * RequestMatcher.matchesPath(
 *     "/api/posts/{postId}/comments/{commentId}",
 *     "/api/posts/123/comments/456"
 * ) // true
 *
 * RequestMatcher.matchesPath(
 *     "/api/posts/{postId}/comments/{commentId}",
 *     "/api/posts/abc/comments/xyz"
 * ) // true
 *
 * RequestMatcher.matchesPath(
 *     "/api/posts/{postId}/comments/{commentId}",
 *     "/api/posts/123/likes/456"
 * ) // false (comments != likes)
 * ```
 *
 * ### Leading/Trailing Slashes
 * ```kotlin
 * RequestMatcher.matchesPath("/api/users", "api/users")      // true (leading slash ignored)
 * RequestMatcher.matchesPath("/api/users/", "/api/users")    // true (trailing slash ignored)
 * RequestMatcher.matchesPath("api/users", "/api/users/")     // true (both ignored)
 * ```
 *
 * ## Future Enhancements
 * For post-MVP iterations, the following features could be added:
 * - Parameter constraints (e.g., `{userId:\d+}` for numeric IDs only)
 * - Wildcard matching
 * - Regular expression support for complex patterns
 * - Query parameter matching
 *
 * @see MockConfigRepository
 */
public object RequestMatcher {
    /**
     * Checks if a request path matches a configured endpoint path pattern.
     *
     * This method splits both paths into segments and compares them one by one.
     * Segments enclosed in curly braces (`{` and `}`) in the config path are
     * treated as parameters and match any value in the request path.
     *
     * ## Algorithm
     * 1. Split both paths by `/` separator
     * 2. Filter out empty segments (from leading/trailing slashes)
     * 3. Check if segment counts match (if not, no match)
     * 4. Compare each segment pair:
     *    - If config segment is a parameter (`{...}`), it matches any request value
     *    - Otherwise, require exact string match (case-sensitive)
     * 5. Return true only if all segments match
     *
     * ## Performance
     * - Time Complexity: O(n) where n is the number of path segments
     * - Space Complexity: O(n) for storing segment lists
     * - Typically very fast as API paths are short (usually < 10 segments)
     *
     * @param configPath The configured endpoint path pattern (may contain `{parameters}`)
     * @param requestPath The actual incoming request path
     * @return `true` if the request path matches the pattern, `false` otherwise
     */
    public fun matchesPath(configPath: String, requestPath: String): Boolean {
        // Split paths by '/' and filter out empty segments
        val configSegments = configPath.split("/").filter { it.isNotEmpty() }
        val requestSegments = requestPath.split("/").filter { it.isNotEmpty() }

        // Paths must have the same number of segments to match
        if (configSegments.size != requestSegments.size) {
            return false
        }

        // Compare each segment
        return configSegments.zip(other = requestSegments).all { (config, request) ->
            isParameterSegment(segment = config) || config == request
        }
    }

    /**
     * Checks if a path segment is a parameter (enclosed in curly braces).
     *
     * A segment is considered a parameter if it:
     * - Starts with `{`
     * - Ends with `}`
     * - Has at least one character between the braces
     *
     * ## Examples
     * ```kotlin
     * isParameterSegment("{userId}")      // true
     * isParameterSegment("{id}")          // true
     * isParameterSegment("{post-id}")     // true
     * isParameterSegment("userId")        // false (no braces)
     * isParameterSegment("{}")            // false (empty)
     * isParameterSegment("{userId")       // false (missing closing brace)
     * isParameterSegment("userId}")       // false (missing opening brace)
     * ```
     *
     * @param segment The path segment to check
     * @return `true` if the segment is a parameter, `false` otherwise
     */
    @Suppress("CommentOverPrivateFunction")
    private fun isParameterSegment(segment: String): Boolean =
        segment.startsWith(prefix = "{") && segment.endsWith(suffix = "}") && segment.length > 2

    /**
     * Extracts the parameter name from a parameter segment.
     *
     * This is a utility method for potential future use (e.g., parameter extraction,
     * validation, or constraint checking). It removes the curly braces from a
     * parameter segment to get the parameter name.
     *
     * ## Examples
     * ```kotlin
     * extractParameterName("{userId}")    // "userId"
     * extractParameterName("{id}")        // "id"
     * extractParameterName("{post-id}")   // "post-id"
     * ```
     *
     * @param segment The parameter segment (must start with `{` and end with `}`)
     * @return The parameter name without braces, or the original segment if not a parameter
     */
    @Suppress("CommentOverPrivateFunction", "UnusedPrivateFunction")
    private fun extractParameterName(segment: String): String = if (isParameterSegment(
            segment = segment
        )
    ) {
        segment.substring(startIndex = 1, endIndex = segment.length - 1)
    } else {
        segment
    }
}
