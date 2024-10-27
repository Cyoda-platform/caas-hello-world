package org.cyoda.example.webserver.connect

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json

fun standardClient() = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000L
    }
    install(HttpRequestRetry)
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.NONE
    }
    install(ContentNegotiation) {
        json()
    }
    expectSuccess = true
}