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
import java.io.IOException

object ErrorHandler {
    fun handleError(e: Exception, operation: String): PeliasError =
        when (e) {
            is JsonParseException, is JsonMappingException -> {
                toError(
                    "Invalid response from backend. The geocoding service returned an unexpected response. Please check your parameters",
                    BadGateway,
                )
            }

            is ClientRequestException -> {
                toError("Bad request: ${e.response.status.description}", BadRequest)
            }

            is ServerResponseException -> {
                toError("Backend service error. The geocoding backend is experiencing issues", BadGateway)
            }

            is IOException -> {
                toError("Connection failed. Unable to connect to geocoding service", ServiceUnavailable)
            }

            is IllegalArgumentException -> {
                toError("Invalid parameters. ${e.message ?: "One or more parameters are invalid"}", BadRequest)
            }

            else -> {
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
