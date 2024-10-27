package org.cyoda.example.webserver.connect

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.cyoda.example.hello.config.ClientConnectionProperties
import org.cyoda.example.webserver.plugins.LoginRequest
import org.cyoda.example.webserver.plugins.LoginResponse
import org.cyoda.example.webserver.plugins.UserSession
import java.time.Instant

fun Routing.loginRoute(
    client: HttpClient,
    connProps: ClientConnectionProperties
) {
    post("/login") {
        val loginRequest = call.receiveParameters().let {
            LoginRequest(
                username = it["username"] ?: "",
                password = it["password"] ?: ""
            )
        }

        try {
            val response: LoginResponse = client.post(connProps.loginUrl) {
                contentType(ContentType.Application.Json)
                header("X-Requested-With", "XMLHttpRequest")
                setBody(loginRequest)
            }.body()

            // Cache the access token in the session
            call.sessions.set(
                UserSession(
                    response.token,
                    response.refreshToken,
                    Instant.now().toEpochMilli(),
                    connProps.refreshUrl
                )
            )

            // Redirect to the landing page to show the registration form
            call.respondRedirect("/")
        } catch (e: Exception) {
            call.respondHtml(HttpStatusCode.Unauthorized) {
                body {
                    p { +"Login failed: ${e.message}" }
                    a(href = "/") { +"Go back" }
                }
            }
        }
    }
}

fun Routing.logoutRoute() {
    post("/logout") {
        // Clear the user session
        call.sessions.clear<UserSession>()
        // Redirect to the landing page after logout
        call.respondRedirect("/")
    }
}


