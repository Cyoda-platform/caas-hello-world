package org.cyoda.example.webserver.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.runBlocking
import org.cyoda.example.hello.integration.HttpClientSupplier
import org.cyoda.example.hello.integration.TokenManager
import org.cyoda.example.simple.integration.CyodaCalculationMemberClient
import org.cyoda.example.webserver.conf.loadClientConnectionProperties
import org.cyoda.example.webserver.conf.loadProcessors

fun Application.cyodaClientModule() {
    val processors = loadProcessors()
    val connProps = loadClientConnectionProperties()
    val objectMapper = ObjectMapper()
    val tokenManager = TokenManager()
    val clientSupplier = HttpClientSupplier(connProps)

    val cyodaHttpClient = runBlocking {
        clientSupplier.connect(
            tokenClient = tokenManager,
            username = connProps.clientId,
            password = connProps.clientSecret
        )
    }

    val calcClient = CyodaCalculationMemberClient(objectMapper, cyodaHttpClient, processors)

    monitor.subscribe(ApplicationStarted) {
        calcClient.joinAsCyodaMemberClient()
    }

    monitor.subscribe(ApplicationStopping) {
        calcClient.shutdownGrpcIntegration()
    }

    monitor.subscribe(ApplicationStopped) {
        // Release resources and unsubscribe from events
        monitor.unsubscribe(ApplicationStarted) {}
        monitor.unsubscribe(ApplicationStopped) {}
    }

}

