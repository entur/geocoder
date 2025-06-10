package no.entur.netex_photon.proxy

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(NON_NULL)
data class FeatureCollection(
    val type: String = "FeatureCollection",
    val features: List<Feature>,
    val bbox: List<Double>? = null
)

@JsonInclude(NON_NULL)
data class Feature(
    val type: String = "Feature",
    val geometry: Geometry,
    val properties: Properties
)

@JsonInclude(NON_NULL)
data class Geometry(
    val type: String,
    val coordinates: List<Double>
)

@JsonInclude(NON_NULL)
data class Properties(
    @JsonProperty("osm_type") val osmType: String? = null,
    @JsonProperty("osm_id") val osmId: Long? = null,
    @JsonProperty("osm_key") val osmKey: String? = null,
    @JsonProperty("osm_value") val osmValue: String? = null,
    val type: String? = null,
    val postcode: String? = null,
    val countrycode: String? = null,
    val id: String? = null,
    val layer: String? = null,
    val source: String? = null,
    val source_id: String? = null,
    val name: String? = null,
    val street: String? = null,
    val accuracy: String? = null,
    val country_a: String? = null,
    val county: String? = null,
    val county_gid: String? = null,
    val locality: String? = null,
    val locality_gid: String? = null,
    val label: String? = null,
    val transport_modes: List<String>? = null,
    val tariff_zones: List<String>? = null,
    val extra: Extra? = null
)

@JsonInclude(NON_NULL)
data class Extra(
    val gid: String? = null,
    val locality_gid: String? = null,
    val country_a: String? = null,
    val locality: String? = null,
    val accuracy: String? = null,
    val source: String? = null,
    val label: String? = null,
    val tariff_zones: String? = null,
    val layer: String? = null,
    val id: String? = null,
    val source_id: String? = null,
    val county_gid: String? = null,
    val transport_modes: String? = null
)

@JsonInclude(NON_NULL)
data class GeocodingMetadata(
    val version: String = "0.2",
    val attribution: String = "http://pelias.mapzen.com/v1/attribution",
    val query: QueryMetadata? = null,
    val engine: EngineMetadata? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonInclude(NON_NULL)
data class QueryMetadata(
    val text: String? = null,
    val parser: String = "addressit",
    val tokens: List<String>? = null,
    val size: Int = 10,
    val layers: List<String> = listOf("address", "venue"),
    val sources: List<String> = listOf("openstreetmap", "whosonfirst"),
    val private: Boolean = false,
    val lang: LangMetadata? = null,
    val querySize: Int = 20
)

@JsonInclude(NON_NULL)
data class LangMetadata(
    val name: String = "Norwegian Bokmål",
    val iso6391: String = "nb",
    val iso6393: String = "nob",
    val defaulted: Boolean = false
)

@JsonInclude(NON_NULL)
data class EngineMetadata(
    val name: String = "Pelias",
    val author: String = "Mapzen",
    val version: String = "1.0"
)
