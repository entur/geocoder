package no.entur.geocoder.proxy.v3

import io.ktor.http.*
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import no.entur.geocoder.proxy.photon.PhotonReverseRequest
import org.slf4j.LoggerFactory

class V3Api(private val photonApi: PhotonApi) {
    suspend fun autocomplete(params: Parameters): V3Result {
        val req = V3AutocompleteRequest.from(params)
        val photonRequest = PhotonAutocompleteRequest.from(req)
        logger.debug("V3 autocomplete request with query='$photonRequest'")

        val photonResult = photonApi.request(photonRequest)

        return V3ResultTransformer.parseAndTransform(photonResult, req)
    }

    suspend fun reverse(params: Parameters): V3Result {
        val req = V3ReverseRequest.from(params)
        val photonRequest = PhotonReverseRequest.from(req)
        logger.debug("V3 reverse geocoding request with query=$photonRequest")

        val photonResult = photonApi.request(photonRequest)

        return V3ResultTransformer.parseAndTransform(photonResult, req)
    }

    private val logger = LoggerFactory.getLogger(V3Api::class.java)
}
