package no.entur.netex_photon.proxy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class FeatureTransformer {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun parseAndTransform(input: String): String {
        val collection: FeatureCollection = mapper.readValue(input)
        val geocoding: GeocodingMetadata? = createGeocodingMetadata(collection)
        val transformedFeatures = collection.features.map { transformFeature(it) }

        val bbox = calculateBoundingBox(transformedFeatures)

        val enhancedCollection = collection.copy(
            features = transformedFeatures,
            geocoding = geocoding,
            bbox = bbox
        )
        return mapper.writeValueAsString(enhancedCollection)
    }

    private fun createGeocodingMetadata(collection: FeatureCollection): GeocodingMetadata {
        val queryText = collection.features.firstOrNull()?.properties?.name ?: ""
        val tokens = if (queryText.isNotEmpty()) queryText.split(" ").filter { it.isNotEmpty() } else null

        return GeocodingMetadata(
            query = QueryMetadata(
                text = queryText,
                tokens = tokens,
                lang = LangMetadata()
            ),
            engine = EngineMetadata(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun calculateBoundingBox(features: List<Feature>): List<Double>? {
        if (features.isEmpty()) return null

        var minLon = Double.MAX_VALUE
        var minLat = Double.MAX_VALUE
        var maxLon = Double.MIN_VALUE
        var maxLat = Double.MIN_VALUE

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

        return if (minLon != Double.MAX_VALUE) {
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
