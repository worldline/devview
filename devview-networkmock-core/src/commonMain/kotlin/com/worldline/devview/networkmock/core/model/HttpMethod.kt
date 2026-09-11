package com.worldline.devview.networkmock.core.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * An HTTP method (verb), modeled after Ktor's own `io.ktor.http.HttpMethod`.
 *
 * `devview-networkmock-core` has no dependency on Ktor — only `devview-networkmock-ktor` does —
 * so this type exists to give [Operation.method] the same open, ordered shape Ktor's type has
 * (companion constants plus [DefaultMethods]) without leaking a Ktor dependency onto
 * `devview-networkmock`, the Compose UI module `-core` exists to decouple from the transport.
 *
 * A [JvmInline] wrapper over the raw method name — any value is representable, so a spec
 * declaring a non-standard verb still round-trips, it just won't appear in [DefaultMethods]'
 * canonical ordering.
 *
 * @property value The uppercase method name (e.g. `"GET"`).
 */
@JvmInline
@Serializable
public value class HttpMethod(public val value: String) {
    override fun toString(): String = value

    public companion object {
        public val Get: HttpMethod = HttpMethod(value = "GET")
        public val Post: HttpMethod = HttpMethod(value = "POST")
        public val Put: HttpMethod = HttpMethod(value = "PUT")
        public val Patch: HttpMethod = HttpMethod(value = "PATCH")
        public val Delete: HttpMethod = HttpMethod(value = "DELETE")
        public val Head: HttpMethod = HttpMethod(value = "HEAD")
        public val Options: HttpMethod = HttpMethod(value = "OPTIONS")

        /**
         * Canonical display order — also exactly the fixed sibling keys OpenAPI's `paths.<path>`
         * declares (see `PathItemObject.operationsByMethod`).
         */
        public val DefaultMethods: List<HttpMethod> =
            listOf(Get, Post, Put, Patch, Delete, Head, Options)
    }
}
