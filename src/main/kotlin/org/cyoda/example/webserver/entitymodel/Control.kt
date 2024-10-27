package org.cyoda.example.webserver.entitymodel

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.coroutines.runBlocking
import org.cyoda.example.hello.config.ClientConnectionProperties
import org.cyoda.example.webserver.plugins.UserSession
import kotlin.text.toInt

fun controlModelStatus(
    client: HttpClient,
    modelName: String,
    modelVersion: Int,
    action: String,
    connProps: ClientConnectionProperties,
    session: UserSession
) =
    runBlocking {
        client.put("${connProps.apiUrl}/treeNode/model/$modelName/$modelVersion/$action") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            contentType(ContentType.Application.Json)
        }
    }


fun Routing.lockUnlockModelRoute(
    client: HttpClient,
    connProps: ClientConnectionProperties
) {
    post("/lock-unlock-model") {
        val parameters = call.receiveParameters()
        val modelName =
            parameters["modelName"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing modelName")
        val modelVersion =
            parameters["modelVersion"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing modelVersion")
        val action = parameters["action"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing action")

        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/")

        try {
            when (action) {
                "LOCK" -> {
                    controlModelStatus(client, modelName, modelVersion.toInt(), "lock", connProps, session)
                }

                "UNLOCK" -> {
                    controlModelStatus(client, modelName, modelVersion.toInt(), "unlock", connProps, session)
                }

                else -> {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid action")
                }
            }

            // After locking/unlocking, reload the entity models table
            call.respondRedirect("/")
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Failed to update model state: ${e.message}")
        }
    }
}
