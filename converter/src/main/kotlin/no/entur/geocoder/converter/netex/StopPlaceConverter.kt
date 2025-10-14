package no.entur.geocoder.converter.netex

import no.entur.geocoder.common.Extra
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.NominatimPlace
import no.entur.geocoder.converter.NominatimPlace.*
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
    ): List<NominatimPlace> {
        val entries = mutableListOf<NominatimPlace>()
        val lat = stopPlace.centroid.location.latitude
        val lon = stopPlace.centroid.location.longitude

        val localityGid = stopPlace.topographicPlaceRef?.ref
        val locality = topoPlaces[localityGid]?.descriptor?.name?.text
        val countyGid = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.parentTopographicPlaceRef?.ref
        val county = topoPlaces[countyGid]?.descriptor?.name?.text
        val country = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.countryRef?.ref
        val transportModes =
            categories.getOrDefault(stopPlace.id, emptyList()).plus(stopPlace.stopPlaceType).filterNotNull()

        val importance = 0.2 +
                (transportModes.size * 0.1).coerceAtMost(0.4) +
                (if (transportModes.contains("railStation")) 0.2 else 0.0)

        val tariffZoneCategories = stopPlace.tariffZones?.tariffZoneRef
            ?.mapNotNull { it.ref?.split(":")?.first()?.let { ref -> "tariff_zone_id.${ref}" } }
            ?.toSet()
            ?: emptySet()

        val categories = listOf("osm.public_transport.stop_place")
            .plus(tariffZoneCategories)
            .plus(country?.let { "country.${it}" })
            .plus(countyGid?.let { "county_gid.${it}" })
            .plus(localityGid?.let { "locality_gid.${it}" })
            .plus("layer.stopplace")
            .plus("source.nsr")

        val stopPlaceContent =
            PlaceContent(
                place_id = abs(stopPlace.id.hashCode().toLong()),
                object_type = "N",
                object_id = abs(stopPlace.id.hashCode().toLong()),
                categories = categories.filterNotNull(),
                rank_address = 30,
                importance = importance,
                parent_place_id = 0,
                name = stopPlace.name.text?.let { Name(it) },
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
                        source = "nsr",
                        accuracy = "point",
                        country_a = Country.getThreeLetterCode(country),
                        county_gid = "$countyGid",
                        locality = locality,
                        locality_gid = localityGid,
                        label = listOfNotNull(stopPlace.name.text, locality).joinToString(","),
                        transport_modes = transportModes.joinToString(","),
                        tariff_zones = (
                                stopPlace.tariffZones
                                    ?.tariffZoneRef
                                    ?.mapNotNull { it.ref }
                                    ?.joinToString(",")
                                ),
                    ),
            )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    fun convertGroupOfStopPlacesToNominatim(
        groupOfStopPlaces: GroupOfStopPlaces,
        topoPlaces: Map<String, TopographicPlace>,
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

        val categories = listOf("osm.public_transport.group_of_stop_places")
            .plus("GroupOfStopPlaces")
            .plus(country?.let { "country.${it}" })
            .plus(countyGid?.let { "county_gid.${it}" })
            .plus(localityGid?.let { "locality_gid.${it}" })
            .plus("layer.groupofstopplaces")
            .plus("source.nsr")
            .filterNotNull()

        val placeContent = PlaceContent(
            place_id = abs(groupOfStopPlaces.id.hashCode().toLong()),
            object_type = "N",
            object_id = abs(groupOfStopPlaces.id.hashCode().toLong()),
            categories = categories,
            rank_address = 30,
            importance = 0.7,
            parent_place_id = 0,
            name = groupName?.let { Name(it) },
            address = Address(
                county = county,
            ),
            postcode = null,
            country_code = (country ?: "no"),
            centroid = listOf(lon, lat),
            bbox = listOf(lat, lon, lat, lon),
            extra = Extra(
                id = groupOfStopPlaces.id,
                source = "nsr",
                accuracy = "point",
                country_a = Country.getThreeLetterCode(country),
                county_gid = countyGid?.let { "whosonfirst:county:$it" },
                locality = locality,
                locality_gid = localityGid?.let { "whosonfirst:locality:$it" },
                label = groupName,
            ),
        )

        return NominatimPlace("Place", listOf(placeContent))
    }

    fun convertNetexParseResult(result: NetexParser.ParseResult): Sequence<NominatimPlace> {
        val stopPlaceEntries = result.stopPlaces.flatMap {
            convertStopPlaceToNominatim(
                it,
                result.topoPlaces,
                result.categories,
            ).asSequence()
        }

        val groupOfStopPlacesEntries = result.groupOfStopPlaces.map {
            convertGroupOfStopPlacesToNominatim(
                it,
                result.topoPlaces,
            )
        }

        return stopPlaceEntries + groupOfStopPlacesEntries
    }
}
