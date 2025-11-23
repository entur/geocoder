package no.entur.geocoder.proxy.v3

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
        return V3ResultTransformer.parseAndTransform(photonResult, req)
    }

    suspend fun reverse(params: Parameters): V3Result {
        val req = V3ReverseRequest.from(params)
        val photonRequest = PhotonReverseRequest.from(req)
        val url = "$photonBaseUrl/reverse"
        logger.debug("V3 reverse geocoding request to $url at (${photonRequest.latitude}, ${photonRequest.longitude})")

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
        return V3ResultTransformer.parseAndTransform(photonResult, req)
    }

    private val logger = LoggerFactory.getLogger(V3Api::class.java)
}
