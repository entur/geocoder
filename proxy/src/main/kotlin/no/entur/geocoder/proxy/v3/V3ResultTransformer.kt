package no.entur.geocoder.proxy.v3

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.entur.geocoder.common.Extra
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonFeature
import no.entur.geocoder.proxy.v3.V3Result.*
import java.math.BigDecimal
import java.math.RoundingMode

class V3ResultTransformer {
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun parseAndTransform(
        photonResult: PhotonResult,
        params: V3AutocompleteParams
    ): String {
        val places = photonResult.features.map { transformFeature(it) }

        val boundingBox = calculateBoundingBox(places)

        val filters = if (params.placeTypes.isNotEmpty() || params.sources.isNotEmpty() || params.countries.isNotEmpty() ||
            params.countyIds.isNotEmpty() || params.localityIds.isNotEmpty() || params.tariffZones.isNotEmpty() ||
            params.tariffZoneAuthorities.isNotEmpty() || params.transportModes.isNotEmpty()) {
            V3Result.Filters(
                placeTypes = params.placeTypes.mapNotNull { mapToPlaceType(it) }.takeIf { it.isNotEmpty() },
                sources = params.sources.takeIf { it.isNotEmpty() },
                countries = params.countries.takeIf { it.isNotEmpty() },
                countyIds = params.countyIds.takeIf { it.isNotEmpty() },
                localityIds = params.localityIds.takeIf { it.isNotEmpty() },
                tariffZones = params.tariffZones.takeIf { it.isNotEmpty() },
                tariffZoneAuthorities = params.tariffZoneAuthorities.takeIf { it.isNotEmpty() },
                transportModes = params.transportModes.takeIf { it.isNotEmpty() }
            )
        } else null

        val result = V3Result(
            results = places,
            metadata = Metadata(
                query = QueryInfo(
                    text = params.query,
                    latitude = null,
                    longitude = null,
                    limit = params.limit,
                    language = params.language,
                    filters = filters
                ),
                resultCount = places.size,
                timestamp = System.currentTimeMillis(),
                boundingBox = boundingBox
            )
        )

        return mapper.writeValueAsString(result)
    }

    fun parseAndTransform(
        photonResult: PhotonResult,
        params: V3ReverseParams
    ): String {
        val places = photonResult.features.map { transformFeature(it) }

        val boundingBox = calculateBoundingBox(places)

        val result = V3Result(
            results = places,
            metadata = Metadata(
                query = QueryInfo(
                    text = null,
                    latitude = params.latitude.toBigDecimalOrNull(),
                    longitude = params.longitude.toBigDecimalOrNull(),
                    limit = params.limit,
                    language = params.language,
                    filters = null
                ),
                resultCount = places.size,
                timestamp = System.currentTimeMillis(),
                boundingBox = boundingBox
            )
        )

        return mapper.writeValueAsString(result)
    }

    private fun transformFeature(feature: PhotonFeature): V3Result.Place {
        val props = feature.properties
        val extra = props.extra
        val coords = feature.geometry.coordinates

        val placeType = determinePlaceType(extra?.source, props.osm_key, props.osm_value)
        val accuracy = parseAccuracy(extra?.accuracy)

        return V3Result.Place(
            id = extra?.id
                ?: (if (props.osm_type != null && props.osm_id != null) "${props.osm_type}:${props.osm_id}" else "unknown"),
            name = props.name ?: props.street ?: props.locality ?: "Unnamed",
            displayName = extra?.label ?: props.label ?: buildDisplayName(props),
            placeType = placeType,
            location = V3Result.Location(
                latitude = coords.getOrElse(1) { BigDecimal.ZERO }.setScale(6, RoundingMode.HALF_UP),
                longitude = coords.getOrElse(0) { BigDecimal.ZERO }.setScale(6, RoundingMode.HALF_UP)
            ),
            address = buildAddress(props, extra),
            categories = extra?.tags?.split(",")?.map { it.substringAfter('.') }?.filter { it.isNotBlank() },
            transportModes = extra?.transport_modes?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() },
            tariffZones = extra?.tariff_zones?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() },
            source = V3Result.DataSource(
                provider = mapProviderName(extra?.source),
                sourceId = buildSourceId(extra?.source, extra?.id, props.osm_type, props.osm_id),
                accuracy = accuracy
            )
        )
    }

    private fun buildAddress(props: PhotonResult.PhotonProperties, extra: Extra?): V3Result.Address? {
        if (props.street == null && props.housenumber == null && props.postcode == null &&
            props.locality == null && props.county == null) {
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
            country = null, // Not provided in Photon response
            countryCode = extra?.country_a
        )
    }

    private fun buildDisplayName(props: PhotonResult.PhotonProperties): String {
        val parts = mutableListOf<String>()

        if (props.name != null) parts.add(props.name)
        if (props.street != null) {
            val streetPart = if (props.housenumber != null) {
                "${props.street} ${props.housenumber}"
            } else {
                props.street
            }
            parts.add(streetPart)
        }
        if (props.postcode != null && props.city != null) {
            parts.add("${props.postcode} ${props.city}")
        } else if (props.city != null) {
            parts.add(props.city)
        }

        return parts.joinToString(", ").ifBlank { "Unknown location" }
    }

    private fun buildSourceId(source: String?, id: String?, osmType: String?, osmId: Long?): String? {
        return when {
            source == "openstreetmap" && id != null -> id
            source == "nsr" && id != null -> "NSR:$id"
            source == "kartverket-matrikkelenadresse" && id != null -> "Kartverket:$id"
            osmType != null && osmId != null -> "OSM:${osmType}:${osmId}"
            id != null -> id
            else -> null
        }
    }

    private fun determinePlaceType(source: String?, osmKey: String?, osmValue: String?): V3Result.PlaceType {
        return when {
            source == "kartverket-matrikkelenadresse" -> V3Result.PlaceType.ADDRESS
            source == "nsr" && osmValue?.contains("stop") == true -> V3Result.PlaceType.STOP_PLACE
            source == "nsr" && osmValue?.contains("station") == true -> V3Result.PlaceType.STATION
            source == "nsr" -> V3Result.PlaceType.VENUE
            osmKey == "highway" -> V3Result.PlaceType.STREET
            osmKey == "place" && osmValue == "city" -> V3Result.PlaceType.LOCALITY
            osmKey == "place" && osmValue == "town" -> V3Result.PlaceType.LOCALITY
            osmKey == "place" && osmValue == "village" -> V3Result.PlaceType.LOCALITY
            osmKey == "place" && osmValue == "suburb" -> V3Result.PlaceType.BOROUGH
            osmKey == "boundary" && osmValue == "administrative" -> V3Result.PlaceType.COUNTY
            osmKey == "amenity" -> V3Result.PlaceType.POI
            osmKey == "shop" -> V3Result.PlaceType.POI
            osmKey == "tourism" -> V3Result.PlaceType.POI
            else -> V3Result.PlaceType.UNKNOWN
        }
    }

    private fun parseAccuracy(accuracy: String?): V3Result.Accuracy? {
        return when (accuracy?.lowercase()) {
            "point" -> V3Result.Accuracy.EXACT
            "centroid" -> V3Result.Accuracy.APPROXIMATE
            "interpolated" -> V3Result.Accuracy.INTERPOLATED
            else -> null
        }
    }

    private fun mapProviderName(source: String?): String {
        return when (source?.lowercase()) {
            "openstreetmap" -> "OpenStreetMap"
            "nsr" -> "National Stop Register"
            "kartverket-matrikkelenadresse" -> "Kartverket"
            else -> source ?: "Unknown"
        }
    }

    private fun mapToPlaceType(type: String): V3Result.PlaceType? {
        return try {
            V3Result.PlaceType.valueOf(type.uppercase())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun calculateBoundingBox(places: List<V3Result.Place>): V3Result.BoundingBox? {
        if (places.isEmpty()) return null

        var minLon = BigDecimal(Double.MAX_VALUE)
        var minLat = BigDecimal(Double.MAX_VALUE)
        var maxLon = BigDecimal(Double.MIN_VALUE)
        var maxLat = BigDecimal(Double.MIN_VALUE)

        places.forEach { place ->
            val lon = place.location.longitude
            val lat = place.location.latitude

            minLon = minOf(minLon, lon)
            minLat = minOf(minLat, lat)
            maxLon = maxOf(maxLon, lon)
            maxLat = maxOf(maxLat, lat)
        }

        return if (minLon != BigDecimal(Double.MAX_VALUE)) {
            V3Result.BoundingBox(
                southwest = V3Result.Location(latitude = minLat, longitude = minLon),
                northeast = V3Result.Location(latitude = maxLat, longitude = maxLon)
            )
        } else {
            null
        }
    }
}

