package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate

/** Calculates centroids for OSM ways and relations */
object GeometryCalculator {
    fun calculateCentroid(coords: List<Coordinate>): Coordinate? {
        if (coords.isEmpty()) return null
        val lat = coords.map { it.lat }.average()
        val lon = coords.map { it.lon }.average()
        return Coordinate(lat, lon)
    }
}
