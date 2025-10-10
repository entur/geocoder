package no.entur.geocoder.proxy

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.util.toMap
import org.slf4j.LoggerFactory
import kotlin.text.split
import kotlin.text.toIntOrNull

object PeliasApi {

    suspend fun RoutingContext.autocompleteRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: ResultTransformer
    ) {
        val params = PeliasAutocompleteParams.fromRequest(call.request)

        val url = "$photonBaseUrl/api"
        logger.info("Proxying /v1/autocomplete to $url with text='${params.text}'")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("q", params.text)
                        parameter("limit", params.size.toString())
                        parameter("lang", params.lang)

                        params.boundaryCountry?.let { parameter("include", "country.$it") }
                        if (params.boundaryCountyIds.isNotEmpty()) {
                            parameter("include", params.boundaryCountyIds.joinToString(",", "county_gid."))
                        }
                        if (params.boundaryLocalityIds.isNotEmpty()) {
                            parameter("include", params.boundaryLocalityIds.joinToString(",", "locality_gid."))
                        }
                        if (params.tariffZones.isNotEmpty()) {
                            parameter("include", params.tariffZones.joinToString(",", "tariff_zone_id."))
                        }
                        if (params.tariffZoneAuthorities.isNotEmpty()) {
                            parameter("include", params.tariffZoneAuthorities.joinToString(",", "tariff_zone_authority."))
                        }
                        if (params.sources.isNotEmpty()) {
                            parameter("include", params.sources.joinToString(",", "source."))
                        }
                        if (params.layers.isNotEmpty()) {
                            parameter("include", params.layers.joinToString(",", "layer."))
                        }
                    }.bodyAsText()

            val json = transformer.parseAndTransform(photonResponse)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error proxying to Photon: $e", e)
            call.respondText(
                """{"error":"Failed to connect to Photon backend: $e"}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.ServiceUnavailable,
            )
        }
    }

    suspend fun RoutingContext.reverseRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: ResultTransformer
    ) {
        val params = PeliasReverseParams.fromRequest(call.request)

        val url = "$photonBaseUrl/reverse"
        logger.info("Proxying /v1/reverse to $url at (${params.lat}, ${params.lon})")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("lat", params.lat)
                        parameter("lon", params.lon)
                        parameter("lang", params.lang)
                        params.radius?.let { parameter("radius", it) }
                        parameter("limit", params.size.toString())
                    }.bodyAsText()

            val json = transformer.parseAndTransform(photonResponse)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error proxying to Photon: $e", e)
            call.respondText(
                """{"error":"Failed to connect to Photon backend: $e"}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.ServiceUnavailable,
            )
        }
    }

    private val logger = LoggerFactory.getLogger(PeliasApi::class.java)
}