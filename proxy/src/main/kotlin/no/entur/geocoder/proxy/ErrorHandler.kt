package no.entur.geocoder.proxy

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadGateway
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import no.entur.geocoder.proxy.pelias.PeliasResult
import org.slf4j.LoggerFactory
import java.io.IOException

object ErrorHandler {
    private val logger = LoggerFactory.getLogger(ErrorHandler::class.java)

    fun handleError(e: Exception, operation: String): PeliasError =
        when (e) {
            is JsonParseException, is JsonMappingException -> {
                logger.error("JSON parsing error: ${e.message}")
                toError(
                    "Invalid response from backend. The geocoding service returned an unexpected response. Please check your parameters",
                    BadGateway,
                )
            }

            is ClientRequestException -> {
                logger.error("Client request error: ${e.response.status}")
                toError("Bad request: ${e.response.status.description}", BadRequest)
            }

            is ServerResponseException -> {
                logger.error("Server response error: ${e.response.status}")
                toError("Backend service error. The geocoding backend is experiencing issues", BadGateway)
            }

            is IOException -> {
                logger.error("I/O error: ${e.message}")
                toError("Connection failed. Unable to connect to geocoding service", ServiceUnavailable)
            }

            is IllegalArgumentException -> {
                logger.info("Invalid request parameters: ${e.message}")
                toError("Invalid parameters. ${e.message ?: "One or more parameters are invalid"}", BadRequest)
            }

            else -> {
                logger.error("Unexpected error: ${e.message}", e)
                toError("$operation failed. An unexpected error occurred", InternalServerError)
            }
        }

    private fun toError(error: String, status: HttpStatusCode): PeliasError =
        PeliasError(
            PeliasResult(
                geocoding =
                    PeliasResult.GeocodingMetadata(
                        errors = listOf(error),
                    ),
            ),
            status,
        )

    data class PeliasError(
        val result: PeliasResult,
        val status: HttpStatusCode,
    )
}
