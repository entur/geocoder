package no.entur.geocoder.proxy.pelias

import java.math.BigDecimal

data class PeliasResult(
    val geocoding: GeocodingMetadata = GeocodingMetadata(),
    val type: String = "FeatureCollection",
    val features: List<PeliasFeature>,
    val bbox: List<BigDecimal>? = null,
) {
    data class PeliasFeature(
        val type: String = "PhotonFeature",
        val geometry: PeliasGeometry,
        val properties: PeliasProperties,
    )

    data class PeliasGeometry(
        val type: String,
        val coordinates: List<BigDecimal>,
    )

    data class PeliasProperties(
        val type: String? = null,
        val countrycode: String? = null,
        val id: String? = null,
        val gid: String? = null,
        val layer: String? = null,
        val source: String? = null,
        val source_id: String? = null,
        val name: String? = null,
        val housenumber: String? = null,
        val street: String? = null,
        val postalcode: String? = null,
        val accuracy: String? = null,
        val country_a: String? = null,
        val county: String? = null,
        val county_gid: String? = null,
        val locality: String? = null,
        val locality_gid: String? = null,
        val borough: String? = null,
        val borough_gid: String? = null,
        val label: String? = null,
        val category: List<String>? = null,
        val city: String? = null,
        val tariff_zones: List<String>? = null,
    )

    data class GeocodingMetadata(
        val version: String = "0.2",
        val attribution: String = "http://pelias.mapzen.com/v1/attribution",
        val query: QueryMetadata? = null,
        val engine: EngineMetadata? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        data class QueryMetadata(
            val text: String? = null,
            val parser: String = "addressit",
            val tokens: List<String>? = null,
            val size: Int = 10,
            val layers: List<String> = listOf("address", "venue"),
            val sources: List<String> = listOf("openstreetmap", "whosonfirst"),
            val private: Boolean = false,
            val lang: LangMetadata? = null,
            val querySize: Int = 20,
        )

        data class LangMetadata(
            val name: String = "Norwegian Bokmål",
            val iso6391: String = "nb",
            val iso6393: String = "nob",
            val defaulted: Boolean = false,
        )

        data class EngineMetadata(
            val name: String = "Pelias",
            val author: String = "Mapzen",
            val version: String = "1.0",
        )
    }
}
