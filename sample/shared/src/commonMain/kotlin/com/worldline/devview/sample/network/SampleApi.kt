package com.worldline.devview.sample.network

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

public class SampleApi {
    private val client = httpClient

    public suspend fun getKtorDocs(): String {
        val response = client.get(urlString = "https://ktor.io/docs")
        return response.bodyAsText()
    }
}
