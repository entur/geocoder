package no.entur.geocoder.converter.netex

import no.entur.geocoder.converter.importance.ImportanceCalculator

/**
 * Calculates importance for stop places based on legacy kakka boost configuration.
 *
 * Configuration reference (pelias.stop.place.boost.config):
 * {"defaultValue":30, "stopTypeFactors":{"busStation":{"*":2},"metroStation":{"*":2},"railStation":{"*":2}}}
 * See: /Users/testower/Developer/github/entur/kakka/helm/kakka/env/values-kub-ent-*.yaml
 *
 * Formula: popularity = defaultValue * (SUM of stopTypeFactors) * interchangeFactor
 * For multimodal parents: factors from all child stop types are summed.
 * Then: importance = log10-normalized popularity
 */
object StopPlaceImportanceCalculator {
    private const val DEFAULT_VALUE = 30

    private val STOP_TYPE_FACTORS = mapOf(
        "busStation" to 2.0,
        "metroStation" to 2.0,
        "railStation" to 2.0
    )

    // Interchange weighting factors
    private val INTERCHANGE_FACTORS = mapOf(
        "recommendedInterchange" to 3.0,
        "preferredInterchange" to 10.0
    )

    /**
     * Calculate importance for a regular stop place based on type and weighting.
     *
     * @param stopPlace The stop place to calculate importance for
     * @return Importance value between 0.1 and 1.0
     */
    fun calculateImportance(stopPlace: StopPlace): Double {
        val popularity = calculatePopularity(stopPlace, emptyList())
        return ImportanceCalculator.calculateImportance(popularity)
    }

    /**
     * Calculate importance for a stop place with child types (multimodal parent).
     * Parent stops never have their own stopPlaceType - importance is derived from children.
     *
     * @param stopPlace The stop place to calculate importance for
     * @param childTypes List of stop types from child stops (for multimodal parents)
     * @return Importance value between 0.1 and 1.0
     */
    fun calculateImportance(stopPlace: StopPlace, childTypes: List<String>): Double {
        val popularity = calculatePopularity(stopPlace, childTypes)
        return ImportanceCalculator.calculateImportance(popularity)
    }

    /**
     * Calculate raw popularity value (boost) for a stop place.
     *
     * This mirrors the logic in kakka's StopPlaceBoostConfiguration.
     * The formula aggregates all stop types (parent's own type + children) and sums their factors.
     */
    private fun calculatePopularity(stopPlace: StopPlace, childTypes: List<String>): Long {
        var popularity = DEFAULT_VALUE.toLong()

        // Collect all stop types: parent's own type (if any) plus children's types
        val allStopTypes = mutableListOf<String>()
        stopPlace.stopPlaceType?.let { allStopTypes.add(it) }
        allStopTypes.addAll(childTypes)

        // Sum all stop type factors (kakka uses sum, not multiplication)
        val sumOfFactors = allStopTypes.sumOf { getStopTypeFactor(it) }

        if (sumOfFactors > 0) {
            popularity = (popularity * sumOfFactors).toLong()
        }

        // Apply interchange weighting factor
        val interchangeFactor = getInterchangeFactor(stopPlace.weighting)
        if (interchangeFactor != null) {
            popularity = (popularity * interchangeFactor).toLong()
        }

        return popularity
    }

    /**
     * Get the boost factor for a specific stop type.
     * Returns the configured factor or 1.0 if not configured.
     */
    private fun getStopTypeFactor(stopPlaceType: String?): Double {
        if (stopPlaceType == null) {
            return 1.0
        }
        return STOP_TYPE_FACTORS[stopPlaceType] ?: 1.0
    }

    /**
     * Get the boost factor for interchange weighting.
     * Returns null if no interchange boost applies.
     */
    private fun getInterchangeFactor(weighting: String?): Double? {
        if (weighting == null) {
            return null
        }
        return INTERCHANGE_FACTORS[weighting]
    }
}
