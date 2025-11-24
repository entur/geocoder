package no.entur.geocoder.proxy.pelias

import io.ktor.http.*
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonReverseRequest
import org.slf4j.LoggerFactory

class PeliasApi(private val photonApi: PhotonApi) {
    suspend fun autocomplete(params: Parameters): PeliasResult {
        logger.debug("/v2/autocomplete: {}'", params.formUrlEncode())
        val req = PeliasAutocompleteRequest.from(params)

        val photonRequest = PhotonAutocompleteRequest.from(req)
        val photonResult = photonApi.request(photonRequest)

        return PeliasResultTransformer.parseAndTransform(photonResult, req)
    }

    suspend fun reverse(params: Parameters): PeliasResult {
        logger.debug("/v2/reverse: {}", params.formUrlEncode())
        val req = PeliasReverseRequest.from(params)

        val photonRequest = PhotonReverseRequest.from(req)

        val photonResult = photonApi.request(photonRequest)
        return PeliasResultTransformer.parseAndTransform(photonResult, req)
    }

    suspend fun place(params: Parameters): PeliasResult {
        logger.debug("/v2/place: {}", params.formUrlEncode())
        val req = PeliasPlaceRequest.from(params)

        val photonRequests = PhotonAutocompleteRequest.from(req)

        val photonResults =
            photonRequests.map { photonRequest ->
                photonApi.request(photonRequest)
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
