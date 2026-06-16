package no.entur.geocoder.proxy.common

data class Extra(
    val locality_gid: String? = null,
    val country_a: String? = null,
    val locality: String? = null,
    val accuracy: String? = null,
    val source: String? = null,
    val tariff_zones: String? = null,
    val fare_zones: String? = null,
    val id: String,
    val county_gid: String? = null,
    val borough: String? = null,
    val borough_gid: String? = null,
    val alt_name: String? = null,
    val description: String? = null,
    val tags: String? = null,
    // Semicolon-separated list of mode:subMode pairs, e.g. "bus:localBus;rail"
    val transport_mode: String? = null,
    // Semicolon-separated list of NeTEx StopPlaceType values, e.g. "onstreetBus;busStation"
    val stop_place_type: String? = null,
    // Stop place hierarchy role ("parent"/"child"/"standalone"); set by the converter only for
    // stop places, absent otherwise and on pre-reindex documents.
    val stop_place_role: String? = null,
)
