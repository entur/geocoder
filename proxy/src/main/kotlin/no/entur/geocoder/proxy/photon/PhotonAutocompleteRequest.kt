package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Category.COUNTRY_PREFIX
import no.entur.geocoder.common.Category.TARIFF_ZONE_AUTH_PREFIX
import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.Source
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasPlaceRequest
import no.entur.geocoder.proxy.photon.LocationBiasCalculator.calculateLocationBias
import no.entur.geocoder.proxy.v3.V3AutocompleteRequest

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
         * see PeliasResultTransformer#filterCityIfGospIsPresent()
         */
        const val RESULT_PRUNING_HEADROOM = 3

        fun from(req: PeliasAutocompleteRequest): PhotonAutocompleteRequest {
            val includes = PhotonFilterBuilder.buildIncludes(req)
            val excludes = PhotonFilterBuilder.buildExcludes(req)

            // Convert Pelias focus to Photon parameters (null focus → no location bias)
            val zoom = Geo.peliasScaleToPhotonZoom(req.focus?.scale)
            val locationBiasScale = req.focus?.let { calculateLocationBias(it.weight ?: 15.0) }

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
                includeHousenumbers = req.sources.contains(Source.LEGACY_OPENADDRESSES) && !req.text.contains("\\s\\d".toRegex()),
            )
        }

        private fun handleLang(lang: String): String = if (lang == "nb") "no" else lang

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

        fun from(req: PeliasPlaceRequest): List<PhotonAutocompleteRequest> =
            req.ids.map { id ->
                PhotonAutocompleteRequest(
                    query = id,
                    limit = 1,
                    debug = req.debug,
                )
            }

        fun from(req: V3AutocompleteRequest): PhotonAutocompleteRequest {
            val includes =
                buildList {
                    if (req.countries.isNotEmpty()) {
                        add(req.countries.joinToString(",") { COUNTRY_PREFIX + it })
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
                    if (req.tariffZoneAuthorities.isNotEmpty()) {
                        add(req.tariffZoneAuthorities.joinToString(",") { TARIFF_ZONE_AUTH_PREFIX + it })
                    }
                    if (req.fareZoneAuthorities.isNotEmpty()) {
                        add(req.fareZoneAuthorities.joinToString(",") { Category.fareZoneAuthorityCategory(it) })
                    }
                    if (req.sources.isNotEmpty()) {
                        add(req.sources.joinToString(",") { "source.$it" })
                    }
                    if (req.placeTypes.isNotEmpty()) {
                        add(req.placeTypes.joinToString(",") { "layer.$it" })
                    }
                }

            return PhotonAutocompleteRequest(
                query = req.query,
                limit = req.limit,
                language = handleLang(req.language),
                includes = includes,
                lat = null,
                lon = null,
                zoom = null,
                locationBiasScale = null,
                debug = false,
            )
        }
    }
}
