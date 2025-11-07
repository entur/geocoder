package no.entur.geocoder.proxy.v3

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.entur.geocoder.proxy.ErrorHandler
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonReverseRequest
import org.slf4j.LoggerFactory

object V3Api {
    suspend fun RoutingContext.autocompleteRequest(photonBaseUrl: String, client: HttpClient) {
        val params =
            try {
                V3AutocompleteParams.fromRequest(call.request)
            } catch (e: Exception) {
                logger.error("Invalid parameters for V3 autocomplete: ${e.message}")
                val error = ErrorHandler.handleError(e, "Autocomplete")
                call.respondText(
                    ErrorHandler.toJson(error),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.fromValue(error.statusCode),
                )
                return
            }

        val photonRequest = PhotonAutocompleteRequest.from(params)
        val url = "$photonBaseUrl/api"
        logger.debug("V3 autocomplete request to $url with query='${photonRequest.query}'")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("q", photonRequest.query)
                        parameter("limit", photonRequest.limit.toString())
                        parameter("lang", photonRequest.language)

                        if (photonRequest.includes.isNotEmpty()) {
                            parameter("include", photonRequest.includes.joinToString(","))
                        }
                    }.bodyAsText()

            val photonResult = PhotonResult.parse(photonResponse)
            val json = V3ResultTransformer.parseAndTransform(photonResult, params)

            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error in V3 autocomplete: ${e.message}", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    suspend fun RoutingContext.reverseRequest(photonBaseUrl: String, client: HttpClient) {
        val params =
            try {
                V3ReverseParams.fromRequest(call.request)
            } catch (e: Exception) {
                logger.error("Invalid parameters for V3 reverse: ${e.message}")
                val error = ErrorHandler.handleError(e, "Reverse geocoding")
                call.respondText(
                    ErrorHandler.toJson(error),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.fromValue(error.statusCode),
                )
                return
            }

        val photonRequest = PhotonReverseRequest.from(params)
        val url = "$photonBaseUrl/reverse"
        logger.debug("V3 reverse geocoding request to $url at (${photonRequest.latitude}, ${photonRequest.longitude})")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("lat", photonRequest.latitude)
                        parameter("lon", photonRequest.longitude)
                        parameter("lang", photonRequest.language)
                        photonRequest.radius?.let { parameter("radius", it) }
                        parameter("limit", photonRequest.limit.toString())

                        if (photonRequest.includes.isNotEmpty()) {
                            parameter("include", photonRequest.includes.joinToString(","))
                        }
                        if (photonRequest.excludes.isNotEmpty()) {
                            parameter("exclude", photonRequest.excludes.joinToString(","))
                        }
                    }.bodyAsText()

            val photonResult = PhotonResult.parse(photonResponse)
            val json = V3ResultTransformer.parseAndTransform(photonResult, params)

            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error in V3 reverse geocoding: ${e.message}", e)
            val error = ErrorHandler.handleError(e, "Reverse geocoding")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    private val logger = LoggerFactory.getLogger(V3Api::class.java)
}
