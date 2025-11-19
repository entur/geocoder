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

    const val peliasDefault: Double = 15.0
    const val photonDefault: Double = 0.2
    const val peliasMax: Double = 40.0
    const val curveStrength: Double = 0.8


    /**
     * Calculate location bias scale for Photon (default 0.2) based on Pelias weight (default 15).
     */
    fun locationBiasCalculator(peliasWeight: Double): Double {
        val exponent = 1.0 - (curveStrength * 0.5)
        val normalizedDefault = (peliasDefault / peliasMax).pow(exponent)
        val scaleFactor = photonDefault / normalizedDefault
        val normalizedInput = (peliasWeight / peliasMax).pow(exponent)
        return min(1.0, normalizedInput * scaleFactor)
    }
}
