package no.entur.geocoder.proxy.health

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import no.entur.geocoder.common.JsonMapper.jacksonMapper
import no.entur.geocoder.proxy.health.HealthCheck.HealthResponse
import no.entur.geocoder.proxy.photon.PhotonApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for HealthCheck endpoints.
 */
class HealthCheckTest {
    companion object {
        private const val DEFAULT_PHOTON_URL = "http://photon"
        private const val CUSTOM_PHOTON_URL = "https://custom.photon.server:8080"
        private const val LIVENESS_ENDPOINT = "/liveness"
        private const val READINESS_ENDPOINT = "/readiness"
        private const val SUCCESS_RESPONSE =
            """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[10.75,59.91]},"properties":{"name":"Oslo"}}]}"""
    }

    /** Configures test application with health check endpoint. */
    private fun ApplicationTestBuilder.setupHealthCheckEndpoint(
        endpoint: String,
        photonUrl: String = DEFAULT_PHOTON_URL,
        mockEngineHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) {
        application {
            install(ContentNegotiation) {
                jackson {
                    setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                }
            }
            routing {
                get(endpoint) {
                    val client = HttpClient(MockEngine(mockEngineHandler))
                    val photonApi = PhotonApi(client, photonUrl)
                    val healthCheck = HealthCheck(photonApi)
                    val response =
                        when (endpoint) {
                            LIVENESS_ENDPOINT -> healthCheck.liveness()
                            READINESS_ENDPOINT -> healthCheck.readiness()
                            else -> HealthResponse(mapOf("error" to "Unknown endpoint"), HttpStatusCode.NotFound)
                        }
                    call.respondText(
                        jacksonMapper.writeValueAsString(response.message),
                        ContentType.Application.Json,
                        response.status,
                    )
                }
            }
        }
    }

    /** Validates health check response. */
    private suspend fun ApplicationTestBuilder.performHealthCheckAndValidate(
        endpoint: String,
        expectedStatus: HttpStatusCode,
        expectedHealthStatus: String,
        expectedReason: String? = null,
    ) {
        val response = client.get(endpoint)
        val result: Map<String, String> = jacksonMapper.readValue(response.bodyAsText())

        assertEquals(expectedStatus, response.status)
        assertEquals(expectedHealthStatus, result["status"])

        expectedReason?.let { assertEquals(it, result["reason"]) }
    }

    /** Creates a successful Photon response handler. */
    private fun createSuccessfulPhotonResponse(): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        val url = request.url.toString()
        assertTrue(url.startsWith("$DEFAULT_PHOTON_URL/api"))
        assertTrue(url.contains("q=Oslo"))
        assertTrue(url.contains("limit=1"))
        respond(SUCCESS_RESPONSE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    /** Creates an error response handler. */
    private fun createErrorResponse(statusCode: HttpStatusCode): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        respond("Error", statusCode)
    }

    /** Creates a timeout response handler (exceeds 5 second timeout). */
    private fun createTimeoutResponse(): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        delay(6000) // Exceeds the 5 second timeout
        respond("OK", HttpStatusCode.OK)
    }

    /** Creates an exception response handler. */
    private fun createExceptionResponse(): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        throw Exception("Network error")
    }

    @Test
    fun `liveness check always returns UP`() =
        testApplication {
            setupHealthCheckEndpoint(LIVENESS_ENDPOINT) {
                respond("OK", HttpStatusCode.OK)
            }
            performHealthCheckAndValidate(LIVENESS_ENDPOINT, HttpStatusCode.OK, "UP")
        }

    @Test
    fun `liveness check with different photon base URLs`() =
        testApplication {
            setupHealthCheckEndpoint(LIVENESS_ENDPOINT, CUSTOM_PHOTON_URL) {
                respond("OK", HttpStatusCode.OK)
            }
            performHealthCheckAndValidate(LIVENESS_ENDPOINT, HttpStatusCode.OK, "UP")
        }

    @Test
    fun `readiness check returns UP when Photon is available`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createSuccessfulPhotonResponse())
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.OK, "UP")
        }

    @Test
    fun `readiness check returns DOWN when Photon returns error`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createErrorResponse(HttpStatusCode.InternalServerError))
            performHealthCheckAndValidate(
                READINESS_ENDPOINT,
                HttpStatusCode.ServiceUnavailable,
                "DOWN",
                "Photon returned 500 Internal Server Error",
            )
        }

    @Test
    fun `readiness check returns DOWN when Photon is not found`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createErrorResponse(HttpStatusCode.NotFound))
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.ServiceUnavailable, "DOWN", "Photon returned 404 Not Found")
        }

    @Test
    fun `readiness check returns DOWN on timeout`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createTimeoutResponse())
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.ServiceUnavailable, "DOWN", "Timeout")
        }

    @Test
    fun `readiness check returns DOWN on network exception`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createExceptionResponse())

            val response = client.get(READINESS_ENDPOINT)
            val result: Map<String, String> = jacksonMapper.readValue(response.bodyAsText())

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals("DOWN", result["status"])
            assertTrue(result["reason"]?.contains("Error") == true)
        }

    @Test
    fun `readiness check handles different HTTP error codes`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createErrorResponse(HttpStatusCode.BadGateway))
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.ServiceUnavailable, "DOWN", "Photon returned 502 Bad Gateway")
        }

    @Test
    fun `readiness check returns DOWN when Photon returns empty results`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT) {
                respond(
                    """{"type":"FeatureCollection","features":[]}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.ServiceUnavailable, "DOWN", "No results returned")
        }

    @Test
    fun `readiness check validates correct photon URL format`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, CUSTOM_PHOTON_URL) { request ->
                assertTrue(request.url.toString().startsWith("$CUSTOM_PHOTON_URL/api"))
                respond(SUCCESS_RESPONSE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.OK, "UP")
        }

    @Test
    fun `readiness check returns DOWN when Photon returns malformed JSON`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT) {
                respond("Not valid JSON at all", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
            val response = client.get(READINESS_ENDPOINT)
            val result: Map<String, String> = jacksonMapper.readValue(response.bodyAsText())

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals("DOWN", result["status"])
            assertTrue(result["reason"]?.startsWith("Error:") == true)
        }
}
