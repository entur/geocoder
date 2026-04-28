package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.common.Category.COUNTRY_PREFIX
import no.entur.geocoder.proxy.common.Category.asCategory
import no.entur.geocoder.proxy.common.Country
import no.entur.geocoder.proxy.common.FocusDefaults
import no.entur.geocoder.proxy.common.Geo
import no.entur.geocoder.proxy.common.LegacySource.openaddresses
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasPlaceRequest
import no.entur.geocoder.proxy.photon.Lang.handleLang
import no.entur.geocoder.proxy.photon.LocationBiasCalculator.calculateLocationBias
import no.entur.geocoder.proxy.v3.V3AutocompleteRequest
import no.entur.geocoder.proxy.v3.V3PlaceRequest

data class PhotonAutocompleteRequest(
    val query: String,
    val limit: Int,
    val language: String = "no",
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val lat: Double? = null,
    val lon: Double? = null,
    val zoom: Int? = null,
    val locationBiasScale: Double? = null,
    val includeHousenumbers: Boolean = false,
    val debug: Boolean = false,
) {
    companion object {
        /**
         * We drop the city (by) if a GOSP exists with the same name. This can only be done after fetching,
         * so we fetch one extra and drop the last result if there is a match.
         *
         * Also fetching more than requested to work around https://github.com/komoot/photon/issues/1061
         *
         * see PeliasResultTransformer#filterCityIfGospIsPresent()
         */
        const val RESULT_PRUNING_HEADROOM = 30

        fun from(req: PeliasAutocompleteRequest): PhotonAutocompleteRequest {
            val includes = PhotonFilterBuilder.buildIncludes(req)
            val excludes = PhotonFilterBuilder.buildExcludes(req)

            // Null focus → no location bias. Otherwise FocusDefaults fills in missing scale/weight.
            val zoom = Geo.peliasScaleToPhotonZoom(req.focus?.scale ?: FocusDefaults.SCALE_KM)
            val locationBiasScale = req.focus?.let { calculateLocationBias(it.weight ?: FocusDefaults.WEIGHT) }

            return PhotonAutocompleteRequest(
                query = handleText(req.text),
                limit = req.size + RESULT_PRUNING_HEADROOM, // We ask for more since we prune away 'by' when there's already a matching GOSP
                language = handleLang(req.lang),
                includes = includes,
                excludes = excludes,
                lat = req.focus?.lat,
                lon = req.focus?.lon,
                zoom = zoom,
                locationBiasScale = locationBiasScale,
                debug = req.debug,
                includeHousenumbers = req.sources.contains(openaddresses.name) && !req.text.contains("\\s\\d".toRegex()),
            )
        }

        val digitPattern = Regex("^(\\d+)\\s+(.+)")

        private fun handleText(text: String): String {
            // 11 Storgata -> Storgata 11
            val match = digitPattern.find(text)
            return if (match == null) {
                text
            } else {
                val digit = match.groupValues[1]
                val rest = match.groupValues[2]
                "$rest $digit"
            }
        }

        private const val LEGACY_OA_PREFIX = "openaddresses:address:"
        private const val KVE_ADDRESS_PREFIX = "KVE:PostalAddress:"

        private fun expandId(id: String): List<String> =
            if (id.startsWith(LEGACY_OA_PREFIX)) {
                val numericId = id.removePrefix(LEGACY_OA_PREFIX)
                listOf(id.asCategory(), (KVE_ADDRESS_PREFIX + numericId).asCategory())
            } else {
                listOf(id.asCategory())
            }

        fun from(req: PeliasPlaceRequest): PhotonAutocompleteRequest =
            PhotonAutocompleteRequest(
                query = "",
                includes = listOf(req.ids.flatMap { expandId(it) }.joinToString(",")),
                limit = req.ids.size + RESULT_PRUNING_HEADROOM,
                debug = req.debug,
            )

        fun from(req: V3PlaceRequest): PhotonAutocompleteRequest =
            PhotonAutocompleteRequest(
                query = "",
                includes = listOf(req.ids.map { it.asCategory() }.joinToString(",")),
                limit = req.ids.size + RESULT_PRUNING_HEADROOM,
            )

        fun from(req: V3AutocompleteRequest): PhotonAutocompleteRequest {
            val includes =
                buildList {
                    if (req.countries.isNotEmpty()) {
                        add(
                            req.countries
                                .mapNotNull { Country.parse(it) }
                                .joinToString(",") { COUNTRY_PREFIX + it.name },
                        )
                    }
                    if (req.countyIds.isNotEmpty()) {
                        add(req.countyIds.joinToString(",") { Category.countyIdsCategory(it) })
                    }
                    if (req.localityIds.isNotEmpty()) {
                        add(req.localityIds.joinToString(",") { Category.localityIdsCategory(it) })
                    }
                    if (req.tariffZones.isNotEmpty()) {
                        add(req.tariffZones.joinToString(",") { Category.tariffZoneIdCategory(it) })
                    }
                    if (req.fareZoneAuthorities.isNotEmpty()) {
                        add(req.fareZoneAuthorities.joinToString(",") { Category.fareZoneAuthorityCategory(it) })
                    }
                    if (req.sources.isNotEmpty()) {
                        add(req.sources.joinToString(",") { "source.${it.replace('-', '.')}" })
                    }
                    if (req.layers.isNotEmpty()) {
                        add(req.layers.joinToString(",") { "layer.$it" })
                    }
                }

            val excludeAddresses =
                if (req.sources.any { it.contains("kartverket") || it.contains("matrikkelen") }) {
                    null
                } else {
                    req.q.takeIf { !it.contains("(\\s\\d|\\d\\s)".toRegex()) }?.let { Category.OSM_ADDRESS }
                }
            val excludes =
                listOfNotNull(
                    PhotonFilterBuilder.buildMultimodalExclude(req.multimodal),
                    excludeAddresses,
                )

            return PhotonAutocompleteRequest(
                query = req.q,
                limit = req.limit,
                language = handleLang(req.lang),
                includes = includes,
                excludes = excludes,
                lat = req.lat,
                lon = req.lon,
                zoom = req.photonZoom(),
                locationBiasScale = req.photonLocationBiasScale(),
                includeHousenumbers =
                    req.sources.any { it.contains("kartverket") || it.contains("matrikkelen") } && !req.q.contains("\\s\\d".toRegex()),
                debug = false,
            )
        }
    }
}
