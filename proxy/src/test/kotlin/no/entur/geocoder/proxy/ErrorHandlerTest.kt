package no.entur.geocoder.proxy

import com.fasterxml.jackson.core.JsonParseException
import io.ktor.http.*
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorHandlerTest {

    @Test
    fun `handleError creates error for JsonParseException`() {
        val exception = JsonParseException(null, "Invalid JSON")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertEquals("Invalid response from backend", error.error)
        assertEquals(HttpStatusCode.BadGateway.value, error.statusCode)
        assertTrue(error.message.contains("unexpected response"))
    }

    @Test
    fun `handleError creates error for IOException`() {
        val exception = IOException("Connection failed")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertEquals("Connection failed", error.error)
        assertEquals(HttpStatusCode.ServiceUnavailable.value, error.statusCode)
        assertEquals("Unable to connect to geocoding service", error.message)
    }

    @Test
    fun `handleError creates error for IllegalArgumentException`() {
        val exception = IllegalArgumentException("Invalid latitude value")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertEquals("Invalid parameters", error.error)
        assertEquals("Invalid latitude value", error.message)
        assertEquals(HttpStatusCode.BadRequest.value, error.statusCode)
    }

    @Test
    fun `handleError creates error for IllegalArgumentException with null message`() {
        val exception = IllegalArgumentException()
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertEquals("Invalid parameters", error.error)
        assertEquals("One or more parameters are invalid", error.message)
        assertEquals(HttpStatusCode.BadRequest.value, error.statusCode)
    }

    @Test
    fun `handleError creates error for generic exception`() {
        val exception = RuntimeException("Something went wrong")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertEquals("geocoding failed", error.error)
        assertEquals("An unexpected error occurred", error.message)
        assertEquals(HttpStatusCode.InternalServerError.value, error.statusCode)
    }

    @Test
    fun `toJson serializes ApiError correctly`() {
        val error = ApiError(
            error = "Test error",
            message = "Test message",
            statusCode = 400
        )

        val json = ErrorHandler.toJson(error)

        assertTrue(json.contains("\"error\":\"Test error\""))
        assertTrue(json.contains("\"message\":\"Test message\""))
        assertTrue(json.contains("\"statusCode\":400"))
    }

    @Test
    fun `handleError preserves operation name in generic errors`() {
        val exception = RuntimeException()
        val error = ErrorHandler.handleError(exception, "reverse geocoding")

        assertEquals("reverse geocoding failed", error.error)
    }
}

