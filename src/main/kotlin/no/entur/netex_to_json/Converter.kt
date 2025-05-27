package no.entur.netex_to_json

import kotlin.math.abs

class Converter {

    fun convertStopPlaceToPlaceEntry(stopPlace: StopPlace, topoPlaces: MutableMap<String, TopographicPlace>): List<NominatimPlace> {
        val entries = mutableListOf<NominatimPlace>()
        val lat = stopPlace.centroid?.location?.latitude ?: 0.0
        val lon = stopPlace.centroid?.location?.longitude ?: 0.0

        val localityGid = stopPlace.topographicPlaceRef?.ref
        val locality: String = topoPlaces[localityGid]?.descriptor?.name?.text ?: "Unknown Locality"
        val countyGid = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.parentTopographicPlaceRef?.ref
        val county: String = topoPlaces[countyGid]?.descriptor?.name?.text ?: "Unknown County"
        val country: String = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.countryRef?.ref ?: "no"

        val stopPlaceContent = PlaceContent(
            place_id = abs(stopPlace.id.hashCode().toLong()), // or use a UUID
            object_type = "N",
            object_id = abs(stopPlace.id.hashCode().toLong()),
            categories = listOf("osm.stop_place"),
            rank_address = 30,
            importance = 0.00001,
            parent_place_id = 0,
            name = stopPlace.name?.text?.let { mapOf("name" to it) },
            address = mapOfNotNull(
                "street" to "NOT_AN_ADDRESS-${stopPlace.id}",
                "county" to county, // "Finnmark",
            ),
            postcode = stopPlace.keyList?.keyValue?.find { it.key == "postcode" }?.value ?: "",
            country_code = country, // "no",
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
                "county_gid" to "whosonfirst:county:" + countyGid, // KVE:TopographicPlace:32
                "locality" to locality, // "Alta",
                "locality_gid" to "whosonfirst:locality:" + localityGid ,
                "label" to "${stopPlace.name?.text}, $locality",
//                "category" to listOf("onstreetBus", "airport"),
//                "tariff_zones" to (stopPlace.tariffZones?.tariffZoneRef?.map { it.ref } ?: emptyList()),
            )
        )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    fun mapOfNotNull(vararg pairs: Pair<String, String?>): Map<String, String> =
        pairs.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    fun convertAll(stopPlaces: Sequence<StopPlace>, topoPlaces: MutableMap<String, TopographicPlace>): Sequence<NominatimPlace> =
        stopPlaces.flatMap { convertStopPlaceToPlaceEntry(it, topoPlaces).asSequence() }

}
