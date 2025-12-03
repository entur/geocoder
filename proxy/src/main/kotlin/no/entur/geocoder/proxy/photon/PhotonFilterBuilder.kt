package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Category.COUNTRY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_PREFIX
import no.entur.geocoder.common.Category.LEGACY_SOURCE_PREFIX
import no.entur.geocoder.common.Category.TARIFF_ZONE_AUTH_PREFIX
import no.entur.geocoder.common.Country
import no.entur.geocoder.common.Source
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
            Country.fromThreeLetterCode(boundaryCountry)?.let { add(COUNTRY_PREFIX + it.name) }
            if (boundaryCountyIds.isNotEmpty()) {
                add(boundaryCountyIds.joinToString(",") { Category.countyIdsCategory(it) })
            }
            if (boundaryLocalityIds.isNotEmpty()) {
                add(boundaryLocalityIds.joinToString(",") { Category.localityIdsCategory(it) })
            }
            if (tariffZones.isNotEmpty()) {
                add(tariffZones.joinToString(",") { Category.tariffZoneIdCategory(it) })
            }
            if (tariffZoneAuthorities.isNotEmpty()) {
                add(tariffZoneAuthorities.joinToString(",") { TARIFF_ZONE_AUTH_PREFIX + it })
            }
            if (sources.isNotEmpty()) {
                add(sources.joinToString(",") { LEGACY_SOURCE_PREFIX + it })
            }
            if (layers.isNotEmpty()) {
                add(layers.joinToString(",") { LEGACY_LAYER_PREFIX + it })
            }
            if (categories.isNotEmpty()) {
                if (categories.none { it == "NO_FILTER" }) {
                    add(categories.joinToString(",") { LEGACY_CATEGORY_PREFIX + it })
                }
            }
        }

    fun buildExcludes(req: PeliasAutocompleteRequest): List<String> =
        listOfNotNull(
            buildMultiModalExclude(req.multiModal),
            buildHouseNumberExclude(req),
        )

    // Exclude addresses unless the query contains a house number or sources=<whatever>
    private fun buildHouseNumberExclude(req: PeliasAutocompleteRequest): String? =
        if (req.sources.contains(Source.LEGACY_OPENADDRESSES)) {
            null
        } else {
            // Typically takes care of "Oslo C" returning addresses.
            req.text
                .takeIf { !it.contains("\\s\\d".toRegex()) }
                ?.let { Category.OSM_ADDRESS }
        }

    fun buildExcludes(req: PeliasReverseRequest): List<String> =
        listOfNotNull(
            buildMultiModalExclude(req.multiModal),
            Category.OSM_ADDRESS, // Always exclude addresses with house numbers in reverse requests
        )

    internal fun buildMultiModalExclude(multiModal: String): String? =
        when (multiModal) {
            "child" -> "multimodal.parent"
            "parent" -> "multimodal.child"
            else -> null
        }
}
