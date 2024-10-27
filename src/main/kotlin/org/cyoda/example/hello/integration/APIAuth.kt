package org.cyoda.example.hello.integration

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

suspend fun HttpClient.getAccessToken(refreshUrl: String, refreshToken:String):String {
    val response = this.get(refreshUrl) {
        header(HttpHeaders.Authorization, "Bearer $refreshToken")
        contentType(ContentType.Application.Json)
    }

    if (response.status == HttpStatusCode.OK) {
        val tokens = response.body<Map<String, String>>()
        return tokens["token"] ?: error("token attribute missing in response: $tokens")
    } else {
        throw IllegalStateException("Failed to refresh tokens: ${response.status}")
    }
}