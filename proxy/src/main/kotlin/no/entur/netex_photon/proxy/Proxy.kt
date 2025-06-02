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
            get("/photon") {
                val query = call.request.queryParameters["q"] ?: ""
                val rewrittenQuery = query // TODO: Add rewrite logic if needed
                val client = HttpClient(CIO)
                val response: String = client.get("http://localhost:2322/api") {
                    url {
                        parameters.append("q", rewrittenQuery)
                    }
                }.bodyAsText()

                val transformer = FeatureTransformer()
                val transformed: FeatureCollection = transformer.parseAndTransform(response)
                call.respondText(transformer.toJsonString(transformed), contentType = Json)
            }
        }
    }.start(wait = true)
}

