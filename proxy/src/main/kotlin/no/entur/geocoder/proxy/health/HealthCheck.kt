package no.entur.geocoder.proxy.health

import io.ktor.http.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import no.entur.geocoder.proxy.photon.PhotonApi
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class HealthCheck(private val photonApi: PhotonApi, private val readinessQuery: String = DEFAULT_READINESS_QUERY) {
    private val logger = LoggerFactory.getLogger(HealthCheck::class.java)

    fun liveness(): HealthResponse =
        respondUp()

    suspend fun readiness(): HealthResponse {
        val reason =
            try {
                withTimeout(5000.milliseconds) {
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

    suspend fun info(): Info {
        val photonStatus = photonApi.status()
        val version = HealthCheck::class.java.getPackage().implementationVersion
        return Info(
            version = version ?: "unknown",
            name = "geocoder-proxy",
            photonVersion = photonStatus["version"] ?: "unknown",
            photonCommit = photonStatus["git_commit"] ?: "unknown",
            photonImportDate = photonStatus["import_date"] ?: "unknown",
        )
    }

    private suspend fun checkPhotonHealth(): String? {
        val result = photonApi.request(PhotonAutocompleteRequest(readinessQuery, 1))

        if (!result.status.isSuccess()) {
            logger.warn("Photon not ready: ${result.status}")
            return "Photon returned ${result.status}"
        }

        if (result.features.isEmpty() ||
            result.features
                .first()
                .properties.name
                ?.contains(readinessQuery, ignoreCase = true) == false
        ) {
            logger.warn("Photon ready but returned no results for test query")
            return "No results returned"
        }

        return null // All checks passed
    }

    private fun respondDown(reason: String) =
        HealthResponse(mapOf("status" to "DOWN", "reason" to reason), HttpStatusCode.ServiceUnavailable)

    private fun respondUp() =
        HealthResponse(mapOf("status" to "UP"))

    data class Info(
        val version: String?,
        val name: String,
        val photonVersion: String?,
        val photonCommit: String?,
        val photonImportDate: String?,
    )

    data class HealthResponse(val message: Any, val status: HttpStatusCode = HttpStatusCode.OK)

    companion object {
        const val DEFAULT_READINESS_QUERY = "Oslo"
    }
}
