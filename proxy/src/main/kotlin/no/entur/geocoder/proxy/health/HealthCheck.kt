package no.entur.geocoder.proxy.health

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.withTimeout
import no.entur.geocoder.proxy.health.HealthCheck.Info.Build
import no.entur.geocoder.proxy.photon.PhotonResult
import org.slf4j.LoggerFactory

class HealthCheck(private val client: HttpClient, private val photonBaseUrl: String) {
    private val logger = LoggerFactory.getLogger(HealthCheck::class.java)

    suspend fun liveness(): Pair<HttpStatusCode, Any> =
        respondUp()

    suspend fun readiness(): Pair<HttpStatusCode, Any> {
        val reason =
            try {
                withTimeout(5000) {
                    checkPhotonHealth()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                logger.warn("Timeout checking Photon health")
                "Timeout"
            } catch (e: Exception) {
                logger.warn("Error checking Photon health: ${e.message}", e)
                "Error: ${e.message ?: e::class.simpleName}"
            }

        return reason?.let { respondDown(it) } ?: respondUp()
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

    private fun respondDown(reason: String) =
        HttpStatusCode.ServiceUnavailable to mapOf("status" to "DOWN", "reason" to reason)

    private fun respondUp() =
        HttpStatusCode.OK to mapOf("status" to "UP")

    data class Info(val build: Build) {
        data class Build(val version: String?, val name: String)
    }

    fun info(): Pair<HttpStatusCode, Info> {
        val version = HealthCheck::class.java.getPackage().implementationVersion
        return HttpStatusCode.OK to Info(Build(version ?: "unknown", "geocoder-proxy"))
    }
}
