package no.entur.geocoder.proxy.pelias

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Extra
import no.entur.geocoder.proxy.pelias.PeliasResult.PeliasProperties
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonFeature
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonProperties
import java.math.BigDecimal
import java.math.RoundingMode

class PeliasResultTransformer {
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun parseAndTransform(photonResult: PhotonResult): String {
        val transformedFeatures = photonResult.features.map { transformFeature(it) }

        val bbox = calculateBoundingBox(transformedFeatures)

        val peliasCollection: PeliasResult = PeliasResult(
            features = transformedFeatures,
            bbox = bbox?.map { it.setScale(6, RoundingMode.HALF_UP) },
        )
        return mapper.writeValueAsString(peliasCollection)
    }

    private fun calculateBoundingBox(features: List<PeliasResult.PeliasFeature>): List<BigDecimal>? {
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

    fun transformFeature(feature: PhotonFeature): PeliasResult.PeliasFeature {
        val props = feature.properties
        val extra = props.extra

        return PeliasResult.PeliasFeature(
            type = feature.type,
            geometry = PeliasResult.PeliasGeometry(
                type = feature.geometry.type,
                coordinates = feature.geometry.coordinates,
            ),
            properties = PeliasProperties(
                id = extra?.id,
                gid = "whosonfirst:address:" + extra?.id,
                layer = transformLayer(extra),
                source = transformSource(extra),
                source_id = extra?.id,
                name = transformName(props),
                street = transformString(props),
                postalcode = props.postcode,
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

    private fun transformString(props: PhotonProperties): String? =
        when {
            props.street != null -> props.street
            props.extra?.source != "kartverket-stedsnavn" -> "NOT_AN_ADDRESS-" + props.extra?.id
            else -> null
        }

    private fun transformName(props: PhotonProperties): String? =
        when {
            props.name != null -> props.name
            props.street != null && props.housenumber != null -> "${props.street} ${props.housenumber}"
            else -> props.street
        }

    fun transformCategory(extra: Extra?): List<String> {
        val category = mutableSetOf<String>()
        extra?.transport_modes?.split(',')?.map { it.trim() }?.let {
            category.addAll(it)
        }
        if (extra?.source == "kartverket-matrikkelenadresse") {
            if (isStreet(extra)) category.add("street") else category.add("vegadresse")
        }
        if (extra?.source == "openstreetmap") {
            category.add("poi")
        }
        if (isGosp(extra)) {
            category.add("GroupOfStopPlaces")
        }
        if (extra?.tags?.isNotBlank() == true) {
            extra.tags?.split(",")?.forEach { tag ->
                val parts = tag.split('.')
                if (parts.size == 2 && parts[1].isNotBlank()) {
                    category.add(parts[1])
                }
            }
        }
        return category.toList()
    }

    fun transformSource(extra: Extra?): String? =
        when {
            isGosp(extra) -> "whosonfirst"
            extra?.source == "openstreetmap" -> "whosonfirst"
            extra?.source == "nsr" -> "openstreetmap"
            extra?.source == "kartverket-matrikkelenadresse" && isStreet(extra) -> "whosonfirst"
            extra?.source == "kartverket-matrikkelenadresse" -> "openaddresses"
            extra?.source == "kartverket-stedsnavn" -> "whosonfirst"
            else -> extra?.source
        }

    fun transformLayer(extra: Extra?): String? =
        when {
            isGosp(extra) -> "address"
            extra?.source == "nsr" -> "venue"
            extra?.source == "openstreetmap" -> "address"
            extra?.source == "kartverket-matrikkelenadresse" -> "address"
            extra?.source == "kartverket-stedsnavn" -> "address"
            else -> extra?.source
        }

    fun isStreet(extra: Extra): Boolean =
        extra.tags?.split(',')?.any { it == Category.OSM_STREET } == true

    fun isGosp(extra: Extra?): Boolean = extra?.id?.contains("GroupOfStopPlaces") == true

    fun transformBoroughGid(boroughGid: String?): String? =
        boroughGid?.let { "whosonfirst:$it" }

    fun transformCountyGid(countyGid: String?): String? =
        countyGid?.let { "whosonfirst:county:$it" }

    fun transformLocalityGid(localityGid: String?): String? =
        localityGid?.let { "whosonfirst:locality:$it" }
}