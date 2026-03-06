package no.entur.geocoder.common

import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

object Util {
    fun String.titleize(): String =
        split(' ').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }

    fun Double.toBigDecimalWithScale(scale: Int = 6): BigDecimal =
        BigDecimal(this).setScale(scale, HALF_UP)

    fun Double?.within(d: Double, d2: Double): Boolean =
        this != null && this >= d && this <= d2
}
