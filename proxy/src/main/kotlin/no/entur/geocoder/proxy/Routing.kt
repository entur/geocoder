package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.entur.geocoder.proxy.health.HealthCheck
import no.entur.geocoder.proxy.pelias.PeliasApi
import no.entur.geocoder.proxy.v3.V3Api
import org.slf4j.LoggerFactory

object Routing {
    fun Application.configureRouting(
        client: HttpClient,
        photonBaseUrl: String,
        appMicrometerRegistry: PrometheusMeterRegistry,
    ) {
        val healthCheck = HealthCheck(client, photonBaseUrl)
        val api = PeliasApi(client, photonBaseUrl)
        val v3api = V3Api(client, photonBaseUrl)

        routing {
            get("/v2/autocomplete") {
                okResponse {
                    api.autocomplete(call.request.queryParameters)
                }
            }

            get("/v2/search") {
                okResponse {
                    api.autocomplete(call.request.queryParameters)
                }
            }

            get("/v2/reverse") {
                okResponse {
                    api.reverse(call.request.queryParameters)
                }
            }

            get("/v2/nearby") {
                okResponse {
                    api.reverse(call.request.queryParameters)
                }
            }

            get("/v2/place") {
                okResponse {
                    api.place(call.request.queryParameters)
                }
            }

            get("/v3/autocomplete") {
                okResponse {
                    v3api.autocomplete(call.request.queryParameters)
                }
            }

            get("/v3/reverse") {
                okResponse {
                    v3api.reverse(call.request.queryParameters)
                }
            }

            get("/") {
                rootRequest()
            }

            get("/liveness") {
                handleResponse {
                    healthCheck.liveness()
                }
            }

            get("/readiness") {
                handleResponse {
                    healthCheck.readiness()
                }
            }

            get("/info") {
                handleResponse {
                    healthCheck.info()
                }
            }

            get("/metrics") {
                okResponse {
                    appMicrometerRegistry.scrape()
                }
            }
        }
    }

    private suspend fun RoutingContext.okResponse(handler: suspend RoutingContext.() -> Any) =
        this.handleResponse { HttpStatusCode.OK to handler() }

    private suspend fun RoutingContext.handleResponse(handler: suspend RoutingContext.() -> Pair<HttpStatusCode, Any>) {
        try {
            val res = handler.invoke(this)
            call.respond(res.first, res.second)
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
