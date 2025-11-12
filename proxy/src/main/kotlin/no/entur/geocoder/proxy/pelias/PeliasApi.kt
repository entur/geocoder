package no.entur.geocoder.proxy.pelias

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
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
        peliasParams: PeliasAutocompleteParams,
    ) {
        val photonRequest = PhotonAutocompleteRequest.from(peliasParams)
        val url = "$photonBaseUrl/api"
        logger.debug("Proxying /v2/autocomplete to $url with text='${photonRequest.query}'")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("q", photonRequest.query)
                        parameter("limit", photonRequest.limit)
                        parameter("lang", photonRequest.language)

                        if (photonRequest.zoom != null) {
                            parameter("zoom", photonRequest.zoom)
                        }
                        if (photonRequest.includes.isNotEmpty()) {
                            parameter("include", photonRequest.includes.joinToString(","))
                        }
                        photonRequest.excludes.forEach {
                            parameter("exclude", it)
                        }
                        if (photonRequest.lat != null && photonRequest.lon != null) {
                            parameter("lat", photonRequest.lat)
                            parameter("lon", photonRequest.lon)
                        }
                    }.bodyAsText()

            val photonResult = PhotonResult.parse(photonResponse)
            val peliasResult = PeliasResultTransformer.parseAndTransform(photonResult, peliasParams.focus?.lat, peliasParams.focus?.lon)
            call.respond(peliasResult)
        } catch (e: Exception) {
            logger.error("Error proxying $photonRequest to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = Json,
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    suspend fun RoutingContext.peliasReverseRequest(
        photonBaseUrl: String,
        client: HttpClient,
        peliasParams: PeliasReverseParams,
    ) {
        val photonRequest = PhotonReverseRequest.from(peliasParams)
        val url = "$photonBaseUrl/reverse"
        logger.debug("Proxying /v2/reverse to $url with lat=${photonRequest.latitude}, lon=${photonRequest.longitude}")

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
                        photonRequest.excludes.forEach {
                            parameter("exclude", it)
                        }
                    }.bodyAsText()

            val photonResult = PhotonResult.parse(photonResponse)
            val peliasResult = PeliasResultTransformer.parseAndTransform(photonResult, peliasParams.lat, peliasParams.lon)
            call.respond(peliasResult)
        } catch (e: Exception) {
            logger.error("Error proxying $photonRequest to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Reverse geocoding")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = Json,
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    suspend fun RoutingContext.peliasPlaceRequest(
        photonBaseUrl: String,
        client: HttpClient,
        peliasParams: PeliasPlaceParams,
    ) {
        val photonRequests = PhotonAutocompleteRequest.from(peliasParams)
        val url = "$photonBaseUrl/api"
        logger.debug("Proxying /v2/autocomplete to $url with text='${peliasParams.ids.joinToString(",")}'")

        try {
            val photonResults =
                photonRequests.map { photonRequest ->
                    val res =
                        client
                            .get(url) {
                                parameter("q", photonRequest.query)
                                parameter("limit", "1")
                            }.bodyAsText()
                    PhotonResult.parse(res)
                }
            val photonResult =
                PhotonResult(
                    type = "FeatureCollection",
                    features = photonResults.mapNotNull { it.features.firstOrNull() },
                )
            val peliasResult = PeliasResultTransformer.parseAndTransform(photonResult)
            call.respond(peliasResult)
        } catch (e: Exception) {
            logger.error("Error proxying $photonRequests to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respondText(
                ErrorHandler.toJson(error),
                contentType = Json,
                status = HttpStatusCode.fromValue(error.statusCode),
            )
        }
    }

    private val logger = LoggerFactory.getLogger(PeliasApi::class.java)
}
