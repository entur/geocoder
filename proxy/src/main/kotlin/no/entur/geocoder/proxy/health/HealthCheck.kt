package no.entur.geocoder.proxy.health

import io.ktor.client.*
import io.ktor.http.*
import kotlinx.coroutines.withTimeout
import no.entur.geocoder.proxy.Response
import no.entur.geocoder.proxy.health.HealthCheck.Info.Build
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import no.entur.geocoder.proxy.photon.PhotonResult
import org.slf4j.LoggerFactory

class HealthCheck(private val client: HttpClient, private val photonBaseUrl: String) {
    private val logger = LoggerFactory.getLogger(HealthCheck::class.java)

    suspend fun liveness(): Response =
        respondUp()

    suspend fun readiness(): Response {
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
        val req = PhotonAutocompleteRequest("Oslo", 1)
        val apiResponse = PhotonApi.request(req, client, "$photonBaseUrl/api")

        if (!apiResponse.status.isSuccess()) {
            logger.warn("Photon not ready: ${apiResponse.status}")
            return "Photon returned ${apiResponse.status}"
        }

        val result =
            try {
                PhotonResult.parse(apiResponse)
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
        Response(mapOf("status" to "DOWN", "reason" to reason), HttpStatusCode.ServiceUnavailable)

    private fun respondUp() =
        Response(mapOf("status" to "UP"), HttpStatusCode.OK)

    data class Info(val build: Build) {
        data class Build(val version: String?, val name: String)
    }

    fun info(): Info {
        val version = HealthCheck::class.java.getPackage().implementationVersion
        return Info(Build(version ?: "unknown", "geocoder-proxy"))
    }
}
