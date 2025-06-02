package no.entur.netex_photon.proxy

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FeatureCollection(
    val type: String = "FeatureCollection",
    val features: List<Feature>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Feature(
    val type: String = "Feature",
    val geometry: Geometry,
    val properties: Properties
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Geometry(
    val type: String,
    val coordinates: List<Double>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Properties(
    @JsonProperty("osm_type") val osmType: String? = null,
    @JsonProperty("osm_id") val osmId: Long? = null,
    @JsonProperty("osm_key") val osmKey: String? = null,
    @JsonProperty("osm_value") val osmValue: String? = null,
    val type: String? = null,
    val postcode: String? = null,
    val countrycode: String? = null,
    val name: String? = null,
    val street: String? = null,
    val county: String? = null,
    val id: String? = null,
    val gid: String? = null,
    val layer: String? = null,
    val source: String? = null,
    val source_id: String? = null,
    val accuracy: String? = null,
    val country_a: String? = null,
    val county_gid: String? = null,
    val locality: String? = null,
    val locality_gid: String? = null,
    val label: String? = null,
    val category: List<String>? = null,
    val tariff_zones: List<String>? = null,
    val extra: Extra? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    val category: String? = null
)

