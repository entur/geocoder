package no.entur.geocoder.proxy.v3

import io.ktor.http.*
import no.entur.geocoder.proxy.common.SearchDefaults
import kotlin.math.log
import kotlin.math.roundToInt

data class V3AutocompleteRequest(
    val q: String = "",
    val limit: Int = SearchDefaults.LIMIT,
    val lang: String = SearchDefaults.LANG,
    val lat: Double? = null,
    val lon: Double? = null,
    /** Focus radius in kilometres. Decimals accepted. */
    val radius: Double? = null,
    val weight: Double? = null,
    override val layers: List<String> = emptyList(),
    override val sources: List<String> = emptyList(),
    override val countries: List<String> = emptyList(),
    override val counties: List<String> = emptyList(),
    override val localities: List<String> = emptyList(),
    /** Fare zone IDs in `AUTH:FareZone:ID` form. Maps to the converter's `fare_zone_id.` indexed prefix; TariffZone-shaped refs will not match. */
    override val fareZones: List<String> = emptyList(),
    /** Fare zone authority codes. Maps to the converter's `fare_zone_authority.` indexed prefix. */
    override val fareZoneAuthorities: List<String> = emptyList(),
    override val multimodal: String = "parent",
    val debug: Boolean = false,
) : V3FilterParams {
    /** Convert radius in km to Photon zoom. Photon: radius = 2.2^(18 - zoom) * 0.1 km (see SearchRequestBase). */
    fun photonZoom(): Int? {
        if (lat == null || lon == null) return null
        val r = radius ?: DEFAULT_RADIUS_KM
        return (18 - log(r / 0.1, 2.2)).roundToInt().coerceIn(0, 18)
    }

    /** Convert weight (0=no bias, 1=max bias) to Photon location_bias_scale (0=max bias, 1=no bias). */
    fun photonLocationBiasScale(): Double? {
        if (lat == null || lon == null) return null
        val w = (weight ?: DEFAULT_WEIGHT).coerceIn(0.0, 1.0)
        return 1.0 - w
    }

    companion object {
        private const val DEFAULT_RADIUS_KM = 50.0
        private const val DEFAULT_WEIGHT = 0.5

        internal val ALLOWED_PARAMS =
            setOf(
                "q", "limit", "lang", "lat", "lon",
                "radius", "weight", "layers", "sources", "countries", "counties",
                "localities", "fareZones", "fareZoneAuthorities", "multimodal", "debug",
            )

        fun from(req: Parameters): V3AutocompleteRequest {
            val unknown = req.names().filterNot { it in ALLOWED_PARAMS }
            require(unknown.isEmpty()) { "Unknown parameter(s): ${unknown.joinToString()}" }

            return V3AutocompleteRequest(
                q = req["q"] ?: "",
                limit = req["limit"]?.toIntOrNull() ?: SearchDefaults.LIMIT,
                lang = req["lang"] ?: SearchDefaults.LANG,
                lat = req["lat"]?.toDoubleOrNull(),
                lon = req["lon"]?.toDoubleOrNull(),
                radius = req["radius"]?.toDoubleOrNull(),
                weight = req["weight"]?.toDoubleOrNull(),
                layers = req.csv("layers"),
                sources = req.csv("sources"),
                countries = req.csv("countries"),
                counties = req.csv("counties"),
                localities = req.csv("localities"),
                fareZones = req.csv("fareZones"),
                fareZoneAuthorities = req.csv("fareZoneAuthorities"),
                multimodal = req["multimodal"] ?: "parent",
                debug = req["debug"].toBoolean(),
            )
        }
    }
}
