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
        val params = call.request.queryParameters

        val query = params["text"] ?: ""
        val size = params["size"]?.toIntOrNull() ?: 10
        val lang = params["lang"] ?: "no"
        val boundaryCountry = params["boundary.country"]
        val boundaryCountyIds = params["boundary.county.ids"]?.split(",") ?: emptyList()
        val boundaryLocalityIds = params["boundary.locality.ids"]?.split(",") ?: emptyList()
        val tariffZones = params["tariff_zone_ids"]?.split(",") ?: emptyList()
        val tariffZoneAuthorities = params["tariff_zone_authorities"]?.split(",") ?: emptyList()
        val sources = params["sources"]?.split(",") ?: emptyList()
        val layers = params["layers"]?.split(",") ?: emptyList()
        val categories = params["transport_mode"]?.split(",") ?: emptyList()

        val url = "$photonBaseUrl/api"
        logger.info("Proxying /v1/autocomplete to $url with ${params.toMap()}")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("q", query)
                        parameter("limit", size.toString())
                        parameter("lang", lang)

                        boundaryCountry?.let { parameter("include", "country.$it") }
                        if (boundaryCountyIds.isNotEmpty()) {
                            parameter("include", boundaryCountyIds.joinToString(",", "county_gid."))
                        }
                        if (boundaryLocalityIds.isNotEmpty()) {
                            parameter("include", boundaryLocalityIds.joinToString(",", "locality_gid."))
                        }
                        if (tariffZones.isNotEmpty()) {
                            parameter("include", tariffZones.joinToString(",", "tariff_zone_id."))
                        }
                        if (tariffZoneAuthorities.isNotEmpty()) {
                            parameter("include", tariffZoneAuthorities.joinToString(",", "tariff_zone_authority."))
                        }
                        if (sources.isNotEmpty()) {
                            parameter("include", sources.joinToString(",", "source."))
                        }
                        if (layers.isNotEmpty()) {
                            parameter("include", layers.joinToString(",", "layer."))
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
        val params = call.request.queryParameters
        val lat = params["point.lat"] ?: ""
        val lon = params["point.lon"] ?: ""
        val radius = params["boundary.circle.radius"]
        val size = params["size"]?.toIntOrNull() ?: 10
        val lang = params["lang"] ?: "no"

        val url = "$photonBaseUrl/reverse"
        logger.info("Proxying /v1/reverse to $url with ${params.toMap()}")

        try {
            val photonResponse =
                client
                    .get(url) {
                        parameter("lat", lat)
                        parameter("lon", lon)
                        parameter("lang", lang)
                        radius?.let { parameter("radius", radius) }
                        parameter("limit", size.toString())
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