package no.entur.geocoder.proxy

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import org.slf4j.LoggerFactory
import kotlin.text.split
import kotlin.text.toIntOrNull

object V3Api {

    suspend fun RoutingContext.autocompleteRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: V3ResultTransformer
    ) {
        val params = call.request.queryParameters

        val query = params["query"] ?: params["q"] ?: ""
        val limit = params["limit"]?.toIntOrNull() ?: 10
        val language = params["language"] ?: params["lang"] ?: "no"

        val placeTypes = params["placeTypes"]?.split(",") ?: emptyList()
        val sources = params["sources"]?.split(",") ?: emptyList()
        val countries = params["countries"]?.split(",") ?: emptyList()
        val countyIds = params["countyIds"]?.split(",") ?: emptyList()
        val localityIds = params["localityIds"]?.split(",") ?: emptyList()
        val tariffZones = params["tariffZones"]?.split(",") ?: emptyList()
        val tariffZoneAuthorities = params["tariffZoneAuthorities"]?.split(",") ?: emptyList()
        val transportModes = params["transportModes"]?.split(",") ?: emptyList()

        val url = "$photonBaseUrl/api"
        logger.info("V3 autocomplete request to $url with query='$query'")

        try {
            val photonResponse = client.get(url) {
                parameter("q", query)
                parameter("limit", limit.toString())
                parameter("lang", language)

                if (countries.isNotEmpty()) {
                    parameter("include", countries.joinToString(",", "country."))
                }
                if (countyIds.isNotEmpty()) {
                    parameter("include", countyIds.joinToString(",", "county_gid."))
                }
                if (localityIds.isNotEmpty()) {
                    parameter("include", localityIds.joinToString(",", "locality_gid."))
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
                if (placeTypes.isNotEmpty()) {
                    parameter("include", placeTypes.joinToString(",", "layer."))
                }
            }.bodyAsText()

            val json = transformer.parseAndTransform(
                photonResponse,
                query = query,
                limit = limit,
                language = language,
                placeTypes = placeTypes,
                sources = sources,
                countries = countries,
                countyIds = countyIds,
                localityIds = localityIds,
                tariffZones = tariffZones,
                tariffZoneAuthorities = tariffZoneAuthorities,
                transportModes = transportModes
            )

            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error in V3 autocomplete: ${e.message}", e)
            call.respondText(
                """{"error":"Geocoding service unavailable","message":"${e.message}"}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.ServiceUnavailable,
            )
        }
    }

    suspend fun RoutingContext.reverseRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: V3ResultTransformer
    ) {
        val params = call.request.queryParameters

        val latitude = params["latitude"] ?: params["lat"] ?: ""
        val longitude = params["longitude"] ?: params["lon"] ?: ""
        val radius = params["radius"]
        val limit = params["limit"]?.toIntOrNull() ?: 10
        val language = params["language"] ?: params["lang"] ?: "no"

        val url = "$photonBaseUrl/reverse"
        logger.info("V3 reverse geocoding request to $url at ($latitude, $longitude)")

        try {
            val photonResponse = client.get(url) {
                parameter("lat", latitude)
                parameter("lon", longitude)
                parameter("lang", language)
                radius?.let { parameter("radius", it) }
                parameter("limit", limit.toString())
            }.bodyAsText()

            val json = transformer.parseAndTransform(
                photonResponse,
                latitude = latitude.toBigDecimalOrNull(),
                longitude = longitude.toBigDecimalOrNull(),
                limit = limit,
                language = language
            )

            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            logger.error("Error in V3 reverse geocoding: ${e.message}", e)
            call.respondText(
                """{"error":"Reverse geocoding service unavailable","message":"${e.message}"}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.ServiceUnavailable,
            )
        }
    }

    private val logger = LoggerFactory.getLogger(V3Api::class.java)
}

