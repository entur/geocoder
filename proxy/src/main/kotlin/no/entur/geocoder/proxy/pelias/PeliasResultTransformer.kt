package no.entur.geocoder.proxy.pelias

import no.entur.geocoder.proxy.common.Category.GOSP
import no.entur.geocoder.proxy.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.proxy.common.Coordinate
import no.entur.geocoder.proxy.common.Coordinate.Companion.coordOrNull
import no.entur.geocoder.proxy.common.Extra
import no.entur.geocoder.proxy.common.Geo
import no.entur.geocoder.proxy.common.InterimIds
import no.entur.geocoder.proxy.common.LegacyLayer.Companion.LEGACY_LAYER_PREFIX
import no.entur.geocoder.proxy.common.LegacySource.Companion.LEGACY_SOURCE_PREFIX
import no.entur.geocoder.proxy.common.Source
import no.entur.geocoder.proxy.common.Text.OSM_TAG_SEPARATOR
import no.entur.geocoder.proxy.common.Util.toBigDecimalWithScale
import no.entur.geocoder.proxy.pelias.PeliasResult.*
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResultFilter
import no.entur.geocoder.proxy.photon.PhotonResult.*
import java.math.BigDecimal
import java.math.RoundingMode

object PeliasResultTransformer {
    private const val KVE_POSTAL_ADDRESS_PREFIX = "KVE:PostalAddress:"
    private const val KVE_PLACE_NAME_PREFIX = "KVE:PlaceName:"

    /**
     * Map v3-shape index IDs to the v2 wire shape. v2 stedsnavn IDs are bare numeric
     * `lokal_id`; v2 postal addresses are also bare numeric. Everything else passes
     * through (after [InterimIds] canonicalisation of interim index shapes).
     */
    private fun normalizeV2Id(id: String, source: String?): String {
        if (source == Source.KARTVERKET_STEDSNAVN) {
            id.removePrefix(KVE_PLACE_NAME_PREFIX).let { if (it != id) return it }
        }
        return InterimIds.canonicaliseOsmId(id).removePrefix(KVE_POSTAL_ADDRESS_PREFIX)
    }

    fun parseAndTransform(result: PhotonResult, request: PeliasAutocompleteRequest): PeliasResult =
        parseAndTransform(
            photonResult = result,
            expectedSize = request.size,
            coord = coordOrNull(request.focus?.lat, request.focus?.lon),
            debug = request.debug,
            dropDuplicatePlaces = true,
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
        dropDuplicatePlaces: Boolean = false,
    ): PeliasResult {
        val errors = photonResult.message?.let { listOf(it) }

        val photonFeatures =
            if (dropDuplicatePlaces) PhotonResultFilter.dropPlacesCoveredByGosp(photonResult.features) else photonResult.features

        val features =
            photonFeatures
                .map { feature ->
                    val distance = coord?.let { calculateDistanceKm(feature.geometry, coord) }
                    transformFeature(feature, distance)
                }.take(expectedSize)

        val debugInfo =
            if (debug && photonResult.properties.isNotEmpty()) {
                photonResult.properties
            } else {
                null
            }

        return PeliasResult(
            geocoding = GeocodingMetadata(debug = debugInfo, errors = errors),
            features = features,
            bbox = calculateBoundingBox(features)?.map { it.setScale(6, RoundingMode.HALF_UP) },
        )
    }

    /** Spans the returned features only, not the extra ones fetched as pruning headroom. */
    private fun calculateBoundingBox(features: List<PeliasFeature>): List<BigDecimal>? {
        val points = features.map { it.geometry.coordinates }.filter { it.size >= 2 }
        if (points.isEmpty()) return null
        return listOf(
            points.minOf { it[0] }, // minLon
            points.minOf { it[1] }, // minLat
            points.maxOf { it[0] }, // maxLon
            points.maxOf { it[1] }, // maxLat
        )
    }

    fun transformFeature(feature: PhotonFeature, distance: Double?): PeliasFeature {
        val props = feature.properties
        val extra = props.extra
        val source = transformSource(extra)
        val layer = transformLayer(extra)

        val name = transformName(props)
        val id = normalizeV2Id(extra.id, extra.source)
        var popularName =
            extra
                .alt_name
                ?.split(";")
                ?.firstOrNull()
                ?.ifBlank { null }

        if (popularName == name) {
            popularName = null
        }
        return PeliasFeature(
            type = feature.type,
            geometry =
                PeliasGeometry(
                    type = feature.geometry.type,
                    coordinates = feature.geometry.coordinates.toBigDecimalList(),
                ),
            properties =
                PeliasProperties(
                    id = id,
                    gid = transformGid(source, layer, id),
                    layer = layer,
                    source = source,
                    source_id = id,
                    name = name,
                    popular_name = popularName,
                    street = transformStreet(props),
                    distance = distance?.toBigDecimalWithScale(3),
                    postalcode = props.postcode,
                    housenumber = props.housenumber,
                    accuracy = extra.accuracy,
                    country_a = extra.country_a,
                    county = props.county,
                    county_gid = transformCountyGid(extra.county_gid),
                    locality = extra.locality,
                    locality_gid = transformLocalityGid(extra.locality_gid),
                    borough = extra.borough,
                    borough_gid = transformBoroughGid(extra.borough_gid),
                    label = createLabel(props),
                    category = transformCategory(extra),
                    mode = transformTransportExtra(extra),
                    // v2 backwards-compat: keep surfacing both zone kinds in one field.
                    // extra.tariff_zones comes from NSR, extra.fare_zones from the fare zone
                    // export; merge them for v2.
                    tariff_zones =
                        listOfNotNull(extra.tariff_zones, extra.fare_zones)
                            .flatMap { it.split(",", ";") }
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .takeIf { it.isNotEmpty() },
                    description = transformDescription(extra),
                ),
        )
    }

    private fun transformDescription(extra: Extra): List<Map<String, String>>? {
        val description = extra.description ?: return null

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
                "${props.street} ${props.housenumber}, ${props.extra.locality}"
            }

            props.name.isNullOrBlank() -> {
                props.extra.locality
            }

            !props.extra.locality.isNullOrEmpty() && props.name != props.extra.locality -> {
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
            props.extra.source != Source.KARTVERKET_STEDSNAVN -> "NOT_AN_ADDRESS-" + props.extra.id
            else -> null
        }

    private fun transformName(props: PhotonProperties): String? =
        when {
            props.name != null -> props.name
            props.street != null && props.housenumber != null -> "${props.street} ${props.housenumber}"
            else -> props.street
        }

    fun transformCategory(extra: Extra): List<String> {
        val fromTags =
            extra.tags
                ?.split(",", ";")
                ?.filter { it.startsWith(LEGACY_CATEGORY_PREFIX) }
                ?.map { it.substringAfterLast(".") }
                .orEmpty()
        val fromStopPlaceType =
            extra.stop_place_type
                ?.split(";")
                ?.filter { it.isNotBlank() }
                .orEmpty()
        return (fromTags + fromStopPlaceType)
    }

    fun transformTransportExtra(extra: Extra): List<Pair<String, String?>>? {
        val transportMode = extra.transport_mode?.takeIf { it.isNotBlank() } ?: return null
        val pairs =
            transportMode.split(OSM_TAG_SEPARATOR).mapNotNull { entry ->
                val mode = entry.substringBefore(":")
                if (mode.isNotBlank()) {
                    val submode = entry.substringAfter(":", "").takeIf { it.isNotBlank() }
                    mode to submode
                } else {
                    null
                }
            }
        return pairs.ifEmpty { null }
    }

    fun transformSource(extra: Extra): String? =
        extra.tags
            ?.split(",", ";")
            ?.firstOrNull { it.startsWith(LEGACY_SOURCE_PREFIX) }
            ?.substringAfterLast(".")

    fun transformLayer(extra: Extra): String? =
        extra.tags
            ?.split(",", ";")
            ?.firstOrNull { it.startsWith(LEGACY_LAYER_PREFIX) }
            ?.substringAfterLast(".")

    fun transformBoroughGid(boroughGid: String?): String? =
        boroughGid
            ?.let(InterimIds::boroughNumber)
            ?.let { "whosonfirst:borough:$it" }

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
