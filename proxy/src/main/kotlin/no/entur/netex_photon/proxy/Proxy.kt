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

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/v1/autocomplete") {
                val query = call.request.queryParameters["text"] ?: ""
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
                val lang = call.request.queryParameters["lang"] ?: "no"

                val client = HttpClient(CIO)
                val response: String = client.get("http://localhost:2322/api") {
                    url {
                        parameters.append("q", query)
                        parameters.append("limit", "$size")
                        parameters.append("lang", lang)
                    }
                }.bodyAsText()

                val transformer = FeatureTransformer()
                val transformed: FeatureCollection = transformer.parseAndTransform(response)
                call.respondText(transformer.toJsonString(transformed), contentType = Json)
            }
            get("/v1/reverse") {
                val lat = call.request.queryParameters["point.lat"] ?: ""
                val lon = call.request.queryParameters["point.lon"] ?: ""
                val radius = call.request.queryParameters["boundary.circle.radius"] ?: ""
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
                val layers = call.request.queryParameters["layers"] ?: ""
                val lang = call.request.queryParameters["lang"] ?: "no"

                val client = HttpClient(CIO)
                val response: String = client.get("http://localhost:2322/reverse") {
                    url {
                        parameters.append("lat", lat)
                        parameters.append("lon", lon)
                        parameters.append("lang", lang)
                        parameters.append("radius", radius)
                        parameters.append("limit", "$size")
                    }
                }.bodyAsText()

                val transformer = FeatureTransformer()
                val transformed: FeatureCollection = transformer.parseAndTransform(response)
                call.respondText(transformer.toJsonString(transformed), contentType = Json)
            }
        }
    }.start(wait = true)
}

