package no.entur.geocoder.proxy

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.common.Extra
import no.entur.geocoder.proxy.FeatureCollection.Feature
import no.entur.geocoder.proxy.FeatureCollection.Properties
import java.math.BigDecimal
import java.math.RoundingMode

class FeatureTransformer {
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun parseAndTransform(input: String): String {
        val collection: FeatureCollection = mapper.readValue(input)
        val transformedFeatures = collection.features.map { transformFeature(it) }

        val bbox = calculateBoundingBox(transformedFeatures)

        val enhancedCollection =
            collection.copy(
                features = transformedFeatures,
                bbox = bbox?.map { it.setScale(6, RoundingMode.HALF_UP) },
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
        } else {
            null
        }
    }

    fun transformFeature(feature: Feature): Feature {
        val props = feature.properties
        val extra = props.extra
        return feature.copy(
            properties =
                Properties(
                    id = extra?.id,
                    layer = transformLayer(extra),
                    source = transformSource(extra?.source),
                    source_id = transformSourceId(extra),
                    name = props.name,
                    street = props.street,
                    housenumber = props.housenumber,
                    accuracy = extra?.accuracy,
                    country_a = extra?.country_a,
                    county = props.county,
                    county_gid = transformCountyGid(extra?.county_gid),
                    locality = extra?.locality,
                    locality_gid = transformLocalityGid(extra?.locality_gid),
                    borough = extra?.borough,
                    borough_gid = transformBoroughGid(extra?.borough_gid),
                    label = extra?.label?.replace(", *".toRegex(), ", ") ?: props.label,
                    category = transformCategory(extra),
                    tariff_zones = extra?.tariff_zones?.split(',')?.map { it.trim() },
                ),
        )
    }

    fun transformSourceId(extra: Extra?): String? =
        if (extra?.source_id != null && extra.source == "osm") {
            "OSM:TopographicPlace:${extra.source_id}"
        } else {
            extra?.source_id
        }

    fun transformCategory(extra: Extra?): List<String> {
        val category = mutableSetOf<String>()
        extra?.transport_modes?.split(',')?.map { it.trim() }?.let {
            category.addAll(it)
        }
        if (extra?.source == "kartverket") {
            category.add("vegadresse")
        }
        if (extra?.source == "osm") {
            category.add("poi")
            extra.layer?.let { category.add(it) }
        }
        return category.toList()
    }

    fun transformSource(source: String?): String? =
        when (source?.lowercase()) {
            "osm" -> "whosonfirst"
            "nsr" -> "openstreetmap"
            "kartverket" -> "openaddresses"
            else -> source
        }

    fun transformLayer(extra: Extra?): String? =
        if (extra?.layer == "stopplace") {
            "venue"
        } else if (extra?.source == "osm") {
            "address"
        } else {
            extra?.layer
        }

    fun transformBoroughGid(boroughGid: String?): String? =
        boroughGid?.let { "whosonfirst:$it" }

    fun transformCountyGid(countyGid: String?): String? =
        countyGid?.let { "whosonfirst:county:$it" }

    fun transformLocalityGid(localityGid: String?): String? =
        localityGid?.let { "whosonfirst:locality:$it" }
}
