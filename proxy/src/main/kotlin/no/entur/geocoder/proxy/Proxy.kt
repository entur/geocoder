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
import java.time.Duration

private val httpClient =
    HttpClient(CIO) {
        install(ClientContentNegotiation) {
            jackson()
        }
    }
private val logger = LoggerFactory.getLogger("Proxy")

private val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun main() {
    val photonBaseUrl = if (Environment.detect() == CONSOLE) "http://localhost:2322" else "http://geocoder-photon"
    val proxyPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting Photon proxy server on port $proxyPort, forwarding to $photonBaseUrl")

    embeddedServer(Netty, port = proxyPort) {
        install(CORS) {
            anyHost()
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }
        install(ServerContentNegotiation) {
            jackson()
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
                        Duration.ofMillis(5).toNanos().toDouble(),
                        Duration.ofMillis(10).toNanos().toDouble(),
                        Duration.ofMillis(25).toNanos().toDouble(),
                        Duration.ofMillis(50).toNanos().toDouble(),
                        Duration.ofMillis(75).toNanos().toDouble(),
                        Duration.ofMillis(100).toNanos().toDouble(),
                        Duration.ofMillis(250).toNanos().toDouble(),
                        Duration.ofMillis(500).toNanos().toDouble(),
                        Duration.ofMillis(750).toNanos().toDouble(),
                        Duration.ofSeconds(1).toNanos().toDouble(),
                        Duration.ofMillis(2500).toNanos().toDouble(),
                        Duration.ofSeconds(5).toNanos().toDouble(),
                        Duration.ofMillis(7500).toNanos().toDouble(),
                        Duration.ofSeconds(10).toNanos().toDouble(),
                    ).build()
        }
        configureRouting(httpClient, photonBaseUrl, appMicrometerRegistry)
    }.start(wait = true)
}
