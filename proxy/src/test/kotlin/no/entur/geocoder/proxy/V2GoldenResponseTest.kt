package no.entur.geocoder.proxy

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.entur.geocoder.proxy.App.Companion.configureApp
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Pins the frozen v2 wire format byte for byte, including key order and null omission.
 * Expected bodies were captured from the Jackson 2 build; only the timestamp is normalized.
 */
class V2GoldenResponseTest {
    @Test
    fun autocomplete() = assertGolden("/v2/autocomplete?text=oslo&size=20&focus.point.lat=59.9&focus.point.lon=10.7", "autocomplete")

    @Test
    fun reverse() = assertGolden("/v2/reverse?point.lat=59.9&point.lon=10.7&size=20", "reverse")

    @Test
    fun place() = assertGolden("/v2/place?ids=NSR:StopPlace:5357", "place")

    @Test
    fun `invalid params`() = assertGolden("/v2/reverse?point.lat=abc", "invalid-params", HttpStatusCode.BadRequest)

    @Test
    fun `photon error`() = assertGolden("/v2/autocomplete?text=oslo&size=20", "photon-error", photonFails = true)

    private fun assertGolden(
        url: String,
        name: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        photonFails: Boolean = false,
    ) = testApplication {
        val mockEngine =
            MockEngine {
                if (photonFails) {
                    respond("boom", HttpStatusCode.InternalServerError)
                } else {
                    respond(resource("photon.json"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, Json.toString()))
                }
            }
        application {
            configureApp(
                client = HttpClient(mockEngine),
                photonBaseUrl = "http://photon-test",
                micrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            )
        }

        val response = client.get(url)

        assertEquals(status, response.status)
        assertEquals(Json, response.contentType()?.withoutParameters())
        assertEquals(resource("$name.json").trimEnd(), response.bodyAsText().replace(timestamp, "\"timestamp\":\"T\""))
    }

    private fun resource(name: String) =
        this::class.java.getResource("/v2-golden/$name")?.readText()
            ?: throw IllegalStateException("$name not found")

    private val timestamp = Regex("\"timestamp\":[0-9]+")
}
