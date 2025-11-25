package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.entur.geocoder.proxy.health.HealthCheck
import no.entur.geocoder.proxy.pelias.PeliasApi
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.v3.V3Api

object Routing {
    fun Application.configureRouting(
        client: HttpClient,
        photonBaseUrl: String,
        appMicrometerRegistry: PrometheusMeterRegistry,
    ) {
        val photonApi = PhotonApi(client, photonBaseUrl)
        val api = PeliasApi(photonApi)
        val v3api = V3Api(photonApi)
        val healthCheck = HealthCheck(photonApi)

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
                val metrics = appMicrometerRegistry.scrape()
                call.respond(HttpStatusCode.OK, metrics)
            }
        }
    }

    private fun readIndexHtml(): ByteArray = (
        this::class.java.classLoader
            .getResourceAsStream("index.html")
            ?.readBytes()
            ?: throw IllegalStateException("index.html not found in resources")
    )
}
