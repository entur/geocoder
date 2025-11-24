package no.entur.geocoder.converter.source

import no.entur.geocoder.converter.ConverterConfig.ImportanceConfig
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

class ImportanceCalculator(private val config: ImportanceConfig) {
    /**
     * Normalize popularity scores to Photon importance (0-1 range).
     * Uses logarithmic normalization to handle wide-ranging values.
     */
    fun calculateImportance(
        popularity: Number,
        minPopularity: Double = config.minPopularity,
        maxPopularity: Double = config.maxPopularity,
        floor: Double = config.floor,
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
