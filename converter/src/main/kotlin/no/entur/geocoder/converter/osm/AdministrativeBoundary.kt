package no.entur.geocoder.converter.osm

/**
 * Represents an administrative boundary (county or municipality) with polygon geometry.
 *
 * @property id The OSM relation ID
 * @property name The name of the administrative area
 * @property adminLevel The administrative level (4 for counties, 7 for municipalities in Norway)
 * @property refCode The reference code (e.g., "46" for Vestland county, "4626" for Øygarden municipality)
 * @property countryCode The ISO 3166-1 alpha-2 country code (e.g., "NO" for Norway)
 * @property centroid The geographic center point as (latitude, longitude)
 * @property bbox The bounding box containing the entire administrative area
 * @property boundaryNodes The polygon vertices as (latitude, longitude) pairs for ray-casting containment tests
 */
data class AdministrativeBoundary(
    val id: Long,
    val name: String,
    val adminLevel: Int,
    val refCode: String?,
    val countryCode: String,
    val centroid: Pair<Double, Double>, // lat, lon
    val bbox: BoundingBox?,
    val boundaryNodes: List<Pair<Double, Double>> = emptyList()
) {
    /**
     * Checks if the given point is within the bounding box.
     * This is a fast preliminary check before doing more expensive polygon containment tests.
     */
    fun isInBoundingBox(lat: Double, lon: Double): Boolean = bbox?.contains(lat, lon) ?: false

    /**
     * Calculates the Euclidean distance from the given point to this boundary's centroid.
     * Used as a fallback when polygon containment tests are inconclusive.
     */
    fun distanceToPoint(lat: Double, lon: Double): Double {
        val dLat = lat - centroid.first
        val dLon = lon - centroid.second
        return kotlin.math.sqrt(dLat * dLat + dLon * dLon)
    }

    /**
     * Ray-casting algorithm to check if a point is inside the polygon.
     *
     * This implementation uses the standard ray-casting algorithm: it casts a ray from the point
     * to infinity and counts how many times it crosses the polygon boundary. If the count is odd,
     * the point is inside; if even, it's outside.
     *
     * Note: This is a simplified implementation that works for most cases but may fail for
     * complex multi-polygons with holes or self-intersections.
     */
    fun containsPoint(lat: Double, lon: Double): Boolean {
        if (boundaryNodes.size < 3) return false

        var inside = false
        var j = boundaryNodes.size - 1

        for (i in boundaryNodes.indices) {
            val (lat1, lon1) = boundaryNodes[i]
            val (lat2, lon2) = boundaryNodes[j]

            if ((lon1 > lon) != (lon2 > lon) &&
                lat < (lat2 - lat1) * (lon - lon1) / (lon2 - lon1) + lat1) {
                inside = !inside
            }
            j = i
        }

        return inside
    }
}

/**
 * Represents a geographic bounding box defined by minimum and maximum latitude/longitude.
 *
 * @property minLat The minimum latitude (southern boundary)
 * @property maxLat The maximum latitude (northern boundary)
 * @property minLon The minimum longitude (western boundary)
 * @property maxLon The maximum longitude (eastern boundary)
 */
data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    /** Checks if the given point is within this bounding box. */
    fun contains(lat: Double, lon: Double): Boolean {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon
    }

    /** Calculate the area of the bounding box (used to prioritize smaller, more specific boundaries). */
    fun area(): Double = (maxLat - minLat) * (maxLon - minLon)

    companion object {
        /**
         * Creates a bounding box from a list of coordinates.
         * Returns null if the coordinate list is empty.
         */
        fun fromCoordinates(coords: List<Pair<Double, Double>>): BoundingBox? {
            if (coords.isEmpty()) return null
            return BoundingBox(
                minLat = coords.minOf { it.first },
                maxLat = coords.maxOf { it.first },
                minLon = coords.minOf { it.second },
                maxLon = coords.maxOf { it.second }
            )
        }
    }
}

/**
 * Index for efficiently looking up administrative boundaries by geographic coordinates.
 *
 * This class maintains separate lists of counties and municipalities and uses a cache
 * to improve performance of repeated lookups at the same coordinates.
 */
class AdministrativeBoundaryIndex {
    private val counties = mutableListOf<AdministrativeBoundary>()
    private val municipalities = mutableListOf<AdministrativeBoundary>()

    // Cache for lookups to improve performance - store both county and municipality together
    private val lookupCache = mutableMapOf<Pair<Double, Double>, Pair<AdministrativeBoundary?, AdministrativeBoundary?>>()

    companion object {
        /** Norwegian county administrative level in OSM */
        const val ADMIN_LEVEL_COUNTY = 4

        /** Norwegian municipality administrative level in OSM */
        const val ADMIN_LEVEL_MUNICIPALITY = 7

        /** Coordinate rounding precision for cache keys (0.01 degrees ≈ 1.1 km) */
        private const val CACHE_PRECISION = 100.0
    }

    fun addBoundary(boundary: AdministrativeBoundary) {
        when (boundary.adminLevel) {
            ADMIN_LEVEL_COUNTY -> counties.add(boundary)
            ADMIN_LEVEL_MUNICIPALITY -> municipalities.add(boundary)
        }
    }

    /**
     * Finds both the county and municipality for the given coordinates in a single lookup.
     * Results are cached to improve performance for nearby lookups.
     */
    fun findCountyAndMunicipality(lat: Double, lon: Double): Pair<AdministrativeBoundary?, AdministrativeBoundary?> {
        val key = roundCoordinate(lat) to roundCoordinate(lon)

        return lookupCache.getOrPut(key) {
            val county = findBestMatch(counties, lat, lon)
            val municipality = findBestMatch(municipalities, lat, lon)
            county to municipality
        }
    }

    fun findCounty(lat: Double, lon: Double): AdministrativeBoundary? =
        findCountyAndMunicipality(lat, lon).first

    fun findMunicipality(lat: Double, lon: Double): AdministrativeBoundary? =
        findCountyAndMunicipality(lat, lon).second

    /**
     * Finds the best matching administrative boundary for the given coordinates.
     *
     * Uses a three-tier approach:
     * 1. Ray-casting for true polygon containment (most accurate)
     * 2. Bounding box + closest centroid (fast fallback)
     * 3. Closest centroid distance (last resort)
     */
    private fun findBestMatch(
        boundaries: List<AdministrativeBoundary>,
        lat: Double,
        lon: Double
    ): AdministrativeBoundary? {
        // First, try ray-casting for boundaries that have polygon data
        val candidatesContainingPoint = boundaries.filter { it.containsPoint(lat, lon) }

        if (candidatesContainingPoint.isNotEmpty()) {
            // If multiple boundaries contain the point (shouldn't happen, but just in case),
            // choose the one with the smallest bounding box (most specific)
            return candidatesContainingPoint.minByOrNull {
                it.bbox?.area() ?: Double.MAX_VALUE
            }
        }

        // Fallback: find boundaries whose bounding box contains the point
        val candidatesInBbox = boundaries.filter { it.isInBoundingBox(lat, lon) }

        if (candidatesInBbox.isNotEmpty()) {
            // Choose the one with the closest centroid
            return candidatesInBbox.minByOrNull { it.distanceToPoint(lat, lon) }
        }

        // Last resort: find the closest boundary by centroid distance
        return boundaries.minByOrNull { it.distanceToPoint(lat, lon) }
    }

    private fun roundCoordinate(coord: Double): Double =
        (coord * CACHE_PRECISION).toInt() / CACHE_PRECISION

    fun getStatistics(): String =
        "Loaded ${counties.size} counties and ${municipalities.size} municipalities"
}

