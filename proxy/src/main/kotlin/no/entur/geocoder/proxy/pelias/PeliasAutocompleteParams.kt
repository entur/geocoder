package no.entur.geocoder.proxy.pelias

import io.ktor.server.request.*
import no.entur.geocoder.proxy.Text.safeVar
import no.entur.geocoder.proxy.Text.safeVars

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
    val categories: List<String> = emptyList(),
    val focus: FocusParams? = null,
    val multiModal: String,
) {
    companion object {
        fun fromRequest(request: ApplicationRequest): PeliasAutocompleteParams {
            val params = request.queryParameters
            return PeliasAutocompleteParams(
                text = params["text"].safeVar() ?: "",
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
                focus =
                    params["focus.point.lat"]?.let { lat ->
                        params["focus.point.lon"]?.let { lon ->
                            FocusParams(lat, lon, params["focus.scale"], params["focus.weight"])
                        }
                    },
                multiModal =
                    when (params["multiModal"]) {
                        "child" -> "child"
                        "parent" -> "parent"
                        "both" -> "both"
                        else -> "parent"
                    },
            )
        }
    }

    data class FocusParams(
        val lat: String,
        val lon: String,
        val scale: String?,
        val weight: String?
    ) {
        init {
            require(lat.isNotBlank()) { "Parameter 'point.lat' is required" }
            require(lon.isNotBlank()) { "Parameter 'point.lon' is required" }
            require(lat.toDoubleOrNull() != null) { "Parameter 'point.lat' must be a valid number" }
            require(lon.toDoubleOrNull() != null) { "Parameter 'point.lon' must be a valid number" }
            val latitude = lat.toDouble()
            val longitude = lon.toDouble()
            require(latitude in -90.0..90.0) { "Parameter 'point.lat' must be between -90 and 90" }
            require(longitude in -180.0..180.0) { "Parameter 'point.lon' must be between -180 and 180" }
            if (scale != null) {
                val scaleValue = scale.split("km")[0]
                require(
                    scale.endsWith("km")
                        && scaleValue.toIntOrNull() != null
                        && scaleValue.toInt() > 0
                ) { "Parameter 'scale' must be a number > 0 followed by 'km'" }
            }
            if (weight != null) {
                val weightValue = weight.toDouble()
                require(weightValue > 0) { "Parameter 'weight' must be a number > 0" }
            }
        }
    }
}

