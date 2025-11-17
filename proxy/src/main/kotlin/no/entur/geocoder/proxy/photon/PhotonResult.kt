package no.entur.geocoder.proxy.photon

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.common.Extra

data class PhotonResult(
    val type: String = "FeatureCollection",
    val features: List<PhotonFeature> = emptyList(),
    val bbox: List<Double>? = null,
) {
    companion object {
        private val mapper: ObjectMapper =
            jacksonObjectMapper().apply {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }

        fun parse(json: String): PhotonResult = mapper.readValue(json)
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
