package no.entur.geocoder.proxy.v3

import io.ktor.http.*
import no.entur.geocoder.proxy.common.SearchDefaults

data class V3ReverseRequest(
    val lat: Double,
    val lon: Double,
    /** Search radius in kilometres. Decimals accepted. Forwarded as-is to Photon's reverse `radius` param (also km). */
    val radius: Double? = null,
    val limit: Int = SearchDefaults.LIMIT,
    val lang: String = SearchDefaults.LANG,
    override val layers: List<String> = emptyList(),
    override val sources: List<String> = emptyList(),
    override val countries: List<String> = emptyList(),
    override val counties: List<String> = emptyList(),
    override val localities: List<String> = emptyList(),
    /** Fare zone IDs in `AUTH:FareZone:ID` form. Maps to the converter's `fare_zone_id.` indexed prefix; TariffZone-shaped refs will not match. */
    override val fareZones: List<String> = emptyList(),
    /** Fare zone authority codes. Maps to the converter's `fare_zone_authority.` indexed prefix. */
    override val fareZoneAuthorities: List<String> = emptyList(),
    /** NeTEx stop place types (e.g. `railStation`, `airport`); restricts results to stop places of those types, excluding other layers. */
    override val stopPlaceTypes: List<String> = emptyList(),
    override val multimodal: String = "parent",
    /** Sort by distance from the query point (default) or by relevance when false. */
    val distanceSort: Boolean = true,
    val debug: Boolean = false,
) : V3FilterParams {
    init {
        require(lat in -90.0..90.0) { "Parameter 'lat' must be between -90 and 90" }
        require(lon in -180.0..180.0) { "Parameter 'lon' must be between -180 and 180" }
        require(limit in 1..SearchDefaults.MAX_LIMIT) { "Parameter 'limit' must be between 1 and ${SearchDefaults.MAX_LIMIT}" }
    }

    companion object {
        internal val ALLOWED_PARAMS =
            setOf(
                "lat", "lon", "radius", "limit", "lang",
                "layers", "sources", "countries", "counties", "localities",
                "fareZones", "fareZoneAuthorities", "stopPlaceTypes", "multimodal", "distanceSort", "debug",
            )

        fun from(req: Parameters): V3ReverseRequest {
            val unknown = req.names().filterNot { it in ALLOWED_PARAMS }
            require(unknown.isEmpty()) { "Unknown parameter(s): ${unknown.joinToString()}" }

            return V3ReverseRequest(
                lat = req["lat"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Parameter 'lat' is required"),
                lon = req["lon"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Parameter 'lon' is required"),
                radius = req["radius"]?.toDoubleOrNull(),
                limit = req["limit"]?.toIntOrNull() ?: SearchDefaults.LIMIT,
                lang = req["lang"] ?: SearchDefaults.LANG,
                layers = req.csv("layers"),
                sources = req.csv("sources"),
                countries = req.csv("countries"),
                counties = req.csv("counties"),
                localities = req.csv("localities"),
                fareZones = req.csv("fareZones"),
                fareZoneAuthorities = req.csv("fareZoneAuthorities"),
                stopPlaceTypes = req.csv("stopPlaceTypes"),
                multimodal = req["multimodal"] ?: "parent",
                distanceSort = req["distanceSort"]?.let {
                    it.toBooleanStrictOrNull()
                        ?: throw IllegalArgumentException("Parameter 'distanceSort' must be true or false")
                } ?: true,
                debug = req["debug"].toBoolean(),
            )
        }
    }
}
