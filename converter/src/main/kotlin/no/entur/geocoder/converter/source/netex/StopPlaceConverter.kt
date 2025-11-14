package no.entur.geocoder.converter.source.netex

import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_ADDRESS
import no.entur.geocoder.common.Category.LEGACY_LAYER_VENUE
import no.entur.geocoder.common.Category.LEGACY_SOURCE_OPENSTREETMAP
import no.entur.geocoder.common.Category.LEGACY_SOURCE_WHOSONFIRST
import no.entur.geocoder.common.Category.OSM_GOSP
import no.entur.geocoder.common.Category.OSM_STOP_PLACE
import no.entur.geocoder.common.Category.SOURCE_NSR
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.ImportanceCalculator
import no.entur.geocoder.common.Source
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.Text.altName
import no.entur.geocoder.converter.source.PlaceId
import no.entur.geocoder.converter.target.NominatimPlace
import no.entur.geocoder.converter.target.NominatimPlace.*
import java.io.File
import java.nio.file.Paths

class StopPlaceConverter : Converter {
    override fun convert(
        input: File,
        output: File,
        isAppending: Boolean,
    ) {
        val parser = NetexParser()
        val result: NetexParser.ParseResult = parser.parseXml(input)
        val entries: Sequence<NominatimPlace> = convertNetexParseResult(result)

        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(entries, outputPath, isAppending)
    }

    fun convertStopPlaceToNominatim(
        stopPlace: StopPlace,
        topoPlaces: Map<String, TopographicPlace>,
        categories: Map<String, List<String>>,
        popularity: Long,
    ): List<NominatimPlace> {
        val entries = mutableListOf<NominatimPlace>()
        val lat = stopPlace.centroid.location.latitude
        val lon = stopPlace.centroid.location.longitude

        val localityGid = stopPlace.topographicPlaceRef?.ref
        val locality = topoPlaces[localityGid]?.descriptor?.name?.text
        val countyGid = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.parentTopographicPlaceRef?.ref
        val county = topoPlaces[countyGid]?.descriptor?.name?.text
        val country = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.countryRef?.ref
        val childStopTypes = categories.getOrDefault(stopPlace.id, emptyList())
        val transportModes = childStopTypes.plus(stopPlace.stopPlaceType).filterNotNull()

        val importance = ImportanceCalculator.calculateImportance(popularity)

        val tariffZoneCategories =
            stopPlace.tariffZones
                ?.tariffZoneRef
                ?.mapNotNull {
                    it.ref
                        ?.split(":")
                        ?.first()
                        ?.let { ref -> "tariff_zone_id.$ref" }
                }?.toSet()
                ?: emptySet()

        val isParentStopPlace = childStopTypes.isNotEmpty()
        val multimodalityCategory =
            when {
                isParentStopPlace -> "multimodal.parent"
                stopPlace.parentSiteRef?.ref != null -> "multimodal.child"
                else -> null
            }

        val tags: List<String> =
            listOf(OSM_STOP_PLACE, LEGACY_LAYER_VENUE)
                .plus(transportModes.map { LEGACY_CATEGORY_PREFIX + it })
                .plus(if (isParentStopPlace) LEGACY_SOURCE_OPENSTREETMAP else LEGACY_SOURCE_WHOSONFIRST)

        val categories: List<String> =
            tags
                .plus(SOURCE_NSR)
                .plus(tariffZoneCategories)
                .plus(country?.let { "country.$it" })
                .plus(countyGid?.let { "county_gid.$it" })
                .plus(localityGid?.let { "locality_gid.$it" })
                .plus(multimodalityCategory)
                .filterNotNull()

        // Extract alternative names from NeTEx AlternativeNames
        val alternativeNames = stopPlace.alternativeNames?.alternativeName
        val altName = alternativeNames?.mapNotNull { it.name?.text }?.joinToString(";")?.ifBlank { null }
        val id = stopPlace.id

        val extra =
            Extra(
                id = id,
                source = Source.NSR,
                accuracy = "point",
                country_a = Country.getThreeLetterCode(country),
                county_gid = countyGid,
                locality = locality,
                locality_gid = localityGid,
                transport_modes = transportModes.joinToString(","),
                tariff_zones = (
                    stopPlace.tariffZones
                        ?.tariffZoneRef
                        ?.mapNotNull { it.ref }
                        ?.joinToString(",")
                ),
                alt_name = altName,
                description = stopPlace.description?.text,
                tags = tags.joinToString(","),
            )

        val placeId = PlaceId.stopplace.create(stopPlace.id)
        val stopPlaceContent =
            PlaceContent(
                place_id = placeId,
                object_type = "N",
                object_id = placeId,
                categories = categories,
                rank_address = 30,
                importance = importance,
                parent_place_id = 0,
                name =
                    stopPlace.name.text?.let {
                        Name(
                            name = it,
                            alt_name = altName(altName, id),
                        )
                    },
                address =
                    Address(
                        county = county,
                    ),
                postcode = null,
                country_code = (country ?: "no"),
                centroid = listOf(lon, lat),
                bbox = listOf(lat, lon, lat, lon),
                extra = extra,
            )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    fun convertGroupOfStopPlacesToNominatim(
        groupOfStopPlaces: GroupOfStopPlaces,
        topoPlaces: Map<String, TopographicPlace>,
        stopPlacePopularities: Map<String, Long>,
        stopPlaces: List<StopPlace>,
    ): NominatimPlace {
        val lat = groupOfStopPlaces.centroid.location.latitude
        val lon = groupOfStopPlaces.centroid.location.longitude

        val groupName = groupOfStopPlaces.name.text

        var locality: String? = groupName
        var localityGid: String? = null
        var county: String? = null
        var countyGid: String? = null
        var country: String? = null

        groupOfStopPlaces.members?.stopPlaceRef?.any { stopPlaceRef ->
            val stopPlace = stopPlaces.find { it.id == stopPlaceRef.ref }
            val topoPlaceRef = stopPlace?.topographicPlaceRef?.ref
            val topoPlace = topoPlaces[topoPlaceRef]
            if (topoPlace?.topographicPlaceType == "municipality") {
                localityGid = topoPlaceRef
                locality = topoPlace.descriptor?.name?.text
                countyGid = topoPlace.parentTopographicPlaceRef?.ref
                county = topoPlaces[countyGid]?.descriptor?.name?.text
                country = topoPlace.countryRef?.ref
                return@any true
            }
            return@any false
        }

        // Calculate importance based on member stop place popularities
        val memberPopularities =
            groupOfStopPlaces.members
                ?.stopPlaceRef
                ?.mapNotNull { it.ref }
                ?.mapNotNull { stopPlacePopularities[it] }
                ?: emptyList()

        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)
        val importance = ImportanceCalculator.calculateImportance(popularity.toLong())

        val tags =
            listOf(OSM_GOSP, LEGACY_LAYER_ADDRESS, LEGACY_SOURCE_WHOSONFIRST)
                .plus(LEGACY_CATEGORY_PREFIX + "GroupOfStopPlaces")

        val categories =
            tags
                .plus(SOURCE_NSR)
                .plus(country?.let { "country.$it" })
                .plus(countyGid?.let { "county_gid.$it" })
                .plus(localityGid?.let { "locality_gid.$it" })
                .filterNotNull()

        val id = groupOfStopPlaces.id
        val placeId = PlaceId.gosp.create(id)
        val placeContent =
            PlaceContent(
                place_id = placeId,
                object_type = "N",
                object_id = placeId,
                categories = categories,
                rank_address = 30,
                importance = importance,
                parent_place_id = 0,
                name = groupName?.let { Name(name = it, alt_name = id) },
                address =
                    Address(
                        county = county,
                    ),
                postcode = null,
                country_code = (country ?: "no"),
                centroid = listOf(lon, lat),
                bbox = listOf(lat, lon, lat, lon),
                extra =
                    Extra(
                        id = id,
                        source = Source.NSR,
                        accuracy = "point",
                        country_a = Country.getThreeLetterCode(country),
                        county_gid = countyGid,
                        locality = locality,
                        locality_gid = localityGid,
                        tags = tags.joinToString(","),
                    ),
            )

        return NominatimPlace("Place", listOf(placeContent))
    }

    fun convertNetexParseResult(result: NetexParser.ParseResult): Sequence<NominatimPlace> {
        val stopPlacesList = result.stopPlaces.toList()

        val stopPlacePopularities =
            stopPlacesList.associate { stopPlace ->
                val childStopTypes = result.categories.getOrDefault(stopPlace.id, emptyList())
                val popularity = StopPlacePopularityCalculator.calculatePopularity(stopPlace, childStopTypes)
                stopPlace.id to popularity
            }

        val stopPlaceEntries =
            stopPlacesList.asSequence().flatMap { stopPlace ->
                val popularity = stopPlacePopularities[stopPlace.id] ?: 0L
                convertStopPlaceToNominatim(
                    stopPlace,
                    result.topoPlaces,
                    result.categories,
                    popularity,
                ).asSequence()
            }

        val groupOfStopPlacesEntries =
            result.groupOfStopPlaces.map {
                convertGroupOfStopPlacesToNominatim(
                    it,
                    result.topoPlaces,
                    stopPlacePopularities,
                    stopPlacesList,
                )
            }

        return stopPlaceEntries + groupOfStopPlacesEntries
    }
}
