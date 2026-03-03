package no.entur.geocoder.proxy.v3

import io.ktor.http.*

data class V3PlaceRequest(val ids: List<String>) {
    init {
        require(ids.isNotEmpty()) { "Parameter 'ids' is required" }
    }

    companion object {
        fun from(req: Parameters) =
            V3PlaceRequest(
                ids = req["ids"]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            )
    }
}
