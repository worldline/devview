package com.worldline.devview.sample.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

public class SampleApi(private val client: HttpClient = httpClient) {
    public suspend fun getKtorDocs(): String {
        val response = client.get(urlString = "https://ktor.io/docs")
        return response.bodyAsText()
    }

    /**
     * Get user from JSONPlaceholder API.
     * This endpoint is configured in the Network Mock test files.
     * You can mock this response through DevView > Network Mock.
     */
    public suspend fun getUser(userId: Int): String {
        val response = client.get(urlString = "https://jsonplaceholder.typicode.com/users/$userId")
        return response.bodyAsText()
    }
}
