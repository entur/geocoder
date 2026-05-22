package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.pelias.PeliasReverseRequest
import no.entur.geocoder.proxy.photon.Lang.handleLang
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

        fun from(req: V3ReverseRequest): PhotonReverseRequest {
            val includes = PhotonFilterBuilder.buildIncludes(req)

            val callerWantsAddresses =
                req.layers.contains("address") ||
                    req.sources.any { it.contains("kartverket") || it.contains("matrikkelen") }
            val excludeAddresses = if (callerWantsAddresses) null else Category.LAYER_ADDRESS

            return PhotonReverseRequest(
                latitude = req.lat,
                longitude = req.lon,
                language = handleLang(req.lang),
                limit = req.limit,
                radius = req.radius,
                includes = includes,
                excludes = listOfNotNull(excludeAddresses, PhotonFilterBuilder.buildMultimodalExclude(req.multimodal)),
                debug = req.debug,
            )
        }
    }
}
