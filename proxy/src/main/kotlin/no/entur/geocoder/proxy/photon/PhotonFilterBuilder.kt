package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasReverseRequest

object PhotonFilterBuilder {
    fun buildIncludes(req: PeliasAutocompleteRequest): List<String> =
        buildIncludes(
            boundaryCountry = req.boundaryCountry,
            boundaryCountyIds = req.boundaryCountyIds,
            boundaryLocalityIds = req.boundaryLocalityIds,
            tariffZones = req.tariffZones,
            tariffZoneAuthorities = req.tariffZoneAuthorities,
            sources = req.sources,
            layers = req.layers,
            categories = req.categories,
        )

    fun buildIncludes(req: PeliasReverseRequest): List<String> =
        buildIncludes(
            boundaryCountry = req.boundaryCountry,
            boundaryCountyIds = req.boundaryCountyIds,
            boundaryLocalityIds = req.boundaryLocalityIds,
            tariffZones = req.tariffZones,
            tariffZoneAuthorities = req.tariffZoneAuthorities,
            sources = req.sources,
            layers = req.layers,
            categories = req.categories,
        )

    private fun buildIncludes(
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

    fun buildExcludes(req: PeliasAutocompleteRequest): List<String> =
        listOfNotNull(
            buildMultiModalExclude(req.multiModal),
            // LEGACY_CATEGORY_PREFIX + "by", // There is no "by" category Pelias
            req.text
                .takeIf { !it.contains("\\s\\d".toRegex()) }
                ?.let { Category.OSM_ADDRESS }, // Exclude addresses unless the query contains a house number
        )

    fun buildExcludes(req: PeliasReverseRequest): List<String> =
        listOfNotNull(
            buildMultiModalExclude(req.multiModal),
            // LEGACY_CATEGORY_PREFIX + "by", // There is no "by" category Pelias
            Category.OSM_ADDRESS, // Always exclude addresses with house numbers in reverse requests
        )

    internal fun buildMultiModalExclude(multiModal: String): String? =
        when (multiModal) {
            "child" -> "multimodal.parent"
            "parent" -> "multimodal.child"
            else -> null
        }
}
