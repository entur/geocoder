package no.entur.netex_photon.proxy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

private val httpClient = HttpClient(CIO) {
    install(ClientContentNegotiation) {
        jackson()
    }
}
private val transformer = FeatureTransformer()
private val logger = LoggerFactory.getLogger("Proxy")

fun main() {
    val photonBaseUrl = System.getenv("PHOTON_BASE_URL") ?: "http://netex-photon-server"
    val proxyPort = System.getenv("PROXY_PORT")?.toIntOrNull() ?: 8080

    embeddedServer(Netty, port = proxyPort) {
        install(ServerContentNegotiation) {
            jackson()
        }
        configureRouting(httpClient, transformer, photonBaseUrl)
    }.start(wait = true)
}

fun Application.configureRouting(
    client: HttpClient,
    transformer: FeatureTransformer,
    photonBaseUrl: String
) {
    routing {
        get("/v1/autocomplete") {
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

            val url = "$photonBaseUrl/api"
            logger.info("Proxying /v1/autocomplete to $url with ${params.toMap()}")

            try {
                val photonResponse = client.get(url) {
                    parameter("q", query)
                    parameter("limit", size.toString())
                    parameter("lang", lang)

                    boundaryCountry?.let { parameter("osm_tag", "extra.country:$it") }
                    for (id in boundaryCountyIds) { parameter("osm_tag", "extra.county_gid:$id") }
                    for (id in boundaryLocalityIds) { parameter("osm_tag", "extra.locality_gid:$id") }
                    for (zone in tariffZones) { parameter("osm_tag", "extra.tariff_zones:*$zone*") }
                    for (authority in tariffZoneAuthorities) { parameter("osm_tag", "extra.tariff_zones:*$authority:*") }
                    for (source in sources) { parameter("osm_tag", "extra.source:$source") }
                    for (layer in layers) { parameter("osm_tag", "extra.layer:$layer") }
                }.bodyAsText()

                val json = transformer.parseAndTransform(photonResponse)
                call.respondText(json, contentType = ContentType.Application.Json)
            } catch (e: Exception) {
                logger.error("Error proxying to Photon: ${e.message}", e)
                call.respondText(
                    """{"error":"Failed to connect to Photon backend: ${e.message}"}""",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.ServiceUnavailable
                )
            }
        }

        get("/v1/reverse") {
            val params = call.request.queryParameters
            val lat = params["point.lat"] ?: ""
            val lon = params["point.lon"] ?: ""
            val radius = params["boundary.circle.radius"]
            val size = params["size"]?.toIntOrNull() ?: 10
            val lang = params["lang"] ?: "no"

            val url = "$photonBaseUrl/reverse"
            logger.info("Proxying /v1/reverse to $url with ${params.toMap()}")

            try {
                val photonResponse = client.get(url) {
                    parameter("lat", lat)
                    parameter("lon", lon)
                    parameter("lang", lang)
                    radius?.let { parameter("radius", radius) }
                    parameter("limit", size.toString())
                }.bodyAsText()

                val json = transformer.parseAndTransform(photonResponse)
                call.respondText(json, contentType = ContentType.Application.Json)
            } catch (e: Exception) {
                logger.error("Error proxying to Photon: ${e.message}", e)
                call.respondText(
                    """{"error":"Failed to connect to Photon backend: ${e.message}"}""",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.ServiceUnavailable
                )
            }
        }

        get("/") {
            val indexHtml = this::class.java.classLoader.getResourceAsStream("index.html")?.readBytes()
                ?: throw IllegalStateException("index.html not found in resources")

            call.respondText(String(indexHtml), contentType = ContentType.Text.Html)
        }

        get("/actuator/health/liveness") {
            call.respondText("""{"status":"UP"}""", contentType = ContentType.Application.Json)
        }

        get("/actuator/health/readiness") {
            try {
                call.respondText("""{"status":"UP"}""", contentType = ContentType.Application.Json)
            } catch (e: Exception) {
                call.respondText(
                    """{"status":"DOWN","details":{"error":"${e.message}"}}""",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.ServiceUnavailable
                )
            }
        }
    }
}
