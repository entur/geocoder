package no.entur.geocoder.proxy.pelias

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.math.BigDecimal

data class PeliasResult(
    val geocoding: GeocodingMetadata = GeocodingMetadata(),
    val type: String = "FeatureCollection",
    val features: List<PeliasFeature> = emptyList(),
    val bbox: List<BigDecimal>? = null,
) {
    data class PeliasFeature(
        val type: String = "PhotonFeature",
        val geometry: PeliasGeometry,
        val properties: PeliasProperties,
    )

    data class PeliasGeometry(
        val type: String,
        val coordinates: List<BigDecimal>,
    )

    data class PeliasProperties(
        val type: String? = null,
        val countrycode: String? = null,
        val id: String? = null,
        val gid: String? = null,
        val layer: String? = null,
        val source: String? = null,
        val source_id: String? = null,
        val name: String? = null,
        val popular_name: String? = null,
        val housenumber: String? = null,
        val street: String? = null,
        val distance: BigDecimal? = null,
        val postalcode: String? = null,
        val accuracy: String? = null,
        val country_a: String? = null,
        val county: String? = null,
        val county_gid: String? = null,
        val locality: String? = null,
        val locality_gid: String? = null,
        val borough: String? = null,
        val borough_gid: String? = null,
        val label: String? = null,
        val category: List<String>? = null,
        @get:JsonSerialize(contentUsing = PairAsObjectSerializer::class)
        val mode: List<Pair<String, String?>>? = null,
        val city: String? = null,
        val tariff_zones: List<String>? = null,
        val description: List<Map<String, String>>? = null,
    )

    class PairAsObjectSerializer : JsonSerializer<Pair<String, String?>>() {
        override fun serialize(value: Pair<String, String?>, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeStartObject()
            gen.writeFieldName(value.first)
            if (value.second != null) gen.writeString(value.second) else gen.writeNull()
            gen.writeEndObject()
        }
    }

    data class GeocodingMetadata(
        val version: String = "0.2",
        val attribution: String = "http://pelias.mapzen.com/v1/attribution",
        val errors: List<String>? = null,
        val engine: EngineMetadata = EngineMetadata(),
        val timestamp: Long = System.currentTimeMillis(),
        val debug: Map<String, Any>? = null,
    ) {
        data class LangMetadata(
            val name: String = "Norwegian Bokmål",
            val iso6391: String = "nb",
            val iso6393: String = "nob",
            val defaulted: Boolean = false,
        )

        data class EngineMetadata(
            val name: String = "Photon",
            val author: String = "Komoot",
            val version: String = "0.7.0",
        )
    }
}
