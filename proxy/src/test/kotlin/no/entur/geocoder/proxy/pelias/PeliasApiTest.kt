package no.entur.geocoder.proxy.pelias

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.entur.geocoder.proxy.App.Companion.configureApp
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest.Companion.RESULT_PRUNING_HEADROOM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PeliasApiTest {
    private lateinit var testClient: HttpClient
    private val photonResponse = """{"type":"FeatureCollection","features":[]}"""
    private val recordedRequests = mutableListOf<HttpRequestData>()

    @BeforeEach
    fun setup() {
        recordedRequests.clear()
        testClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        recordedRequests.add(request)
                        respond(photonResponse, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                }
            }
    }

    private fun Application.setupRouting() {
        configureApp(
            testClient,
            "http://localhost:2322",
            PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        )
    }

    @Test
    fun `autocomplete endpoint returns expected response`() =
        testApplication {
            recordedRequests.clear()
            application { setupRouting() }
            val response =
                client.get("/v2/autocomplete?text=Oslo&size=1&lang=en&multiModal=parent") {
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            assertEquals(HttpStatusCode.OK, response.status)

            assertEquals(1, recordedRequests.size)
            val req = recordedRequests.first()
            assertTrue(req.url.encodedPath.endsWith("/api"))
            assertEquals("oslo", req.url.parameters["q"])
            assertEquals("${(1 + RESULT_PRUNING_HEADROOM)}", req.url.parameters["limit"])
            assertEquals("en", req.url.parameters["lang"])
        }

    @Test
    fun `reverse endpoint returns expected response`() =
        testApplication {
            recordedRequests.clear()
            application { setupRouting() }
            val response =
                client.get("/v2/reverse?point.lat=59.91&point.lon=10.75&lang=no&limit=10&multiModal=parent") {
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            assertEquals(HttpStatusCode.OK, response.status)

            assertEquals(1, recordedRequests.size)
            val req = recordedRequests.first()
            assertTrue(req.url.encodedPath.endsWith("/reverse"))
            assertEquals("59.91", req.url.parameters["lat"])
            assertEquals("10.75", req.url.parameters["lon"])
            assertEquals("no", req.url.parameters["lang"])
            assertEquals("10", req.url.parameters["limit"])
        }

    @Test
    fun `place endpoint returns expected response`() =
        testApplication {
            recordedRequests.clear()
            application { setupRouting() }
            val response =
                client.get("/v2/place?ids=foo:bar:baz,abc:def:xyz&multiModal=parent") {
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            assertEquals(HttpStatusCode.OK, response.status)

            assertEquals(2, recordedRequests.size)
            val req1 = recordedRequests[0]
            val req2 = recordedRequests[1]
            assertTrue(req1.url.encodedPath.endsWith("/api"))
            assertTrue(req2.url.encodedPath.endsWith("/api"))
            assertEquals("foo:bar:baz", req1.url.parameters["q"])
            assertEquals("1", req1.url.parameters["limit"])
            assertEquals("abc:def:xyz", req2.url.parameters["q"])
            assertEquals("1", req2.url.parameters["limit"])
        }
}
