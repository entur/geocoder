package no.entur.geocoder.proxy.v3

import io.ktor.client.*
import io.ktor.http.*
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonReverseRequest
import org.slf4j.LoggerFactory

class V3Api(private val client: HttpClient, private val photonBaseUrl: String) {
    suspend fun autocomplete(params: Parameters): V3Result {
        val req = V3AutocompleteRequest.from(params)
        val photonRequest = PhotonAutocompleteRequest.from(req)
        val url = "$photonBaseUrl/api"
        logger.debug("V3 autocomplete request to $url with query='${photonRequest.query}'")

        val photonResponse = PhotonApi.request(photonRequest, client, photonBaseUrl)

        val photonResult = PhotonResult.parse(photonResponse)
        return V3ResultTransformer.parseAndTransform(photonResult, req)
    }

    suspend fun reverse(params: Parameters): V3Result {
        val req = V3ReverseRequest.from(params)
        val photonRequest = PhotonReverseRequest.from(req)
        val url = "$photonBaseUrl/reverse"
        logger.debug("V3 reverse geocoding request to $url at (${photonRequest.latitude}, ${photonRequest.longitude})")

        val photonResponse = PhotonApi.request(photonRequest, client, photonBaseUrl)

        val photonResult = PhotonResult.parse(photonResponse)
        return V3ResultTransformer.parseAndTransform(photonResult, req)
    }

    private val logger = LoggerFactory.getLogger(V3Api::class.java)
}
