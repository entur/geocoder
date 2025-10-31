package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.entur.geocoder.proxy.pelias.PeliasApi.peliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasApi.peliasPlaceRequest
import no.entur.geocoder.proxy.pelias.PeliasApi.peliasReverseRequest
import no.entur.geocoder.proxy.pelias.PeliasResultTransformer
import no.entur.geocoder.proxy.v3.V3ResultTransformer
import no.entur.geocoder.proxy.v3.V3Api.autocompleteRequest as v3AutocompleteRequest
import no.entur.geocoder.proxy.v3.V3Api.reverseRequest as v3ReverseRequest

object Routing {
    fun Application.configureRouting(
        client: HttpClient,
        photonBaseUrl: String,
        appMicrometerRegistry: PrometheusMeterRegistry,
    ) {
        val transformer = PeliasResultTransformer()
        val v3Transformer = V3ResultTransformer()

        routing {
            get("/v2/autocomplete") {
                peliasAutocompleteRequest(photonBaseUrl, client, transformer)
            }

            get("/v2/reverse") {
                peliasReverseRequest(photonBaseUrl, client, transformer)
            }

            get("/v2/nearby") {
                peliasReverseRequest(photonBaseUrl, client, transformer)
            }

            get("/v2/place") {
                peliasPlaceRequest(photonBaseUrl, client, transformer)
            }

            get("/v3/autocomplete") {
                v3AutocompleteRequest(photonBaseUrl, client, v3Transformer)
            }

            get("/v3/reverse") {
                v3ReverseRequest(photonBaseUrl, client, v3Transformer)
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

            get("/metrics") {
                call.respond(appMicrometerRegistry.scrape())
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
