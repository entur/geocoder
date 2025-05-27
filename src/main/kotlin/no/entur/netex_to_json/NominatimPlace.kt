package no.entur.netex_to_json

data class NominatimPlace(
    val type: String,
    val content: List<PlaceContent>
)

data class NominatimHeader(
    val type: String,
    val content: HeaderContent
)

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

data class CountryInfoEntry(
    val country_code: String,
    val name: Map<String, String> = emptyMap()
)

data class PlaceContent(
    val place_id: Long,
    val object_type: String,
    val object_id: Long,
    val categories: List<String>,
    val rank_address: Int,
    val importance: Double,
    val parent_place_id: Long? = null,
    val name: Map<String, String>?,
    val address: Map<String, String>?,
    val postcode: String?,
    val country_code: String,
    val centroid: List<Double>,
    val bbox: List<Double> = emptyList(),
    val extratags: Map<String, Any> = emptyMap()
)

