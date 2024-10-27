package org.cyoda.example.webserver.entitymodel

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.cyoda.example.hello.config.ClientConnectionProperties
import org.cyoda.example.hello.serializations.UuidKSerializer
import org.cyoda.example.webserver.plugins.UserSession
import java.util.UUID

@Serializable
data class EntityModelDto(
    @Serializable(with = UuidKSerializer::class) val id: UUID,
    val modelName: String,
    val modelVersion: Int,
    val currentState: String,
)


fun fetchEntityModels(client: HttpClient, connProps: ClientConnectionProperties, session: UserSession): List<EntityModelDto> =
    runBlocking {
        client.get("${connProps.apiUrl}/treeNode/model/") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            contentType(ContentType.Application.Json)
        }.body()
    }