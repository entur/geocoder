package no.entur.geocoder.common

import no.entur.geocoder.common.Util.toBigDecimalWithScale

data class Coordinate(val lat: Double, val lon: Double) {
    val bigLat by lazy { lat.toBigDecimalWithScale() }
    val bigLon by lazy { lon.toBigDecimalWithScale() }

    fun centroid() = listOf(bigLon, bigLat)

    fun bbox() = listOf(bigLon, bigLat, bigLon, bigLat)

    companion object {
        val ZERO = Coordinate(0.0, 0.0)

        fun coordOrNull(lat: Double?, lon: Double?): Coordinate? =
            if (lat != null && lon != null) {
                Coordinate(lat, lon)
            } else {
                null
            }
    }
}
