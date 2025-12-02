package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Category.COUNTRY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_ADDRESS
import no.entur.geocoder.common.Category.LEGACY_LAYER_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_VENUE
import no.entur.geocoder.common.Category.LEGACY_SOURCE_PREFIX
import no.entur.geocoder.common.Category.TARIFF_ZONE_AUTH_PREFIX
import no.entur.geocoder.common.Country
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
                addAll(boundaryCountyIds.map { Category.countyIdsCategory(it) })
            }
            if (boundaryLocalityIds.isNotEmpty()) {
                addAll(boundaryLocalityIds.map { Category.localityIdsCategory(it) })
            }
            if (tariffZones.isNotEmpty()) {
                addAll(tariffZones.map { Category.tariffZoneIdCategory(it) })
            }
            if (tariffZoneAuthorities.isNotEmpty()) {
                addAll(tariffZoneAuthorities.map { TARIFF_ZONE_AUTH_PREFIX + it })
            }
            if (sources.isNotEmpty()) {
                addAll(sources.map { LEGACY_SOURCE_PREFIX + it })
            }
            if (layers.isNotEmpty()) {
                addAll(layers.map { LEGACY_LAYER_PREFIX + it })
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
        ).plus(buildLayerExcludes(req.layers))

    fun buildExcludes(req: PeliasReverseRequest): List<String> =
        listOfNotNull(
            buildMultiModalExclude(req.multiModal),
            // LEGACY_CATEGORY_PREFIX + "by", // There is no "by" category Pelias
            Category.OSM_ADDRESS, // Always exclude addresses with house numbers in reverse requests
        ).plus(buildLayerExcludes(req.layers))

    internal fun buildLayerExcludes(layers: List<String>) =
        listOfNotNull(
            if (layers.contains(LEGACY_LAYER_ADDRESS) && !layers.contains(LEGACY_LAYER_VENUE)) LEGACY_LAYER_VENUE else null,
            if (layers.contains(LEGACY_LAYER_VENUE) && !layers.contains(LEGACY_LAYER_ADDRESS)) LEGACY_LAYER_ADDRESS else null,
        )

    internal fun buildMultiModalExclude(multiModal: String): String? =
        when (multiModal) {
            "child" -> "multimodal.parent"
            "parent" -> "multimodal.child"
            else -> null
        }
}
