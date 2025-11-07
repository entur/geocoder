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
    val exclude: String = Category.OSM_ADDRESS, // Exclude addresses with house numbers in reverse requests
) {
    companion object {
        fun from(params: V3ReverseParams): PhotonReverseRequest =
            PhotonReverseRequest(
                latitude = params.lat,
                longitude = params.lon,
                language = params.language,
                limit = params.limit,
                radius = params.radius,
            )

        fun from(params: PeliasReverseParams): PhotonReverseRequest =
            PhotonReverseRequest(
                latitude = params.lat,
                longitude = params.lon,
                language = params.lang,
                limit = params.size,
                radius = params.radius,
            )
    }
}
