package org.cyoda.example.hello.integration

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

@Component
class TokenManager() {

    private val tokenClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    lateinit var refreshToken: String

    fun getAccessTokenBlocking(refreshUrl: String):String = runBlocking {
        getAccessToken(refreshUrl)
    }

    suspend fun getAccessToken(refreshUrl: String) = tokenClient.getAccessToken(refreshUrl,refreshToken)

}