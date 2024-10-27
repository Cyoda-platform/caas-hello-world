package org.cyoda.example.webserver.conf

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.cyoda.example.hello.Processor
import org.cyoda.example.hello.config.ClientConnectionProperties
import kotlin.Int
import kotlin.reflect.KMutableProperty1

fun Application.loadCookieConfig(): CookieConfig {
    val config = HoconApplicationConfig(ConfigFactory.load())
    return CookieConfig(
        cookieEncryptKey = config.property("ktor.session.cookie-encrypt-key").getString(),
        cookieSignKey = config.property("ktor.session.cookie-sign-key").getString()
    )
}

fun Application.loadProcessors(): Map<String, Processor> {
    val config = HoconApplicationConfig(ConfigFactory.load())
    config.property("app.processors")
    return mapOf()
}


const val CONN_PREFIX = "cyoda.connection"
fun Application.loadClientConnectionProperties(): ClientConnectionProperties {
    val config = HoconApplicationConfig(ConfigFactory.load())
    return ClientConnectionProperties().apply {
        apiUrl = config.propertyOrDefault(ClientConnectionProperties::apiUrl,"http://localhost:8082/api")

        loginUrl = config.propertyOrDefault(ClientConnectionProperties::loginUrl,"")
        refreshUrl = config.propertyOrDefault(ClientConnectionProperties::refreshUrl,"")

        clientId = config.propertyOrDefault(ClientConnectionProperties::clientId,"my-client-id")
        clientSecret = config.propertyOrDefault(ClientConnectionProperties::clientSecret,"my-client-secret")

        grpcServer = config.propertyOrDefault(ClientConnectionProperties::grpcServer,"localhost")
        grpcServerPort = config.propertyOrDefault(ClientConnectionProperties::grpcServerPort,443)
        grpcServerUseTls = config.propertyOrDefault(ClientConnectionProperties::grpcServerUseTls,true)
        setDefaults()
    }
}

fun String.camelCaseToHyphen(): String {
    return this.replace(Regex("([a-z])([A-Z])"), "$1-$2")
        .lowercase()
}

private fun <T> KMutableProperty1<ClientConnectionProperties, T>.toConfigPropertyName() = name.camelCaseToHyphen()

private inline fun <reified T> HoconApplicationConfig.propertyOrDefault(field: KMutableProperty1<ClientConnectionProperties, T>, default: T): T {
    val string = this.propertyOrNull("$CONN_PREFIX.${field.toConfigPropertyName()}")?.getString()
    return when (T::class) {
        String::class -> (string?.toString() ?: default)
        Boolean::class -> (string?.toBoolean() ?: default)
        Int::class -> (string?.toIntOrNull() ?: default)
        Long::class -> (string?.toLongOrNull() ?: default)
        Double::class -> (string?.toDoubleOrNull() ?: default)
        LocalDate::class -> (string?.let { LocalDate.parse(it) } ?: default)
        LocalTime::class -> (string?.let { LocalTime.parse(it) } ?: default)
        LocalDateTime::class -> (string?.let { LocalDateTime.parse(it) } ?: default)
        else -> throw UnsupportedOperationException("${T::class.qualifiedName} isn't supported!")
    } as T
}



