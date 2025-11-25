package no.entur.geocoder.proxy.health

import io.ktor.http.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import no.entur.geocoder.proxy.ProxyResponse
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import org.slf4j.LoggerFactory

class HealthCheck(private val photonApi: PhotonApi) {
    private val logger = LoggerFactory.getLogger(HealthCheck::class.java)

    fun liveness(): ProxyResponse =
        respondUp()

    suspend fun readiness(): ProxyResponse {
        val reason =
            try {
                withTimeout(5000) {
                    checkPhotonHealth()
                }
            } catch (_: TimeoutCancellationException) {
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
        val result = photonApi.request(req)

        if (!result.status.isSuccess()) {
            logger.warn("Photon not ready: ${result.status}")
            return "Photon returned ${result.status}"
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
        ProxyResponse(mapOf("status" to "DOWN", "reason" to reason), HttpStatusCode.ServiceUnavailable)

    private fun respondUp() =
        ProxyResponse(mapOf("status" to "UP"))

    data class Info(
        val version: String?,
        val name: String,
        val photonVersion: String?,
        val photonImportDate: String?,
    )

    suspend fun info(): Info {
        val photonStatus = photonApi.status()
        val version = HealthCheck::class.java.getPackage().implementationVersion
        return Info(
            version = version ?: "unknown",
            name = "geocoder-proxy",
            photonVersion = photonStatus["version"] ?: "unknown",
            photonImportDate = photonStatus["import_date"] ?: "unknown",
        )
    }
}
