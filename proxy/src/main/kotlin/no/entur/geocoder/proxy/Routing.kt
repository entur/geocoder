package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.slf4j.LoggerFactory

object Routing {

    fun Application.configureRouting(
        client: HttpClient,
        transformer: FeatureTransformer,
        photonBaseUrl: String,
    ) {
        routing {
            get("/v1/autocomplete") {
                autocompleteRequest(photonBaseUrl, client, transformer)
            }

            get("/v1/reverse") {
                reverseRequest(photonBaseUrl, client, transformer)
            }

            get("/") {
                rootRequest()
            }

            get("/actuator/health/liveness") {
                livenessRequest()
            }

            get("/actuator/health/readiness") {
                readinessRequest()
            }
        }
    }


    private suspend fun RoutingContext.autocompleteRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: FeatureTransformer
    ) {
        val params = call.request.queryParameters

        val query = params["text"] ?: ""
        val size = params["size"]?.toIntOrNull() ?: 10
        val lang = params["lang"] ?: "no"
        val boundaryCountry = params["boundary.country"]?.lowercase()
        val boundaryCountyIds = params["boundary.county.ids"]?.lowercase()?.split(",") ?: emptyList()
        val boundaryLocalityIds = params["boundary.locality.ids"]?.lowercase()?.split(",") ?: emptyList()
        val tariffZones = params["tariff_zone_ids"]?.lowercase()?.split(",") ?: emptyList()
        val tariffZoneAuthorities = params["tariff_zone_authorities"]?.lowercase()?.split(",") ?: emptyList()
        val sources = params["sources"]?.lowercase()?.split(",") ?: emptyList()
        val layers = params["layers"]?.lowercase()?.split(",") ?: emptyList()
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

    private suspend fun RoutingContext.reverseRequest(
        photonBaseUrl: String,
        client: HttpClient,
        transformer: FeatureTransformer
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

    private suspend fun RoutingContext.rootRequest() {
        val indexHtml =
            this::class.java.classLoader
                .getResourceAsStream("index.html")
                ?.readBytes()
                ?: throw IllegalStateException("index.html not found in resources")

        call.respondText(String(indexHtml), contentType = ContentType.Text.Html)
    }

    private suspend fun RoutingContext.livenessRequest() {
        call.respondText("""{"status":"UP"}""", contentType = ContentType.Application.Json)
    }

    private suspend fun RoutingContext.readinessRequest() {
        try {
            call.respondText("""{"status":"UP"}""", contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            call.respondText(
                """{"status":"DOWN","details":{"error":"${e.message}"}}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.ServiceUnavailable,
            )
        }
    }


    private val logger = LoggerFactory.getLogger(Routing::class.java)
}