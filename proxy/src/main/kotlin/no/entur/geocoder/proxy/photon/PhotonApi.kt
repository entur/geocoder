package no.entur.geocoder.proxy.photon

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.entur.geocoder.common.Util.within

object PhotonApi {
    /**
     * Allowed parameters are:
     * [include, location_bias_scale, debug, dedupe, bbox, lon, zoom, layer, q, limit, osm_tag, exclude, geometry, lang, lat]
     */
    suspend fun request(req: PhotonAutocompleteRequest, client: HttpClient, url: String): PhotonApiResponse {
        val response =
            client
                .get(url) {
                    parameter("q", req.query)
                    parameter("limit", req.limit)
                    parameter("lang", req.language)
                    if (req.locationBiasScale.within(0.0, 1.0)) {
                        parameter("location_bias_scale", req.locationBiasScale)
                    }
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
                    parameter("debug", req.debug)
                }
        return PhotonApiResponse(
            body = response.bodyAsText(),
            url = response.request.url.toString(),
        )
    }

    /**
     * Allowed parameters are:
     * [include, debug, dedupe, query_string_filter, lon, layer, limit, osm_tag, distance_sort, exclude, geometry, lang, radius, lat]
     */
    suspend fun request(req: PhotonReverseRequest, client: HttpClient, url: String): PhotonApiResponse {
        val response =
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
                    parameter("debug", req.debug)
                }
        return PhotonApiResponse(
            body = response.bodyAsText(),
            url = response.request.url.toString(),
        )
    }

    data class PhotonApiResponse(
        val body: String,
        val url: String,
    )
}
