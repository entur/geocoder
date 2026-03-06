package no.entur.geocoder.common

import no.entur.geocoder.common.Util.titleize
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilTest {
    @Test
    fun `titleize should correctly format strings`() {
        assertEquals("Hello World", "HELLO WORLD".titleize())
    }

    @Test
    fun `should format decimals properly`() {
        val num: Double = 12.23456789
        assertEquals(12.23.toBigDecimal(), num.toBigDecimalWithScale(2))
    }
}
