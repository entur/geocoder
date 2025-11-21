package no.entur.geocoder.proxy.pelias

import io.ktor.http.*
import no.entur.geocoder.proxy.Text.safeVars

data class PeliasPlaceRequest(val ids: List<String> = emptyList(), val debug: Boolean = false) {
    init {
        require(ids.isNotEmpty()) { "Parameter 'ids' is required" }
        require(ids.all { it.split(":").size == 3 }) { "id must be colon separated" }
    }

    companion object {
        fun from(req: Parameters) =
            PeliasPlaceRequest(
                ids =
                    req["ids"]
                        ?.split(",")
                        ?.map { it.split(":").takeLast(3).joinToString(":") }
                        .safeVars()
                        ?: emptyList(),
                debug = req["debug"].toBoolean(),
            )
    }
}
