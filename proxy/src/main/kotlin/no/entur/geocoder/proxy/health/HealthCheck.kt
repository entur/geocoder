package no.entur.geocoder.proxy.health

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import no.entur.geocoder.proxy.health.HealthCheck.Info.Build
import no.entur.geocoder.proxy.photon.PhotonResult
import org.slf4j.LoggerFactory

class HealthCheck(
    private val client: HttpClient,
    private val photonBaseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(HealthCheck::class.java)

    suspend fun checkLiveness(call: ApplicationCall) = respondUp(call)

    suspend fun checkReadiness(call: ApplicationCall) {
        try {
            val reason =
                withTimeout(5000) {
                    checkPhotonHealth()
                }

            reason?.let { respondDown(call, it) } ?: respondUp(call)
        } catch (_: TimeoutCancellationException) {
            logger.warn("Photon readiness check timed out")
            respondDown(call, "Timeout")
        } catch (e: Exception) {
            logger.error("Readiness check failed", e)
            respondDown(call, "Error: ${e.message}")
        }
    }

    private suspend fun checkPhotonHealth(): String? {
        val query = "Oslo"
        val response = client.get("$photonBaseUrl/api?q=$query&limit=1")

        if (!response.status.isSuccess()) {
            logger.warn("Photon not ready: ${response.status}")
            return "Photon returned ${response.status}"
        }

        val result =
            try {
                PhotonResult.parse(response.bodyAsText())
            } catch (e: Exception) {
                logger.warn("Failed to parse Photon response: ${e.message}")
                return "Invalid response format"
            }

        if (result.features.isEmpty() ||
            result.features
                .first()
                .properties.name
                ?.contains(query) == false
        ) {
            logger.warn("Photon ready but returned no results for test query")
            return "No results returned"
        }

        return null // All checks passed
    }

    private suspend fun respondDown(call: ApplicationCall, reason: String) {
        call.respond(
            HttpStatusCode.ServiceUnavailable,
            mapOf("status" to "DOWN", "reason" to reason),
        )
    }

    private suspend fun respondUp(call: ApplicationCall) {
        call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
    }

    data class Info(val build: Build) {
        data class Build(val version: String?, val name: String)
    }

    suspend fun info(call: RoutingCall) {
        val version = HealthCheck::class.java.getPackage().implementationVersion
        call.respond(Info(Build(version ?: "unknown", "geocoder-proxy")))
    }
}
