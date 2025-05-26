package no.entur.netex_to_json

class Converter {

    fun convertStopPlaceToPlaceEntry(stopPlace: StopPlace): List<NominatimEntry> {
        val entries = mutableListOf<NominatimEntry>()
        val lat = stopPlace.centroid?.location?.latitude ?: 0.0
        val lon = stopPlace.centroid?.location?.longitude ?: 0.0

        val stopPlaceEntry = PlaceEntry(
            place_id = stopPlace.id.hashCode().toLong(), // or use a UUID
            object_type = "N",
            object_id = stopPlace.id.hashCode().toLong(),
            categories = listOf("osm.stop_place"),
            rank_address = 30,
            importance = 0.00001,
            name = stopPlace.name?.text?.let { mapOf("name" to it) },
            address = mapOfNotNull(
                "street" to "NOT_AN_ADDRESS-${stopPlace.id}",
                "county" to "Finnmark", // TODO: Hvor kommer dette fra?
                "locality" to "Alta", // TODO: Hvor kommer dette fra?
            ),
            postcode = stopPlace.keyList?.keyValue?.find { it.key == "postcode" }?.value,
            country_code = "no", // or derive from context
            centroid = listOf(lon, lat),
            parent_place_id = null,
            extratags = mapOf(
                "id" to stopPlace.id,
                "gid" to "openstreetmap:venue:${stopPlace.id}",
                "layer" to "venue",
                "source" to "openstreetmap",
                "source_id" to stopPlace.id,
                "accuracy" to "point",
                "country_a" to stopPlace.id.substringBefore(":"),
                "county_gid" to "whosonfirst:county:KVE:TopographicPlace:32", // TODO: Hvor kommer dette fra?
                "locality_gid" to "whosonfirst:locality:${stopPlace.topographicPlaceRef?.ref}",
                "category" to listOf("onstreetBus", "airport"),
                "tariff_zones" to (stopPlace.tariffZones?.tariffZoneRef?.map { it.ref } ?: emptyList()),
            )
        )
        entries.add(NominatimEntry("Place", listOf(stopPlaceEntry)))

        return entries
    }

    fun mapOfNotNull(vararg pairs: Pair<String, String?>): Map<String, String> =
        pairs.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    fun convertAll(stopPlaces: Sequence<StopPlace>): Sequence<NominatimEntry> =
        stopPlaces.flatMap { convertStopPlaceToPlaceEntry(it).asSequence() }

}
