package no.entur.geocoder.proxy

import io.ktor.server.request.ApplicationRequest

data class PeliasAutocompleteParams(
    val text: String = "",
    val size: Int = 10,
    val lang: String = "no",
    val boundaryCountry: String? = null,
    val boundaryCountyIds: List<String> = emptyList(),
    val boundaryLocalityIds: List<String> = emptyList(),
    val tariffZones: List<String> = emptyList(),
    val tariffZoneAuthorities: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val layers: List<String> = emptyList(),
    val categories: List<String> = emptyList()
) {
    companion object {
        fun fromRequest(request: ApplicationRequest): PeliasAutocompleteParams {
            val params = request.queryParameters
            return PeliasAutocompleteParams(
                text = params["text"] ?: "",
                size = params["size"]?.toIntOrNull() ?: 10,
                lang = params["lang"] ?: "no",
                boundaryCountry = params["boundary.country"],
                boundaryCountyIds = params["boundary.county.ids"]?.split(",") ?: emptyList(),
                boundaryLocalityIds = params["boundary.locality.ids"]?.split(",") ?: emptyList(),
                tariffZones = params["tariff_zone_ids"]?.split(",") ?: emptyList(),
                tariffZoneAuthorities = params["tariff_zone_authorities"]?.split(",") ?: emptyList(),
                sources = params["sources"]?.split(",") ?: emptyList(),
                layers = params["layers"]?.split(",") ?: emptyList(),
                categories = params["transport_mode"]?.split(",") ?: emptyList()
            )
        }
    }
}

data class PeliasReverseParams(
    val lat: String = "",
    val lon: String = "",
    val radius: String? = null,
    val size: Int = 10,
    val lang: String = "no"
) {
    companion object {
        fun fromRequest(request: ApplicationRequest): PeliasReverseParams {
            val params = request.queryParameters
            return PeliasReverseParams(
                lat = params["point.lat"] ?: "",
                lon = params["point.lon"] ?: "",
                radius = params["boundary.circle.radius"],
                size = params["size"]?.toIntOrNull() ?: 10,
                lang = params["lang"] ?: "no"
            )
        }
    }
}

