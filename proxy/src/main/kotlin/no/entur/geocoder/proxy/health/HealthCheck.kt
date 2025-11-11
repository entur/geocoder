package no.entur.geocoder.proxy.health

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

class HealthCheck(
    private val client: HttpClient,
    private val photonBaseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(HealthCheck::class.java)

    suspend fun checkLiveness(call: ApplicationCall) {
        call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
    }

    suspend fun checkReadiness(call: ApplicationCall) {
        try {
            withTimeout(3000) {
                val response = client.get("$photonBaseUrl/api?q=Oslo&limit=1")
                if (response.status.isSuccess()) {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
                } else {
                    logger.warn("Photon not ready: ${response.status}")
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "DOWN", "reason" to "Photon unavailable")
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            logger.warn("Photon readiness check timed out")
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("status" to "DOWN", "reason" to "Timeout")
            )
        } catch (e: Exception) {
            logger.error("Readiness check failed", e)
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("status" to "DOWN", "reason" to "Error: ${e.message}")
            )
        }
    }
}

