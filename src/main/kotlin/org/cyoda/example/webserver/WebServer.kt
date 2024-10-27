package org.cyoda.example.webserver

import io.ktor.server.application.*
import org.cyoda.example.webserver.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    cyodaClientModule()
    configureTemplating()
    configureMonitoring()
    configureSerialization()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
