package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.entur.geocoder.proxy.health.HealthCheck
import no.entur.geocoder.proxy.pelias.PeliasApi.peliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasApi.peliasPlaceRequest
import no.entur.geocoder.proxy.pelias.PeliasApi.peliasReverseRequest
import no.entur.geocoder.proxy.v3.V3Api.autocompleteRequest as v3AutocompleteRequest
import no.entur.geocoder.proxy.v3.V3Api.reverseRequest as v3ReverseRequest

object Routing {
    fun Application.configureRouting(
        client: HttpClient,
        photonBaseUrl: String,
        appMicrometerRegistry: PrometheusMeterRegistry,
    ) {
        val healthCheck = HealthCheck(client, photonBaseUrl)

        routing {
            get("/v2/autocomplete") {
                peliasAutocompleteRequest(photonBaseUrl, client)
            }

            get("/v2/search") {
                peliasAutocompleteRequest(photonBaseUrl, client)
            }

            get("/v2/reverse") {
                peliasReverseRequest(photonBaseUrl, client)
            }

            get("/v2/nearby") {
                peliasReverseRequest(photonBaseUrl, client)
            }

            get("/v2/place") {
                peliasPlaceRequest(photonBaseUrl, client)
            }

            get("/v3/autocomplete") {
                v3AutocompleteRequest(photonBaseUrl, client)
            }

            get("/v3/reverse") {
                v3ReverseRequest(photonBaseUrl, client)
            }

            get("/") {
                rootRequest()
            }

            get("/actuator/health/liveness") {
                healthCheck.checkLiveness(call)
            }

            get("/actuator/health/readiness") {
                healthCheck.checkReadiness(call)
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
}

