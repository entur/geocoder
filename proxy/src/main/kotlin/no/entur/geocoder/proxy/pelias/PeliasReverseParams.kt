package no.entur.geocoder.proxy.pelias

import io.ktor.server.request.*
import no.entur.geocoder.proxy.Text.safeVar
import no.entur.geocoder.proxy.Text.safeVars
import java.math.BigDecimal

data class PeliasReverseParams(
    val lat: BigDecimal,
    val lon: BigDecimal,
    val radius: Int? = null,
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
        fun fromRequest(request: ApplicationRequest): PeliasReverseParams {
            val params = request.queryParameters
            return PeliasReverseParams(
                lat = params["point.lat"]?.toBigDecimalOrNull() ?: throw IllegalArgumentException("Parameter 'point.lat' is required"),
                lon = params["point.lon"]?.toBigDecimalOrNull() ?: throw IllegalArgumentException("Parameter 'point.lon' is required"),
                radius = params["boundary.circle.radius"]?.toIntOrNull(),
                size = params["size"]?.toIntOrNull() ?: 10,
                lang = params["lang"].safeVar() ?: "no",
                boundaryCountry = params["boundary.country"]?.safeVar(),
                boundaryCountyIds = params["boundary.county.ids"]?.split(",").safeVars() ?: emptyList(),
                boundaryLocalityIds = params["boundary.locality.ids"]?.split(",").safeVars() ?: emptyList(),
                tariffZones = params["tariff_zone_ids"]?.split(",")?.safeVars() ?: emptyList(),
                tariffZoneAuthorities = params["tariff_zone_authorities"]?.split(",").safeVars() ?: emptyList(),
                sources = params["sources"]?.split(",").safeVars() ?: emptyList(),
                layers = params["layers"]?.split(",").safeVars() ?: emptyList(),
                categories = params["categories"]?.split(",").safeVars() ?: emptyList(),
                multiModal =
                    when (params["multiModal"]) {
                        "child" -> "child"
                        "parent" -> "parent"
                        "all" -> "all"
                        else -> "parent"
                    },
            )
        }
    }
}
