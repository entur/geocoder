package no.entur.geocoder.proxy.v3

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
data class V3Result(
    val results: List<Place>,
    val metadata: Metadata,
) {
    data class Place(
        val id: String,
        val name: String,
        val displayName: String,
        val placeType: PlaceType,
        val location: Location,
        val address: Address? = null,
        val categories: List<String>? = null,
        val transportModes: List<String>? = null,
        val tariffZones: List<String>? = null,
        val source: DataSource,
    )

    data class Location(
        val latitude: BigDecimal,
        val longitude: BigDecimal,
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
        val country: String? = null,
        val countryCode: String? = null,
    )

    data class DataSource(
        val provider: String,
        val sourceId: String? = null,
        val accuracy: Accuracy? = null,
    )

    enum class PlaceType {
        ADDRESS,
        STREET,
        LOCALITY,
        BOROUGH,
        COUNTY,
        VENUE,
        STOP_PLACE,
        STATION,
        POI,
        UNKNOWN,
    }

    enum class Accuracy {
        EXACT,
        INTERPOLATED,
        APPROXIMATE,
        UNKNOWN,
    }

    data class Metadata(
        val query: QueryInfo,
        val resultCount: Int,
        val timestamp: Long = System.currentTimeMillis(),
        val boundingBox: BoundingBox? = null,
    )

    data class QueryInfo(
        val text: String? = null,
        val latitude: BigDecimal? = null,
        val longitude: BigDecimal? = null,
        val limit: Int,
        val language: String,
        val filters: Filters? = null,
    )

    data class Filters(
        val placeTypes: List<PlaceType>? = null,
        val sources: List<String>? = null,
        val countries: List<String>? = null,
        val countyIds: List<String>? = null,
        val localityIds: List<String>? = null,
        val tariffZones: List<String>? = null,
        val tariffZoneAuthorities: List<String>? = null,
        val transportModes: List<String>? = null,
    )

    data class BoundingBox(
        val southwest: Location,
        val northeast: Location,
    )
}
