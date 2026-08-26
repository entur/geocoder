package no.entur.geocoder.proxy.v3

import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.common.Category.containsTag
import no.entur.geocoder.proxy.common.Coordinate
import no.entur.geocoder.proxy.common.Country
import no.entur.geocoder.proxy.common.Extra
import no.entur.geocoder.proxy.common.Geo
import no.entur.geocoder.proxy.common.InterimIds
import no.entur.geocoder.proxy.common.Source
import no.entur.geocoder.proxy.common.Util.toBigDecimalWithScale
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonFeature
import no.entur.geocoder.proxy.photon.PhotonResultFilter
import no.entur.geocoder.proxy.v3.V3Result.Metadata
import no.entur.geocoder.proxy.v3.V3Result.QueryInfo
import java.math.BigDecimal
import java.time.Instant

object V3ResultTransformer {
    fun parseAndTransform(
        result: PhotonResult,
        req: V3AutocompleteRequest,
    ): V3Result {
        val features =
            PhotonResultFilter
                .dropPlacesCoveredByGosp(result.features)
                .map { transformFeature(it) }
                .take(req.limit)

        return V3Result(
            features = features,
            bbox = calculateBbox(features),
            metadata =
                Metadata(
                    query =
                        QueryInfo(
                            text = req.q,
                            limit = req.limit,
                            lang = req.lang,
                            filters = buildFiltersEcho(req),
                        ),
                    resultCount = features.size,
                    timestamp = Instant.now().toString(),
                    debug = debugInfo(result, req.debug),
                ),
        )
    }

    fun parseAndTransform(
        result: PhotonResult,
        req: V3ReverseRequest,
    ): V3Result {
        val origin = Coordinate(req.lat, req.lon)
        val features = result.features.map { transformFeature(it, origin) }

        return V3Result(
            features = features,
            bbox = calculateBbox(features),
            metadata =
                Metadata(
                    query =
                        QueryInfo(
                            lat = req.lat,
                            lon = req.lon,
                            limit = req.limit,
                            lang = req.lang,
                            filters = buildFiltersEcho(req),
                        ),
                    resultCount = features.size,
                    timestamp = Instant.now().toString(),
                    debug = debugInfo(result, req.debug),
                ),
        )
    }

    /**
     * Echo the request filters back in the response metadata. Returns null when no filters were
     * applied (so the field is omitted from the JSON via `@JsonInclude(NON_NULL)`).
     */
    private fun buildFiltersEcho(params: V3FilterParams): V3Result.Filters? {
        if (params.layers.isEmpty() &&
            params.sources.isEmpty() &&
            params.countries.isEmpty() &&
            params.counties.isEmpty() &&
            params.localities.isEmpty() &&
            params.fareZones.isEmpty() &&
            params.fareZoneAuthorities.isEmpty() &&
            params.stopPlaceTypes.isEmpty() &&
            params.multimodal == "parent"
        ) {
            return null
        }
        return V3Result.Filters(
            layers = params.layers.mapNotNull { mapToLayer(it) }.takeIf { it.isNotEmpty() },
            sources = params.sources.takeIf { it.isNotEmpty() },
            countries = params.countries.takeIf { it.isNotEmpty() },
            counties = params.counties.takeIf { it.isNotEmpty() },
            localities = params.localities.takeIf { it.isNotEmpty() },
            fareZones = params.fareZones.takeIf { it.isNotEmpty() },
            fareZoneAuthorities = params.fareZoneAuthorities.takeIf { it.isNotEmpty() },
            stopPlaceTypes = params.stopPlaceTypes.takeIf { it.isNotEmpty() },
            multimodal = params.multimodal.takeIf { it != "parent" },
        )
    }

    fun parseAndTransform(
        result: PhotonResult,
        req: V3PlaceRequest,
    ): V3Result {
        val features = result.features.map { transformFeature(it) }
        return V3Result(
            features = features,
            metadata =
                Metadata(
                    query =
                        QueryInfo(
                            limit = req.ids.size,
                            lang = req.lang,
                        ),
                    resultCount = features.size,
                    timestamp = Instant.now().toString(),
                    debug = debugInfo(result, req.debug),
                ),
        )
    }

    private fun debugInfo(result: PhotonResult, debug: Boolean): Map<String, Any>? =
        if (debug && result.properties.isNotEmpty()) result.properties else null

    private fun transformFeature(feature: PhotonFeature, origin: Coordinate? = null): V3Result.Feature {
        val props = feature.properties
        val extra = props.extra
        val coords = feature.geometry.coordinates

        val layer = determineLayer(extra.source, props.osm_key, extra.tags)
        val distance = origin?.let { calculateDistanceKm(coords, it) }

        val defaultName =
            props.name
                ?: if (props.street != null && props.housenumber != null) {
                    "${props.street} ${props.housenumber}"
                } else {
                    props.street
                        ?: extra.locality
                        ?: "Unnamed"
                }
        val labelName =
            extra.alt_name
                ?.split(";")
                ?.firstOrNull()
                ?.ifBlank { null }
                ?.takeIf { it != defaultName }
        val displayName = defaultName + extra.locality?.let { ", $it" }.orEmpty()

        return V3Result.Feature(
            bbox = featureBbox(props.extent),
            geometry =
                V3Result.Geometry(
                    type = feature.geometry.type,
                    coordinates =
                        listOf(
                            coords.getOrNull(0)?.toBigDecimalWithScale() ?: BigDecimal.ZERO, // lon
                            coords.getOrNull(1)?.toBigDecimalWithScale() ?: BigDecimal.ZERO, // lat
                        ),
                ),
            properties =
                V3Result.Place(
                    id = InterimIds.canonicaliseOsmId(extra.id),
                    names =
                        V3Result.Names(
                            default = defaultName,
                            label = labelName,
                            display = displayName,
                        ),
                    layer = layer,
                    address = buildAddress(props, extra),
                    categories =
                        extra.tags
                            ?.split(",", ";")
                            ?.filter { it.startsWith("legacy.category.") }
                            ?.map { it.substringAfterLast('.') }
                            ?.filter { it.isNotBlank() }
                            // OSM entities carry the same value on several tags, so the
                            // index holds repeats.
                            ?.distinct(),
                    fareZones =
                        extra.fare_zones
                            ?.split(",", ";")
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() }
                            ?.takeIf { it.isNotEmpty() },
                    transportModes = parseTransportModes(extra.transport_mode),
                    stopPlaceTypes =
                        extra.stop_place_type
                            ?.split(";")
                            ?.filter { it.isNotBlank() }
                            // A parent stop repeats a type once per child stop of that type.
                            ?.distinct()
                            ?.takeIf { it.isNotEmpty() },
                    stopPlaceRole = parseStopPlaceRole(extra.stop_place_role),
                    source = mapProviderName(extra.source),
                    distance = distance,
                    description = parseDescription(extra.description),
                ),
        )
    }

    private fun buildAddress(props: PhotonResult.PhotonProperties, extra: Extra): V3Result.Address? {
        if (props.street == null &&
            props.housenumber == null &&
            props.postcode == null &&
            extra.locality == null &&
            props.county == null
        ) {
            return null
        }

        return V3Result.Address(
            streetName = props.street,
            houseNumber = props.housenumber,
            postalCode = props.postcode,
            locality = extra.locality ?: props.city,
            localityId = extra.locality_gid,
            borough = extra.borough,
            boroughId = extra.borough_gid?.let(InterimIds::canonicaliseBoroughGid),
            county = props.county,
            countyId = extra.county_gid,
            countryCode = iso3ToIso2(extra.country_a),
        )
    }

    private fun determineLayer(source: String?, osmKey: String?, tags: String?): V3Result.Layer =
        when (source) {
            Source.KARTVERKET_ADRESSE -> {
                V3Result.Layer.address
            }

            Source.KARTVERKET_STEDSNAVN -> {
                V3Result.Layer.place
            }

            Source.NSR -> {
                if (tags.containsTag(Category.LAYER_GOSP)) {
                    V3Result.Layer.groupOfStopPlaces
                } else {
                    V3Result.Layer.stopPlace
                }
            }

            else -> {
                if (osmKey == "highway") V3Result.Layer.street else V3Result.Layer.poi
            }
        }

    /** Map `extra.stop_place_role` to the response enum; absent or unknown values give null. */
    private fun parseStopPlaceRole(value: String?): V3Result.StopPlaceRole? =
        V3Result.StopPlaceRole.entries.firstOrNull { it.name == value }

    private fun parseTransportModes(transportMode: String?): List<V3Result.TransportMode>? =
        transportMode
            ?.split(";")
            ?.filter { it.isNotBlank() }
            ?.map { entry ->
                val parts = entry.trim().split(":")
                V3Result.TransportMode(mode = parts[0], subMode = parts.getOrNull(1))
            }?.takeIf { it.isNotEmpty() }

    private fun mapProviderName(source: String?): String = source ?: "unknown"

    private fun iso3ToIso2(iso3: String?): String? = Country.fromThreeLetterCode(iso3)?.name

    private fun mapToLayer(type: String): V3Result.Layer? =
        V3Result.Layer.entries.firstOrNull { it.name.equals(type, ignoreCase = true) }

    private val langPrefix = Regex("^[a-z]{3}:")

    private fun parseDescription(raw: String?): Map<String, String>? {
        if (raw.isNullOrBlank()) return null
        val segments = raw.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        // Treat as lang-prefixed only if every segment looks like `xyz:...`. A single
        // free-text description containing a stray `tel:`/`www:`/`e.g.:` would otherwise
        // be mis-parsed.
        return if (segments.all { langPrefix.containsMatchIn(it) }) {
            segments
                .mapNotNull { seg ->
                    val colon = seg.indexOf(':')
                    val lang = seg.take(colon).trim()
                    val text = seg.substring(colon + 1).trim()
                    if (lang.isNotEmpty() && text.isNotEmpty()) lang to text else null
                }.toMap()
                .takeIf { it.isNotEmpty() }
        } else {
            mapOf("nor" to raw.trim())
        }
    }

    /** Photon's extent is the NW + SE corners; GeoJSON bbox is [minLon, minLat, maxLon, maxLat]. */
    private fun featureBbox(extent: List<Double>?): List<BigDecimal>? =
        extent
            ?.takeIf { it.size == 4 }
            ?.let { (minLon, maxLat, maxLon, minLat) ->
                listOf(minLon, minLat, maxLon, maxLat).map { v -> v.toBigDecimalWithScale() }
            }

    private fun calculateDistanceKm(coords: List<Double>, origin: Coordinate): BigDecimal? {
        if (coords.size < 2) return null
        val featureCoord = Coordinate(coords[1], coords[0])
        return (Geo.haversineDistance(featureCoord, origin) / 1000.0).toBigDecimalWithScale(3)
    }

    private fun calculateBbox(features: List<V3Result.Feature>): List<BigDecimal>? {
        val points = features.map { it.geometry.coordinates }.filter { it.size >= 2 }
        if (points.isEmpty()) return null
        return listOf(
            points.minOf { it[0] }, // minLon
            points.minOf { it[1] }, // minLat
            points.maxOf { it[0] }, // maxLon
            points.maxOf { it[1] }, // maxLat
        )
    }
}
