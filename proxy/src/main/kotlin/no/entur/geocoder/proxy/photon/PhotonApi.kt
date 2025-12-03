package no.entur.geocoder.proxy.photon

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.entur.geocoder.common.JsonMapper.jacksonMapper
import no.entur.geocoder.common.Util.within

class PhotonApi(private val client: HttpClient, private val baseUrl: String) {
    /**
     * Allowed parameters are:
     * [include, location_bias_scale, debug, dedupe, bbox, lon, zoom, layer, q, limit, osm_tag, exclude, geometry, lang, lat, include_housenumbers]
     */
    suspend fun request(req: PhotonAutocompleteRequest): PhotonResult {
        val response =
            client
                .get("$baseUrl/api") {
                    parameter("q", req.query)
                    parameter("limit", req.limit)
                    parameter("lang", req.language)
                    if (req.locationBiasScale.within(0.0, 1.0)) {
                        parameter("location_bias_scale", req.locationBiasScale)
                    }
                    if (req.zoom != null) {
                        parameter("zoom", req.zoom)
                    }
                    req.includes.forEach {
                        parameter("include", it)
                    }
                    req.excludes.forEach {
                        parameter("exclude", it)
                    }
                    if (req.lat != null && req.lon != null) {
                        parameter("lat", req.lat)
                        parameter("lon", req.lon)
                    }
                    parameter("debug", req.debug)
                    if (req.includeHousenumbers) parameter("include_housenumbers", true)
                }

        return if (response.status.isSuccess()) {
            PhotonResult.parse(response.bodyAsText(), response.request.url, response.status)
        } else {
            PhotonResult(status = response.status)
        }
    }

    /**
     * Allowed parameters are:
     * [include, debug, dedupe, query_string_filter, lon, layer, limit, osm_tag, distance_sort, exclude, geometry, lang, radius, lat]
     */
    suspend fun request(req: PhotonReverseRequest): PhotonResult {
        val response =
            client
                .get("$baseUrl/reverse") {
                    parameter("lat", req.latitude)
                    parameter("lon", req.longitude)
                    parameter("lang", req.language)
                    req.radius?.let { parameter("radius", it) }
                    parameter("limit", req.limit.toString())

                    req.includes.forEach {
                        parameter("include", it)
                    }
                    req.excludes.forEach {
                        parameter("exclude", it)
                    }
                    parameter("debug", req.debug)
                }
        return if (response.status.isSuccess()) {
            PhotonResult.parse(response.bodyAsText(), response.request.url, response.status)
        } else {
            PhotonResult(status = response.status)
        }
    }

    suspend fun status(): Map<String, String> =
        try {
            jacksonMapper.readValue(client.get("$baseUrl/status").bodyAsText())
        } catch (_: Exception) {
            emptyMap()
        }
}
