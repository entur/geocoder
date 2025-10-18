package no.entur.geocoder.converter.osm

object Poi {
    /**
     * Check if a specific OSM tag (key/value pair) matches configured POI filters.
     *
     * Replaces the old whitelist approach that only checked keys.
     * Now requires exact key/value matches from OSMPopularityCalculator filter list.
     *
     * @param key OSM tag key
     * @param value OSM tag value
     * @return true if this tag matches a configured filter
     */
    fun isWantedTag(key: String, value: String): Boolean {
        return OSMPopularityCalculator.hasFilter(key, value)
    }
}