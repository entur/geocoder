package no.entur.geocoder.proxy.common

import no.entur.geocoder.proxy.common.Util.toBigDecimalWithScale

data class Coordinate(val lat: Double, val lon: Double) {
    val bigLat by lazy { lat.toBigDecimalWithScale() }
    val bigLon by lazy { lon.toBigDecimalWithScale() }

    fun bbox() = listOf(bigLon, bigLat, bigLon, bigLat)

    companion object {
        fun coordOrNull(lat: Double?, lon: Double?): Coordinate? =
            if (lat != null && lon != null) {
                Coordinate(lat, lon)
            } else {
                null
            }
    }
}
