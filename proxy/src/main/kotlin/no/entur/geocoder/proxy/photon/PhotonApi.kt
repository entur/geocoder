package no.entur.geocoder.proxy.photon

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

object PhotonApi {
    suspend fun request(req: PhotonAutocompleteRequest, client: HttpClient, url: String) =
        client
            .get(url) {
                parameter("q", req.query)
                parameter("limit", req.limit)
                parameter("lang", req.language)

                if (req.zoom != null) {
                    parameter("zoom", req.zoom)
                }
                if (req.includes.isNotEmpty()) {
                    parameter("include", req.includes.joinToString(","))
                }
                req.excludes.forEach {
                    parameter("exclude", it)
                }
                if (req.lat != null && req.lon != null) {
                    parameter("lat", req.lat)
                    parameter("lon", req.lon)
                }
            }.bodyAsText()

    suspend fun request(req: PhotonReverseRequest, client: HttpClient, url: String) =
        client
            .get(url) {
                parameter("lat", req.latitude)
                parameter("lon", req.longitude)
                parameter("lang", req.language)
                req.radius?.let { parameter("radius", it) }
                parameter("limit", req.limit.toString())

                if (req.includes.isNotEmpty()) {
                    parameter("include", req.includes.joinToString(","))
                }
                req.excludes.forEach {
                    parameter("exclude", it)
                }
            }.bodyAsText()
}
