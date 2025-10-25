package no.entur.geocoder.converter.stedsnavn

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
enum class StedsnavnSpellingStatus(val code: String, val description: String) {
    /**
     * Vedtatt (Approved) - Officially approved place name
     */
    VEDTATT("vedtatt", "Approved"),

    /**
     * Godkjent (Approved) - Approved place name
     */
    GODKJENT("godkjent", "Approved"),

    /**
     * Privat (Private) - Private place name (accepted)
     */
    PRIVAT("privat", "Private"),

    /**
     * Samlevedtak (Collective decision) - Approved by collective decision
     */
    SAMLEVEDTAK("samlevedtak", "Collective decision");

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
        fun isAccepted(status: String?): Boolean {
            if (status.isNullOrEmpty()) {
                return false
            }

            val normalizedStatus = status.trim().lowercase()
            return entries.any { it.code == normalizedStatus }
        }

        /**
         * Get the enum value for a spelling status code.
         *
         * @param status The spelling status code from GML (case-insensitive)
         * @return The enum value, or null if not accepted
         */
        fun fromString(status: String?): StedsnavnSpellingStatus? {
            if (status.isNullOrEmpty()) {
                return null
            }

            val normalizedStatus = status.trim().lowercase()
            return entries.find { it.code == normalizedStatus }
        }
    }
}