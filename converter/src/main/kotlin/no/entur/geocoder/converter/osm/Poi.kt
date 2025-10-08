package no.entur.geocoder.converter.osm

object Poi {

    private val poiKeys =
        setOf(
            "amenity", "shop", "tourism", "leisure", "historic", "office", "craft",
            "public_transport", "railway", "station", "aeroway", "natural", "waterway",
        )

    fun isWantedKey(key: String) = poiKeys.contains(key)
}