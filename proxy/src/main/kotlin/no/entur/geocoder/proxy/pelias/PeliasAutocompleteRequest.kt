package no.entur.geocoder.proxy.pelias

import io.ktor.http.*
import no.entur.geocoder.proxy.Text.safeVar
import no.entur.geocoder.proxy.Text.safeVars

data class PeliasAutocompleteRequest(
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
    val multiModal: String = "parent",
    val debug: Boolean = false,
    val experimental: Boolean = false,
) {
    init {
        require(text.isNotBlank()) { errorMessage }
    }

    companion object {
        const val errorMessage = "text cannot be blank"

        fun from(req: Parameters) =
            PeliasAutocompleteRequest(
                text = req["text"]?.safeVar() ?: "",
                size = req["size"]?.toIntOrNull() ?: 10,
                lang = req["lang"].safeVar() ?: "no",
                boundaryCountry = req["boundary.country"]?.safeVar(),
                boundaryCountyIds = req["boundary.county_ids"]?.split(",").safeVars() ?: emptyList(),
                boundaryLocalityIds = req["boundary.locality_ids"]?.split(",").safeVars() ?: emptyList(),
                tariffZones = req["tariff_zone_ids"]?.split(",")?.safeVars() ?: emptyList(),
                tariffZoneAuthorities = req["tariff_zone_authorities"]?.split(",").safeVars() ?: emptyList(),
                sources = req["sources"]?.split(",").safeVars() ?: emptyList(),
                layers = req["layers"]?.split(",").safeVars() ?: emptyList(),
                categories = req["categories"]?.split(",").safeVars() ?: emptyList(),
                focus =
                    req["focus.point.lat"]?.let { latStr ->
                        req["focus.point.lon"]?.let { lonStr ->
                            FocusParams.from(
                                lat = latStr,
                                lon = lonStr,
                                scale = req["focus.scale"],
                                weight = req["focus.weight"],
                            )
                        }
                    },
                multiModal =
                    when (req["multiModal"]) {
                        "child" -> "child"
                        "parent" -> "parent"
                        "all" -> "all"
                        else -> "parent"
                    },
                debug = req["debug"].toBoolean(),
                experimental = req["experimental"].toBoolean(),
            )
    }

    data class FocusParams(
        val lat: Double,
        val lon: Double,
        val scale: Int? = null,
        val weight: Double? = null,
    ) {
        init {
            require(lat in -90.0..90.0) { "Parameter 'focus.point.lat' must be between -90 and 90" }
            require(lon in -180.0..180.0) { "Parameter 'focus.point.lon' must be between -180 and 180" }
            if (scale != null) {
                require(scale > 0) { "Parameter 'focus.scale' must be a number > 0" }
            }
            if (weight != null) {
                require(weight > 0) { "Parameter 'focus.weight' must be a number > 0" }
            }
        }

        companion object {
            fun from(lat: String, lon: String, scale: String?, weight: String?): FocusParams {
                val latValue =
                    lat.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Parameter 'focus.point.lat' must be a valid number")
                val lonValue =
                    lon.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Parameter 'focus.point.lon' must be a valid number")

                val scaleValue =
                    scale?.let { scaleStr ->
                        val scaleNumeric = scaleStr.removeSuffix("km")
                        scaleNumeric.toIntOrNull()
                            ?: throw IllegalArgumentException("Parameter 'focus.scale' must be a number followed by 'km'")
                    }

                val weightValue =
                    weight?.let { weightStr ->
                        weightStr.toDoubleOrNull()
                            ?: throw IllegalArgumentException("Parameter 'focus.weight' must be a valid number")
                    }

                return FocusParams(latValue, lonValue, scaleValue, weightValue)
            }
        }
    }
}
