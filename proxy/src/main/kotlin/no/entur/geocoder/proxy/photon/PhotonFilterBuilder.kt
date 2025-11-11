package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX

object PhotonFilterBuilder {
    fun buildIncludes(
        boundaryCountry: String?,
        boundaryCountyIds: List<String>,
        boundaryLocalityIds: List<String>,
        tariffZones: List<String>,
        tariffZoneAuthorities: List<String>,
        sources: List<String>,
        layers: List<String>,
        categories: List<String>,
    ): List<String> =
        buildList {
            boundaryCountry?.let { add("country.$it") }
            if (boundaryCountyIds.isNotEmpty()) {
                addAll(boundaryCountyIds.map { "county_gid.$it" })
            }
            if (boundaryLocalityIds.isNotEmpty()) {
                addAll(boundaryLocalityIds.map { "locality_gid.$it" })
            }
            if (tariffZones.isNotEmpty()) {
                addAll(tariffZones.map { "tariff_zone_id.$it" })
            }
            if (tariffZoneAuthorities.isNotEmpty()) {
                addAll(tariffZoneAuthorities.map { "tariff_zone_authority.$it" })
            }
            if (sources.isNotEmpty()) {
                addAll(sources.map { "legacy.source.$it" })
            }
            if (layers.isNotEmpty()) {
                addAll(layers.map { "legacy.layer.$it" })
            }
            if (categories.isNotEmpty()) {
                if (categories.none { it == "NO_FILTER" }) {
                    addAll(categories.map { LEGACY_CATEGORY_PREFIX + it })
                }
            }
        }

    fun buildMultiModalExclude(multiModal: String): String? =
        when (multiModal) {
            "child" -> "multimodal.parent"
            "parent" -> "multimodal.child"
            else -> null
        }
}
