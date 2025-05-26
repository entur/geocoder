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
                "city" to stopPlace.topographicPlaceRef?.ref,
                "street" to stopPlace.keyList?.keyValue?.find { it.key == "street" }?.value
            ),
            postcode = stopPlace.keyList?.keyValue?.find { it.key == "postcode" }?.value,
            country_code = "no", // or derive from context
            centroid = listOf(lon, lat),
            bbox = listOf(lon, lat, lon, lat), // approximate
            parent_place_id = null,
            housenumber = stopPlace.keyList?.keyValue?.find { it.key == "housenumber" }?.value
        )
        entries.add(NominatimEntry("Place", listOf(stopPlaceEntry)))

        return entries
    }

    fun mapOfNotNull(vararg pairs: Pair<String, String?>): Map<String, String> =
        pairs.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    fun convertAll(stopPlaces: Sequence<StopPlace>): Sequence<NominatimEntry> =
        stopPlaces.flatMap { convertStopPlaceToPlaceEntry(it).asSequence() }

}
