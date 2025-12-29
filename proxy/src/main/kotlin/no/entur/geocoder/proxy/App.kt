package no.entur.geocoder.proxy

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.AuthenticationFailedCause.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.entur.geocoder.proxy.Environment.CONSOLE
import no.entur.geocoder.proxy.health.HealthCheck
import no.entur.geocoder.proxy.pelias.PeliasApi
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.v3.V3Api
import org.slf4j.LoggerFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class App {
    val photonBaseUrl = if (Environment.detect() == CONSOLE) "http://localhost:2322" else "http://geocoder-photon"
    val proxyPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080

    fun startServer() {
        logger.info("Starting geocoder-proxy on port $proxyPort, forwarding to $photonBaseUrl")

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

            // Align with CORS config in Apigee:
            // See https://console.cloud.google.com/apigee/proxies/geocoder/develop/32/policies/assignMessage.addCors?project=ent-apigee-shr-001
            install(CORS) {
                anyHost()
                anyMethod()
                allowCredentials = true
                allowNonSimpleContentTypes = true
                allowHeader("entur-pos")
                allowHeader("et-client-name")
                allowHeader("et-client-id")
                allowHeader("x-correlation-id")
                allowHeader("x-requested-with")
            }
            install(Authentication) {
                provider("apigee-auth") {
                    authenticate { context ->
                        val expectedSecret: String = System.getenv("APIGEE_SECRET") ?: run {
                            logger.warn("Couldn't find APIGEE_SECRET env var, using default dummy secret")
                            "dummy-secret"
                        }
                        val incomingSecret = context.call.request.header("x-apigee-secret")

                        if (incomingSecret == expectedSecret) {
                            context.principal(UserIdPrincipal("apigee-proxy"))
                        } else {
                            context.challenge("apigee-auth", InvalidCredentials) { _, call ->
                                call.respond(HttpStatusCode.Unauthorized, "Invalid or missing auth")
                            }
                        }
                    }
                }
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
                authenticate("apigee-auth") {
                    get("/v2/autocomplete") {
                        val result = api.autocomplete(call.request.queryParameters)
                        call.respond(result)
                    }

                    get("/v2/search") {
                        val result = api.autocomplete(call.request.queryParameters)
                        call.respond(result)
                    }

                    get("/v2/reverse") {
                        val result = api.reverse(call.request.queryParameters)
                        call.respond(result)
                    }

                    get("/v2/nearby") {
                        val result = api.reverse(call.request.queryParameters)
                        call.respond(result)
                    }

                    get("/v2/place") {
                        val result = api.place(call.request.queryParameters)
                        call.respond(result)
                    }

                    get("/v3/autocomplete") {
                        val result = v3api.autocomplete(call.request.queryParameters)
                        call.respond(result)
                    }

                    get("/v3/reverse") {
                        val result = v3api.reverse(call.request.queryParameters)
                        call.respond(result)
                    }
                }
                get("/") {
                    val indexHtml = readFile("index.html")
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
                    call.respond(info)
                }

                get("/metrics") {
                    val metrics = micrometerRegistry.scrape()
                    call.respond(metrics)
                }

                get("/v2/openapi.yaml") {
                    val openapi = readFile("openapi.yml")
                    call.respondText(String(openapi), contentType = ContentType.parse("application/yaml"))
                }
            }
        }

        private val httpClient =
            HttpClient(CIO) {
                install(ClientContentNegotiation) {
                    jackson()
                }
            }

        private fun readFile(name: String): ByteArray = (
            this::class.java.classLoader
                .getResourceAsStream(name)
                ?.readBytes()
                ?: throw IllegalStateException("$name not found")
            )

        private val logger = LoggerFactory.getLogger("App")
        private val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

        // milliseconds in nanoseconds
        private val Int.millis: Double get() = this * 1_000_000.0

        // seconds in nanoseconds
        private val Double.seconds: Double get() = this * 1_000_000_000.0
    }
}

fun main() {
    App().startServer()
}
