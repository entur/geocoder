package no.entur.geocoder.proxy

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import java.io.IOException

data class ApiError(
    val error: String,
    val message: String,
    val statusCode: Int
)

object ErrorHandler {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun handleError(e: Exception, operation: String): ApiError {
        return when (e) {
            is JsonParseException, is JsonMappingException -> {
                ApiError(
                    error = "Invalid response from backend",
                    message = "The geocoding service returned an unexpected response. Please check your parameters.",
                    statusCode = HttpStatusCode.BadGateway.value
                )
            }
            is ClientRequestException -> {
                ApiError(
                    error = "Invalid request",
                    message = "Bad request: ${e.response.status.description}",
                    statusCode = HttpStatusCode.BadRequest.value
                )
            }
            is ServerResponseException -> {
                ApiError(
                    error = "Backend service error",
                    message = "The geocoding backend is experiencing issues",
                    statusCode = HttpStatusCode.BadGateway.value
                )
            }
            is IOException -> {
                ApiError(
                    error = "Connection failed",
                    message = "Unable to connect to geocoding service",
                    statusCode = HttpStatusCode.ServiceUnavailable.value
                )
            }
            is IllegalArgumentException -> {
                ApiError(
                    error = "Invalid parameters",
                    message = e.message ?: "One or more parameters are invalid",
                    statusCode = HttpStatusCode.BadRequest.value
                )
            }
            else -> {
                ApiError(
                    error = "$operation failed",
                    message = "An unexpected error occurred",
                    statusCode = HttpStatusCode.InternalServerError.value
                )
            }
        }
    }

    fun toJson(error: ApiError): String {
        return mapper.writeValueAsString(error)
    }
}

