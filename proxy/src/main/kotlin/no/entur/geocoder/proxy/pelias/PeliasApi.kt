package no.entur.geocoder.proxy.pelias

import io.ktor.client.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.entur.geocoder.proxy.ErrorHandler
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonReverseRequest
import org.slf4j.LoggerFactory

object PeliasApi {
    suspend fun RoutingContext.peliasAutocompleteRequest(
        photonBaseUrl: String,
        client: HttpClient,
        req: PeliasAutocompleteRequest,
    ) {
        logger.debug("/v2/autocomplete: {}'", req)
        val photonRequest = PhotonAutocompleteRequest.from(req)
        val url = "$photonBaseUrl/api"

        try {
            val photonResponse = PhotonApi.request(photonRequest, client, url)

            val photonResult = PhotonResult.parse(photonResponse.body, photonResponse.url)
            val peliasResult = PeliasResultTransformer.parseAndTransform(photonResult, req)
            call.respond(peliasResult)
        } catch (e: Exception) {
            logger.error("Error proxying $photonRequest to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respond(error.status, error.result)
        }
    }

    suspend fun RoutingContext.peliasReverseRequest(
        photonBaseUrl: String,
        client: HttpClient,
        req: PeliasReverseRequest,
    ) {
        logger.debug("/v2/reverse: {}", req)
        val photonRequest = PhotonReverseRequest.from(req)
        val url = "$photonBaseUrl/reverse"

        try {
            val photonResponse = PhotonApi.request(photonRequest, client, url)
            val photonResult = PhotonResult.parse(photonResponse.body, photonResponse.url)
            val result = PeliasResultTransformer.parseAndTransform(photonResult, req)
            call.respond(result)
        } catch (e: Exception) {
            logger.error("Error proxying $photonRequest to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Reverse geocoding")
            call.respond(error.status, error.result)
        }
    }

    suspend fun RoutingContext.peliasPlaceRequest(
        photonBaseUrl: String,
        client: HttpClient,
        req: PeliasPlaceRequest,
    ) {
        logger.debug("/v2/place: {}", req)
        val photonRequests = PhotonAutocompleteRequest.from(req)
        val url = "$photonBaseUrl/api"

        try {
            val photonResults =
                photonRequests.map { photonRequest ->
                    val res = PhotonApi.request(photonRequest, client, url)
                    PhotonResult.parse(res.body, res.url)
                }
            val photonResult =
                PhotonResult(
                    type = "FeatureCollection",
                    features = photonResults.mapNotNull { it.features.firstOrNull() },
                )
            val result = PeliasResultTransformer.parseAndTransform(photonResult, req)
            call.respond(result)
        } catch (e: Exception) {
            logger.error("Error proxying $photonRequests to Photon: $e", e)
            val error = ErrorHandler.handleError(e, "Autocomplete")
            call.respond(error.status, error.result)
        }
    }

    private val logger = LoggerFactory.getLogger(PeliasApi::class.java)
}
