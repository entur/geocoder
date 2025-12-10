package no.entur.geocoder.proxy.photon

import no.entur.geocoder.common.Category
import no.entur.geocoder.proxy.pelias.PeliasReverseRequest
import no.entur.geocoder.proxy.v3.V3ReverseRequest

data class PhotonReverseRequest(
    val latitude: Double,
    val longitude: Double,
    val language: String,
    val limit: Int,
    val radius: Double? = null,
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val debug: Boolean = false,
) {
    companion object {
        fun from(req: PeliasReverseRequest): PhotonReverseRequest {
            val includes = PhotonFilterBuilder.buildIncludes(req)
            val excludes = PhotonFilterBuilder.buildExcludes(req)

            return PhotonReverseRequest(
                latitude = req.lat,
                longitude = req.lon,
                language = handleLang(req.lang),
                limit = req.size,
                radius = req.radius,
                includes = includes,
                excludes = excludes,
                debug = req.debug,
            )
        }

        fun from(req: V3ReverseRequest): PhotonReverseRequest =
            PhotonReverseRequest(
                latitude = req.lat,
                longitude = req.lon,
                language = handleLang(req.language),
                limit = req.limit,
                radius = req.radius,
                excludes = listOf(Category.OSM_ADDRESS), // Exclude addresses with house numbers in reverse requests
                debug = false,
            )

        private fun handleLang(lang: String): String = if (lang == "nb") "no" else lang
    }
}
