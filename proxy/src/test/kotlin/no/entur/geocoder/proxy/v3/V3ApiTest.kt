package no.entur.geocoder.proxy.v3

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.entur.geocoder.proxy.App.Companion.configureApp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Pins the wire format of requests the v3 endpoints send to Photon. */
class V3ApiTest {
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
    fun `autocomplete sends bbox to photon as comma-separated string`() =
        testApplication {
            application { setupRouting() }
            val response =
                client.get("/v3/autocomplete?q=oslo&bbox=10.5,59.8,10.9,60.0") {
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("10.5,59.8,10.9,60.0", recordedRequests.single().url.parameters["bbox"])
        }

    @Test
    fun `autocomplete omits bbox when not requested`() =
        testApplication {
            application { setupRouting() }
            client.get("/v3/autocomplete?q=oslo") {
            }
            assertNull(recordedRequests.single().url.parameters["bbox"])
        }

    @Test
    fun `reverse sends distance_sort to photon only when disabled`() =
        testApplication {
            application { setupRouting() }
            client.get("/v3/reverse?lat=59.91&lon=10.75&distanceSort=false") {
            }
            assertEquals("false", recordedRequests.single().url.parameters["distance_sort"])

            recordedRequests.clear()
            client.get("/v3/reverse?lat=59.91&lon=10.75") {
            }
            // Photon defaults to distance sorting; the default is not sent.
            assertNull(recordedRequests.single().url.parameters["distance_sort"])
        }

    @Test
    fun `stopPlaceTypes is sent to photon as a stop_place_type include group`() =
        testApplication {
            application { setupRouting() }
            client.get("/v3/reverse?lat=59.91&lon=10.75&stopPlaceTypes=railStation,airport") {
            }
            val includes = recordedRequests.single().url.parameters.getAll("include").orEmpty()
            assertEquals(true, includes.contains("stop_place_type.railStation,stop_place_type.airport"), "Got: $includes")
        }

    @Test
    fun `reverse rejects garbage distanceSort`() =
        testApplication {
            application { setupRouting() }
            val response =
                client.get("/v3/reverse?lat=59.91&lon=10.75&distanceSort=maybe") {
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
