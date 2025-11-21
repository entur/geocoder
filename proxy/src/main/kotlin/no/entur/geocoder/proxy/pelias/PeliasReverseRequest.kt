package no.entur.geocoder.proxy.pelias

import io.ktor.http.*
import no.entur.geocoder.proxy.Text.safeVar
import no.entur.geocoder.proxy.Text.safeVars

data class PeliasReverseRequest(
    val lat: Double,
    val lon: Double,
    val radius: Double? = null,
    val size: Int = 10,
    val lang: String = "no",
    val boundaryCountry: String? = null,
    val boundaryCountyIds: List<String> = emptyList(),
    val boundaryLocalityIds: List<String> = emptyList(),
    val tariffZones: List<String> = emptyList(),
    val tariffZoneAuthorities: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val layers: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val multiModal: String,
    val debug: Boolean = false,
) {
    init {
        require(lat in -90.0..90.0) { "Parameter 'point.lat' must be between -90 and 90" }
        require(lon in -180.0..180.0) { "Parameter 'point.lon' must be between -180 and 180" }
    }

    companion object {
        fun from(req: Parameters) =
            PeliasReverseRequest(
                lat = req["point.lat"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Parameter 'point.lat' is required"),
                lon = req["point.lon"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Parameter 'point.lon' is required"),
                radius = req["boundary.circle.radius"]?.toDoubleOrNull(),
                size = req["size"]?.toIntOrNull() ?: 10,
                lang = req["lang"].safeVar() ?: "no",
                boundaryCountry = req["boundary.country"]?.safeVar(),
                boundaryCountyIds = req["boundary.county.ids"]?.split(",").safeVars() ?: emptyList(),
                boundaryLocalityIds = req["boundary.locality.ids"]?.split(",").safeVars() ?: emptyList(),
                tariffZones = req["tariff_zone_ids"]?.split(",")?.safeVars() ?: emptyList(),
                tariffZoneAuthorities = req["tariff_zone_authorities"]?.split(",").safeVars() ?: emptyList(),
                sources = req["sources"]?.split(",").safeVars() ?: emptyList(),
                layers = req["layers"]?.split(",").safeVars() ?: emptyList(),
                categories = req["categories"]?.split(",").safeVars() ?: emptyList(),
                multiModal =
                    when (req["multiModal"]) {
                        "child" -> "child"
                        "parent" -> "parent"
                        "all" -> "all"
                        else -> "parent"
                    },
                debug = req["debug"].toBoolean(),
            )
    }
}
