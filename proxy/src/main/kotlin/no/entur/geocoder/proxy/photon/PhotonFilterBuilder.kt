package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.common.Category.COUNTRY_PREFIX
import no.entur.geocoder.proxy.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.proxy.common.Category.TARIFF_ZONE_AUTH_PREFIX
import no.entur.geocoder.proxy.common.Country
import no.entur.geocoder.proxy.common.LegacyLayer.Companion.LEGACY_LAYER_PREFIX
import no.entur.geocoder.proxy.common.LegacySource.Companion.LEGACY_SOURCE_PREFIX
import no.entur.geocoder.proxy.common.LegacySource.openaddresses
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasReverseRequest
import no.entur.geocoder.proxy.v3.V3FilterParams

object PhotonFilterBuilder {
    private const val KVE_PREFIX = "KVE:TopographicPlace:"
    private const val STOP_PLACE_LAYER = "stopPlace"
    private val DIGIT_ONLY_PATTERN = Regex("^\\d+$")
    private val HOUSE_NUMBER_HINT = Regex("(\\s\\d|\\d\\s)")

    fun textHasHouseNumber(text: String): Boolean = HOUSE_NUMBER_HINT.containsMatchIn(text)

    private fun normalizeTopographicPlaceId(id: String): String =
        if (id.matches(DIGIT_ONLY_PATTERN)) {
            "$KVE_PREFIX$id"
        } else {
            id
        }

    fun buildIncludes(req: PeliasAutocompleteRequest): List<String> =
        buildIncludes(
            boundaryCountry = req.boundaryCountry,
            boundaryCountyIds = req.boundaryCountyIds,
            boundaryLocalityIds = req.boundaryLocalityIds,
            tariffZones = req.tariffZones,
            tariffZoneAuthorities = req.tariffZoneAuthorities,
            fareZoneAuthorities = req.fareZoneAuthorities,
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
            fareZoneAuthorities = req.fareZoneAuthorities,
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
        fareZoneAuthorities: List<String>,
        sources: List<String>,
        layers: List<String>,
        categories: List<String>,
    ): List<String> =
        buildList {
            Country.fromThreeLetterCode(boundaryCountry)?.let { add(COUNTRY_PREFIX + it.name) }
            if (boundaryCountyIds.isNotEmpty()) {
                add(boundaryCountyIds.joinToString(",") { Category.countyIdsCategory(normalizeTopographicPlaceId(it)) })
            }
            if (boundaryLocalityIds.isNotEmpty()) {
                add(boundaryLocalityIds.joinToString(",") { Category.localityIdsCategory(normalizeTopographicPlaceId(it)) })
            }
            if (tariffZones.isNotEmpty()) {
                // v2 backwards-compat: real NeTEx data has both :TariffZone: and :FareZone: refs
                // inside <TariffZones>, and v2 callers pass either shape. The converter splits the
                // two into distinct indexed prefixes (tariff_zone_id. vs fare_zone_id.), so route
                // each input ref to its matching prefix here.
                //
                // The `:FareZone:` substring branch MUST stay in sync with
                // nominatim-converter/src/source/stopplace/convert.rs::append_tariff_zone_categories
                // (pass 1) - both decide TariffZone vs FareZone the same way.
                add(
                    tariffZones.joinToString(",") { ref ->
                        if (ref.contains(":FareZone:")) Category.fareZoneIdCategory(ref)
                        else Category.tariffZoneIdCategory(ref)
                    },
                )
            }
            if (tariffZoneAuthorities.isNotEmpty()) {
                add(tariffZoneAuthorities.joinToString(",") { TARIFF_ZONE_AUTH_PREFIX + it })
            }
            if (fareZoneAuthorities.isNotEmpty()) {
                add(fareZoneAuthorities.joinToString(",") { Category.fareZoneAuthorityCategory(it) })
            }
            if (sources.isNotEmpty()) {
                add(sources.joinToString(",") { LEGACY_SOURCE_PREFIX + it })
            }
            if (layers.isNotEmpty()) {
                add(layers.joinToString(",") { mapLayer(it) })
            }
            if (categories.isNotEmpty()) {
                if (categories.none { it == "NO_FILTER" }) {
                    add(categories.joinToString(",") { LEGACY_CATEGORY_PREFIX + it })
                }
            }
        }

    /**
     * Build the Photon `include` filter list for a v3 request (autocomplete or reverse).
     * `fareZones` here is strict: refs must be FareZone-shaped to match the converter's
     * `fare_zone_id.` indexed prefix - TariffZone-shaped refs will not match anything.
     */
    fun buildIncludes(params: V3FilterParams): List<String> =
        buildList {
            if (params.countries.isNotEmpty()) {
                add(
                    params.countries
                        .mapNotNull { Country.parse(it) }
                        .joinToString(",") { COUNTRY_PREFIX + it.name },
                )
            }
            if (params.counties.isNotEmpty()) {
                add(params.counties.joinToString(",") { Category.countyIdsCategory(it) })
            }
            if (params.localities.isNotEmpty()) {
                add(params.localities.joinToString(",") { Category.localityIdsCategory(it) })
            }
            if (params.fareZones.isNotEmpty()) {
                add(params.fareZones.joinToString(",") { Category.fareZoneIdCategory(it) })
            }
            if (params.fareZoneAuthorities.isNotEmpty()) {
                add(params.fareZoneAuthorities.joinToString(",") { Category.fareZoneAuthorityCategory(it) })
            }
            if (params.sources.isNotEmpty()) {
                add(params.sources.joinToString(",") { "source.${it.replace('-', '.')}" })
            }
            when {
                params.layers.contains(STOP_PLACE_LAYER) && params.stopPlaceTypes.isNotEmpty() -> {
                    // Union, not exclusion: stopPlaceTypes constrains only the stopPlace layer;
                    // other requested layers pass additively. Photon ANDs separate include params
                    // and ORs within one, so the two groups below intersect to:
                    //   layer in otherLayers OR (layer == stopPlace AND type in stopPlaceTypes)
                    val otherLayers = params.layers.filterNot { it == STOP_PLACE_LAYER }
                    add(params.layers.joinToString(",") { Category.LAYER_PREFIX + it })
                    add(
                        (otherLayers.map { Category.LAYER_PREFIX + it } +
                            params.stopPlaceTypes.map { Category.STOP_PLACE_TYPE_PREFIX + it }).joinToString(","),
                    )
                }
                params.layers.isNotEmpty() ->
                    // Also covers layers-without-stopPlace + types: types has nothing to constrain.
                    add(params.layers.joinToString(",") { Category.LAYER_PREFIX + it })
                params.stopPlaceTypes.isNotEmpty() ->
                    add(params.stopPlaceTypes.joinToString(",") { Category.STOP_PLACE_TYPE_PREFIX + it })
            }
        }

    fun buildExcludes(req: PeliasAutocompleteRequest): List<String> =
        listOfNotNull(
            buildMultimodalExclude(req.multiModal),
            buildHouseNumberExclude(req),
        )

    // Exclude addresses unless the query contains a house number or sources=<whatever>
    // (typically takes care of "Oslo C" returning addresses).
    private fun buildHouseNumberExclude(req: PeliasAutocompleteRequest): String? =
        if (req.sources.contains(openaddresses.name)) null
        else req.text.takeIf { !textHasHouseNumber(it) }?.let { Category.LAYER_ADDRESS }

    fun buildExcludes(req: PeliasReverseRequest): List<String> =
        listOfNotNull(
            buildMultimodalExclude(req.multiModal),
            if (req.sources.contains(openaddresses.name)) {
                null
            } else {
                Category.LAYER_ADDRESS // Exclude addresses with house numbers in reverse requests
            },
        )

    private fun mapLayer(layer: String): String =
        when (layer) {
            "venue" -> Category.LAYER_STOP_PLACE
            else -> LEGACY_LAYER_PREFIX + layer
        }

    internal fun buildMultimodalExclude(multimodal: String): String? =
        when (multimodal) {
            "child" -> "multimodal.parent"
            "parent" -> "multimodal.child"
            else -> null
        }
}
