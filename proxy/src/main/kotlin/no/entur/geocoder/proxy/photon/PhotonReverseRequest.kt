package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category
import no.entur.geocoder.proxy.pelias.PeliasReverseParams
import no.entur.geocoder.proxy.v3.V3ReverseParams
import java.math.BigDecimal

data class PhotonReverseRequest(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val language: String,
    val limit: Int,
    val radius: Int? = null,
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
) {
    companion object {
        fun from(params: PeliasReverseParams): PhotonReverseRequest {
            val includes = PhotonFilterBuilder.buildIncludes(
                boundaryCountry = params.boundaryCountry,
                boundaryCountyIds = params.boundaryCountyIds,
                boundaryLocalityIds = params.boundaryLocalityIds,
                tariffZones = params.tariffZones,
                tariffZoneAuthorities = params.tariffZoneAuthorities,
                sources = params.sources,
                layers = params.layers,
                categories = params.categories,
            )
            val excludes = listOfNotNull(
                Category.OSM_ADDRESS, // Always exclude addresses with house numbers in reverse requests
                PhotonFilterBuilder.buildMultiModalExclude(params.multiModal)
            )

            return PhotonReverseRequest(
                latitude = params.lat,
                longitude = params.lon,
                language = params.lang,
                limit = params.size,
                radius = params.radius,
                includes = includes,
                excludes = excludes,
            )
        }

        fun from(params: V3ReverseParams): PhotonReverseRequest =
            PhotonReverseRequest(
                latitude = params.lat,
                longitude = params.lon,
                language = params.language,
                limit = params.limit,
                radius = params.radius,
                excludes = listOf(Category.OSM_ADDRESS), // Exclude addresses with house numbers in reverse requests
            )
    }
}
