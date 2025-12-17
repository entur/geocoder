package no.entur.geocoder.converter.target

import kotlin.math.abs

// Prefix nominatim place IDs to avoid document collisions
enum class NominatimId(val prefix: Int) {
    address(100),
    street(200),
    stedsnavn(300),
    stopplace(400),
    gosp(450),
    osm(500),
    poi(600),
    ;

    fun create(id: Long): Long = create("" + id)

    fun create(id: String): Long {
        val num = abs(id.toLongOrNull() ?: id.hashCode().toLong())
        return "${prefix}$num".toLong()
    }
}
