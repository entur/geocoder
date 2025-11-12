package no.entur.geocoder.proxy.pelias

import io.ktor.http.*
import io.ktor.server.application.*
import no.entur.geocoder.common.Util.titleize
import no.entur.geocoder.proxy.Text.safeVar
import no.entur.geocoder.proxy.Text.safeVars
import java.math.BigDecimal

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
        fun ApplicationCall.peliasAutocompleteParams(): PeliasAutocompleteParams {
            val params = request.queryParameters
            return PeliasAutocompleteParams(
                text = handleText(params),
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
                    params["focus.point.lat"]?.let { latStr ->
                        params["focus.point.lon"]?.let { lonStr ->
                            FocusParams.from(
                                lat = latStr,
                                lon = lonStr,
                                scale = params["focus.scale"],
                                weight = params["focus.weight"],
                            )
                        }
                    },
                multiModal =
                    when (params["multiModal"]) {
                        "child" -> "child"
                        "parent" -> "parent"
                        "all" -> "all"
                        else -> "parent"
                    },
            )
        }

        // Photon handles short (and fuzzy) queries differently to longer ones.
        // The fuzzy search "Olso" doesn't resolve to "Oslo", while "olso" does.
        // The non-fuzzy search "Lille" gives better results than "lille". "Lill" and "lill" are equivalent (and both good).
        private fun handleText(params: Parameters): String =
            params["text"]
                .safeVar()
                ?.let {
                    if (it.length <= 4) it.lowercase() else it.titleize()
                } ?: ""
    }

    data class FocusParams(
        val lat: BigDecimal,
        val lon: BigDecimal,
        val scale: Int?,
        val weight: Double?,
    ) {
        init {
            require(lat.toDouble() in -90.0..90.0) { "Parameter 'focus.point.lat' must be between -90 and 90" }
            require(lon.toDouble() in -180.0..180.0) { "Parameter 'focus.point.lon' must be between -180 and 180" }
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
                    lat.toBigDecimalOrNull()
                        ?: throw IllegalArgumentException("Parameter 'focus.point.lat' must be a valid number")
                val lonValue =
                    lon.toBigDecimalOrNull()
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
