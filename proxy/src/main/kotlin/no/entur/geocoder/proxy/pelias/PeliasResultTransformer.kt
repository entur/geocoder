package no.entur.geocoder.proxy.pelias

import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_PREFIX
import no.entur.geocoder.common.Category.LEGACY_SOURCE_PREFIX
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.Source
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import no.entur.geocoder.proxy.pelias.PeliasResult.PeliasFeature
import no.entur.geocoder.proxy.pelias.PeliasResult.PeliasProperties
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.*
import java.math.BigDecimal
import java.math.RoundingMode

object PeliasResultTransformer {
    fun parseAndTransform(photonResult: PhotonResult, lat: BigDecimal? = null, lon: BigDecimal? = null): PeliasResult {
        val transformedFeatures =
            photonResult.features.map { feature ->
                val distance = lat?.let { lon?.let { calculateDistanceKm(feature.geometry, lat, lon) } }
                transformFeature(feature, distance)
            }

        val bbox = calculateBoundingBox(transformedFeatures)

        return PeliasResult(
            features = transformedFeatures,
            bbox = bbox?.map { it.setScale(6, RoundingMode.HALF_UP) },
        )
    }

    private fun calculateBoundingBox(features: List<PeliasFeature>): List<BigDecimal>? {
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

    fun transformFeature(feature: PhotonFeature, distance: BigDecimal?): PeliasFeature {
        val props = feature.properties
        val extra = props.extra
        val source = transformSource(extra)
        val layer = transformLayer(extra)

        return PeliasFeature(
            type = feature.type,
            geometry =
                PeliasResult.PeliasGeometry(
                    type = feature.geometry.type,
                    coordinates = feature.geometry.coordinates,
                ),
            properties =
                PeliasProperties(
                    id = extra?.id,
                    gid = transformGid(source, layer, extra?.id),
                    layer = layer,
                    source = source,
                    source_id = extra?.id,
                    name = transformName(props),
                    popular_name =
                        extra
                            ?.alt_name
                            ?.split(";")
                            ?.firstOrNull()
                            ?.ifBlank { null },
                    street = transformStreet(props),
                    distance = distance,
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
                    label = createLabel(props),
                    category = transformCategory(extra),
                    tariff_zones = extra?.tariff_zones?.split(',')?.map { it.trim() },
                ),
        )
    }

    private fun createLabel(props: PhotonProperties): String? =
        when {
            props.name.isNullOrBlank() && !props.housenumber.isNullOrEmpty() -> {
                "${props.street} ${props.housenumber}, ${props.extra?.locality}"
            }
            props.name.isNullOrBlank() -> {
                props.extra?.locality
            }
            !props.extra?.locality.isNullOrEmpty() && props.name != props.extra.locality -> {
                "${props.name}, ${props.extra.locality}"
            }
            else -> {
                props.name
            }
        }

    private fun transformGid(source: String?, layer: String?, extraId: String?): String? =
        extraId?.let { "$source:$layer:$it" }

    private fun transformStreet(props: PhotonProperties): String? =
        when {
            props.street != null -> props.street
            props.extra?.source != Source.KARTVERKET_STEDSNAVN -> "NOT_AN_ADDRESS-" + props.extra?.id
            else -> null
        }

    private fun transformName(props: PhotonProperties): String? =
        when {
            props.name != null -> props.name
            props.street != null && props.housenumber != null -> "${props.street} ${props.housenumber}"
            else -> props.street
        }

    fun transformCategory(extra: Extra?): List<String> =
        extra
            ?.tags
            ?.split(",")
            ?.filter { it.startsWith(LEGACY_CATEGORY_PREFIX) }
            ?.map { it.substringAfterLast(".") }
            ?: emptyList()

    fun transformSource(extra: Extra?): String? =
        extra
            ?.tags
            ?.split(",")
            ?.firstOrNull { it.startsWith(LEGACY_SOURCE_PREFIX) }
            ?.substringAfterLast(".")

    fun transformLayer(extra: Extra?): String? =
        extra
            ?.tags
            ?.split(",")
            ?.firstOrNull { it.startsWith(LEGACY_LAYER_PREFIX) }
            ?.substringAfterLast(".")

    fun transformBoroughGid(boroughGid: String?): String? =
        boroughGid?.let { "whosonfirst:$it" }

    fun transformCountyGid(countyGid: String?): String? =
        countyGid?.let { "whosonfirst:county:$it" }

    fun transformLocalityGid(localityGid: String?): String? =
        localityGid?.let { "whosonfirst:locality:$it" }

    const val PELIAS_DISTANCE_FUDGE_FACTOR = 1.001119

    internal fun calculateDistanceKm(
        geometry: PhotonGeometry,
        lat: BigDecimal,
        lon: BigDecimal,
    ): BigDecimal? {
        val featureCoords = geometry.coordinates
        if (featureCoords.size < 2) return null

        val lon1 = featureCoords[0].toDouble()
        val lat1 = featureCoords[1].toDouble()
        val lon2 = lon.toDouble()
        val lat2 = lat.toDouble()
        val distance = Geo.haversineDistance(lat1, lon1, lat2, lon2)

        return ((distance * PELIAS_DISTANCE_FUDGE_FACTOR) / 1000).toBigDecimalWithScale(3)
    }
}
