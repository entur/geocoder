package no.entur.geocoder.converter.source.poi

import no.entur.geocoder.common.Category.OSM_CUSTOM_POI
import no.entur.geocoder.common.Coordinate
import no.entur.geocoder.common.Country
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.Source.CUSTOM_POI
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.source.PlaceId
import no.entur.geocoder.converter.source.stopplace.NetexParser
import no.entur.geocoder.converter.target.NominatimPlace
import no.entur.geocoder.converter.target.NominatimPlace.*
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime

class PoiConverter : Converter {
    override fun convert(
        input: File,
        output: File,
        isAppending: Boolean,
    ) {
        val entries = extractTopographicPlaces(input)
        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(entries.asSequence(), outputPath, isAppending)
    }

    private fun extractTopographicPlaces(input: File): List<NominatimPlace> {
        val topoPlaces = NetexParser().extractTopoPlaces(input)
        val now = LocalDateTime.of(2025, 11, 14, 0, 0, 0)
        return topoPlaces.values
            .filter { topoPlace ->
                val validFrom = topoPlace.validBetween?.fromDate
                val validTo = topoPlace.validBetween?.toDate
                when {
                    validFrom != null && validTo != null -> !now.isBefore(validFrom) && !now.isAfter(validTo)
                    validFrom != null -> !now.isBefore(validFrom)
                    validTo != null -> !now.isAfter(validTo)
                    else -> true // If no valid period, include by default
                }
            }.map { topoPlace ->
                val id = topoPlace.id ?: ""
                val name = topoPlace.descriptor?.name?.text ?: ""
                val coord =
                    Coordinate(
                        topoPlace.centroid?.location?.longitude ?: 0.0,
                        topoPlace.centroid?.location?.latitude ?: 0.0,
                    )
                val country = Geo.getCountry(coord) ?: Country.no
                val extra =
                    Extra(
                        id = id,
                        source = CUSTOM_POI,
                        tags = OSM_CUSTOM_POI,
                        country_a = country.threeLetterCode,
                    )
                val placeId = PlaceId.poi.create(id)
                val placeContent =
                    PlaceContent(
                        place_id = placeId,
                        object_type = "N",
                        object_id = placeId,
                        categories = listOf(OSM_CUSTOM_POI, "country.${country.name}"),
                        rank_address = 30,
                        importance = 0.5,
                        name = Name(name = name),
                        address = Address(),
                        postcode = null,
                        country_code = country.name,
                        centroid = coord.centroid(),
                        bbox = coord.bbox(),
                        extra = extra,
                    )
                NominatimPlace("Place", listOf(placeContent))
            }
    }
}
