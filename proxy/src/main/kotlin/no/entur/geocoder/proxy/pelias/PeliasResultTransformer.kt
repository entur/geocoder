package no.entur.geocoder.proxy.pelias

import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_PREFIX
import no.entur.geocoder.common.Category.LEGACY_SOURCE_PREFIX
import no.entur.geocoder.common.Coordinate
import no.entur.geocoder.common.Coordinate.Companion.coordOrNull
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.Source
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import no.entur.geocoder.proxy.pelias.PeliasResult.*
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.*
import java.math.BigDecimal
import java.math.RoundingMode

object PeliasResultTransformer {
    fun parseAndTransform(result: PhotonResult, request: PeliasAutocompleteRequest): PeliasResult =
        parseAndTransform(
            photonResult = result,
            expectedSize = request.size,
            coord = coordOrNull(request.focus?.lat, request.focus?.lon),
            debug = request.debug,
        )

    fun parseAndTransform(result: PhotonResult, request: PeliasReverseRequest): PeliasResult =
        parseAndTransform(
            photonResult = result,
            expectedSize = request.size,
            coord = coordOrNull(request.lat, request.lon),
            debug = request.debug,
        )

    fun parseAndTransform(result: PhotonResult, request: PeliasPlaceRequest): PeliasResult =
        parseAndTransform(
            photonResult = result,
            expectedSize = request.ids.size,
            debug = request.debug,
        )

    internal fun parseAndTransform(
        photonResult: PhotonResult,
        expectedSize: Int,
        coord: Coordinate? = null,
        debug: Boolean = false,
    ): PeliasResult {
        val errors = photonResult.message?.let { listOf(it) }

        val transformedFeatures =
            photonResult.features.map { feature ->
                val distance = coord?.let { calculateDistanceKm(feature.geometry, coord) }
                transformFeature(feature, distance)
            }

        val bbox = calculateBoundingBox(transformedFeatures)

        val debugInfo =
            if (debug && photonResult.properties.isNotEmpty()) {
                photonResult.properties
            } else {
                null
            }

        return PeliasResult(
            geocoding = GeocodingMetadata(debug = debugInfo, errors = errors),
            features = filterCityIfGospIsPresent(transformedFeatures, expectedSize),
            bbox = bbox?.map { it.setScale(6, RoundingMode.HALF_UP) },
        )
    }

    /**
     * We're making room for more results by using PhotonAutocompleteRequest#RESULT_PRUNING_HEADROOM in the request
     */
    private fun filterCityIfGospIsPresent(features: List<PeliasFeature>, expectedSize: Int): List<PeliasFeature> {
        val gospList =
            features
                .filter { it.properties.category?.contains("GroupOfStopPlaces") == true }
                .map { it.properties.name }
        val filtered =
            features.filter {
                !(it.properties.category?.contains("by") == true && gospList.contains(it.properties.name))
            }
        return filtered.take(expectedSize)
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

    fun transformFeature(feature: PhotonFeature, distance: Double?): PeliasFeature {
        val props = feature.properties
        val extra = props.extra
        val source = transformSource(extra)
        val layer = transformLayer(extra)

        return PeliasFeature(
            type = feature.type,
            geometry =
                PeliasGeometry(
                    type = feature.geometry.type,
                    coordinates = feature.geometry.coordinates.toBigDecimalList(),
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
                    distance = distance?.toBigDecimalWithScale(3),
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
                    description = transformDescription(extra),
                ),
        )
    }

    private fun transformDescription(extra: Extra?): List<Map<String, String>>? {
        val description = extra?.description ?: return null

        return if (description.contains("\\w{3}:".toRegex())) {
            // Parse entries with language prefixes: "nor:text" or "nor:text;eng:text"
            description.split(";").mapNotNull { part ->
                val colonIndex = part.indexOf(":")
                if (colonIndex > 0) {
                    val langCode = part.take(colonIndex).trim()
                    val text = part.substring(colonIndex + 1).trim()
                    mapOf(langCode to text)
                } else {
                    null
                }
            }
        } else {
            listOf(mapOf("nor" to description))
        }
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
        coord: Coordinate,
    ): Double? {
        val featureCoords = geometry.coordinates
        if (featureCoords.size < 2) return null

        val coord1 = Coordinate(featureCoords[1], featureCoords[0])
        val distance = Geo.haversineDistance(coord1, coord)

        return ((distance * PELIAS_DISTANCE_FUDGE_FACTOR) / 1000)
    }
}

private fun List<Double>.toBigDecimalList(): List<BigDecimal> = this.map { it.toBigDecimalWithScale() }
