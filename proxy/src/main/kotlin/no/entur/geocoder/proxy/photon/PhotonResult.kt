package no.entur.geocoder.proxy.photon

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.JsonMapper.jacksonMapper

data class PhotonResult(
    val type: String = "FeatureCollection",
    val features: List<PhotonFeature> = emptyList(),
    val properties: Map<String, Any> = emptyMap(),
    @JsonIgnore val status: HttpStatusCode = HttpStatusCode.OK,
) {
    companion object {
        fun parse(body: String, url: Url, status: HttpStatusCode = HttpStatusCode.OK): PhotonResult {
            val result: PhotonResult = jacksonMapper.readValue(body)
            return result.copy(
                properties = result.properties + ("query_url" to url.toString()),
                status = status,
            )
        }
    }

    data class PhotonFeature(
        val type: String = "PhotonFeature",
        val geometry: PhotonGeometry,
        val properties: PhotonProperties,
    )

    data class PhotonGeometry(
        val type: String,
        val coordinates: List<Double>, // [lon, lat]
    )

    data class PhotonProperties(
        val osm_type: String? = null,
        val osm_id: Long? = null,
        val osm_key: String? = null,
        val osm_value: String? = null,
        val type: String? = null,
        val postcode: String? = null,
        val housenumber: String? = null,
        val countrycode: String? = null,
        val name: String? = null,
        val city: String? = null,
        val street: String? = null,
        val county: String? = null,
        val extra: Extra? = null,
    )
}
