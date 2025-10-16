package no.entur.geocoder.proxy

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import org.slf4j.LoggerFactory

object PeliasApi {

    suspend fun RoutingContext.autocompleteRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: PeliasResultTransformer
    ) {
        val params = try {
            PeliasAutocompleteParams.fromRequest(call.request)
        } catch (e: Exception) {
            logger.error("Invalid parameters for Pelias autocomplete: ${e.message}")
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode)
            )
            return
        }

        val photonRequest = PhotonAutocompleteRequest.from(params)
        val url = "$photonBaseUrl/api"
        logger.info("Proxying /v2/autocomplete to $url with text='${photonRequest.query}'")

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
            val json = transformer.parseAndTransform(photonResult)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error proxying to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode)
            )
        }
    }

    suspend fun RoutingContext.reverseRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: PeliasResultTransformer
    ) {
        val params = try {
            PeliasReverseParams.fromRequest(call.request)
        } catch (e: Exception) {
            logger.error("Invalid parameters for Pelias reverse: ${e.message}")
            val error = ErrorHandler.handleError(e, "Reverse geocoding")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode)
            )
            return
        }

        val photonRequest = PhotonReverseRequest.from(params)
        val url = "$photonBaseUrl/reverse"
        logger.info("Proxying /v2/reverse to $url at (${photonRequest.latitude}, ${photonRequest.longitude})")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("lat", photonRequest.latitude)
                        parameter("lon", photonRequest.longitude)
                        parameter("lang", photonRequest.language)
                        photonRequest.radius?.let { parameter("radius", it) }
                        parameter("limit", photonRequest.limit.toString())
                    }.bodyAsText()

            val photonResult = PhotonResult.parse(photonResponse)
            val json = transformer.parseAndTransform(photonResult)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error proxying to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Reverse geocoding")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode)
            )
        }
    }

    private val logger = LoggerFactory.getLogger(PeliasApi::class.java)
}

