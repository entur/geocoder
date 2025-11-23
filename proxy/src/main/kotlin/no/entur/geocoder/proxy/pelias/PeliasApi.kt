package no.entur.geocoder.proxy.pelias

import io.ktor.client.*
import io.ktor.http.*
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonReverseRequest
import org.slf4j.LoggerFactory

class PeliasApi(private val client: HttpClient, private val photonBaseUrl: String) {
    suspend fun autocomplete(params: Parameters): PeliasResult {
        val req = PeliasAutocompleteRequest.from(params)
        logger.debug("/v2/autocomplete: {}'", req)

        val photonRequest = PhotonAutocompleteRequest.from(req)
        val photonResponse = PhotonApi.request(photonRequest, client, "$photonBaseUrl/api")

        val photonResult = PhotonResult.parse(photonResponse.body, photonResponse.url)
        return PeliasResultTransformer.parseAndTransform(photonResult, req)
    }

    suspend fun reverse(params: Parameters): PeliasResult {
        val req = PeliasReverseRequest.from(params)
        logger.debug("/v2/reverse: {}", req)

        val photonRequest = PhotonReverseRequest.from(req)

        val photonResponse = PhotonApi.request(photonRequest, client, "$photonBaseUrl/reverse")
        val photonResult = PhotonResult.parse(photonResponse.body, photonResponse.url)
        return PeliasResultTransformer.parseAndTransform(photonResult, req)
    }

    suspend fun place(params: Parameters): PeliasResult {
        val req = PeliasPlaceRequest.from(params)
        logger.debug("/v2/place: {}", req)

        val photonRequests = PhotonAutocompleteRequest.from(req)

        val photonResults =
            photonRequests.map { photonRequest ->
                val res = PhotonApi.request(photonRequest, client, "$photonBaseUrl/api")
                PhotonResult.parse(res.body, res.url)
            }
        val photonResult =
            PhotonResult(
                type = "FeatureCollection",
                features = photonResults.mapNotNull { it.features.firstOrNull() },
            )
        return PeliasResultTransformer.parseAndTransform(photonResult, req)
    }

    private val logger = LoggerFactory.getLogger(PeliasApi::class.java)
}
