package com.worldline.devview.sample.network

import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

public fun HttpClientConfig<*>.baseHttpLogging() {
    install(plugin = Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                KermitLogger.v(messageString = message, throwable = null, tag = "HTTP Client")
            }
        }
        level = LogLevel.ALL
    }
}
