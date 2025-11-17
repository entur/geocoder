package no.entur.geocoder.proxy.v3

import io.ktor.server.application.*

data class V3AutocompleteRequest(
    val query: String = "",
    val limit: Int = 10,
    val language: String = "no",
    val placeTypes: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val countyIds: List<String> = emptyList(),
    val localityIds: List<String> = emptyList(),
    val tariffZones: List<String> = emptyList(),
    val tariffZoneAuthorities: List<String> = emptyList(),
    val transportModes: List<String> = emptyList(),
) {
    companion object {
        fun ApplicationCall.v3AutocompleteRequest(): V3AutocompleteRequest {
            val params = request.queryParameters
            return V3AutocompleteRequest(
                query = params["query"] ?: params["q"] ?: "",
                limit = params["limit"]?.toIntOrNull() ?: 10,
                language = params["language"] ?: params["lang"] ?: "no",
                placeTypes = params["placeTypes"]?.split(",") ?: emptyList(),
                sources = params["sources"]?.split(",") ?: emptyList(),
                countries = params["countries"]?.split(",") ?: emptyList(),
                countyIds = params["countyIds"]?.split(",") ?: emptyList(),
                localityIds = params["localityIds"]?.split(",") ?: emptyList(),
                tariffZones = params["tariffZones"]?.split(",") ?: emptyList(),
                tariffZoneAuthorities = params["tariffZoneAuthorities"]?.split(",") ?: emptyList(),
                transportModes = params["transportModes"]?.split(",") ?: emptyList(),
            )
        }
    }
}
