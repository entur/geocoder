package no.entur.geocoder.converter.source.stopplace

import no.entur.geocoder.converter.ConverterConfig.StopPlaceConfig

/**
 * Calculates popularity (boost) values for stop places based on configurable boost configuration.
 *
 * Configuration reference (pelias.stop.place.boost.config):
 * {"defaultValue":50, "stopTypeFactors":{"busStation":2.0,"metroStation":2.0,"railStation":2.0}, "interchangeFactors":{...}}
 *
 * Formula: popularity = defaultValue * (SUM of stopTypeFactors) * interchangeFactor
 * For multimodal parents: factors from all child stop types are summed.
 */
class StopPlacePopularityCalculator(private val config: StopPlaceConfig) {
    internal val defaultValue = config.defaultValue

    /**
     * Calculate raw popularity value (boost) for a stop place.
     *
     * This mirrors the logic in kakka's StopPlaceBoostConfiguration.
     * The formula aggregates all stop types (parent's own type + children) and sums their factors.
     *
     * @param stopPlace The stop place to calculate popularity for
     * @param childTypes List of stop types from child stops (for multimodal parents)
     * @return Raw popularity value (not normalized)
     */
    fun calculatePopularity(stopPlace: StopPlace, childTypes: List<String> = emptyList()): Long {
        var popularity = config.defaultValue.toLong()

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
        return config.stopTypeFactors[stopPlaceType] ?: 1.0
    }

    /**
     * Get the boost factor for interchange weighting.
     * Returns null if no interchange boost applies.
     */
    private fun getInterchangeFactor(weighting: String?): Double? {
        if (weighting == null) {
            return null
        }
        return config.interchangeFactors[weighting]
    }
}
