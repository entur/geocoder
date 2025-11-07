package no.entur.geocoder.converter.source.stedsnavn

/**
 * Place types from Kartverket's place name registry (stedsnavn).
 *
 * Defines the 5 target settlement types for geocoding, matching kakka's place type whitelist.
 * These represent urban settlements and built-up areas suitable for geocoding results.
 *
 * GML field: `app:navneobjekttype`
 *
 * Production configuration (matching kakka):
 * `geocoderPlaceTypeWhitelist: tettsteddel,bydel,by,tettsted,tettbebyggelse`
 *
 * All target place types receive the same popularity value (40) in kakka, with no
 * differentiation by settlement size.
 *
 * Reference: kakka helm/kakka/env/values-kub-ent-*.yaml (pelias.geocoderPlaceTypeWhitelist)
 */
enum class StedsnavnPlaceType {
    /**
     * By (City/Town) - Largest settlement type
     */
    by,

    /**
     * Bydel (City district) - District within a city
     */
    bydel,

    /**
     * Tettsted (Urban settlement) - Concentrated built-up area
     */
    tettsted,

    /**
     * Tettsteddel (Small town part) - Part of a small town/settlement
     */
    tettsteddel,

    /**
     * Tettbebyggelse (Built-up area) - Generic built-up area
     */
    tettbebyggelse,

    ;

    companion object {
        /**
         * Check if a place type is a target type for geocoding.
         *
         * @param type The place type from GML (case-insensitive)
         * @return true if the type is a target type, false otherwise
         */
        fun isTarget(type: String?): Boolean = entries.any { it.name == type?.lowercase() }

        /**
         * Get the enum value for a place type.
         *
         * @param type The place type from GML (case-insensitive)
         * @return The enum value, or null if not a target type
         */
        fun fromString(type: String?): StedsnavnPlaceType? = entries.find { it.name == type?.lowercase() }

        /**
         * Get all target type names as a set (for filtering).
         *
         * @return Set of target type names (lowercase)
         */
        fun targetTypes(): Set<String> = entries.map { it.name }.toSet()
    }
}
