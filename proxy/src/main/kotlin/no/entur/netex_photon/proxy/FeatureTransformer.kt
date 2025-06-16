package no.entur.netex_photon.proxy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.math.RoundingMode

class FeatureTransformer {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun parseAndTransform(input: String): String {
        val collection: FeatureCollection = mapper.readValue(input)
        val transformedFeatures = collection.features.map { transformFeature(it) }

        val bbox = calculateBoundingBox(transformedFeatures)

        val enhancedCollection = collection.copy(
            features = transformedFeatures,
            bbox = bbox?.map { it.setScale(6, RoundingMode.HALF_UP) }
        )
        return mapper.writeValueAsString(enhancedCollection)
    }

    private fun calculateBoundingBox(features: List<Feature>): List<BigDecimal>? {
        if (features.isEmpty()) return null

        var minLon = BigDecimal(Double.MAX_VALUE)
        var minLat = BigDecimal(Double.MAX_VALUE)
        var maxLon = BigDecimal(Double.MIN_VALUE)
        var maxLat = BigDecimal(Double.MIN_VALUE)

        features.forEach { feature ->
            val coords = feature.geometry.coordinates
            if (coords.size >= 2) {
                val lon = coords[0]
                val lat = coords[1]

                minLon = minOf(minLon, lon)
                minLat = minOf(minLat, lat)
                maxLon = maxOf(maxLon, lon)
                maxLat = maxOf(maxLat, lat)
            }
        }

        return if (minLon != BigDecimal(Double.MAX_VALUE)) {
            listOf(minLon, minLat, maxLon, maxLat)
        } else null
    }

    fun transformFeature(feature: Feature): Feature {
        val props = feature.properties
        val extra = props.extra
        return feature.copy(
            properties = Properties(
                id = extra?.id,
                layer = extra?.layer,
                source = extra?.source,
                source_id = extra?.source_id,
                name = props.name,
                street = props.street,
                accuracy = extra?.accuracy,
                country_a = extra?.country_a,
                county = props.county,
                county_gid = extra?.county_gid,
                locality = extra?.locality,
                locality_gid = extra?.locality_gid,
                label = extra?.label?.replace(", *".toRegex(), ", ") ?: props.label,
                transport_modes = extra?.transport_modes?.split(',')?.map { it.trim() },
                tariff_zones = extra?.tariff_zones?.split(',')?.map { it.trim() } // Remove extra from output
            )
        )
    }
}
