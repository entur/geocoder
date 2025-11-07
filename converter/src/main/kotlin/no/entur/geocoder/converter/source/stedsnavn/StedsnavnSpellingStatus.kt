package no.entur.geocoder.converter.source.stedsnavn

/**
 * Spelling status codes from Kartverket's place name registry (stedsnavn).
 *
 * Implements quality control filtering matching kakka's KartverketFeatureSpellingStatusCode.java.
 * Only place names with approved/active spelling status should be included in geocoding results.
 *
 * GML field: `app:skrivemåtestatus`
 *
 * Reference: kakka/src/main/java/no/entur/kakka/geocoder/geojson/KartverketFeatureSpellingStatusCode.java
 */
enum class StedsnavnSpellingStatus {
    /**
     * Vedtatt (Approved) - Officially approved place name
     */
    vedtatt,

    /**
     * Godkjent (Approved) - Approved place name
     */
    godkjent,

    /**
     * Privat (Private) - Private place name (accepted)
     */
    privat,

    /**
     * Samlevedtak (Collective decision) - Approved by collective decision
     */
    samlevedtak,

    ;

    companion object {
        /**
         * Check if a spelling status code is accepted (active/approved).
         *
         * Accepted statuses: vedtatt, godkjent, privat, samlevedtak
         *
         * Rejected statuses (not included in enum):
         * - uvurdert (unevaluated)
         * - avslått (rejected)
         * - foreslått (proposed)
         * - klage (complaint)
         * - internasjonal (international)
         * - historisk (historical)
         *
         * @param status The spelling status code from GML (case-insensitive)
         * @return true if the status is accepted, false otherwise
         */
        fun isAccepted(status: String?): Boolean = entries.any { it.name == status?.lowercase() }

        /**
         * Get the enum value for a spelling status code.
         *
         * @param status The spelling status code from GML (case-insensitive)
         * @return The enum value, or null if not accepted
         */
        fun fromString(status: String?): StedsnavnSpellingStatus? = entries.find { it.name == status?.lowercase() }
    }
}
