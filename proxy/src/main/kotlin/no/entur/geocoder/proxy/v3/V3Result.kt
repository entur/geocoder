package no.entur.geocoder.proxy.v3

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class V3Result(
    val type: String = "FeatureCollection",
    val features: List<Feature>,
    val metadata: Metadata,
    val bbox: List<BigDecimal>? = null,
) {
    data class Feature(
        val type: String = "Feature",
        val geometry: Geometry,
        val properties: Place,
    )

    data class Geometry(
        val type: String,
        val coordinates: List<BigDecimal>, // [lon, lat]
    )

    data class Place(
        val id: String,
        val name: Names,
        val layer: Layer,
        val source: String,
        val address: Address? = null,
        val categories: List<String>? = null,
        val fareZones: List<String>? = null,
        val transportModes: List<TransportMode>? = null,
        val stopPlaceTypes: List<String>? = null,
        /** Distance from the reverse query point in kilometres (3-decimal precision). Present on reverse responses only. */
        val distance: BigDecimal? = null,
        /** Per-language description. Keys are ISO 639-2 alpha-3 language codes (e.g. `nor`, `eng`). */
        val description: Map<String, String>? = null,
    )

    data class Names(
        val default: String,
        val label: String? = null,
        val display: String,
    )

    data class TransportMode(
        val mode: String,
        val subMode: String? = null,
    )

    data class Address(
        val streetName: String? = null,
        val houseNumber: String? = null,
        val postalCode: String? = null,
        val locality: String? = null,
        val localityId: String? = null,
        val borough: String? = null,
        val boroughId: String? = null,
        val county: String? = null,
        val countyId: String? = null,
        val countryCode: String? = null,
    )

    enum class Layer {
        address,
        street,
        stopPlace,
        groupOfStopPlaces,
        poi,
        place,
    }

    data class Metadata(
        val query: QueryInfo,
        val resultCount: Int,
        val timestamp: String = Instant.now().toString(),
        val debug: Map<String, Any>? = null,
    )

    data class QueryInfo(
        val text: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val limit: Int,
        val language: String,
        val filters: Filters? = null,
    )

    data class Filters(
        val layers: List<Layer>? = null,
        val sources: List<String>? = null,
        val countries: List<String>? = null,
        val counties: List<String>? = null,
        val localities: List<String>? = null,
        val fareZones: List<String>? = null,
        val fareZoneAuthorities: List<String>? = null,
        val multimodal: String? = null,
    )
}
