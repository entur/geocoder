package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.ImportanceCalculator
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteParams
import no.entur.geocoder.proxy.pelias.PeliasPlaceParams
import no.entur.geocoder.proxy.v3.V3AutocompleteParams

data class PhotonAutocompleteRequest(
    val query: String,
    val limit: Int,
    val language: String,
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val lat: String?,
    val lon: String?,
    val zoom: String?,
    val weight: String?,
) {
    companion object {
        fun from(params: PeliasAutocompleteParams): PhotonAutocompleteRequest {
            val includes: List<String> =
                buildList {
                    params.boundaryCountry?.let { add("country.$it") }
                    if (params.boundaryCountyIds.isNotEmpty()) {
                        addAll(params.boundaryCountyIds.map { "county_gid.$it" })
                    }
                    if (params.boundaryLocalityIds.isNotEmpty()) {
                        addAll(params.boundaryLocalityIds.map { "locality_gid.$it" })
                    }
                    if (params.tariffZones.isNotEmpty()) {
                        addAll(params.tariffZones.map { "tariff_zone_id.$it" })
                    }
                    if (params.tariffZoneAuthorities.isNotEmpty()) {
                        addAll(params.tariffZoneAuthorities.map { "tariff_zone_authority.$it" })
                    }
                    if (params.sources.isNotEmpty()) {
                        addAll(params.sources.map { "legacy.source.$it" })
                    }
                    if (params.layers.isNotEmpty()) {
                        addAll(params.layers.map { "legacy.layer.$it" })
                    }
                    if (params.categories.isNotEmpty()) {
                        if (params.categories.none { it == "NO_FILTER" }) {
                            addAll(params.categories.map { LEGACY_CATEGORY_PREFIX + it })
                        }
                    }
                }
            val excludes =
                buildList {
                    when (params.multiModal) {
                        "child" -> add("multimodal.parent")
                        "parent" -> add("multimodal.child")
                        "both" -> {
                            // No exclusion
                        }
                    }
                }

            val zoom =
                params.focus?.scale?.split("km")?.get(0)?.toDoubleOrNull().let {
                    Geo.radiusToZoom((it ?: 2500.0) / 5.0)
                }

            val weight =
                params.focus?.weight?.toDoubleOrNull().let {
                    // Weight factor is flipped in Photon, so closer to 0 is more important
                    // We therefore subtract the normalized value from 1
                    // Considering this, we have a relatively low default focus weight in v1 (~0.8)
                    // compared to the default in Photon (0.2)
                    (1.0 - ImportanceCalculator.calculateImportance(it ?: 15.0)).toString()
                }

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
