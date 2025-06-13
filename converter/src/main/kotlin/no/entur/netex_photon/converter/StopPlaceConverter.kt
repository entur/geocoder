package no.entur.netex_photon.converter

import no.entur.netex_photon.converter.ConverterUtils.mapOfNotNull
import no.entur.netex_photon.converter.NominatimPlace.PlaceContent
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Paths
import kotlin.collections.get
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
        val lat = stopPlace.centroid?.location?.latitude?.setScale(6, RoundingMode.HALF_UP) ?: BigDecimal.ZERO
        val lon = stopPlace.centroid?.location?.longitude?.setScale(6, RoundingMode.HALF_UP) ?: BigDecimal.ZERO

        val localityGid = stopPlace.topographicPlaceRef?.ref
        val locality = topoPlaces[localityGid]?.descriptor?.name?.text
        val countyGid = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.parentTopographicPlaceRef?.ref
        val county = topoPlaces[countyGid]?.descriptor?.name?.text
        val country = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.countryRef?.ref
        val transportModes =
            categories.getOrDefault(stopPlace.id, emptyList()).plus(stopPlace.stopPlaceType).filterNotNull()

        val stopPlaceContent = PlaceContent(
            place_id = abs(stopPlace.id.hashCode().toLong()),
            object_type = "N",
            object_id = abs(stopPlace.id.hashCode().toLong()),
            categories = emptyList(),
            rank_address = 30,
            importance = 0.00001,
            parent_place_id = 0,
            name = stopPlace.name.text?.let { mapOf("name" to it) },
            address = mapOfNotNull(
                "county" to county,
            ),
            postcode = "unknown",
            country_code = (country ?: "no"),
            centroid = listOf(lon.toDouble(), lat.toDouble()),
            bbox = listOf(lat.toDouble(), lon.toDouble(), lat.toDouble(), lon.toDouble()),
            extratags = mapOf(
                "id" to stopPlace.id,
                "layer" to "stopplace",
                "source" to "nsr",
                "source_id" to stopPlace.id,
                "accuracy" to "point",
                "country_a" to Country.getThreeLetterCode(country),
                "county_gid" to "$countyGid",
                "locality" to (locality ?: "unknown"),
                "locality_gid" to "$localityGid",
                "label" to listOfNotNull(stopPlace.name.text, locality).joinToString(","),
                "transport_modes" to transportModes.joinToString(","),
                "tariff_zones" to (stopPlace.tariffZones?.tariffZoneRef?.mapNotNull { it.ref }?.joinToString(",")
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
