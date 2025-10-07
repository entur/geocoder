package no.entur.geocoder.converter

import no.entur.geocoder.converter.Util.titleize
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilTest {
    @Test
    fun `titleize should correctly format strings`() {
        assertEquals("Hello World", "HELLO WORLD".titleize())
    }
}
