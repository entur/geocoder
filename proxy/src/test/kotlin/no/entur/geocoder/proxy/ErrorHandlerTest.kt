package no.entur.geocoder.proxy

import com.fasterxml.jackson.core.JsonParseException
import io.ktor.http.*
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ErrorHandlerTest {
    @Test
    fun `handleError creates error for JsonParseException`() {
        val exception = JsonParseException(null, "Invalid JSON")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertContains(msg(error), "Invalid response from backend")
        assertEquals(HttpStatusCode.BadGateway, error.status)
        assertContains(msg(error), "unexpected response")
    }

    @Test
    fun `handleError creates error for IOException`() {
        val exception = IOException("Connection failed")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertContains(msg(error), "Connection failed")
        assertEquals(HttpStatusCode.ServiceUnavailable, error.status)
        assertContains(msg(error), "Unable to connect to geocoding service")
    }

    @Test
    fun `handleError creates error for IllegalArgumentException`() {
        val exception = IllegalArgumentException("Invalid latitude value")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertContains(msg(error), "Invalid parameters")
        assertContains(msg(error), "Invalid latitude value")
        assertEquals(HttpStatusCode.BadRequest, error.status)
    }

    @Test
    fun `handleError creates error for IllegalArgumentException with null message`() {
        val exception = IllegalArgumentException()
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertContains(msg(error), "Invalid parameters")
        assertContains(msg(error), "One or more parameters are invalid")
        assertEquals(HttpStatusCode.BadRequest, error.status)
    }

    @Test
    fun `handleError creates error for generic exception`() {
        val exception = RuntimeException("Something went wrong")
        val error = ErrorHandler.handleError(exception, "geocoding")

        assertContains(msg(error), "geocoding failed")
        assertContains(msg(error), "An unexpected error occurred")
        assertEquals(HttpStatusCode.InternalServerError, error.status)
    }

    @Test
    fun `handleError preserves operation name in generic errors`() {
        val exception = RuntimeException()
        val error = ErrorHandler.handleError(exception, "reverse geocoding")

        assertContains(msg(error), "reverse geocoding failed")
    }

    private fun msg(error: ErrorHandler.PeliasError): String =
        error.result.geocoding.errors
            .orEmpty()
            .first()
}
