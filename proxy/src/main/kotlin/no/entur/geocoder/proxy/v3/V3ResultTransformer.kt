package no.entur.geocoder.proxy.v3

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Country
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Source
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonFeature
import no.entur.geocoder.proxy.v3.V3Result.Metadata
import no.entur.geocoder.proxy.v3.V3Result.QueryInfo
import java.math.BigDecimal

object V3ResultTransformer {
    fun parseAndTransform(
        result: PhotonResult,
        req: V3AutocompleteRequest,
    ): V3Result {
        val features = result.features.map { transformFeature(it) }

        val filters =
            if (req.layers.isNotEmpty() ||
                req.sources.isNotEmpty() ||
                req.countries.isNotEmpty() ||
                req.countyIds.isNotEmpty() ||
                req.localityIds.isNotEmpty() ||
                req.tariffZones.isNotEmpty() ||
                req.tariffZoneAuthorities.isNotEmpty() ||
                req.multiModal != "parent"
            ) {
                V3Result.Filters(
                    layers = req.layers.mapNotNull { mapToLayer(it) }.takeIf { it.isNotEmpty() },
                    sources = req.sources.takeIf { it.isNotEmpty() },
                    countries = req.countries.takeIf { it.isNotEmpty() },
                    countyIds = req.countyIds.takeIf { it.isNotEmpty() },
                    localityIds = req.localityIds.takeIf { it.isNotEmpty() },
                    tariffZones = req.tariffZones.takeIf { it.isNotEmpty() },
                    tariffZoneAuthorities = req.tariffZoneAuthorities.takeIf { it.isNotEmpty() },
                    multiModal = req.multiModal.takeIf { it != "parent" },
                )
            } else {
                null
            }

        return V3Result(
            features = features,
            bbox = calculateBbox(features),
            metadata =
                Metadata(
                    query =
                        QueryInfo(
                            text = req.query,
                            limit = req.limit,
                            language = req.language,
                            filters = filters,
                        ),
                    resultCount = features.size,
                    timestamp = System.currentTimeMillis(),
                ),
        )
    }

    fun parseAndTransform(
        result: PhotonResult,
        req: V3ReverseRequest,
    ): V3Result {
        val features = result.features.map { transformFeature(it) }

        return V3Result(
            features = features,
            bbox = calculateBbox(features),
            metadata =
                Metadata(
                    query =
                        QueryInfo(
                            latitude = req.lat,
                            longitude = req.lon,
                            limit = req.limit,
                            language = req.language,
                        ),
                    resultCount = features.size,
                    timestamp = System.currentTimeMillis(),
                ),
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
                            language = "no",
                        ),
                    resultCount = features.size,
                    timestamp = System.currentTimeMillis(),
                ),
        )
    }

    private fun transformFeature(feature: PhotonFeature): V3Result.Feature {
        val props = feature.properties
        val extra = props.extra
        val coords = feature.geometry.coordinates

        val layer = determineLayer(extra?.source, props.osm_key, props.osm_value, extra?.tags)
        val accuracy = parseAccuracy(extra?.accuracy)

        val defaultName = props.name ?: props.street ?: props.extra?.locality ?: "Unnamed"
        val labelName =
            extra
                ?.alt_name
                ?.split(";")
                ?.firstOrNull()
                ?.ifBlank { null }
                ?.takeIf { it != defaultName }
        val displayName = defaultName + extra?.locality?.let { ", $it" }.orEmpty()

        return V3Result.Feature(
            geometry =
                V3Result.Geometry(
                    coordinates =
                        listOf(
                            coords.getOrNull(0)?.toBigDecimalWithScale() ?: BigDecimal.ZERO, // lon
                            coords.getOrNull(1)?.toBigDecimalWithScale() ?: BigDecimal.ZERO, // lat
                        ),
                ),
            properties =
                V3Result.Place(
                    id =
                        extra?.id
                            ?: (if (props.osm_type != null && props.osm_id != null) "${props.osm_type}:${props.osm_id}" else "unknown"),
                    name =
                        V3Result.Names(
                            default = defaultName,
                            label = labelName,
                            display = displayName,
                        ),
                    layer = layer,
                    address = buildAddress(props, extra),
                    categories =
                        extra
                            ?.tags
                            ?.split(",", ";")
                            ?.filter { it.startsWith("legacy.category.") }
                            ?.map { it.substringAfterLast('.') }
                            ?.filter { it.isNotBlank() },
                    tariffZones =
                        extra
                            ?.tariff_zones
                            ?.split(",", ";")
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() },
                    transportModes = parseTransportModes(extra?.transport_mode),
                    stopPlaceTypes =
                        extra
                            ?.stop_place_type
                            ?.split(";")
                            ?.filter { it.isNotBlank() }
                            ?.takeIf { it.isNotEmpty() },
                    source =
                        V3Result.DataSource(
                            provider = mapProviderName(extra?.source),
                            sourceId = buildSourceId(extra?.source, extra?.id, props.osm_type, props.osm_id),
                            accuracy = accuracy,
                        ),
                ),
        )
    }

    private fun buildAddress(props: PhotonResult.PhotonProperties, extra: Extra?): V3Result.Address? {
        if (props.street == null &&
            props.housenumber == null &&
            props.postcode == null &&
            props.extra?.locality == null &&
            props.county == null
        ) {
            return null
        }

        return V3Result.Address(
            streetName = props.street,
            houseNumber = props.housenumber,
            postalCode = props.postcode,
            locality = extra?.locality ?: props.city,
            localityId = extra?.locality_gid,
            borough = extra?.borough,
            boroughId = extra?.borough_gid,
            county = props.county,
            countyId = extra?.county_gid,
            countryCode = iso3ToIso2(extra?.country_a),
        )
    }

    private fun buildSourceId(source: String?, id: String?, osmType: String?, osmId: Long?): String? =
        when {
            source == Source.OSM && id != null -> id
            source == Source.NSR && id != null -> "NSR:$id"
            source == Source.KARTVERKET_ADRESSE && id != null -> "Kartverket:$id"
            osmType != null && osmId != null -> "OSM:$osmType:$osmId"
            id != null -> id
            else -> null
        }

    private fun determineLayer(source: String?, osmKey: String?, osmValue: String?, tags: String?): V3Result.Layer =
        when {
            source == Source.KARTVERKET_ADRESSE -> V3Result.Layer.address
            source == Source.NSR && tags?.contains(Category.OSM_GOSP) == true -> V3Result.Layer.group_of_stop_places
            source == Source.NSR && osmValue?.contains("stop") == true -> V3Result.Layer.stop_place
            source == Source.NSR && osmValue?.contains("station") == true -> V3Result.Layer.stop_place
            source == Source.NSR -> V3Result.Layer.poi
            osmKey == "highway" -> V3Result.Layer.street
            else -> V3Result.Layer.poi
        }

    private fun parseTransportModes(transportMode: String?): List<V3Result.TransportMode>? =
        transportMode
            ?.split(";")
            ?.filter { it.isNotBlank() }
            ?.map { entry ->
                val parts = entry.trim().split(":")
                V3Result.TransportMode(mode = parts[0], subMode = parts.getOrNull(1))
            }?.takeIf { it.isNotEmpty() }

    private fun parseAccuracy(accuracy: String?): String? = accuracy?.lowercase()?.takeIf { it.isNotBlank() }

    private fun mapProviderName(source: String?): String = source ?: "unknown"

    private fun iso3ToIso2(iso3: String?): String? = Country.fromThreeLetterCode(iso3)?.name

    private fun mapToLayer(type: String): V3Result.Layer? =
        try {
            V3Result.Layer.valueOf(type.lowercase())
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun calculateBbox(features: List<V3Result.Feature>): List<BigDecimal>? {
        if (features.isEmpty()) return null

        var minLon = BigDecimal(Double.MAX_VALUE)
        var minLat = BigDecimal(Double.MAX_VALUE)
        var maxLon = BigDecimal(Double.MIN_VALUE)
        var maxLat = BigDecimal(Double.MIN_VALUE)

        features.forEach { feature ->
            val coords = feature.geometry.coordinates
            val lon = coords.getOrNull(0) ?: return@forEach
            val lat = coords.getOrNull(1) ?: return@forEach

            minLon = minOf(minLon, lon)
            minLat = minOf(minLat, lat)
            maxLon = maxOf(maxLon, lon)
            maxLat = maxOf(maxLat, lat)
        }

        return if (minLon != BigDecimal(Double.MAX_VALUE)) {
            listOf(minLon, minLat, maxLon, maxLat)
        } else {
            null
        }
    }
}
