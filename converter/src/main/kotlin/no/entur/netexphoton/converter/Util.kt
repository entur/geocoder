package no.entur.netexphoton.converter

import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

object Util {
    fun String.titleize(): String =
        split(' ').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }

    fun Double.toBigDecimalWithScale(scale: Int = 6): BigDecimal = BigDecimal(this).setScale(scale, HALF_UP)
}
