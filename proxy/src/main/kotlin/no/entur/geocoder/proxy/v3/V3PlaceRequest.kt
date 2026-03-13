package no.entur.geocoder.proxy.v3

import io.ktor.http.*

data class V3PlaceRequest(val ids: List<String>) {
    init {
        require(ids.isNotEmpty()) { "Parameter 'ids' is required" }
    }

    companion object {
        internal val ALLOWED_PARAMS = setOf("ids")

        fun from(req: Parameters): V3PlaceRequest {
            val unknown = req.names().filterNot { it in ALLOWED_PARAMS }
            require(unknown.isEmpty()) { "Unknown parameter(s): ${unknown.joinToString()}" }

            return V3PlaceRequest(
                ids = req["ids"]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            )
        }
    }
}
