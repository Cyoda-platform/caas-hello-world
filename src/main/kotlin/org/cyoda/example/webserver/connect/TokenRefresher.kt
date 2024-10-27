package org.cyoda.example.webserver.connect

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.cyoda.example.hello.serializations.LocalDateTimeKSerializer
import org.cyoda.example.webserver.plugins.UserSession
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit


@Serializable
data class RefreshResponse(
    val token: String,
    @Serializable(with = LocalDateTimeKSerializer::class) val tokenExpiry: LocalDateTime
)


suspend fun UserSession.refreshIfNeeded(refreshWindow: Duration, client:HttpClient): UserSession? {
    val session = this
    val currentTime = Clock.systemUTC().instant()

    val needRefresh = currentTime.plus(refreshWindow.toLong(DurationUnit.SECONDS), ChronoUnit.SECONDS)
        .isAfter(Instant.ofEpochMilli(session.expiryTime))

    return if (needRefresh) {
        val refreshed = client.get(session.refreshUrl) {
            header(HttpHeaders.Authorization, "Bearer ${session.refreshToken}")
            contentType(ContentType.Application.Json)
        }.body<RefreshResponse>()

        UserSession(
            refreshed.token,
            session.refreshToken,
            refreshed.tokenExpiry.toInstant(ZoneOffset.UTC).toEpochMilli(),
            session.refreshUrl
        )
    } else null
}