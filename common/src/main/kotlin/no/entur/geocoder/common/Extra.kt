package no.entur.geocoder.common

data class Extra(
    val locality_gid: String? = null,
    val country_a: String? = null,
    val locality: String? = null,
    val accuracy: String? = null,
    val source: String? = null,
    val tariff_zones: String? = null,
    val id: String? = null,
    val county_gid: String? = null,
    val transport_modes: String? = null,
    val borough: String? = null,
    val borough_gid: String? = null,
    val alt_name: String? = null,
    val tags: String? = null,
)
