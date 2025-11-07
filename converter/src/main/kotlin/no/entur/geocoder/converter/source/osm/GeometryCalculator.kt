package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Util.toBigDecimalWithScale
import java.math.BigDecimal

/** Calculates centroids for OSM ways and relations */
object GeometryCalculator {
    fun calculateCentroid(coords: List<Pair<Double, Double>>): Pair<BigDecimal, BigDecimal>? {
        if (coords.isEmpty()) return null
        val lon = coords.map { it.first }.average().toBigDecimalWithScale()
        val lat = coords.map { it.second }.average().toBigDecimalWithScale()
        return lon to lat
    }
}

