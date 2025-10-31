package no.entur.geocoder.proxy.pelias

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

object PeliasApi {
    suspend fun RoutingContext.peliasAutocompleteRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: PeliasResultTransformer,
    ) {
        val peliasParams =
            try {
                PeliasAutocompleteParams.fromRequest(call.request)
            } catch (e: Exception) {
                logger.error("Invalid parameters for Pelias autocomplete: ${e.message}")
                val error = ErrorHandler.handleError(e, "Autocomplete")
                call.respondText(
                    ErrorHandler.toJson(error),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.fromValue(error.statusCode),
                )
                return
            }

        val photonRequest = PhotonAutocompleteRequest.from(peliasParams)
        val url = "$photonBaseUrl/api"
        logger.info("Proxying /v2/autocomplete to $url with text='${photonRequest.query}'")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("q", photonRequest.query)
                        parameter("limit", photonRequest.limit.toString())
                        parameter("lang", photonRequest.language)

                        if (photonRequest.zoom != null) {
                            parameter("zoom", photonRequest.zoom)
                        }
                        if (photonRequest.includes.isNotEmpty()) {
                            parameter("include", photonRequest.includes.joinToString(","))
                        }
                        if (photonRequest.excludes.isNotEmpty()) {
                            parameter("exclude", photonRequest.excludes.joinToString(","))
                        }
                        if (photonRequest.lat != null && photonRequest.lon != null) {
                            parameter("lat", photonRequest.lat)
                            parameter("lon", photonRequest.lon)
                        }
                    }.bodyAsText()

            val photonResult = PhotonResult.parse(photonResponse)
            val json = transformer.parseAndTransform(photonResult, peliasParams.focus)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error proxying to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    suspend fun RoutingContext.peliasReverseRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: PeliasResultTransformer,
    ) {
        val peliasParams =
            try {
                PeliasReverseParams.fromRequest(call.request)
            } catch (e: Exception) {
                logger.error("Invalid parameters for Pelias reverse: ${e.message}")
                val error = ErrorHandler.handleError(e, "Reverse geocoding")
                call.respondText(
                    ErrorHandler.toJson(error),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.fromValue(error.statusCode),
                )
                return
            }

        val photonRequest = PhotonReverseRequest.from(peliasParams)
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
                        parameter("exclude", photonRequest.exclude)
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
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    suspend fun RoutingContext.peliasPlaceRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: PeliasResultTransformer,
    ) {
        val peliasParams =
            try {
                PeliasPlaceParams.fromRequest(call.request)
            } catch (e: Exception) {
                logger.error("Invalid parameters for Pelias place: ${e.message}")
                val error = ErrorHandler.handleError(e, "Place")
                call.respondText(
                    ErrorHandler.toJson(error),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.fromValue(error.statusCode),
                )
                return
            }

        val photonRequests = PhotonAutocompleteRequest.from(peliasParams)
        val url = "$photonBaseUrl/api"
        logger.info("Proxying /v2/autocomplete to $url with ids='${peliasParams.ids}'")

        try {
            val photonResults = photonRequests.map { photonRequest ->
                val res = client.get(url) {
                    parameter("q", photonRequest.query)
                    parameter("limit", "1")
                }.bodyAsText()
                PhotonResult.parse(res)
            }
            val photonResult = PhotonResult(
                type = "FeatureCollection",
                features = photonResults.map { it.features.first() },
            )
            val json = transformer.parseAndTransform(photonResult, null)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error proxying to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    private val logger = LoggerFactory.getLogger(PeliasApi::class.java)
}
