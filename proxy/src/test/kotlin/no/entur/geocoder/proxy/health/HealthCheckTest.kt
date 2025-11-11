package no.entur.geocoder.proxy.health

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for HealthCheck endpoints.
 */
class HealthCheckTest {
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private const val DEFAULT_PHOTON_URL = "http://photon"
        private const val CUSTOM_PHOTON_URL = "https://custom.photon.server:8080"
        private const val LIVENESS_ENDPOINT = "/actuator/health/liveness"
        private const val READINESS_ENDPOINT = "/actuator/health/readiness"
        private const val SUCCESS_RESPONSE = """{"type":"FeatureCollection","features":[]}"""
    }

    /** Configures test application with health check endpoint. */
    private fun ApplicationTestBuilder.setupHealthCheckEndpoint(
        endpoint: String,
        photonUrl: String = DEFAULT_PHOTON_URL,
        mockEngineHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) {
        application {
            install(ContentNegotiation) { jackson() }
            routing {
                get(endpoint) {
                    val healthCheck = HealthCheck(HttpClient(MockEngine(mockEngineHandler)), photonUrl)
                    when (endpoint) {
                        LIVENESS_ENDPOINT -> healthCheck.checkLiveness(call)
                        READINESS_ENDPOINT -> healthCheck.checkReadiness(call)
                    }
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
        val result: Map<String, String> = objectMapper.readValue(response.bodyAsText())

        assertEquals(expectedStatus, response.status)
        assertEquals(expectedHealthStatus, result["status"])

        expectedReason?.let { assertEquals(it, result["reason"]) }
    }

    /** Creates a successful Photon response handler. */
    private fun createSuccessfulPhotonResponse(
        validateUrl: Boolean = false,
    ): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        if (validateUrl) assertEquals("$DEFAULT_PHOTON_URL/api?q=Oslo&limit=1", request.url.toString())
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
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createSuccessfulPhotonResponse(validateUrl = true))
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.OK, "UP")
        }

    @Test
    fun `readiness check returns DOWN when Photon returns error`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createErrorResponse(HttpStatusCode.InternalServerError))
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.ServiceUnavailable, "DOWN", "Photon unavailable")
        }

    @Test
    fun `readiness check returns DOWN when Photon is not found`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createErrorResponse(HttpStatusCode.NotFound))
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.ServiceUnavailable, "DOWN", "Photon unavailable")
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
            val result: Map<String, String> = objectMapper.readValue(response.bodyAsText())

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals("DOWN", result["status"])
            assertTrue(result["reason"]?.contains("Error") == true)
        }

    @Test
    fun `readiness check handles different HTTP error codes`() =
        testApplication {
            setupHealthCheckEndpoint(READINESS_ENDPOINT, mockEngineHandler = createErrorResponse(HttpStatusCode.BadGateway))
            performHealthCheckAndValidate(READINESS_ENDPOINT, HttpStatusCode.ServiceUnavailable, "DOWN", "Photon unavailable")
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
}
