package no.entur.geocoder.proxy.v3

import io.ktor.http.*

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
        fun from(req: Parameters) =
            V3AutocompleteRequest(
                query = req["query"] ?: req["q"] ?: "",
                limit = req["limit"]?.toIntOrNull() ?: 10,
                language = req["language"] ?: req["lang"] ?: "no",
                placeTypes = req["placeTypes"]?.split(",") ?: emptyList(),
                sources = req["sources"]?.split(",") ?: emptyList(),
                countries = req["countries"]?.split(",") ?: emptyList(),
                countyIds = req["countyIds"]?.split(",") ?: emptyList(),
                localityIds = req["localityIds"]?.split(",") ?: emptyList(),
                tariffZones = req["tariffZones"]?.split(",") ?: emptyList(),
                tariffZoneAuthorities = req["tariffZoneAuthorities"]?.split(",") ?: emptyList(),
                transportModes = req["transportModes"]?.split(",") ?: emptyList(),
            )
    }
}
