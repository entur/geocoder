package no.entur.geocoder.proxy.v3

import io.ktor.server.application.*
import java.math.BigDecimal

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
    val transportModes: List<String> = emptyList(),
) {
    companion object {
        fun ApplicationCall.v3AutocompleteParams(): V3AutocompleteParams {
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
                transportModes = params["transportModes"]?.split(",") ?: emptyList(),
            )
        }
    }
}

data class V3ReverseParams(
    val lat: BigDecimal,
    val lon: BigDecimal,
    val radius: Int? = null,
    val limit: Int = 10,
    val language: String = "no",
) {
    init {
        require(lat.toDouble() in -90.0..90.0) { "Parameter 'latitude' must be between -90 and 90" }
        require(lon.toDouble() in -180.0..180.0) { "Parameter 'longitude' must be between -180 and 180" }
    }

    companion object {
        fun ApplicationCall.v3ReverseParams(): V3ReverseParams {
            val params = request.queryParameters
            return V3ReverseParams(
                lat = params["latitude"]?.toBigDecimalOrNull() ?: throw IllegalArgumentException("Parameter 'latitude' is required"),
                lon =
                    params["longitude"]?.toBigDecimalOrNull()
                        ?: throw IllegalArgumentException("Parameter 'longitude' is required"),
                radius = params["radius"]?.toIntOrNull() ?: 10,
                limit = params["limit"]?.toIntOrNull() ?: 10,
                language = params["language"] ?: params["lang"] ?: "no",
            )
        }
    }
}
