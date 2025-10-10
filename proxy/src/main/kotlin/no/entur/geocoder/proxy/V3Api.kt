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
        val params = V3AutocompleteParams.fromRequest(call.request)

        val url = "$photonBaseUrl/api"
        logger.info("V3 autocomplete request to $url with query='${params.query}'")

        try {
            val photonResponse = client.get(url) {
                parameter("q", params.query)
                parameter("limit", params.limit.toString())
                parameter("lang", params.language)

                if (params.countries.isNotEmpty()) {
                    parameter("include", params.countries.joinToString(",", "country."))
                }
                if (params.countyIds.isNotEmpty()) {
                    parameter("include", params.countyIds.joinToString(",", "county_gid."))
                }
                if (params.localityIds.isNotEmpty()) {
                    parameter("include", params.localityIds.joinToString(",", "locality_gid."))
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
                if (params.placeTypes.isNotEmpty()) {
                    parameter("include", params.placeTypes.joinToString(",", "layer."))
                }
            }.bodyAsText()

            val json = transformer.parseAndTransform(
                photonResponse,
                query = params.query,
                limit = params.limit,
                language = params.language,
                placeTypes = params.placeTypes,
                sources = params.sources,
                countries = params.countries,
                countyIds = params.countyIds,
                localityIds = params.localityIds,
                tariffZones = params.tariffZones,
                tariffZoneAuthorities = params.tariffZoneAuthorities,
                transportModes = params.transportModes
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
        val params = V3ReverseParams.fromRequest(call.request)

        val url = "$photonBaseUrl/reverse"
        logger.info("V3 reverse geocoding request to $url at (${params.latitude}, ${params.longitude})")

        try {
            val photonResponse = client.get(url) {
                parameter("lat", params.latitude)
                parameter("lon", params.longitude)
                parameter("lang", params.language)
                params.radius?.let { parameter("radius", it) }
                parameter("limit", params.limit.toString())
            }.bodyAsText()

            val json = transformer.parseAndTransform(
                photonResponse,
                latitude = params.latitude.toBigDecimalOrNull(),
                longitude = params.longitude.toBigDecimalOrNull(),
                limit = params.limit,
                language = params.language
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

