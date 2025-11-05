package no.entur.geocoder.proxy.pelias

import io.ktor.server.request.*
import no.entur.geocoder.proxy.Text.safeVars

data class PeliasPlaceParams(val ids: List<String> = emptyList()) {
    init {
        require(ids.isNotEmpty()) { "Parameter 'ids' is required" }
        require(ids.all { it.split(":").size == 3 }) { "id must be colon separated" }
    }

    companion object {
        fun fromRequest(request: ApplicationRequest): PeliasPlaceParams {
            val params = request.queryParameters
            return PeliasPlaceParams(
                ids = params["ids"]
                    ?.split(",")
                    ?.map { it.split(":").takeLast(3).joinToString(":") }
                    .safeVars()
                    ?: emptyList(),
            )
        }
    }
}
