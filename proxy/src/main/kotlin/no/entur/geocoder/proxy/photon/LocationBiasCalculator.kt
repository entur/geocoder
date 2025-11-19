package no.entur.geocoder.proxy.photon

import kotlin.math.max
import kotlin.math.pow

object LocationBiasCalculator {
    private const val PELIAS_MAX_WEIGHT: Double = 50.0
    private const val CURVE_EXPONENT: Double = 0.185

    /**
     * Maps Pelias focus.weight to Photon location_bias_scale with inverted semantics.
     * Higher Pelias weight = more location bias = lower Photon scale.
     * Preserves: weight=0 → scale=1.0, weight=15 → scale=0.2, weight=50 → scale=0.0.
     */
    fun calculateLocationBias(peliasWeight: Double): Double {
        val weight = max(0.0, peliasWeight)
        val normalized = weight / PELIAS_MAX_WEIGHT
        val curved = normalized.pow(CURVE_EXPONENT)
        return max(0.0, 1.0 - curved)
    }
}
