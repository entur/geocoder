package no.entur.netex_photon.converter

import java.math.BigDecimal
import java.math.RoundingMode

object ConverterUtils {
    // Utility function to create a map from pairs, excluding those with null values.
    fun mapOfNotNull(vararg pairs: Pair<String, String?>): Map<String, String> =
        pairs.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    fun String.titleize(): String =
        split(' ').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }

    fun Double.toBigDecimalWithScale(scale: Int = 6): BigDecimal =
        BigDecimal(this).setScale(scale, RoundingMode.HALF_UP)
}
