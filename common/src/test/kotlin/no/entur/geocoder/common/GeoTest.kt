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

    @Test
    fun `radiusToZoom with small radius`() {
        val zoom = Geo.radiusToZoom(100.0)
        assertEquals(9, zoom)
    }

    @Test
    fun `radiusToZoom with medium radius`() {
        val zoom = Geo.radiusToZoom(1000.0)
        assertEquals(6, zoom)
    }

    @Test
    fun `radiusToZoom with large radius`() {
        val zoom = Geo.radiusToZoom(10000.0)
        assertEquals(2, zoom)
    }

    @Test
    fun `radiusToZoom with very small radius`() {
        val zoom = Geo.radiusToZoom(10.0)
        assertEquals(12, zoom)
    }

    @Test
    fun `radiusToZoom with 1 kilometer radius`() {
        val zoom = Geo.radiusToZoom(1.0)
        assertEquals(16, zoom)
    }

    @Test
    fun `radiusToZoom with very large radius clamps to minimum`() {
        val zoom = Geo.radiusToZoom(100000.0)
        assertEquals(0, zoom)
    }

    @Test
    fun `radiusToZoom with very small radius clamps to maximum`() {
        val zoom = Geo.radiusToZoom(0.1)
        assertEquals(18, zoom)
    }

    @Test
    fun `radiusToZoom edge case at zoom 18`() {
        val zoom = Geo.radiusToZoom(0.25)
        assertEquals(18, zoom)
    }

    @Test
    fun `radiusToZoom edge case at zoom 0`() {
        val zoom = Geo.radiusToZoom(65536.0)
        assertEquals(0, zoom)
    }

    @Test
    fun `radiusToZoom inverse relationship verification`() {
        for (zoom in 0..18) {
            val radius = (1 shl (18 - zoom)) * 0.25
            val calculatedZoom = Geo.radiusToZoom(radius)
            assertEquals(zoom, calculatedZoom, "Zoom level $zoom should be produced by radius $radius")
        }
    }

    @Test
    fun `radiusToZoom boundary values produce expected zoom levels`() {
        assertEquals(0, Geo.radiusToZoom(65536.0))
        assertEquals(10, Geo.radiusToZoom(64.0))
        assertEquals(18, Geo.radiusToZoom(0.25))
    }
}
