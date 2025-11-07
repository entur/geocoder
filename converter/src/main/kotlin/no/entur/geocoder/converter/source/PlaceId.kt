package no.entur.geocoder.converter.source

import kotlin.math.abs

// Prefix nominatim place IDs to avoid document collisions
enum class PlaceId(val prefix: Int) {
    address(1000),
    street(2000),
    stedsnavn(3000),
    stopplace(4000),
    osm(5000),
    ;

    fun create(id: Long): Long = create("" + id)

    fun create(id: String): Long {
        val num = abs(id.toLongOrNull() ?: id.hashCode().toLong())
        return "${prefix}$num".toLong()
    }
}
