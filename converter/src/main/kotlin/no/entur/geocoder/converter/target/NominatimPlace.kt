package no.entur.geocoder.converter.target

import no.entur.geocoder.common.Extra
import java.math.BigDecimal

data class NominatimPlace(
    val type: String,
    val content: List<PlaceContent>,
) {
    data class PlaceContent(
        val place_id: Long,
        val object_type: String,
        val object_id: Long,
        val categories: List<String>,
        val rank_address: Int,
        val importance: Double,
        val parent_place_id: Long? = null,
        val name: Name? = null,
        val address: Address,
        val housenumber: String? = null,
        val postcode: String?,
        val country_code: String,
        val centroid: List<BigDecimal>,
        val bbox: List<BigDecimal> = emptyList(),
        val extra: Extra,
    )

    data class Address(
        val street: String? = null,
        val city: String? = null,
        val county: String? = null, // Fylke
    )

    // https://github.com/komoot/photon/blob/master/src/main/java/de/komoot/photon/nominatim/model/NameMap.java#L16
    data class Name(
        val name: String,
        val alt_name: String? = null,
    )
}

data class CountryInfoEntry(
    val country_code: String,
    val name: Map<String, String> = emptyMap(),
)

data class NominatimHeader(
    val type: String,
    val content: HeaderContent,
) {
    data class HeaderContent(
        val version: String,
        val generator: String,
        val database_version: String,
        val data_timestamp: String,
        val features: Features,
    )

    data class Features(
        val sorted_by_country: Boolean,
        val has_addresslines: Boolean,
    )
}
