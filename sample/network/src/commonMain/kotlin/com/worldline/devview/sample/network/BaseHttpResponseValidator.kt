package com.worldline.devview.sample.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.call.save
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.charsets.MalformedInputException

public val ExpectSuccessAttributeKey: AttributeKey<Boolean> = AttributeKey<Boolean>(
    name = "ExpectSuccessAttributeKey"
)
private val ValidateMark = AttributeKey<Unit>(name = "ValidateMark")
private val LOGGER = KtorSimpleLogger(name = "com.aruba.ipago.data.network.HttpClient")
private const val BODY_FAILED_DECODING: String = "<body failed decoding>"

public fun HttpClientConfig<*>.baseHttpResponseValidator() {
    install(plugin = HttpCallValidator) {
        validateResponse { response ->
            val expectSuccess = response.call.attributes[ExpectSuccessAttributeKey]
            if (!expectSuccess) {
                LOGGER.trace(
                    "Skipping default response validation for ${response.call.request.url}"
                )
                return@validateResponse
            }

            val statusCode = response.status.value
            val originCall = response.call
            if (statusCode < 300 || originCall.attributes.contains(key = ValidateMark)) {
                return@validateResponse
            }

            val exceptionCall = originCall.save().apply {
                attributes.put(key = ValidateMark, value = Unit)
            }

            val exceptionResponse = exceptionCall.response
            val exceptionResponseText = try {
                exceptionResponse.bodyAsText()
            } catch (_: MalformedInputException) {
                BODY_FAILED_DECODING
            }

            if (!response.status.isSuccess()) {
                throw NetworkException(
                    code = statusCode,
                    requestUrl = response.request.url.encodedPath,
                    responseBody = exceptionResponseText
                )
            }
        }
    }
}

public class NetworkException(
    public val code: Int,
    public val requestUrl: String,
    public val responseBody: String
) : Exception(
    "Network request failed with status code $code for url $requestUrl. Response body: $responseBody"
)
