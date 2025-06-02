package no.entur.netex_photon.proxy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@Serializable
private data class PhotonFeature(
    val type: String,
    val properties: PhotonProperties,
    val geometry: JsonObject
)

@Serializable
private data class PhotonProperties(
    val osm_type: String? = null,
    val osm_id: Long? = null,
    val osm_key: String? = null,
    val osm_value: String? = null,
    val type: String? = null,
    val postcode: String? = null,
    val countrycode: String? = null,
    val name: String? = null,
    val street: String? = null,
    val county: String? = null,
    val extra: JsonObject? = null
)