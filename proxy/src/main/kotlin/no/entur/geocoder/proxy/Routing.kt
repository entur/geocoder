package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import no.entur.geocoder.proxy.PeliasApi.autocompleteRequest
import no.entur.geocoder.proxy.PeliasApi.reverseRequest
import org.slf4j.LoggerFactory

object Routing {

    fun Application.configureRouting(
        client: HttpClient,
        transformer: ResultTransformer,
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
}