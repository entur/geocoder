package no.entur.netexphoton.converter.netex

import no.entur.netexphoton.common.Extra
import no.entur.netexphoton.converter.Converter
import no.entur.netexphoton.converter.JsonWriter
import no.entur.netexphoton.converter.NominatimPlace
import no.entur.netexphoton.converter.NominatimPlace.*
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
            .plus(country?.let { "country:${it}" })
            .plus(countyGid?.let { "county_gid:${it}" })
            .plus(localityGid?.let { "locality_gid:${it}" })
            .plus("layer:stopplace")
            .plus("source:nsr")

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
                postcode = "unknown",
                country_code = (country ?: "no"),
                centroid = listOf(lon, lat),
                bbox = listOf(lat, lon, lat, lon),
                extra =
                    Extra(
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
                        tariff_zones = (
                                stopPlace.tariffZones
                                    ?.tariffZoneRef
                                    ?.mapNotNull { it.ref }
                                    ?.joinToString(",")
                                    ?: "unknown"
                                ),
                    ),
            )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    fun convertNetexParseResult(result: NetexParser.ParseResult): Sequence<NominatimPlace> =
        result.stopPlaces.flatMap {
            convertStopPlaceToNominatim(
                it,
                result.topoPlaces,
                result.categories,
            ).asSequence()
        }
}
