package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import org.openstreetmap.osmosis.core.domain.v0_6.Way
import java.io.File
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Represents a street segment (a single line between two coordinates).
 */
data class StreetSegment(
    val name: String,
    val start: Coordinate,
    val end: Coordinate,
    val bbox: BoundingBox,
) {
    /**
     * Calculates the minimum distance from a point to this line segment.
     * Uses perpendicular distance when possible, otherwise distance to nearest endpoint.
     */
    fun distanceToPoint(point: Coordinate): Double {
        // Convert to approximate meters for accurate distance calculation
        // At 60° latitude, 1° longitude ≈ 55.8 km, 1° latitude ≈ 111 km
        val latScale = 111000.0
        val lonScale = 111000.0 * cos(Math.toRadians(point.lat))

        val px = (point.lon - start.lon) * lonScale
        val py = (point.lat - start.lat) * latScale
        val dx = (end.lon - start.lon) * lonScale
        val dy = (end.lat - start.lat) * latScale

        val segmentLengthSquared = dx * dx + dy * dy

        if (segmentLengthSquared == 0.0) {
            // Start and end are the same point
            return sqrt(px * px + py * py)
        }

        // Calculate projection parameter t, clamped to [0, 1]
        val t = maxOf(0.0, minOf(1.0, (px * dx + py * dy) / segmentLengthSquared))

        // Find the closest point on the segment
        val closestX = t * dx
        val closestY = t * dy

        // Return distance from point to closest point on segment
        val distX = px - closestX
        val distY = py - closestY
        return sqrt(distX * distX + distY * distY)
    }
}

/**
 * Index for finding the nearest street to a given coordinate.
 *
 * Uses a grid-based spatial index for efficient lookups. Streets are stored
 * as line segments, and the nearest segment's street name is returned.
 */
class StreetIndex {
    private val segments = mutableListOf<StreetSegment>()

    // Grid-based spatial index for faster lookups
    // Key: grid cell (latCell, lonCell), Value: list of segment indices
    private val spatialIndex = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()

    // Cache for lookups to improve performance
    private val lookupCache = mutableMapOf<Pair<Int, Int>, String?>()

    companion object {
        /** Grid cell size in degrees (approximately 500m at 60° latitude) */
        private const val GRID_SIZE = 0.005

        /** Maximum search radius in grid cells */
        private const val MAX_SEARCH_RADIUS = 10

        /** Maximum distance in meters to consider a street match */
        private const val MAX_DISTANCE_METERS = 100.0

        /** Cache precision (0.001 degrees ≈ 100m) */
        private const val CACHE_PRECISION = 1000.0

        /** Highway types to include (roads that typically have addresses) */
        val HIGHWAY_TYPES =
            setOf(
                "motorway", "trunk", "primary", "secondary", "tertiary",
                "unclassified", "residential", "living_street", "pedestrian",
                "service", "road",
            )

        /** Filter for street ways (highway with name) */
        val STREET_FILTER: (Entity) -> Boolean = { entity ->
            if (entity is Way) {
                val tags = entity.tags.associate { it.key to it.value }
                val highway = tags["highway"]
                tags.containsKey("name") && highway != null && highway in HIGHWAY_TYPES
            } else {
                false
            }
        }
    }

    /**
     * Adds a street way to the index.
     *
     * @param name The street name
     * @param coordinates The list of coordinates forming the street's geometry
     */
    fun addStreet(name: String, coordinates: List<Coordinate>) {
        if (coordinates.size < 2) return

        for (i in 0 until coordinates.size - 1) {
            val start = coordinates[i]
            val end = coordinates[i + 1]
            val bbox =
                BoundingBox(
                    minLat = minOf(start.lat, end.lat),
                    maxLat = maxOf(start.lat, end.lat),
                    minLon = minOf(start.lon, end.lon),
                    maxLon = maxOf(start.lon, end.lon),
                )

            val segment = StreetSegment(name, start, end, bbox)
            val segmentIndex = segments.size
            segments.add(segment)

            // Add to spatial index - add to all grid cells the segment touches
            val minLatCell = (bbox.minLat / GRID_SIZE).toInt()
            val maxLatCell = (bbox.maxLat / GRID_SIZE).toInt()
            val minLonCell = (bbox.minLon / GRID_SIZE).toInt()
            val maxLonCell = (bbox.maxLon / GRID_SIZE).toInt()

            for (latCell in minLatCell..maxLatCell) {
                for (lonCell in minLonCell..maxLonCell) {
                    val cell = latCell to lonCell
                    spatialIndex.getOrPut(cell) { mutableListOf() }.add(segmentIndex)
                }
            }
        }
    }

    /**
     * Finds the nearest street name to the given coordinate.
     *
     * @param coord The coordinate to search from
     * @return The name of the nearest street, or null if no street is within range
     */
    fun findNearestStreet(coord: Coordinate): String? {
        val cacheKey = roundCoordinate(coord.lat) to roundCoordinate(coord.lon)

        return lookupCache.getOrPut(cacheKey) {
            findNearestStreetUncached(coord)
        }
    }

    private fun findNearestStreetUncached(coord: Coordinate): String? {
        val latCell = (coord.lat / GRID_SIZE).toInt()
        val lonCell = (coord.lon / GRID_SIZE).toInt()

        var nearestSegment: StreetSegment? = null
        var nearestDistance = Double.MAX_VALUE

        // Search in expanding rings around the target cell
        for (radius in 0..MAX_SEARCH_RADIUS) {
            var foundInRing = false

            for (dLat in -radius..radius) {
                for (dLon in -radius..radius) {
                    // Only check cells on the ring boundary (optimization)
                    if (radius > 0 && kotlin.math.abs(dLat) != radius && kotlin.math.abs(dLon) != radius) {
                        continue
                    }

                    val cell = (latCell + dLat) to (lonCell + dLon)
                    val segmentIndices = spatialIndex[cell] ?: continue

                    for (index in segmentIndices) {
                        val segment = segments[index]
                        val distance = segment.distanceToPoint(coord)

                        if (distance < nearestDistance) {
                            nearestDistance = distance
                            nearestSegment = segment
                            foundInRing = true
                        }
                    }
                }
            }

            // If we found something and the next ring can't possibly be closer, stop
            if (foundInRing && nearestDistance < (radius + 1) * GRID_SIZE * 111000) {
                break
            }
        }

        return if (nearestDistance <= MAX_DISTANCE_METERS) {
            nearestSegment?.name
        } else {
            null
        }
    }

    private fun roundCoordinate(value: Double): Int =
        (value * CACHE_PRECISION).toInt()

    fun getStatistics(): String =
        "Loaded ${segments.size} street segments in ${spatialIndex.size} grid cells"
}
