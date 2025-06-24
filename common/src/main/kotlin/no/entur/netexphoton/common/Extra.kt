package no.entur.netexphoton.common

import com.fasterxml.jackson.annotation.JsonInclude

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
    val transport_modes: String? = null,
    val borough: String? = null,
    val borough_gid: String? = null,
)