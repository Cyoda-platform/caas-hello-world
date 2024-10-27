package org.cyoda.example.webserver.entitymodel

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.cyoda.example.hello.config.ClientConnectionProperties
import org.cyoda.example.webserver.connect.refreshIfNeeded
import org.cyoda.example.webserver.plugins.UserSession
import kotlin.time.Duration.Companion.minutes

fun Routing.registerModelRoute(
    client: HttpClient,
    connProps: ClientConnectionProperties
) {
    post("/register-entity-model") {

        val currentSession = call.sessions.get<UserSession>()

        if (currentSession == null) {
            call.respondRedirect("/")
            return@post
        }

        val refreshed = currentSession.refreshIfNeeded(2.minutes, client)

        val session = if (refreshed != null) {
            call.sessions.set(refreshed)
            refreshed
        } else currentSession


        // Extract parameters from the form
        val parameters = call.receiveParameters()
        val modelName = parameters["modelName"] ?: ""
        val modelVersion = parameters["modelVersion"] ?: ""
        val entityModelJson = parameters["entityModelJson"] ?: ""

        // Ensure that modelName and modelVersion are not empty
        if (modelName.isBlank() || modelVersion.isBlank()) {
            call.respondHtml(HttpStatusCode.BadRequest) {
                body {
                    p { +"Model Name and Model Version are required." }
                    a(href = "/") { +"Go back" }
                }
            }
            return@post
        }

        try {
            // Construct the API endpoint with modelName and modelVersion in the path
            val url = "${connProps.apiUrl}/treeNode/model/import/JSON/SAMPLE_DATA/$modelName/$modelVersion"

            val accessToken = session.accessToken

            client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody(entityModelJson)
            }

            call.respondRedirect("/")

        } catch (e: Exception) {
            call.respondHtml(HttpStatusCode.BadRequest) {
                body {
                    p { +"Failed to register entity model: ${e.message}" }
                    a(href = "/") { +"Go back" }
                }
            }
        }
    }
}
