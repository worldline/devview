package com.worldline.devview.networkmock.core.model

public enum class StatusCodeFamily {
    INFORMATIONAL,
    SUCCESSFUL,
    REDIRECTION,
    CLIENT_ERROR,
    SERVER_ERROR,
    UNKNOWN;

    public val displayName: String
        get() = when (this) {
            INFORMATIONAL -> "Informational"
            SUCCESSFUL -> "Successful"
            REDIRECTION -> "Redirection"
            CLIENT_ERROR -> "Client Error"
            SERVER_ERROR -> "Server Error"
            UNKNOWN -> "Unknown"
        }

    public companion object {
        public fun fromStatusCode(statusCode: Int): StatusCodeFamily = when (statusCode) {
            in 100..199 -> INFORMATIONAL
            in 200..299 -> SUCCESSFUL
            in 300..399 -> REDIRECTION
            in 400..499 -> CLIENT_ERROR
            in 500..599 -> SERVER_ERROR
            else -> UNKNOWN
        }
    }
}
