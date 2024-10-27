package org.cyoda.example.hello

import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse
import org.cyoda.example.hello.integration.CyodaHttpClient

interface Processor {

    val name:String
    suspend fun process(client: CyodaHttpClient, request: EntityProcessorCalculationRequest): EntityProcessorCalculationResponse
}