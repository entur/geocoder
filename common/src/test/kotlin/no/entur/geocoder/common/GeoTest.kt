package no.entur.geocoder.common

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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

    @ParameterizedTest
    @MethodSource("borderCountryCoordinates")
    fun `getCountryCode returns correct country for border coordinates`(testCase: Triple<Double, Double, Country>) {
        val (lat, lon, expectedCode) = testCase
        val actualCode = Geo.getCountry(lat, lon)
        assertEquals(expectedCode, actualCode, "Expected $expectedCode for ($lat, $lon), got $actualCode")
    }

    companion object {
        @JvmStatic
        fun borderCountryCoordinates() =
            listOf(
                Triple(59.91386, 10.75224, Country.no),
                // Norway–Sweden
                Triple(59.47787, 11.71967, Country.no),
                Triple(59.49251, 11.78009, Country.se),
                // Sweden–Finland
                Triple(65.8481, 24.1466, Country.fi),
                Triple(65.8350, 24.1300, Country.se),
                // Denmark–Sweden near Øresund Bridge
                Triple(55.6210, 12.6500, Country.dk),
                Triple(55.5700, 12.9800, Country.se),
                // Denmark–Germany near Kruså/Padborg
                Triple(54.8205, 9.3980, Country.de),
                Triple(54.8675, 9.4175, Country.dk),
                // Oscar Torp-heimen (resolves wrongly to Sweden)
                // Triple(59.09735, 11.25770, Country.no),
            )
    }
}
