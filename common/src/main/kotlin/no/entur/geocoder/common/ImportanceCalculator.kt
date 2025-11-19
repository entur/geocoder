package no.entur.geocoder.common

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ImportanceCalculator {
    private const val MIN_POPULARITY = 1.0
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
        floor: Double = IMPORTANCE_FLOOR,
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

    private const val PELIAS_MAX_WEIGHT: Double = 50.0
    private const val CURVE_EXPONENT: Double = 0.185

    /**
     * Maps Pelias focus.weight to Photon location_bias_scale with inverted semantics.
     * Higher Pelias weight = more location bias = lower Photon scale.
     * Preserves: weight=0 → scale=1.0, weight=15 → scale=0.2, weight=50 → scale=0.0.
     */
    fun locationBiasCalculator(peliasWeight: Double): Double {
        val weight = max(0.0, peliasWeight)
        val normalized = weight / PELIAS_MAX_WEIGHT
        val curved = normalized.pow(CURVE_EXPONENT)
        return max(0.0, 1.0 - curved)
    }
}
