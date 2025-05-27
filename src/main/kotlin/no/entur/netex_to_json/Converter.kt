package no.entur.netex_to_json

import no.entur.netex_to_json.NominatimPlace.PlaceContent
import kotlin.math.abs

class Converter {

    fun convertStopPlaceToNominatim(
        stopPlace: StopPlace,
        topoPlaces: Map<String, TopographicPlace>,
        categories: Map<String, List<String>>
    ): List<NominatimPlace> {
        val entries = mutableListOf<NominatimPlace>()
        val lat = stopPlace.centroid?.location?.latitude ?: 0.0
        val lon = stopPlace.centroid?.location?.longitude ?: 0.0

        val localityGid = stopPlace.topographicPlaceRef?.ref
        val locality = topoPlaces[localityGid]?.descriptor?.name?.text
        val countyGid = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.parentTopographicPlaceRef?.ref
        val county = topoPlaces[countyGid]?.descriptor?.name?.text
        val country = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.countryRef?.ref
        val categoryList =
            categories.getOrDefault(stopPlace.id, emptyList()).plus(stopPlace.stopPlaceType).filterNotNull()

        val stopPlaceContent = PlaceContent(
            place_id = abs(stopPlace.id.hashCode().toLong()), // or use a UUID
            object_type = "N",
            object_id = abs(stopPlace.id.hashCode().toLong()),
            categories = emptyList(),
            rank_address = 30,
            importance = 0.00001,
            parent_place_id = 0,
            name = stopPlace.name.text?.let { mapOf("name" to it) },
            address = mapOfNotNull(
                "street" to "NOT_AN_ADDRESS-${stopPlace.id}",
                "county" to county, // "Finnmark",
            ),
            postcode = "unknown",
            country_code = (country ?: "no"), // "no",
            centroid = listOf(lon, lat),
            bbox = listOf(lat, lon, lat, lon),
            extratags = mapOf(
                "id" to stopPlace.id,
                "gid" to "openstreetmap:venue:${stopPlace.id}",
                "layer" to "venue",
                "source" to "openstreetmap",
                "source_id" to stopPlace.id,
                "accuracy" to "point",
                "country_a" to Country.getThreeLetterCode(country),
                "county_gid" to "whosonfirst:county:$countyGid", // KVE:TopographicPlace:32
                "locality" to (locality ?: "unknown"), // "Alta",
                "locality_gid" to "whosonfirst:locality:$localityGid",
                "label" to listOfNotNull(stopPlace.name.text, locality).joinToString(","),
                "category" to categoryList.joinToString(","),
                "tariff_zones" to (stopPlace.tariffZones?.tariffZoneRef?.mapNotNull { it.ref }?.joinToString(",")
                    ?: "unknown"),
            )
        )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    fun mapOfNotNull(vararg pairs: Pair<String, String?>): Map<String, String> =
        pairs.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    fun convertAll(result: NetexParser.ParseResult): Sequence<NominatimPlace> =
        result.stopPlaces.flatMap {
            convertStopPlaceToNominatim(
                it,
                result.topoPlaces,
                result.categories
            ).asSequence()
        }

}
