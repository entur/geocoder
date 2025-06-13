package no.entur.netex_photon.converter

import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateConverterTest {

    @Test
    fun `convert UTM33 to LatLon`() {
        val eastingValue = 310012.98
        val northingValue = 6770754.69

        val (latitude, longitude) = CoordinateConverter.convertUTM33ToLatLon(eastingValue, northingValue)

        assertEquals(61.025715, latitude, 0.000001)
        assertEquals(11.483291, longitude, 0.000001)
    }
}