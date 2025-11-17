package no.entur.geocoder.proxy.pelias

import io.ktor.server.application.*
import no.entur.geocoder.proxy.Text.safeVar
import no.entur.geocoder.proxy.Text.safeVars
import java.math.BigDecimal

data class PeliasReverseRequest(
    val lat: BigDecimal,
    val lon: BigDecimal,
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
) {
    init {
        val latitude = lat.toDouble()
        val longitude = lon.toDouble()
        require(latitude in -90.0..90.0) { "Parameter 'point.lat' must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Parameter 'point.lon' must be between -180 and 180" }
    }

    companion object {
        fun ApplicationCall.peliasReverseRequest(): PeliasReverseRequest {
            val req = request.queryParameters
            return PeliasReverseRequest(
                lat = req["point.lat"]?.toBigDecimalOrNull() ?: throw IllegalArgumentException("Parameter 'point.lat' is required"),
                lon = req["point.lon"]?.toBigDecimalOrNull() ?: throw IllegalArgumentException("Parameter 'point.lon' is required"),
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
            )
        }
    }
}
