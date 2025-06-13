package no.entur.netex_photon.converter

import no.entur.netex_photon.converter.ConverterUtils.titleize
import kotlin.test.Test
import kotlin.test.assertEquals

class ConverterUtilsTest {

    @Test
    fun `titleize should correctly format strings`() {
        assertEquals("Hello World", "HELLO WORLD".titleize())
    }

}