package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Geo
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
    val debug: Boolean = false,
) {
    companion object {
        /**
         * We drop the city (by) if a GOSP exists with the same name. This can only be done after fetching,
         * so we fetch one extra and drop the last result if there is a match.
         */
        const val CITY_AND_GOSP_LIST_HEADROOM = 3

        fun from(req: PeliasAutocompleteRequest): PhotonAutocompleteRequest {
            val includes = PhotonFilterBuilder.buildIncludes(req)
            val excludes = PhotonFilterBuilder.buildExcludes(req)

            // Convert Pelias focus to Photon parameters (null focus → no location bias)
            val zoom = Geo.peliasScaleToPhotonZoom(req.focus?.scale)
            val locationBiasScale = req.focus?.let { calculateLocationBias(it.weight ?: 15.0) }

            return PhotonAutocompleteRequest(
                query = req.text,
                limit = req.size + CITY_AND_GOSP_LIST_HEADROOM, // Hack when we filter 'by' when there's already a matching GOSP
                language = req.lang,
                includes = includes,
                excludes = excludes,
                lat = req.focus?.lat,
                lon = req.focus?.lon,
                zoom = zoom,
                locationBiasScale = locationBiasScale,
                debug = req.debug,
            )
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
                        addAll(req.countries.map { "country.$it" })
                    }
                    if (req.countyIds.isNotEmpty()) {
                        addAll(req.countyIds.map { "county_gid.$it" })
                    }
                    if (req.localityIds.isNotEmpty()) {
                        addAll(req.localityIds.map { "locality_gid.$it" })
                    }
                    if (req.tariffZones.isNotEmpty()) {
                        addAll(req.tariffZones.map { "tariff_zone_id.$it" })
                    }
                    if (req.tariffZoneAuthorities.isNotEmpty()) {
                        addAll(req.tariffZoneAuthorities.map { "tariff_zone_authority.$it" })
                    }
                    if (req.sources.isNotEmpty()) {
                        addAll(req.sources.map { "source.$it" })
                    }
                    if (req.placeTypes.isNotEmpty()) {
                        addAll(req.placeTypes.map { "layer.$it" })
                    }
                }

            return PhotonAutocompleteRequest(
                query = req.query,
                limit = req.limit,
                language = req.language,
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
