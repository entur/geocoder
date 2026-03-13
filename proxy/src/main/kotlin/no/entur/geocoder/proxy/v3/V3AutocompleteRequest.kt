package no.entur.geocoder.proxy.v3

import io.ktor.http.*
import kotlin.math.ln
import kotlin.math.roundToInt

data class V3AutocompleteRequest(
    val query: String = "",
    val limit: Int = 10,
    val language: String = "no",
    val lat: Double? = null,
    val lon: Double? = null,
    val radius: Double? = null,
    val weight: Double? = null,
    val layers: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val countyIds: List<String> = emptyList(),
    val localityIds: List<String> = emptyList(),
    val tariffZones: List<String> = emptyList(),
    val fareZoneAuthorities: List<String> = emptyList(),
    val multimodal: String = "parent",
) {
    /** Convert radius in km to Photon zoom level. Photon formula: radius = (1 shl (18 - zoom)) * 0.25 km */
    fun photonZoom(): Int? {
        if (lat == null || lon == null) return null
        val r = radius ?: DEFAULT_RADIUS_KM
        return (18 - ln(r / 0.25) / LN2).roundToInt().coerceIn(0, 18)
    }

    /** Convert weight (0=no bias, 1=max bias) to Photon location_bias_scale (0=max bias, 1=no bias). */
    fun photonLocationBiasScale(): Double? {
        if (lat == null || lon == null) return null
        val w = (weight ?: DEFAULT_WEIGHT).coerceIn(0.0, 1.0)
        return 1.0 - w
    }

    companion object {
        private const val DEFAULT_RADIUS_KM = 50.0
        private const val DEFAULT_WEIGHT = 0.8
        private val LN2 = ln(2.0)

        private val ALLOWED_PARAMS = setOf(
            "q", "limit", "lang", "lat", "lon",
            "radius", "weight", "layers", "sources", "countries", "countyIds",
            "localityIds", "tariffZones", "fareZoneAuthorities", "multimodal",
        )

        fun from(req: Parameters): V3AutocompleteRequest {
            val unknown = req.names().filterNot { it in ALLOWED_PARAMS }
            require(unknown.isEmpty()) { "Unknown parameter(s): ${unknown.joinToString()}" }

            val lat = req["lat"]?.toDoubleOrNull()
            val lon = req["lon"]?.toDoubleOrNull()
            return V3AutocompleteRequest(
                query = req["q"] ?: "",
                limit = req["limit"]?.toIntOrNull() ?: 10,
                language = req["lang"] ?: "no",
                lat = lat,
                lon = lon,
                radius = req["radius"]?.toDoubleOrNull(),
                weight = req["weight"]?.toDoubleOrNull(),
                layers = req["layers"]?.split(",") ?: emptyList(),
                sources = req["sources"]?.split(",") ?: emptyList(),
                countries = req["countries"]?.split(",") ?: emptyList(),
                countyIds = req["countyIds"]?.split(",") ?: emptyList(),
                localityIds = req["localityIds"]?.split(",") ?: emptyList(),
                tariffZones = req["tariffZones"]?.split(",") ?: emptyList(),
                fareZoneAuthorities = req["fareZoneAuthorities"]?.split(",") ?: emptyList(),
                multimodal = req["multimodal"] ?: "parent",
            )
        }
    }
}
