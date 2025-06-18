package no.entur.netexphoton.converter

import no.entur.netexphoton.converter.Util.titleize
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilTest {
    @Test
    fun `titleize should correctly format strings`() {
        assertEquals("Hello World", "HELLO WORLD".titleize())
    }
}
