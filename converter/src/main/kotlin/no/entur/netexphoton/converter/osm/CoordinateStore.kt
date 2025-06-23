package no.entur.netexphoton.converter.osm

// Memory-efficient coordinate storage
class CoordinateStore(initialCapacity: Int = 500000) {
    private var ids = LongArray(initialCapacity)
    private var lons = FloatArray(initialCapacity) // Float saves 50% memory vs Double
    private var lats = FloatArray(initialCapacity)
    private var size = 0
    private val loadFactor = 0.7

    fun put(id: Long, lon: Double, lat: Double) {
        if (size >= ids.size * loadFactor) {
            resize()
        }

        var index = hash(id)
        while (ids[index] != 0L && ids[index] != id) {
            index = (index + 1) % ids.size
        }

        if (ids[index] == 0L) size++
        ids[index] = id
        lons[index] = lon.toFloat()
        lats[index] = lat.toFloat()
    }

    fun get(id: Long): Pair<Double, Double>? {
        var index = hash(id)
        while (ids[index] != 0L) {
            if (ids[index] == id) {
                return Pair(lons[index].toDouble(), lats[index].toDouble())
            }
            index = (index + 1) % ids.size
        }
        return null
    }

    private fun hash(id: Long): Int =
        ((id * 2654435761L) % ids.size).toInt().let {
            if (it < 0) it + ids.size else it
        }

    private fun resize() {
        val oldIds = ids
        val oldLons = lons
        val oldLats = lats
        val oldSize = ids.size

        ids = LongArray(oldSize * 2)
        lons = FloatArray(oldSize * 2)
        lats = FloatArray(oldSize * 2)
        size = 0

        for (i in 0 until oldSize) {
            if (oldIds[i] != 0L) {
                put(oldIds[i], oldLons[i].toDouble(), oldLats[i].toDouble())
            }
        }
    }

    fun memoryUsage(): String {
        val bytesPerEntry = 8 + 4 + 4 // Long + Float + Float
        val usedBytes = size * bytesPerEntry
        return "${usedBytes / 1024 / 1024}MB"
    }
}
