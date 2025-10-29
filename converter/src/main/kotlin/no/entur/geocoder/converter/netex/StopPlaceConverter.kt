package no.entur.geocoder.converter.netex

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Source
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.PlaceId
import no.entur.geocoder.converter.importance.ImportanceCalculator
import no.entur.geocoder.converter.photon.NominatimPlace
import no.entur.geocoder.converter.photon.NominatimPlace.*
import java.io.File
import java.nio.file.Paths
import kotlin.math.abs

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

        val multimodalityCategory =
            when {
                childStopTypes.isNotEmpty() -> "multimodal.parent"
                stopPlace.parentSiteRef?.ref != null -> "multimodal.child"
                else -> null
            }

        val categories =
            listOf(Category.OSM_STOP_PLACE)
                .plus(tariffZoneCategories)
                .plus(country?.let { "country.$it" })
                .plus(countyGid?.let { "county_gid.$it" })
                .plus(localityGid?.let { "locality_gid.$it" })
                .plus(multimodalityCategory)
                .plus("layer.stopplace")
                .plus(Category.SOURCE_NSR)
                .filterNotNull()

        // Extract alternative names from NeTEx AlternativeNames
        val alternativeNames = stopPlace.alternativeNames?.alternativeName
        val altName = alternativeNames?.mapNotNull { it.name?.text }?.joinToString(";")?.ifBlank { null }

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
                name = stopPlace.name.text?.let { Name(name = it, alt_name = altName) },
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
                        id = stopPlace.id,
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
                    ),
            )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    fun convertGroupOfStopPlacesToNominatim(
        groupOfStopPlaces: GroupOfStopPlaces,
        topoPlaces: Map<String, TopographicPlace>,
        stopPlacePopularities: Map<String, Long>,
    ): NominatimPlace {
        val lat = groupOfStopPlaces.centroid.location.latitude
        val lon = groupOfStopPlaces.centroid.location.longitude

        val groupName = groupOfStopPlaces.name.text

        var locality: String? = groupName
        var localityGid: String? = null
        var county: String? = null
        var countyGid: String? = null
        var country: String? = null

        for ((gid, topoPlace) in topoPlaces) {
            if (topoPlace.descriptor?.name?.text == groupName) {
                if (topoPlace.topographicPlaceType == "municipality") {
                    localityGid = gid
                    locality = topoPlace.descriptor?.name?.text
                    countyGid = topoPlace.parentTopographicPlaceRef?.ref
                    county = topoPlaces[countyGid]?.descriptor?.name?.text
                    country = topoPlace.countryRef?.ref
                    break
                }
            }
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

        val categories =
            listOf(Category.OSM_GOSP)
                .plus("GroupOfStopPlaces")
                .plus(country?.let { "country.$it" })
                .plus(countyGid?.let { "county_gid.$it" })
                .plus(localityGid?.let { "locality_gid.$it" })
                .plus("layer.groupofstopplaces")
                .plus(Category.SOURCE_NSR)
                .filterNotNull()

        val placeContent =
            PlaceContent(
                place_id = abs(groupOfStopPlaces.id.hashCode().toLong()),
                object_type = "N",
                object_id = abs(groupOfStopPlaces.id.hashCode().toLong()),
                categories = categories,
                rank_address = 30,
                importance = importance,
                parent_place_id = 0,
                name = groupName?.let { Name(name = it) },
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
                        id = groupOfStopPlaces.id,
                        source = Source.NSR,
                        accuracy = "point",
                        country_a = Country.getThreeLetterCode(country),
                        county_gid = countyGid,
                        locality = locality,
                        locality_gid = localityGid,
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
                )
            }

        return stopPlaceEntries + groupOfStopPlacesEntries
    }
}
