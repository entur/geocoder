package no.entur.netex_photon.proxy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class FeatureTransformer {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun parseAndTransform(input: String): FeatureCollection {
        val collection: FeatureCollection = mapper.readValue(input)
        return collection.copy(
            features = collection.features.map { transformFeature(it) }
        )
    }

    fun transformFeature(feature: Feature): Feature {
        val props = feature.properties
        val extra = props.extra
        return feature.copy(properties = Properties(
            id = extra?.id,
            gid = extra?.gid,
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
            label = extra?.label?.replace(",", ", ") ?: props.label,
            category = extra?.category?.split(',')?.map { it.trim() },
            tariff_zones = extra?.tariff_zones?.split(',')?.map { it.trim() },
            // All other fields from properties (for compatibility, but not in output)
            osmType = null,
            osmId = null,
            osmKey = null,
            osmValue = null,
            type = null,
            postcode = null,
            countrycode = null,
            extra = null // Remove extra from output
        ))
    }

    fun toJsonString(collection: FeatureCollection): String = mapper.writeValueAsString(collection)
}
