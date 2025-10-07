package no.entur.netexphoton.proxy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.entur.netexphoton.proxy.Environment.CONSOLE
import no.entur.netexphoton.proxy.Routing.configureRouting
import org.slf4j.LoggerFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

private val httpClient =
    HttpClient(CIO) {
        install(ClientContentNegotiation) {
            jackson()
        }
    }
private val transformer = FeatureTransformer()
private val logger = LoggerFactory.getLogger("Proxy")

fun main() {
    val photonBaseUrl = if (Environment.detect() == CONSOLE) "http://localhost:2322" else "http://netex-photon-server"
    val proxyPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting Photon proxy server on port $proxyPort, forwarding to $photonBaseUrl")

    embeddedServer(Netty, port = proxyPort) {
        install(ServerContentNegotiation) {
            jackson()
        }
        configureRouting(httpClient, transformer, photonBaseUrl)
    }.start(wait = true)
}




