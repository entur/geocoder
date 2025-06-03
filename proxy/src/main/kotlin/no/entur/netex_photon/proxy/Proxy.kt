package no.entur.netex_photon.proxy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.jackson.*

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

private val httpClient = HttpClient(CIO) {
    install(ClientContentNegotiation) {
        jackson()
    }
}
private val transformer = FeatureTransformer()

fun main() {
    val photonBaseUrl = System.getenv("PHOTON_BASE_URL") ?: "http://photon:2322"
    embeddedServer(Netty, port = 8080) {
        install(ServerContentNegotiation) {
            jackson()
        }
        configureRouting(httpClient, transformer, photonBaseUrl)
        configureHealthEndpoints()
    }.start(wait = true)
}

fun Application.configureRouting(
    client: HttpClient,
    transformer: FeatureTransformer,
    photonBaseUrl: String
) {
    routing {
        get("/v1/autocomplete") {
            val query = call.request.queryParameters["text"] ?: ""
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
            val lang = call.request.queryParameters["lang"] ?: "no"

            val photonResponse = client.get("$photonBaseUrl/api") {
                parameter("q", query)
                parameter("limit", size.toString())
                parameter("lang", lang)
            }.bodyAsText()

            val json = transformer.parseAndTransform(photonResponse)
            call.respondText(json, contentType = ContentType.Application.Json)
        }

        get("/v1/reverse") {
            val lat = call.request.queryParameters["point.lat"] ?: ""
            val lon = call.request.queryParameters["point.lon"] ?: ""
            val radius = call.request.queryParameters["boundary.circle.radius"] ?: ""
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
            val lang = call.request.queryParameters["lang"] ?: "no"

            val photonResponse = client.get("$photonBaseUrl/reverse") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("lang", lang)
                parameter("radius", radius)
                parameter("limit", size.toString())
            }.bodyAsText()

            val json = transformer.parseAndTransform(photonResponse)
            call.respondText(json, contentType = ContentType.Application.Json)
        }
    }
}

fun Application.configureHealthEndpoints() {
    routing {
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
