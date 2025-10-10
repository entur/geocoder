package no.entur.geocoder.proxy

import io.ktor.server.request.ApplicationRequest

data class V3AutocompleteParams(
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
    val transportModes: List<String> = emptyList()
) {
    companion object {
        fun fromRequest(request: ApplicationRequest): V3AutocompleteParams {
            val params = request.queryParameters
            return V3AutocompleteParams(
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
                transportModes = params["transportModes"]?.split(",") ?: emptyList()
            )
        }
    }
}

data class V3ReverseParams(
    val latitude: String = "",
    val longitude: String = "",
    val radius: String? = null,
    val limit: Int = 10,
    val language: String = "no"
) {
    init {
        require(latitude.isNotBlank()) { "Parameter 'latitude' is required" }
        require(longitude.isNotBlank()) { "Parameter 'longitude' is required" }
        require(latitude.toDoubleOrNull() != null) { "Parameter 'latitude' must be a valid number" }
        require(longitude.toDoubleOrNull() != null) { "Parameter 'longitude' must be a valid number" }
        val lat = latitude.toDouble()
        val lon = longitude.toDouble()
        require(lat in -90.0..90.0) { "Parameter 'latitude' must be between -90 and 90" }
        require(lon in -180.0..180.0) { "Parameter 'longitude' must be between -180 and 180" }
    }

    companion object {
        fun fromRequest(request: ApplicationRequest): V3ReverseParams {
            val params = request.queryParameters
            return V3ReverseParams(
                latitude = params["latitude"] ?: params["lat"] ?: "",
                longitude = params["longitude"] ?: params["lon"] ?: "",
                radius = params["radius"],
                limit = params["limit"]?.toIntOrNull() ?: 10,
                language = params["language"] ?: params["lang"] ?: "no"
            )
        }
    }
}

