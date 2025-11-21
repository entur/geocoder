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
import org.slf4j.LoggerFactory
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
                handleRequest {
                    peliasAutocompleteRequest(photonBaseUrl, client)
                }
            }

            get("/v2/search") {
                handleRequest {
                    peliasAutocompleteRequest(photonBaseUrl, client)
                }
            }

            get("/v2/reverse") {
                handleRequest {
                    peliasReverseRequest(photonBaseUrl, client)
                }
            }

            get("/v2/nearby") {
                handleRequest {
                    peliasReverseRequest(photonBaseUrl, client)
                }
            }

            get("/v2/place") {
                handleRequest {
                    peliasPlaceRequest(photonBaseUrl, client)
                }
            }

            get("/v3/autocomplete") {
                handleRequest {
                    v3AutocompleteRequest(photonBaseUrl, client)
                }
            }

            get("/v3/reverse") {
                handleRequest {
                    v3ReverseRequest(photonBaseUrl, client)
                }
            }

            get("/") {
                rootRequest()
            }

            get("/liveness") {
                healthCheck.checkLiveness(call)
            }

            get("/readiness") {
                healthCheck.checkReadiness(call)
            }

            get("/info") {
                healthCheck.info(call)
            }

            get("/metrics") {
                call.respond(appMicrometerRegistry.scrape())
            }
        }
    }

    private suspend fun RoutingContext.handleRequest(handler: suspend RoutingContext.() -> Unit) {
        try {
            handler()
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid request parameters: ${e.message}")
            val error = ErrorHandler.handleError(e, "Invalid parameters")
            call.respond(error.status, error.result)
        } catch (e: Exception) {
            logger.error("Unexpected error: ${e.message}", e)
            val error = ErrorHandler.handleError(e, "Request processing")
            call.respond(error.status, error.result)
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

    private val logger = LoggerFactory.getLogger(Routing::class.java)
}
