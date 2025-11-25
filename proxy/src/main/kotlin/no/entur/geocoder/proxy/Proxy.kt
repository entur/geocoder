package no.entur.geocoder.proxy

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.entur.geocoder.proxy.Environment.CONSOLE
import no.entur.geocoder.proxy.health.HealthCheck
import no.entur.geocoder.proxy.pelias.PeliasApi
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.v3.V3Api
import org.slf4j.LoggerFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main() {
    Proxy().startServer()
}

class Proxy {
    val photonBaseUrl = if (Environment.detect() == CONSOLE) "http://localhost:2322" else "http://geocoder-photon"
    val proxyPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080

    fun startServer() {
        logger.info("Starting Photon proxy server on port $proxyPort, forwarding to $photonBaseUrl")

        embeddedServer(Netty, port = proxyPort) {
            configureApp(httpClient, photonBaseUrl, appMicrometerRegistry)
        }.start(wait = true)
    }

    companion object {
        fun Application.configureApp(client: HttpClient, photonBaseUrl: String, micrometerRegistry: PrometheusMeterRegistry) {
            val photonApi = PhotonApi(client, photonBaseUrl)
            val api = PeliasApi(photonApi)
            val v3api = V3Api(photonApi)
            val healthCheck = HealthCheck(photonApi)

            install(CORS) {
                anyHost()
                allowCredentials = true
                allowNonSimpleContentTypes = true
                allowHeader("et-client-name")
                allowHeader("x-correlation-id")
            }
            install(ServerContentNegotiation) {
                jackson {
                    setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                }
            }
            install(StatusPages) {
                exception<Exception> { call, cause ->
                    val error = ErrorHandler.handleError(cause, "Request processing")
                    call.respond(error.status, error.result)
                }
            }
            install(MicrometerMetrics) {
                registry = micrometerRegistry
                meterBinders =
                    listOf(
                        JvmMemoryMetrics(),
                        JvmGcMetrics(),
                        ProcessorMetrics(),
                    )
                distributionStatisticConfig =
                    DistributionStatisticConfig
                        .Builder()
                        .percentilesHistogram(true)
                        .serviceLevelObjectives(
                            5.millis, 10.millis, 25.millis, 50.millis, 75.millis,
                            100.millis, 250.millis, 500.millis, 750.millis,
                            1.0.seconds, 2.5.seconds, 5.0.seconds, 7.5.seconds, 10.0.seconds,
                        ).build()
            }

            routing {
                get("/v2/autocomplete") {
                    val result = api.autocomplete(call.request.queryParameters)
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/v2/search") {
                    val result = api.autocomplete(call.request.queryParameters)
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/v2/reverse") {
                    val result = api.reverse(call.request.queryParameters)
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/v2/nearby") {
                    val result = api.reverse(call.request.queryParameters)
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/v2/place") {
                    val result = api.place(call.request.queryParameters)
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/v3/autocomplete") {
                    val result = v3api.autocomplete(call.request.queryParameters)
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/v3/reverse") {
                    val result = v3api.reverse(call.request.queryParameters)
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/") {
                    val indexHtml = readIndexHtml()
                    call.respondText(String(indexHtml), contentType = ContentType.Text.Html)
                }

                get("/liveness") {
                    val liveness = healthCheck.liveness()
                    call.respond(liveness.status, liveness.message)
                }

                get("/readiness") {
                    val readiness = healthCheck.readiness()
                    call.respond(readiness.status, readiness.message)
                }

                get("/info") {
                    val info = healthCheck.info()
                    call.respond(HttpStatusCode.OK, info)
                }

                get("/metrics") {
                    val metrics = micrometerRegistry.scrape()
                    call.respond(HttpStatusCode.OK, metrics)
                }
            }
        }

        private val httpClient =
            HttpClient(CIO) {
                install(ClientContentNegotiation) {
                    jackson()
                }
            }

        private fun readIndexHtml(): ByteArray = (
            this::class.java.classLoader
                .getResourceAsStream("index.html")
                ?.readBytes()
                ?: throw IllegalStateException("index.html not found in resources")
        )

        private val logger = LoggerFactory.getLogger("Proxy")
        private val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

        // milliseconds in nanoseconds
        private val Int.millis: Double get() = this * 1_000_000.0

        // seconds in nanoseconds
        private val Double.seconds: Double get() = this * 1_000_000_000.0
    }
}
