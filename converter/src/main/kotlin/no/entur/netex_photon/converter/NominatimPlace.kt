package no.entur.netex_photon.converter

import java.math.BigDecimal
import no.entur.netex_photon.common.domain.Extra

data class NominatimPlace(
    val type: String,
    val content: List<PlaceContent>
) {
    data class PlaceContent(
        val place_id: Long,
        val object_type: String,
        val object_id: Long,
        val categories: List<String>,
        val rank_address: Int,
        val importance: Double,
        val parent_place_id: Long? = null,
        val name: Name?,
        val address: Address,
        val housenumber: String? = null,
        val postcode: String?,
        val country_code: String,
        val centroid: List<BigDecimal>,
        val bbox: List<BigDecimal> = emptyList(),
        val extratags: Extra
    )

    data class Address(
        val street: String? = null,
        val city: String? = null,
        val county: String? = null, // Fylke
    )

    data class Name(
        val name: String
    )
}

data class CountryInfoEntry(
    val country_code: String,
    val name: Map<String, String> = emptyMap()
)


data class NominatimHeader(
    val type: String,
    val content: HeaderContent
) {

    data class HeaderContent(
        val version: String,
        val generator: String,
        val database_version: String,
        val data_timestamp: String,
        val features: Features
    )

    data class Features(
        val sorted_by_country: Boolean,
        val has_addresslines: Boolean
    )
}
