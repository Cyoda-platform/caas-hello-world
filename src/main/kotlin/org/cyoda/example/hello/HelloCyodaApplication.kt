package org.cyoda.example.hello

import org.cyoda.example.hello.config.ClientConnectionProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

/**
 * Before running the client, you need to provide the [ClientConnectionProperties]
 */
@SpringBootApplication(exclude=[DataSourceAutoConfiguration::class])
@EnableConfigurationProperties(ClientConnectionProperties::class)
class HelloCyodaApplication

fun main(args: Array<String>) {
    runApplication<HelloCyodaApplication>(*args)
}
