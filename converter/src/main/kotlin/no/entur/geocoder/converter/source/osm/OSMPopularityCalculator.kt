package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.converter.ConverterConfig.OsmConfig

/**
 * Calculates popularity (boost) values for OSM POIs based on configured filters with priority values.
 *
 * Configuration can be provided via ConverterConfig or uses default values.
 * Formula: popularity = DEFAULT_VALUE × priority (when multiple match, use highest)
 *
 */
class OSMPopularityCalculator(private val config: OsmConfig) {
    /**
     * Represents a single POI filter with key, value, and priority.
     * Priority values range from 1 (lowest) to 9 (highest).
     */
    private data class POIFilter(
        val key: String,
        val value: String,
        val priority: Int,
    )

    private val filters: List<POIFilter> =
        config.filters.map { POIFilter(it.key, it.value, it.priority) }

    /**
     * Calculate raw popularity value for OSM tags.
     *
     * @param tags Map of OSM tags (key -> value)
     * @return Raw popularity value (not normalized). Returns 0.0 if no filters match.
     */
    fun calculatePopularity(tags: Map<String, String>): Double {
        // Find all matching filters
        val matchedPriorities =
            filters
                .filter { filter ->
                    tags[filter.key] == filter.value
                }.map { it.priority }

        if (matchedPriorities.isEmpty()) {
            return 0.0 // No match = not a wanted POI
        }

        // Use highest priority when multiple filters match
        val highestPriority = matchedPriorities.maxOrNull() ?: 1
        return config.defaultValue * highestPriority
    }

    /**
     * Check if a specific key/value pair matches any configured filter.
     * Used for filtering during OSM parsing.
     *
     * @param key OSM tag key
     * @param value OSM tag value
     * @return true if this key/value pair is in the filter list
     */
    fun hasFilter(key: String, value: String): Boolean = filters.any { it.key == key && it.value == value }

    /**
     * Get all unique keys that have filters configured.
     * Useful for quick pre-filtering.
     *
     * @return Set of all filter keys
     */
    fun getFilterKeys(): Set<String> = filters.map { it.key }.toSet()
}
