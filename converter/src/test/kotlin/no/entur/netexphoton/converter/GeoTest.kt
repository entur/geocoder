package no.entur.netexphoton.converter

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class GeoTest {
    @Test
    fun `convert UTM33 to LatLon`() {
        val eastingValue = 310012.98
        val northingValue = 6770754.69

        val (latitude, longitude) = Geo.convertUTM33ToLatLon(eastingValue, northingValue)

        assertEquals(BigDecimal("61.025715"), latitude)
        assertEquals(BigDecimal("11.483291"), longitude)
    }
}
