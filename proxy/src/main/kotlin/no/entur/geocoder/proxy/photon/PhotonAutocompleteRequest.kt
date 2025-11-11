package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.ImportanceCalculator
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteParams
import no.entur.geocoder.proxy.pelias.PeliasPlaceParams
import no.entur.geocoder.proxy.v3.V3AutocompleteParams
import java.math.BigDecimal

data class PhotonAutocompleteRequest(
    val query: String,
    val limit: Int,
    val language: String,
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val lat: BigDecimal?,
    val lon: BigDecimal?,
    val zoom: Int?,
    val weight: Double?,
) {
    companion object {
        fun from(params: PeliasAutocompleteParams): PhotonAutocompleteRequest {
            val includes =
                PhotonFilterBuilder.buildIncludes(
                    boundaryCountry = params.boundaryCountry,
                    boundaryCountyIds = params.boundaryCountyIds,
                    boundaryLocalityIds = params.boundaryLocalityIds,
                    tariffZones = params.tariffZones,
                    tariffZoneAuthorities = params.tariffZoneAuthorities,
                    sources = params.sources,
                    layers = params.layers,
                    categories = params.categories,
                )
            val excludes = listOfNotNull(PhotonFilterBuilder.buildMultiModalExclude(params.multiModal))

            val zoom =
                params.focus?.scale?.let {
                    Geo.radiusToZoom(it.toDouble() / 5.0)
                } ?: Geo.radiusToZoom(2500.0 / 5.0)

            // Weight factor is flipped in Photon, so closer to 0 is more important
            // We therefore subtract the normalized value from 1
            // Considering this, we have a relatively low default focus weight in v1 (~0.8)
            // compared to the default in Photon (0.2)
            val weight = (1.0 - ImportanceCalculator.calculateImportance(params.focus?.weight ?: 15.0))

            return PhotonAutocompleteRequest(
                query = params.text,
                limit = params.size,
                language = params.lang,
                includes = includes,
                excludes = excludes,
                lat = params.focus?.lat,
                lon = params.focus?.lon,
                zoom = zoom,
                weight = weight,
            )
        }

        fun from(params: PeliasPlaceParams): List<PhotonAutocompleteRequest> =
            params.ids.map { id ->
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

        fun from(params: V3AutocompleteParams): PhotonAutocompleteRequest {
            val includes =
                buildList {
                    if (params.countries.isNotEmpty()) {
                        addAll(params.countries.map { "country.$it" })
                    }
                    if (params.countyIds.isNotEmpty()) {
                        addAll(params.countyIds.map { "county_gid.$it" })
                    }
                    if (params.localityIds.isNotEmpty()) {
                        addAll(params.localityIds.map { "locality_gid.$it" })
                    }
                    if (params.tariffZones.isNotEmpty()) {
                        addAll(params.tariffZones.map { "tariff_zone_id.$it" })
                    }
                    if (params.tariffZoneAuthorities.isNotEmpty()) {
                        addAll(params.tariffZoneAuthorities.map { "tariff_zone_authority.$it" })
                    }
                    if (params.sources.isNotEmpty()) {
                        addAll(params.sources.map { "source.$it" })
                    }
                    if (params.placeTypes.isNotEmpty()) {
                        addAll(params.placeTypes.map { "layer.$it" })
                    }
                }

            return PhotonAutocompleteRequest(
                query = params.query,
                limit = params.limit,
                language = params.language,
                includes = includes,
                lat = null,
                lon = null,
                zoom = null,
                weight = null,
            )
        }
    }
}
