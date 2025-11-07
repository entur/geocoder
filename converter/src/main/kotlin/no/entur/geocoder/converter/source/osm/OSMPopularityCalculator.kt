package no.entur.geocoder.converter.source.osm

/**
 * Calculates popularity (boost) values for OSM POIs based on configured filters with priority values.
 *
 * Configuration based on osmpoi.json with 56 filter entries.
 * Formula: popularity = DEFAULT_VALUE × priority (when multiple match, use highest)
 *
 */
object OSMPopularityCalculator {
    private const val DEFAULT_VALUE = 1.0 // Base popularity matching kakka's pelias.poi.boost

    /**
     * Represents a single POI filter with key, value, and priority.
     * Priority values range from 1 (lowest) to 9 (highest).
     */
    private data class POIFilter(
        val key: String,
        val value: String,
        val priority: Int,
    )

    /**
     * All 56 filter configurations from osmpoi.json.
     * Ordered by priority (high to low) for readability.
     */
    private val filters =
        listOf(
            // Priority 9 - Highest importance (education, healthcare, retail)
            POIFilter("amenity", "hospital", 9),
            POIFilter("amenity", "school", 9),
            POIFilter("amenity", "university", 9),
            POIFilter("shop", "mall", 9),
            // Priority 8 - Cultural, recreation, education
            POIFilter("amenity", "college", 8),
            POIFilter("amenity", "place_of_worship", 8),
            POIFilter("amenity", "exhibition_centre", 8),
            POIFilter("leisure", "park", 8),
            POIFilter("leisure", "sports_centre", 8),
            POIFilter("leisure", "stadium", 8),
            POIFilter("tourism", "museum", 8),
            POIFilter("landuse", "winter_sports", 8),
            // Priority 7 - Social services, recreation
            POIFilter("amenity", "library", 7),
            POIFilter("amenity", "kindergarten", 7),
            POIFilter("amenity", "nursing_home", 7),
            POIFilter("amenity", "food_court", 7),
            POIFilter("leisure", "ice_rink", 7),
            // Priority 6 - Hospitality, specific landmarks
            POIFilter("tourism", "hotel", 6),
            POIFilter("amenity", "cafe", 6),
            POIFilter("amenity", "restaurant", 6),
            POIFilter("amenity", "social_facility", 6),
            POIFilter("natural", "beach", 6),
            POIFilter("name", "Barcode", 6),
            // Priority 5 - Tourism, events, sports
            POIFilter("tourism", "motel", 5),
            POIFilter("tourism", "hostel", 5),
            POIFilter("tourism", "alpine_hut", 5),
            POIFilter("tourism", "camp_site", 5),
            POIFilter("tourism", "gallery", 5),
            POIFilter("tourism", "theme_park", 5),
            POIFilter("amenity", "community_centre", 5),
            POIFilter("amenity", "dentist", 5),
            POIFilter("amenity", "concert_hall", 5),
            POIFilter("amenity", "events_venue", 5),
            POIFilter("amenity", "conference_centre", 5),
            POIFilter("landuse", "cemetery", 5),
            POIFilter("office", "government", 5),
            POIFilter("name", "Munch brygge", 5),
            POIFilter("sport", "karting", 5),
            POIFilter("sport", "motor", 5),
            // Priority 4 - Healthcare, tourism
            POIFilter("amenity", "embassy", 4),
            POIFilter("amenity", "clinic", 4),
            POIFilter("amenity", "golf_course", 4),
            POIFilter("tourism", "guest_house", 4),
            // Priority 3 - Specific facilities
            POIFilter("amenity", "prison", 3),
            POIFilter("shop", "furniture", 3),
            // Priority 2 - Local services
            POIFilter("amenity", "doctors", 2),
            POIFilter("amenity", "police", 2),
            // Priority 1 - General attractions and services
            POIFilter("amenity", "cinema", 1),
            POIFilter("amenity", "theatre", 1),
            POIFilter("amenity", "bank", 1),
            POIFilter("amenity", "courthouse", 1),
            POIFilter("amenity", "townhall", 1),
            POIFilter("tourism", "attraction", 1),
            POIFilter("name", "Entur AS", 1),
            POIFilter("custom_poi", "festival", 1), // Non-standard OSM tag
        )

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
        return DEFAULT_VALUE * highestPriority
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
