package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.ImportanceCalculator
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasPlaceRequest
import no.entur.geocoder.proxy.v3.V3AutocompleteRequest

data class PhotonAutocompleteRequest(
    val query: String,
    val limit: Int,
    val language: String,
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val lat: Double?,
    val lon: Double?,
    val zoom: Int?,
    val weight: Double?,
) {
    companion object {
        /**
         * We drop the "by" if a GOSP exists with the same name. This can only be done after fetching,
         * so we fetch one extra and drop the last result if there is a match.
         */
        const val CITY_AND_GOSP_LIST_HEADROOM = 1

        fun from(req: PeliasAutocompleteRequest): PhotonAutocompleteRequest {
            val includes = PhotonFilterBuilder.buildIncludes(req)
            val excludes = PhotonFilterBuilder.buildExcludes(req)

            val zoom =
                req.focus?.scale?.let {
                    Geo.radiusToZoom(it.toDouble() / 5.0)
                } ?: Geo.radiusToZoom(2500.0 / 5.0)

            // Weight factor is flipped in Photon, so closer to 0 is more important
            // We therefore subtract the normalized value from 1
            // Considering this, we have a relatively low default focus weight in v1 (~0.8)
            // compared to the default in Photon (0.2)
            val weight = (1.0 - ImportanceCalculator.calculateImportance(req.focus?.weight ?: 15.0))

            return PhotonAutocompleteRequest(
                query = req.text,
                limit = req.size + CITY_AND_GOSP_LIST_HEADROOM, // Hack when we filter 'by' when there's already a matching GOSP
                language = req.lang,
                includes = includes,
                excludes = excludes,
                lat = req.focus?.lat,
                lon = req.focus?.lon,
                zoom = zoom,
                weight = weight,
            )
        }

        fun from(req: PeliasPlaceRequest): List<PhotonAutocompleteRequest> =
            req.ids.map { id ->
                PhotonAutocompleteRequest(
                    query = id,
                    limit = 1,
                    language = "no",
                    lat = null,
                    lon = null,
                    zoom = null,
                    weight = null,
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
                weight = null,
            )
        }
    }
}
