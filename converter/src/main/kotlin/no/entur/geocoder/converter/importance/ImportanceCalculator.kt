package no.entur.geocoder.converter.importance

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

object ImportanceCalculator {
    private const val MIN_POPULARITY = 20.0
    private const val MAX_POPULARITY = 1_000_000_000.0
    private const val IMPORTANCE_FLOOR = 0.1

    /**
     * Normalize popularity scores to Photon importance (0-1 range).
     * Uses logarithmic normalization to handle wide-ranging values.
     */
    fun calculateImportance(
        popularity: Number,
        minPopularity: Double = MIN_POPULARITY,
        maxPopularity: Double = MAX_POPULARITY,
        floor: Double = IMPORTANCE_FLOOR
    ): Double {
        val pop = popularity.toDouble()

        // Apply log10 transformation
        val logPop = log10(pop)
        val logMin = log10(minPopularity)
        val logMax = log10(maxPopularity)

        // Normalize to 0-1 range
        val normalized = (logPop - logMin) / (logMax - logMin)

        // Apply floor and clip to valid range
        val scaled = floor + (normalized * (1.0 - floor))

        return max(floor, min(1.0, scaled))
    }
}
