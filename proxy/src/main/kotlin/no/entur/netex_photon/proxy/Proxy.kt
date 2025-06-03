package no.entur.netex_photon.proxy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val httpClient = HttpClient(CIO)
private val transformer = FeatureTransformer()

fun main() {
    embeddedServer(Netty, port = 8080) {
        configureRouting(httpClient, transformer, "http://localhost:2322")
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
                url {
                    parameters.append("q", query)
                    parameters.append("limit", size.toString())
                    parameters.append("lang", lang)
                }
            }.bodyAsText()

            val json = transformer.parseAndTransform(photonResponse)
            call.respondText(json, contentType = Json)
        }

        get("/v1/reverse") {
            val lat = call.request.queryParameters["point.lat"] ?: ""
            val lon = call.request.queryParameters["point.lon"] ?: ""
            val radius = call.request.queryParameters["boundary.circle.radius"] ?: ""
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
            val lang = call.request.queryParameters["lang"] ?: "no"

            val photonResponse = client.get("$photonBaseUrl/reverse") {
                url {
                    parameters.append("lat", lat)
                    parameters.append("lon", lon)
                    parameters.append("lang", lang)
                    parameters.append("radius", radius)
                    parameters.append("limit", size.toString())
                }
            }.bodyAsText()

            val json = transformer.parseAndTransform(photonResponse)
            call.respondText(json, contentType = Json)
        }
    }
}
