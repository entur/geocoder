package no.entur.geocoder.proxy.v3

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

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
        val type: String = "Point",
        val coordinates: List<BigDecimal>, // [lon, lat]
    )

    data class Place(
        val id: String,
        val name: Names,
        val layer: Layer,
        val address: Address? = null,
        val categories: List<String>? = null,
        val tariffZones: List<String>? = null,
        val transportModes: List<TransportMode>? = null,
        val stopPlaceTypes: List<String>? = null,
        val source: DataSource,
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

    data class DataSource(
        val provider: String,
        val sourceId: String? = null,
        val accuracy: String? = null,
    )

    enum class Layer {
        address,
        street,
        stopPlace,
        groupOfStopPlaces,
        poi,
    }

    data class Metadata(
        val query: QueryInfo,
        val resultCount: Int,
        val timestamp: Long = System.currentTimeMillis(),
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
        val countyIds: List<String>? = null,
        val localityIds: List<String>? = null,
        val tariffZones: List<String>? = null,
        val tariffZoneAuthorities: List<String>? = null,
        val multiModal: String? = null,
    )
}
