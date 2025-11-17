package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate

class CoordinateStore(initialCapacity: Int = 500_000) {
    companion object {
        private val baseLat = -90.0
        private val baseLon = -180.0
        private val scale = 1e5 // ~1.1m precision
        private val loadFactor = 0.7
    }

    private var ids = LongArray(initialCapacity)
    private var deltaLons = IntArray(initialCapacity)
    private var deltaLats = IntArray(initialCapacity)
    private var size = 0

    fun put(id: Long, coord: Coordinate) {
        if (size >= ids.size * loadFactor) {
            resize()
        }

        var index = hash(id)
        while (ids[index] != 0L && ids[index] != id) {
            index = (index + 1) % ids.size
        }

        if (ids[index] == 0L) size++
        ids[index] = id
        deltaLons[index] = ((coord.lon - baseLon) * scale).toInt()
        deltaLats[index] = ((coord.lat - baseLat) * scale).toInt()
    }

    fun get(id: Long): Coordinate? {
        var index = hash(id)
        while (ids[index] != 0L) {
            if (ids[index] == id) {
                val lat = baseLat + deltaLats[index] / scale
                val lon = baseLon + deltaLons[index] / scale
                return Coordinate(lat, lon)
            }
            index = (index + 1) % ids.size
        }
        return null
    }

    private fun hash(id: Long): Int = Math.floorMod(id * 2654435761L, ids.size)

    private fun resize() {
        val oldIds = ids
        val oldDeltaLons = deltaLons
        val oldDeltaLats = deltaLats
        val oldSize = ids.size

        ids = LongArray(oldSize * 2)
        deltaLons = IntArray(oldSize * 2)
        deltaLats = IntArray(oldSize * 2)
        size = 0

        for (i in 0 until oldSize) {
            if (oldIds[i] != 0L) {
                val lon = baseLon + oldDeltaLons[i] / scale
                val lat = baseLat + oldDeltaLats[i] / scale
                put(oldIds[i], Coordinate(lat, lon))
            }
        }
    }
}
