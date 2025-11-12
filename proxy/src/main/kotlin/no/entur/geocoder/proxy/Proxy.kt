package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.cors.routing.*
import no.entur.geocoder.proxy.Environment.CONSOLE
import no.entur.geocoder.proxy.Routing.configureRouting
import org.slf4j.LoggerFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

private val httpClient =
    HttpClient(CIO) {
        install(ClientContentNegotiation) {
            jackson()
        }
    }
private val logger = LoggerFactory.getLogger("Proxy")

private val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

// milliseconds in nanoseconds
private val Int.millis: Double get() = this * 1_000_000.0

// seconds in nanoseconds
private val Double.seconds: Double get() = this * 1_000_000_000.0

fun main() {
    val photonBaseUrl = if (Environment.detect() == CONSOLE) "http://localhost:2322" else "http://geocoder-photon"
    val proxyPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting Photon proxy server on port $proxyPort, forwarding to $photonBaseUrl")

    embeddedServer(Netty, port = proxyPort) {
        install(CORS) {
            anyHost()
            allowCredentials = true
            allowNonSimpleContentTypes = true
            allowHeader("et-client-name")
            allowHeader("x-correlation-id")
        }
        install(ServerContentNegotiation) {
            jackson {
                setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            }
        }
        install(MicrometerMetrics) {
            registry = appMicrometerRegistry
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
        configureRouting(httpClient, photonBaseUrl, appMicrometerRegistry)
    }.start(wait = true)
}
