package no.entur.netex_photon.converter

import no.entur.netex_photon.common.domain.Extra
import no.entur.netex_photon.converter.NominatimPlace.Address
import no.entur.netex_photon.converter.NominatimPlace.Name
import no.entur.netex_photon.converter.NominatimPlace.PlaceContent
import java.io.File
import java.nio.file.Paths
import kotlin.math.abs

class StopPlaceConverter {

    fun convert(input: File, output: File, isAppending: Boolean = false) {
        val parser = NetexParser()
        val result: NetexParser.ParseResult = parser.parseXml(input)
        val entries: Sequence<NominatimPlace> = convertNetexParseResult(result)

        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(entries, outputPath, isAppending)
    }

    fun convertStopPlaceToNominatim(
        stopPlace: StopPlace,
        topoPlaces: Map<String, TopographicPlace>,
        categories: Map<String, List<String>>
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

        val importance = 0.2 + (transportModes.size * 0.1).coerceAtMost(0.5)

        val stopPlaceContent = PlaceContent(
            place_id = abs(stopPlace.id.hashCode().toLong()),
            object_type = "N",
            object_id = abs(stopPlace.id.hashCode().toLong()),
            categories = emptyList(),
            rank_address = 30,
            importance = importance,
            parent_place_id = 0,
            name = stopPlace.name.text?.let { Name(it) },
            address = Address(
                county = county,
            ),
            postcode = "unknown",
            country_code = (country ?: "no"),
            centroid = listOf(lon, lat),
            bbox = listOf(lat, lon, lat, lon),
            extratags = Extra(
                id = stopPlace.id,
                layer = "stopplace",
                source = "nsr",
                source_id = stopPlace.id,
                accuracy = "point",
                country_a = Country.getThreeLetterCode(country),
                county_gid = "$countyGid",
                locality = (locality ?: "unknown"),
                locality_gid = "$localityGid",
                label = listOfNotNull(stopPlace.name.text, locality).joinToString(","),
                transport_modes = transportModes.joinToString(","),
                tariff_zones = (stopPlace.tariffZones?.tariffZoneRef?.mapNotNull { it.ref }?.joinToString(",")
                    ?: "unknown"),
            )
        )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    fun convertNetexParseResult(result: NetexParser.ParseResult): Sequence<NominatimPlace> =
        result.stopPlaces.flatMap {
            convertStopPlaceToNominatim(
                it,
                result.topoPlaces,
                result.categories
            ).asSequence()
        }
}
