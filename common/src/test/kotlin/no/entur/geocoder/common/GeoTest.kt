package no.entur.geocoder.common

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

    @Test
    fun `haversine distance calculation`() {
        val lat1 = 60.39126
        val lon1 = 5.32205
        val lat2 = 59.91386
        val lon2 = 10.75224

        val distance = Geo.haversineDistance(lat1, lon1, lat2, lon2)

        assertEquals(305072.3952385879, distance) // Distance in meters
    }
}
